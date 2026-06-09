# talent-job 实现说明

> 模块路径：`talent-ai-backend/talent-job`  
> 端口：**8082**  
> 数据库：**talent_job_db**  
> 定位：招聘岗位、投递申请、招聘阶段流转（招聘流程「中枢」）

相关文档：

- [talent-gateway 实现说明](./talent-gateway实现说明.md)
- [talent-auth 实现说明](./talent-auth实现说明.md)
- [talent-resume 实现说明](./talent-resume实现说明.md)
- [talent-ai-agent 实现说明](./talent-ai-agent实现说明.md)

---

## 一、它解决什么问题？

`talent-job` 负责 **「招什么岗」** 和 **「谁投了哪个岗」**，是连接候选人、HR、简历、AI 匹配的枢纽。

| 职责 | 说明 |
|------|------|
| **岗位管理** | HR 发布 / 编辑 / 删除 / 列表 / 详情 |
| **投递管理** | 候选人投递简历，生成投递单 |
| **阶段流转** | 记录 `current_stage`，写 `application_stage_log` |
| **跨服务编排** | 投递时 Feign 调 auth、resume、ai-agent |
| **对内 API** | 供 resume、ai-agent 查询投递、同步匹配分、同步初筛状态 |

它**不负责**：

- 用户登录、档案（talent-auth）
- 简历内容与附件（talent-resume）
- AI 解析与打分（talent-ai-agent）
- 面试安排（talent-interview）

```
┌──────────────┐     /api/job/**      ┌─────────────┐
│  HR / 候选人  │ ──────────────────► │ talent-job  │
│   前端       │     /api/delivery/** │   :8082     │
└──────────────┘                     └──────┬──────┘
                                            │
              ┌─────────────────────────────┼─────────────────────────────┐
              │                             │                             │
              ▼                             ▼                             ▼
       talent_auth_db               talent_resume                   talent-ai-agent
       (Feign 查档案)               (Feign 校验简历)                 (Feign 触发解析)
              │                             │                             │
              └─────────────────────────────┴─────────────────────────────┘
                                    投递成功后 AI 链路自动启动
```

---

## 二、在系统中的位置

网关路由（`talent-gateway/application.yml`）：

| 路径前缀 | 转发到 |
|----------|--------|
| `/api/job/**` | `lb://talent-job` |
| `/api/delivery/**` | `lb://talent-job` |

前端所有岗位、投递相关请求都经 **8080 网关** 进入本服务；Feign 服务间调用则 **直连 Nacos 上的 talent-job:8082**。

当前用户身份来自网关注入：

- `X-User-Id` — 当前登录用户 ID  
- `X-User-Role` — `CANDIDATE` / `HR` / `ADMIN` 等  

本服务**不解析 JWT**，与 talent-auth 的分工一致。

---

## 三、模块目录结构

```
talent-job/
├── pom.xml
└── src/main/java/com/talent/job/
    ├── TalentJobApplication.java       # 启动 + Nacos + Feign
    ├── config/MybatisPlusConfig.java   # 分页插件
    ├── constant/
    │   └── JobApplicationConstants.java # 阶段/状态枚举与映射
    ├── controller/
    │   ├── JobPostController.java      # /api/job  岗位
    │   ├── JobDeliveryController.java  # /api/delivery  投递
    │   ├── JobInternalController.java  # /api/job/internal  Feign 对内
    │   ├── JobApplicationController.java      # 脚手架 /jobApplication
    │   └── ApplicationStageLogController.java # 脚手架
    ├── service/ + service/impl/
    │   ├── JobPostServiceImpl
    │   └── JobApplicationServiceImpl   # 投递核心逻辑 ★
    ├── feign/
    │   ├── AuthFeignClient.java
    │   ├── ResumeFeignClient.java
    │   └── AiFeignClient.java
    ├── entity/   JobPost, JobApplication, ApplicationStageLog
    ├── dto/ / vo/
    └── mapper/ + resources/mapper/*.xml
```

---

## 四、依赖与技术栈

| 依赖 | 用途 |
|------|------|
| `spring-boot-starter-web` | REST API |
| `mybatis-plus` | ORM、分页、逻辑删除 |
| `openfeign` + `loadbalancer` | 调用 auth / resume / ai-agent |
| `nacos-discovery` | 注册为 `talent-job` |
| `talent-common` | 公共工具（本模块 API 多返回 `Map` 而非统一 `R`） |

启动类：

```java
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.talent.job.mapper")
public class TalentJobApplication { ... }
```

---

## 五、数据库：talent_job_db

脚本：`docs/sql/talent_job_schema.sql`

