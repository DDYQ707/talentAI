# talent-ai-agent 实现说明

> 模块路径：`talent-ai-backend/talent-ai-agent`  
> 端口：**8084**  
> 数据库：**talent_ai_db**  
> 定位：AI 智能体 — 简历解析、人岗匹配、通义千问（Qwen-Max）调用

相关文档：

- [talent-gateway 实现说明](./talent-gateway实现说明.md)
- [talent-job 实现说明](./talent-job实现说明.md)
- [talent-resume 实现说明](./talent-resume实现说明.md)
- [本地部署与接入指南](../智能招聘与人才画像分析系统%20本地部署指南.md)（含 DashScope Key 配置）

---

## 一、它解决什么问题？

业务上需要：**把 MinIO 里的简历 PDF/Word 变成结构化 JSON**，再 **和岗位 JD 比对打出匹配分**，供 HR 初筛参考。

| 职责 | 说明 |
|------|------|
| **简历解析** | Tika 抽文本 → Qwen-Max 结构化 → 存 `ai_resume_parse_result` |
| **人岗匹配** | 简历 JSON + 岗位 JD → Qwen-Max 打分与优劣势 → 存 `ai_match_record` |
| **异步任务** | `@Async` 处理，不阻塞投递主流程 |
| **结果回写** | Feign 把 `match_score` 同步到 `talent-job` 的投递单 |
| **LLM 底座** | LangChain4j + 阿里云 DashScope 兼容 OpenAI API |

它**不负责**：

- 存简历文件（talent-resume + MinIO）
- 创建投递单（talent-job）
- 用户登录（talent-auth）

```
投递成功 (talent-job)
       │ Feign POST /internal/ai/parse/submit
       ▼
┌─────────────────────────────────────────────────────────┐
│                  talent-ai-agent :8084                   │
│  1. Feign resume → attachment MinIO 信息                  │
│  2. MinIO 下载 → Tika 抽文本                             │
│  3. Qwen-Max 解析 JSON → ai_resume_parse_result         │
│  4. Qwen-Max 人岗匹配 → ai_match_record                  │
│  5. Feign job → sync-match-score                         │
└─────────────────────────────────────────────────────────┘
       ▲                           │
       │ Feign job/resume          │ DashScope HTTPS
       └───────────────────────────┘
```

---

## 二、网关与路由

| 路径 | 网关路由 | 鉴权 |
|------|----------|------|
| `/api/ai/**` | `lb://talent-ai-agent` | 需 JWT（HR 查结果、健康检查） |
| `/internal/ai/**` | 同上 | **白名单免 Token**（job Feign 触发解析） |

内部接口走 gateway 时也在白名单；Feign 通常 **直连 Nacos 8084**。

---

## 三、模块目录结构

```
talent-ai-agent/
├── pom.xml                    # langchain4j-open-ai、tika、minio
└── src/main/java/com/talent/agent/
    ├── AiAgentApplication.java  # @EnableAsync @EnableFeignClients
    ├── config/
    │   ├── DashScopeProperties.java
    │   ├── DashScopeStartupValidator.java
    │   ├── MinioProperties / MinioConfig
    │   ├── FeignConfig.java           # 透传 X-User-Id（若需要）
    │   └── AgentExceptionHandler.java
    ├── controller/
    │   ├── AiController.java          # /api/ai  对外查询
    │   ├── AiTestChatController.java  # /api/ai/test/chat  Sprint1 验证
    │   └── internal/AiInternalController.java  # /internal/ai
    ├── client/                        # Feign 出站
    │   ├── ResumeFeignClient.java
    │   └── JobFeignClient.java
    ├── service/
    │   ├── AiParseServiceImpl         # 创建解析任务
    │   ├── AiParseTaskProcessor       # 异步解析 ★
    │   ├── AiMatchServiceImpl
    │   ├── AiMatchTaskProcessor       # 异步匹配 ★
    │   ├── ResumeTextExtractService   # Apache Tika
    │   ├── ResumeLlmParseServiceImpl  # LLM 解析 Prompt
    │   ├── ResumeJobMatchServiceImpl  # LLM 匹配 Prompt
    │   ├── LlmChatServiceImpl         # 统一调 Qwen
    │   ├── MinioDownloadService
    │   └── JobBriefQueryServiceImpl
    ├── domain/entity|dto|vo
    └── mapper/
```

---

## 四、依赖与技术栈

| 依赖 | 用途 |
|------|------|
| `langchain4j-open-ai` | 以 OpenAI 兼容方式调 DashScope |
| `apache-tika` | PDF/Word → 纯文本 |
| `io.minio` | 从 `talent-resumes` 桶下载附件 |
| `mybatis-plus` | AI 任务/结果表 |
| `openfeign` | resume、job |
| `spring @EnableAsync` | 解析/匹配后台执行 |

