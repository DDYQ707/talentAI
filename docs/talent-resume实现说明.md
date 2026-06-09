# talent-resume 实现说明

> 模块路径：`talent-ai-backend/talent-resume`  
> 端口：**8083**  
> 数据库：**talent_resume_db**  
> 对象存储：**MinIO**（桶名 `talent-resumes`）  
> 定位：简历底座 — 附件存储、在线简历、HR 初筛、对内 Feign

相关文档：

- [talent-gateway 实现说明](./talent-gateway实现说明.md)
- [talent-auth 实现说明](./talent-auth实现说明.md)
- [talent-job 实现说明](./talent-job实现说明.md)
- [talent-ai-agent 实现说明](./talent-ai-agent实现说明.md)

---

## 一、它解决什么问题？

候选人侧需要 **上传 PDF/Word** 或 **在线填写** 教育/工作/技能；HR 侧需要 **列表、预览、改初筛状态**；投递与 AI 解析需要 **查附件在 MinIO 的位置**。

| 职责 | 说明 |
|------|------|
| **附件简历** | 上传 → MinIO → `resume_attachment` 落库 |
| **在线简历** | `resume_education` / `resume_work_experience` / `resume_skill` |
| **简历合并** | 每候选人尽量 **一条主简历**（`ResumeConsolidationService`） |
| **HR 管理** | 分页列表、详情、更新 `screen_status` |
| **预签名预览** | 私有桶通过临时 URL 给浏览器预览 PDF |
| **对内 API** | job / ai-agent Feign 查归属、附件、投递后改状态 |

它**不负责**：

- 用户登录（talent-auth）
- 投递单、岗位（talent-job）
- LLM 解析文件内容（talent-ai-agent，但会从本服务拿 bucket/objectKey）

```
┌─────────────┐   /api/resume/**   ┌───────────────┐
│  候选人/HR   │ ────────────────► │ talent-resume │
│   前端       │                   │    :8083      │
└─────────────┘                   └───────┬───────┘
                                          │
              ┌───────────────────────────┼───────────────────────────┐
              ▼                           ▼                           ▼
      talent_resume_db              MinIO :9000                  Feign
      resume / attachment           talent-resumes               auth / job
              ▲                           ▲
              │                           │
       talent-job 投递              talent-ai-agent
       校验归属 / 标记初筛           下载附件解析
```

---

## 二、网关与身份

网关路由：`Path=/api/resume/**` → `lb://talent-resume`

当前用户来自网关注入：

- `X-User-Id` — 候选人或 HR 的用户 ID  
- `X-User-Role` — `CANDIDATE` / `HR` / `ADMIN`  

本模块不解析 JWT。HR 接口在 Service 层用 `assertHrRole` 校验角色。

---

## 三、模块目录结构

```
talent-resume/
├── pom.xml                          # + minio SDK、openfeign
└── src/main/java/com/talent/resume/
    ├── TalentResumeApplication.java
    ├── config/
    │   ├── MinioConfig.java         # MinioClient Bean
    │   ├── MinioProperties.java
    │   ├── MybatisPlusConfig.java
    │   └── ResumeExceptionHandler.java
    ├── constant/ResumeConstants.java
    ├── controller/
    │   ├── ResumeController.java           # /api/resume  通用
    │   ├── FileController.java               # /api/resume/file  上传/预览
    │   ├── OnlineResumeController.java       # /api/resume/online  在线简历
    │   ├── HrResumeController.java           # /api/resume/hr  HR 管理
    │   └── ResumeInternalController.java     # /api/resume/internal  Feign
    ├── service/
    │   ├── ResumeService.java                # 核心 ★
    │   ├── OnlineResumeService.java          # 在线简历 CRUD
    │   ├── ResumeConsolidationService.java   # 一账号一简历合并 ★
    │   └── MinioStorageService.java          # MinIO 上传/预签名
    ├── feign/AuthFeignClient, JobFeignClient
    ├── entity/  Resume, ResumeAttachment, ResumeEducation, ...
    └── mapper/ + resources/mapper/
```

---

## 四、依赖与技术栈

| 依赖 | 用途 |
|------|------|
| `spring-boot-starter-web` | REST + 文件上传（multipart 最大 10MB） |
| `mybatis-plus` | 五张业务表 CRUD |
| `io.minio:minio` | 对象存储 |
| `openfeign` | 调 auth（候选人摘要）、job（投递同步） |
| `nacos-discovery` | 注册名 `talent-resume` |
| `talent-common` | 统一响应 `R<T>` |

---

## 五、数据库：talent_resume_db

脚本：`docs/sql/talent_resume_schema.sql`

| 表 | 说明 |
|----|------|
| `resume` | 简历主表（附件型 / 在线型共用） |
| `resume_attachment` | 附件元数据 + MinIO bucket/objectKey |
| `resume_education` | 在线简历 — 教育 |
| `resume_work_experience` | 在线简历 — 工作 |
| `resume_skill` | 在线简历 — 技能 |