| 表 | 代码中是否使用 | 说明 |
|----|----------------|------|
| `job_post` | ✅ | 招聘岗位 |
| `job_application` | ✅ | 投递单 |
| `application_stage_log` | ✅ | 阶段变更日志 |
| `offer` / `offer_approval` | ⏳ 表已建 | 前端有 Offer 页，后端 CRUD 尚未在本模块实现 |

### 冗余快照字段（微服务拆库设计）

`job_application` 存 `job_title`、`candidate_name`，`job_post` 存 `dept_name`、`publisher_name` 等——**避免跨库 JOIN**，写入时从 auth/resume 或请求上下文一次性快照。

### 招聘阶段 current_stage

定义见 `JobApplicationConstants`：

| 值 | 含义 |
|----|------|
| 1 | 简历已投递 |
| 2 | AI 初筛 |
| 3 | HR 初面 |
| … | 后续可扩展技术面、终面等 |
| 6 | Offer |

### 投递 status

| 值 | 含义 |
|----|------|
| 1 | 进行中 |
| 2 | 已录用 |
| 3 | 已淘汰 |
| 4 | 已撤回 |

---

## 六、对外 API（经网关）

### 6.1 JobPostController — `/api/job`

| 方法 | 路径 | 角色/鉴权 | 说明 |
|------|------|-----------|------|
| POST | `/publish` | 需登录（HR） | 发布岗位，Feign 拉发布人姓名 |
| GET | `/list` | 可匿名* | 分页列表，支持 title、status 筛选 |
| GET | `/detail?id=` | 可匿名* | 岗位详情 |
| PUT | `/{id}` | 需登录 | HR 更新岗位 |
| DELETE | `/{id}` | 需登录 | 逻辑删除岗位 |

\* 网关对 `/api/job/**` 默认需 Token；若未登录访问 list/detail 会 401，前端候选人浏览岗位时已登录或使用公开策略需另行配置。

### 6.2 JobDeliveryController — `/api/delivery`

| 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|
| POST | `/submit` | **CANDIDATE** | 投递简历（核心） |
| GET | `/my` | **CANDIDATE** | 我的投递记录分页 |

### 6.3 前端对应关系

| 前端文件 | API |
|----------|-----|
| `api/job.ts` / `api/hrJob.ts` | `/api/job/list`、`detail`、`publish`、PUT、DELETE |
| `api/delivery.ts` | `/api/delivery/submit`、`/my` |

---

## 七、对内 API — JobInternalController

路径：`/api/job/internal/**`（Feign 直连 8082，一般不经过网关鉴权逻辑）

| 接口 | 调用方 | 作用 |
|------|--------|------|
| `GET /application/by-id` | talent-ai-agent | 解析完成后查 jobId |
| `GET /application/latest-by-resume` | talent-resume | 简历页展示最近投递 |
| `GET /application/latest-by-candidate` | talent-resume | 按候选人查最近投递 |
| `GET /post/brief` | ai-agent（经 JobBriefQueryService） | 人岗匹配拉 JD |
| `POST /application/sync-match-score` | talent-ai-agent | AI 打分回写 `match_score` |
| `POST /application/sync-by-screen-status` | talent-resume | HR 改初筛状态时同步投递阶段 |
| `POST /application/latest-by-candidates` | talent-resume | HR 简历列表批量查投递 |

---

## 八、Feign：job 调谁、谁调 job

### 8.1 job 出站（本模块 `feign/`）

```java
@FeignClient(name = "talent-auth")
// getUserName、getProfileCompleteness（投递前校验档案）

@FeignClient(name = "talent-resume")
// getResumeOwnership、getResumeBrief、markPendingOnDelivery

@FeignClient(name = "talent-ai-agent")
// POST /internal/ai/parse/submit
```

### 8.2 入站（其他模块的 JobFeignClient）

| 模块 | 典型用途 |
|------|----------|
| talent-ai-agent | 查投递/岗位 JD、回写 matchScore |
| talent-resume | 同步 screen_status、查投递摘要 |
| talent-interview | 关联 applicationId（面试模块） |

---

## 九、核心流程：候选人投递（最重要）

入口：`JobDeliveryController.submit` → `JobApplicationServiceImpl.submitApplication`

```
1. 校验 X-User-Id、X-User-Role == CANDIDATE

2. Feign → auth：档案是否 complete（必填项齐才能投）

3. Feign → resume：getResumeOwnership
   ├─ 简历存在且属于当前候选人
   └─ 若有主简历 ID 则替换为 primaryResumeId

4. 查 job_post：存在且 status == 招聘中(1)

5. 防重复：同 job + 同 candidate 且 status=进行中 则 409

6. Feign → auth：getUserName 写入 candidate_name 快照

7. 事务内：
   ├─ INSERT job_application（stage=1 简历已投递）
   ├─ INSERT application_stage_log
   └─ UPDATE job_post SET applied_count = applied_count + 1

8. Feign → resume：markPendingOnDelivery（screen_status 待初筛）

9. 异步链路（失败不回滚主事务）：
   Feign → resume：getResumeBrief（取 attachmentId）
   若有附件 → Feign → ai-agent：submitParse
   → AI 解析 → 人岗匹配 → 回写 match_score
```