---

## 五、环境变量与配置（必看）

```yaml
# application.yml
ai:
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}   # 必须，勿写进 Git
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    model: qwen-max
    timeout-seconds: 60

minio:
  endpoint: http://127.0.0.1:9000
  bucket-name: talent-resumes       # 与 talent-resume 一致
```

启动时 `DashScopeStartupValidator` 会打日志（**只显示 Key 前缀**）。未配置 Key 时服务能起，但 LLM 调用会失败。

详细步骤见 [本地部署与接入指南](../智能招聘与人才画像分析系统%20本地部署指南.md) 第九节。

---

## 六、数据库：talent_ai_db

脚本：`docs/sql/talent_ai_agent_schema.sql`（已含 Sprint1 字段扩展）

### 当前代码使用的表

| 表 | 作用 |
|----|------|
| `ai_model` | 模型配置（默认 seed `qwen-max`） |
| `ai_parse_task` | 解析任务队列与状态 |
| `ai_resume_parse_result` | 结构化解析 JSON |
| `ai_match_record` | 人岗匹配结果 |

### 任务/匹配状态（parse & match 通用）

| 值 | 含义 |
|----|------|
| 0 | 待处理 |
| 1 | 处理中 |
| 2 | 成功 |
| 3 | 失败 |

### 库中已有、代码尚未实现的表（规划）

`ai_chat_session`、`ai_chat_message`、`ai_talent_profile`、`ai_interview_question`、`ai_audit_log` — 见全量 DDL，后续 Sprint 使用。

---

## 七、API 一览

### 7.1 对外 — AiController `/api/ai`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 服务存活 |
| GET | `/parse/latest?resumeId=` | HR 查最新解析任务 |
| GET | `/match/by-application?applicationId=` | HR 查某投递的匹配结果 |
| GET | `/match/latest?resumeId=&jobId=` | 按简历+岗位查最新匹配 |

### 7.2 测试 — AiTestChatController `/api/ai/test`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/chat` | Sprint1 LLM 连通性测试 `{ "prompt": "..." }` |

### 7.3 内部 — AiInternalController `/internal/ai`

| 方法 | 路径 | 调用方 | 说明 |
|------|------|--------|------|
| POST | `/parse/submit` | talent-job | **投递后触发解析** |
| GET | `/parse/task?taskId=` | 调试 | 任务详情 |
| GET | `/parse/latest-by-resume` | 调试 | 同对外 |
| POST | `/match/submit` | 解析完成后自动 / 手动 | 触发匹配 |
| GET | `/match/by-application` | 调试 | 同对外 |

### 7.4 前端

| 文件 | API |
|------|-----|
| `api/ai.ts` | parse/latest、match/by-application、match/latest |
| `views/hr/ResumeDetailView.vue` | 展示匹配分、雷达图等 |

---

## 八、Feign 协作

### 出站

```java
@FeignClient(name = "talent-resume")
// internal/attachment/{id} — 拿 bucket/objectKey
// internal/ownership、b brief/{resumeId}

@FeignClient(name = "talent-job")
// internal/application/by-id — 解析后查 jobId
// internal/post/brief — 匹配时拉 JD
// internal/application/sync-match-score — 回写分数
```

### 入站

| 来源 | 接口 |
|------|------|
| talent-job | `POST /internal/ai/parse/submit` |

---

## 九、核心流程 1：简历解析（AiParseTaskProcessor）

```
POST /internal/ai/parse/submit
  Body: attachmentId, resumeId, applicationId, candidateId, fileName, fileType
    │
    ├─ INSERT ai_parse_task (status=0)
    └─ @Async processAsync(taskId, request)
           │
           ├─ Feign resume → 附件 bucket/objectKey
           ├─ MinioDownloadService.download
           ├─ ResumeTextExtractService (Tika) → 纯文本
           ├─ ResumeLlmParseService → LlmChatService (Qwen)
           │     System Prompt：只输出 JSON，字段见 ParsedResumeDto
           ├─ AiResumeParseResultService.save → ai_resume_parse_result
           ├─ UPDATE task status=2
           └─ triggerMatchAfterParse → AiMatchService.submitMatch
```

- 文本过长会在 Prompt 层截断（约 12000 字符）  
- 任一步失败：task status=3，`error_message` 记录原因  
- **失败不回滚** talent-job 已完成的投递

---

## 十、核心流程 2：人岗匹配（AiMatchTaskProcessor）

```
submitMatch(applicationId, jobId, resumeId, parseTaskId?)
    │
    ├─ INSERT ai_match_record (score=0, status=0)
    └─ @Async processAsync(matchId, request)
           │
           ├─ 加载 ai_resume_parse_result.parsed_json
           ├─ JobBriefQueryService → Feign job/post/brief (JD)
           ├─ ResumeJobMatchService → LlmChatService (Qwen)
           │     输出：matchScore、advantages、disadvantages、dimensionScores...
           ├─ MatchResultPersistService.saveSuccess
           └─ Feign job syncApplicationMatchScore
```

