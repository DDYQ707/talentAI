# Sprint1 收尾清单 — Qwen-Max 最小调用验证

> **阶段目标**：`talent-ai-agent` 接入通义千问 Qwen-Max，提供可测试的 LLM 调用能力  
> **阶段范围**：仅 LLM 连通性验证，**不含**投递触发、LLM 简历结构化、人岗匹配闭环  
> **完成日期**：2026-06-01

---

## 1. 本阶段交付物

### 1.1 代码（talent-ai-agent）

| 交付项 | 路径 / 说明 | 状态 |
|--------|-------------|------|
| DashScope 配置 | `application.yml` → `ai.dashscope.*` | ✅ |
| 配置类 | `DashScopeProperties` | ✅ |
| 启动校验 | `DashScopeStartupValidator`（不打印完整 Key） | ✅ |
| LLM 服务 | `LlmChatService` / `LlmChatServiceImpl`（LangChain4j） | ✅ |
| 业务异常 | `AgentBusinessException` | ✅ |
| 测试接口 | `POST /api/ai/test/chat` | ✅ |
| Maven 依赖 | `langchain4j-open-ai` | ✅ |
| 编译 | `mvn -pl talent-ai-agent -am compile` → BUILD SUCCESS | ✅ |

### 1.2 文档

| 文档 | 说明 |
|------|------|
| [DashScope-API-Key本地接入指南.md](./DashScope-API-Key本地接入指南.md) | 队友本地配 Key、验证接口 |
| [API接入流程指南.md](./API接入流程指南.md) | 整体 API / 下一阶段流程 |
| [sql/20260601_ai_sprint1_patch.sql](./sql/20260601_ai_sprint1_patch.sql) | AI 表增量补丁（实体已对齐） |

### 1.3 明确未做（下一阶段）

- [ ] 投递成功后 Feign 调用 `/internal/ai/parse/submit`
- [ ] LLM 简历 JSON 结构化写入 `ai_resume_parse_result`
- [ ] 解析完成后 `/internal/ai/match/submit` 人岗匹配
- [ ] 前端 HR 联调

---

## 2. 负责人自检（提交前）

### 2.1 安全

- [ ] `application.yml` 中 **仅** `${DASHSCOPE_API_KEY}`，无硬编码 Key
- [ ] 代码 / 文档 / 聊天记录中 **无** 真实 `sk-` 密钥
- [ ] 若 Key 曾在 yml 或截图中泄露 → 已在百炼控制台 **轮换** 新 Key

快速自查命令（项目根目录）：

```powershell
# 不应匹配到真实 Key（仅文档占位符 sk-你的... 可接受）
git grep -i "sk-[a-f0-9]\{24,\}" -- . ":(exclude)docs/DashScope-API-Key本地接入指南.md"
```

### 2.2 功能验收

- [ ] IDEA 配置 `DASHSCOPE_API_KEY` 后启动 `AiAgentApplication`
- [ ] 启动日志出现：`DashScope 配置已加载`，`apiKeyPrefix=sk-...` 正确
- [ ] 测试接口返回 200：

```powershell
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8084/api/ai/test/chat" `
  -ContentType "application/json" `
  -Body '{"prompt":"请只返回：AI服务连接成功"}'
```

- [ ] `/internal/ai/**` 行为与改前一致（未破坏内部接口）

### 2.3 数据库补丁

在 MySQL 客户端执行（库名以本地为准，默认 `talent_ai_db`）：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p talent_ai_db < docs/sql/20260601_ai_sprint1_patch.sql
```

验证（可选）：

```sql
USE talent_ai_db;
SHOW COLUMNS FROM ai_parse_task LIKE 'application_id';
SELECT model_code, status FROM ai_model WHERE model_code = 'qwen-max';
```

- [ ] 补丁已执行
- [ ] `ai_model` 存在 `qwen-max` 记录

### 2.4 编译

```powershell
cd talent-ai-backend
$env:JAVA_HOME = "C:\Program Files\java\jdk-21\jdk-21.0.8"   # 或本机 JDK 17/21 路径
mvn -pl talent-ai-agent -am compile
```

- [ ] BUILD SUCCESS

---

## 3. 团队协作

- [ ] 将 [DashScope-API-Key本地接入指南.md](./DashScope-API-Key本地接入指南.md) 发给队友
- [ ] 说明：**每人用自己的百炼 Key**，不要共用、不要提交 Git
- [ ] 统一本地 JDK：`JAVA_HOME` 指向 **17 或 21**（勿用 JDK 8）

---

## 4. Git 提交建议

**建议纳入本次提交的文件：**

```
talent-ai-backend/talent-ai-agent/          # LLM 相关代码与 pom
docs/DashScope-API-Key本地接入指南.md
docs/Sprint1-LLM验证-收尾清单.md            # 本文档
docs/API接入流程指南.md                     # 若已更新
docs/sql/20260601_ai_sprint1_patch.sql      # 若尚未入库
```

**不要提交：**

- `.idea/workspace.xml`
- `target/`
- 任何含真实 API Key 的文件
- `docker/minio/data/` 等本地数据

**建议 commit message：**

```
feat(ai-agent): Sprint1 Qwen-Max 最小 LLM 调用验证

接入 DashScope OpenAI 兼容模式，新增 LlmChatService 与 /api/ai/test/chat
测试接口；补充 DashScope 本地接入文档与 Sprint1 收尾清单。
```

---

## 5. 下一阶段入口

Sprint1 收尾完成后，按 [API接入流程指南.md §5](./API接入流程指南.md) 进入 **Sprint2-A**：

> 在 `AiParseService` 文本抽取成功后，调用 `LlmChatService` 做简历 JSON 结构化，写入 `ai_resume_parse_result`。

---

## 6. 签字确认（可选）

| 角色 | 姓名 | 日期 | 备注 |
|------|------|------|------|
| 开发 | | | LLM 测试接口 code=200 |
| Review | | | 无 Key 泄露 |