相关代码：`JobApplicationServiceImpl.java` 第 58～288 行。

### 在线简历无附件时

`triggerAiParseOnDelivery` 会打日志并跳过 AI 解析，投递仍成功。

---

## 十、HR 初筛与 resume 同步

HR 在 resume 服务修改 `resume.screen_status` 时，resume 会 Feign 调用：

```
POST /api/job/internal/application/sync-by-screen-status
```

`JobApplicationServiceImpl.syncLatestApplicationByScreenStatus` 会：

1. 找该候选人**最近一条**投递  
2. 按 `JobApplicationConstants.stageForScreenStatus` 更新 `current_stage`、`status`  
3. 写一条 `application_stage_log`

| resume.screen_status | 投递 current_stage | 投递 status |
|----------------------|-------------------|-------------|
| 1 待初筛 | 1 简历已投递 | 进行中 |
| 2 面试中 | 3 HR初面 | 进行中 |
| 3 已录用 | 6 Offer | 已录用 |
| 4 已淘汰 | 2 AI初筛* | 已淘汰 |

\* 映射见 `JobApplicationConstants`，与产品文案可能还需后续对齐。

---

## 十一、岗位发布流程（简要）

`JobPostController.publishJob`：

1. 从 `X-User-Id` 取 HR 用户 ID  
2. Feign `auth.getUserName` → `publisher_name`  
3. 默认 `deptId=1`、`deptName=技术部`（可后续接 auth 部门）  
4. `status=1` 招聘中，`publishedAt=now`  
5. `jobPostService.save`

---

## 十二、脚手架 Controller（网关不可达）

以下路径**没有** `/api/job` 前缀，网关**不会转发**，仅供代码生成或直连 8082 调试：

| Controller | 路径 |
|------------|------|
| JobApplicationController | `/jobApplication` |
| ApplicationStageLogController | `/applicationStageLog` |

正式业务请使用 `/api/delivery`、`/api/job/internal`。

---

## 十三、配置说明

```yaml
# application.yml
server:
  port: 8082
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/talent_job_db?...

# bootstrap.yml
spring:
  application:
    name: talent-job    # Feign / 网关 lb:// 使用此名
  cloud:
    nacos:
      discovery:
        namespace: talent-ai-dev
```

---

## 十四、本地调试要点

1. 依赖服务：至少 **talent-auth**、**talent-resume** 已启动（投递会 Feign 它们）  
2. AI 链路：还需 **talent-ai-agent** + `DASHSCOPE_API_KEY`  
3. 投递测试账号：`userType=1` 的候选人，且 auth 档案 `complete=true`  
4. 联调走 **8080**：`POST /api/delivery/submit`  
5. 查看阶段日志：查表 `application_stage_log`

---

## 十五、常见问题

### Q1：投递提示「请先完善个人信息」

auth 的 `/api/auth/candidate/profile/complete` 返回 `complete=false`。去候选人端完善档案。

### Q2：投递成功但没有 AI 分数

- 是否上传了简历附件（纯在线简历无 attachmentId 不会触发解析）  
- ai-agent 是否启动、Key 是否配置  
- 看 job 服务日志 `投递后触发 AI 解析...`

### Q3：HR 能看到岗位列表但候选人 401

网关对 `/api/job/list` 也需 Token；候选人端浏览岗位前需登录。

### Q4：match_score 存在哪？

`job_application.match_score`，由 ai-agent 通过 `sync-match-score` 回写。

### Q5：Offer 功能在哪？

数据库表 `offer` 已在 `talent_job_schema.sql`，本模块**尚无** Offer Controller，前端 Offer 页待后端补齐。

---

## 十六、相关文件索引

| 文件 | 说明 |
|------|------|
| `controller/JobPostController.java` | 岗位 CRUD |
| `controller/JobDeliveryController.java` | 投递入口 |
| `controller/JobInternalController.java` | 微服务内部 API |
| `service/impl/JobApplicationServiceImpl.java` | 投递 + 同步逻辑 |
| `constant/JobApplicationConstants.java` | 阶段/状态常量 |
| `feign/*.java` | 出站 Feign |
| `docs/sql/talent_job_schema.sql` | 建表 |

---

## 十七、一句话总结

**talent-job = 招聘流程的数据中枢**：管岗位和投递单，在候选人投递时串联 auth、resume、AI 三条线，并通过 internal 接口把匹配分、初筛状态与各微服务保持同步。