匹配分 **0～100** 写入：

- `ai_match_record.match_score`
- `job_application.match_score`（经 job 内部 API）

---

## 十一、LLM 调用层（LlmChatServiceImpl）

统一入口，屏蔽 DashScope 细节：

```java
OpenAiChatModel.builder()
    .baseUrl(dashScopeProperties.getBaseUrl())
    .apiKey(dashScopeProperties.getApiKey())
    .modelName("qwen-max")
    .timeout(...)
    .build();
```

两个业务 Prompt 封装：

| 类 | 场景 |
|----|------|
| `ResumeLlmParseServiceImpl` | 简历文本 → 结构化 JSON |
| `ResumeJobMatchServiceImpl` | JD + 简历 JSON → 匹配 JSON |

均要求模型 **只返回 JSON**（无 Markdown 包裹），再用 Jackson 反序列化；失败抛 `AgentBusinessException`。

---

## 十二、与投递链的衔接（端到端）

```
1. 候选人 POST /api/delivery/submit        (talent-job)
2. job → resume ownership / on-delivery
3. job → POST /internal/ai/parse/submit    (talent-ai-agent)
4. agent 异步：MinIO + Tika + Qwen 解析
5. agent 异步：Qwen 匹配 + sync-match-score (talent-job)
6. HR GET /api/ai/match/by-application     (前端 ResumeDetail)
```

**前提**：简历有 **PDF/Word 附件**；纯在线简历无 attachmentId 时 job 会跳过 AI。

---

## 十三、启动类要点

```java
@EnableAsync                    // 解析/匹配异步
@EnableFeignClients(basePackages = "com.talent.agent.client")
@EnableConfigurationProperties(DashScopeProperties.class, MinioProperties.class)
public class AiAgentApplication
```

---

## 十四、本地调试

1. 执行 `docs/sql/talent_ai_agent_schema.sql`  
2. Docker：MinIO、MySQL、Nacos  
3. 启动 talent-resume、talent-job、**talent-ai-agent**  
4. IDEA 运行配置设置 `DASHSCOPE_API_KEY=sk-...`  
5. 连通性：`POST http://localhost:8080/api/ai/test/chat`（需网关 + 登录 Token，或直连 8084 测）  

```powershell
# 直连 agent 测 LLM（绕过网关）
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8084/api/ai/test/chat" `
  -ContentType "application/json" `
  -Body '{"prompt":"请只返回：AI服务连接成功"}'
```

6. 完整链路：候选人上传附件 → 投递 → 看 job 日志「已触发 AI 解析」→ 查 `ai_parse_task` / `ai_match_record`

---

## 十五、常见问题

### Q1：服务启动正常但解析一直失败

- 检查 `DASHSCOPE_API_KEY` 是否在 **启动进程** 的环境变量里  
- 看 agent 日志 `LLM chat failed` 或 task `error_message`  
- 确认 MinIO 可访问、附件 bucket/objectKey 正确

### Q2：parse/submit 200 但 match_score 一直空

- 异步任务需等待数十秒（Qwen 两次调用）  
- 查 `ai_match_record.match_status` 是否为 2  
- 查 job 服务是否启动（sync-match-score Feign）

### Q3：/internal/ai 要 Token 吗？

网关 **白名单放行**；生产应配合内网隔离，避免公网裸调 internal。

### Q4：为何 agent 也配 MinIO？

解析时要 **下载** resume 服务上传的文件；agent 不经过 resume 服务中转文件字节，只拿 metadata 后直连 MinIO。

---

## 十六、相关文件索引

| 文件 | 说明 |
|------|------|
| `service/AiParseTaskProcessor.java` | 异步解析流水线 |
| `service/AiMatchTaskProcessor.java` | 异步匹配流水线 |
| `service/impl/LlmChatServiceImpl.java` | Qwen 调用 |
| `service/impl/ResumeLlmParseServiceImpl.java` | 解析 Prompt |
| `service/impl/ResumeJobMatchServiceImpl.java` | 匹配 Prompt |
| `service/ResumeTextExtractService.java` | Tika 抽文本 |
| `controller/internal/AiInternalController.java` | Feign 入口 |
| `controller/AiController.java` | HR 查询 API |
| `talent-job/.../AiFeignClient.java` | job 触发解析 |
| `docs/sql/talent_ai_agent_schema.sql` | 建表 |

---

## 十七、一句话总结

**talent-ai-agent = 招聘场景的 AI 工人**：从 MinIO 取简历 → Tika 读字 → Qwen 读懂并打分 → 结果落库并回写 job，让 HR 在简历详情里看到 **匹配分与优劣势**。