### resume 主表关键字段

| 字段 | 含义 |
|------|------|
| `candidate_id` | 逻辑外键 → auth 的 `sys_user.id` |
| `parse_status` | 0 未解析 / 1 解析中 / 2 成功 / 3 失败（AI 侧更新） |
| `screen_status` | 1 待初筛 / 2 面试中 / 3 已录用 / 4 已淘汰 |
| `summary` | 个人总结（在线简历） |
| `is_default` | 是否默认简历 |

### 两种简历形态

| 类型 | 标识 | 存储 |
|------|------|------|
| **附件简历** | 有 `resume_attachment` 记录 | 文件在 MinIO，DB 存 objectKey |
| **在线简历** | 无附件，有 education/work/skill 子表 | 纯 MySQL |

投递选简历时（`listAttachmentResumes`）**只返回有附件的主简历**，在线简历 alone 不能用于触发 AI 解析投递链。

---

## 六、MinIO 文件存储

配置（`application.yml`）：

```yaml
minio:
  endpoint: http://127.0.0.1:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: talent-resumes
```

`MinioStorageService`：

1. 首次上传时 **自动创建桶** `talent-resumes`  
2. objectKey 规则：`yyyyMMdd/c{candidateId}/{uuid}-{文件名}`  
3. 允许扩展名：`pdf`、`doc`、`docx`，最大 **10MB**  
4. 预览：`getPresignedPreviewUrl`，默认有效期 **2 小时**（`ResumeConstants.PREVIEW_EXPIRE_HOURS`）

附件表保存 `bucket_name`、`object_key`，AI 服务通过 internal 接口拿到后自行从 MinIO 下载。

---

## 七、简历合并（ResumeConsolidationService）

产品规则：**每个候选人账号只保留一份「主简历」**，避免重复记录。

| 方法 | 作用 |
|------|------|
| `getPrimaryResume` | 取主简历，多份则先合并 |
| `getOrCreatePrimaryResume` | 上传前获取或创建主记录 |
| `consolidateCandidateResumes` | 合并重复记录，附件/在线内容迁移到 keepId |
| `dedupeForHrList` | HR 列表按 candidateId 去重 |
| `consolidateAllDuplicateCandidatesInDatabase` | HR 打开列表前批量合并全库重复 |

合并策略：优先 **有附件**、**更新时间更新** 的记录。

---

## 八、对外 API（经网关）

### 8.1 候选人 — 附件

| Controller | 方法 | 路径 | 说明 |
|------------|------|------|------|
| FileController | POST | `/api/resume/file/upload` | 上传文件 → MinIO + 落库 |
| FileController | GET | `/api/resume/file/preview/{attachmentId}` | 预签名预览 |
| FileController | GET | `/api/resume/file/preview/by-resume/{resumeId}` | 按简历 ID 预览最新附件 |
| ResumeController | GET | `/api/resume/attachment/my` | 可投递的附件简历列表 |
| ResumeController | DELETE | `/api/resume/{id}` | 删除简历 |

### 8.2 候选人 — 在线简历

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/resume/online/my` | 我的在线简历列表 |
| GET | `/api/resume/online/{id}` | 详情（含教育/工作/技能） |
| POST | `/api/resume/online` | 创建 |
| PUT | `/api/resume/online/{id}` | 更新 |
| DELETE | `/api/resume/online/{id}` | 删除 |

### 8.3 HR

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/resume/hr/page` | 简历分页（keyword、screenStatus） |
| GET | `/api/resume/hr/{resumeId}` | 详情（聚合 auth 档案 + job 投递 + 在线内容） |
| PATCH | `/api/resume/hr/{resumeId}/screen-status` | 改初筛状态，**同步 job 投递单** |
| POST | `/api/resume/hr/consolidate` | 手动触发全库重复合并 |

### 8.4 通用 / 跨服务可读

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/resume/brief/{resumeId}` | 简历摘要（job 投递后触发 AI 时用） |

### 8.5 前端 API 文件

| 文件 | 用途 |
|------|------|
| `api/resume.ts` | 上传、预览、附件列表 |
| `api/onlineResume.ts` | 在线简历 CRUD |
| `api/hrResume.ts` | HR 列表、详情、初筛状态 |

---

## 九、对内 API — ResumeInternalController

路径：`/api/resume/internal/**`（Feign 直连 8083）

| 接口 | 调用方 | 作用 |
|------|--------|------|
| `GET /ownership?resumeId=` | talent-job | 投递前校验归属，返回 `primaryResumeId` |
| `GET /attachment/{attachmentId}` | talent-ai-agent | 取 MinIO bucket/objectKey 下载解析 |
| `POST /on-delivery` | talent-job | 投递成功 → `screen_status=待初筛` |

---

## 十、Feign 协作

### 10.1 resume 出站

```java
@FeignClient(name = "talent-auth")
// getUserName、internal/candidateBrief — HR 列表/详情展示候选人信息

@FeignClient(name = "talent-job")
// latest-by-resume、candidate、sync-by-screen-status、latest-by-candidates
```

### 10.2 入站（谁调 resume）

| 模块 | 典型接口 |
|------|----------|
| talent-job | ownership、on-delivery、brief |
| talent-ai-agent | internal/attachment、ownership、brief |

---

## 十一、核心流程

### 11.1 候选人上传附件

```
POST /api/resume/file/upload  (multipart file)
  → validateFile(pdf/doc/docx, ≤10MB)
  → getOrCreatePrimaryResume(candidateId)
  → clearAttachments（新上传覆盖旧附件）
  → MinioStorageService.upload → objectKey
  → INSERT resume_attachment
  → consolidateCandidateResumes（合并重复 resume 行）
```

### 11.2 候选人投递（与 job 联动）

```
talent-job submitApplication
  → Feign getResumeOwnership(resumeId)
  → Feign markPendingOnDelivery → screen_status = 1
  → Feign getResumeBrief → 有 attachmentId 则触发 AI
```

### 11.3 HR 改初筛状态

```
PATCH /api/resume/hr/{id}/screen-status  { screenStatus, remark }
  → 更新 resume.screen_status
  → Feign job sync-by-screen-status
     → 更新 job_application.current_stage / status
     → 写 application_stage_log
```

| screen_status | 含义 |
|---------------|------|
| 1 | 待初筛 |
| 2 | 面试中 |
| 3 | 已录用 |
| 4 | 已淘汰 |

### 11.4 HR 查看简历详情

`getHrResumeDetail` 聚合多源数据：

1. 主简历 + 最新附件或在线子表  
2. Feign auth → 候选人姓名、手机、城市等  
3. Feign job → 最近投递岗位、matchScore、appliedAt  

预览 PDF：前端再调 `GET /api/resume/file/preview/{attachmentId}` 拿预签名 URL。

---

## 十二、权限模型（简要）

| 操作 | 候选人 | HR / ADMIN |
|------|--------|------------|
| 上传/删自己的简历 | ✅ | — |
| 预览附件 | 仅本人附件 | 全部 |
| HR 列表/改状态 | — | ✅ |

实现：`assertCanAccessAttachment` / `assertHrRole` 在 `ResumeService` 内。

---

## 十三、配置与启动

```yaml
server:
  port: 8083
spring:
  servlet:
    multipart:
      max-file-size: 10MB
  datasource:
    url: jdbc:mysql://.../talent_resume_db
```

依赖基础设施：

- MySQL `talent_resume_db` 已初始化  
- **MinIO** Docker 容器 `:9000` 运行中（见部署指南）  
- 联调投递/HR 列表时需 **talent-auth**、**talent-job** 已启动  

---

## 十四、常见问题

### Q1：上传成功但 AI 不解析

投递链要求 **有附件**。仅在线填写、未上传 PDF/Word 时，job 会跳过 AI（见 talent-job 文档）。

### Q2：MinIO 连接失败

确认 Docker `talent-minio` 运行，控制台 http://localhost:9001 可登录；`endpoint` 为 `http://127.0.0.1:9000`。

### Q3：HR 列表同一人出现多条

调用 `POST /api/resume/hr/consolidate` 或打开列表时自动 `consolidateAllDuplicateCandidatesInDatabase`。

### Q4：预览链接过期

预签名默认 2 小时，过期后重新请求 preview 接口。

### Q5：parse_status 谁更新？

主要由 **talent-ai-agent** 在解析任务完成后回写（若已实现回调/Feign）；本模块表字段已预留。

### Q6：与 auth 里 candidate_profile 的区别？

| | auth `candidate_profile` | resume 模块 |
|--|--------------------------|-------------|
| 用途 | 账号级档案、能否投递 | 简历内容与附件 |
| 投递校验 | auth `/profile/complete` | resume ownership + 附件 |

---

## 十五、相关文件索引

| 文件 | 说明 |
|------|------|
| `service/ResumeService.java` | 上传、HR、internal、预览 |
| `service/ResumeConsolidationService.java` | 一账号一简历 |
| `service/MinioStorageService.java` | MinIO |
| `service/OnlineResumeService.java` | 在线简历 |
| `controller/FileController.java` | 上传/预览 HTTP |
| `controller/HrResumeController.java` | HR HTTP |
| `controller/ResumeInternalController.java` | Feign 入口 |
| `constant/ResumeConstants.java` | 初筛状态、预览时长 |
| `docs/sql/talent_resume_schema.sql` | 建表 |

---

## 十六、一句话总结

**talent-resume = 简历与文件的「仓库管理员」**：MySQL 管结构和初筛状态，MinIO 管 PDF/Word，通过 Feign 与 auth、job、ai-agent 衔接，让投递和 HR 看简历有据可查。
