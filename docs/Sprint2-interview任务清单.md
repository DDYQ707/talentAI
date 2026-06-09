# Sprint2 — 面试模块端到端任务清单

> **目标**：打通 HR 安排面试 → 面试官执行任务 → 提交评价 的 MVP 闭环  
> **范围**：`talent-interview` 后端 + 前端 3 个页面 + 简历详情「安排面试」入口  
> **不在本 Sprint**：AI 面试官模式、人才库、Offer、工作台统计、独立面试题生成 API

相关文档：

- [本地部署与接入指南](./智能招聘与人才画像分析系统%20本地部署指南.md)
- [talent-interview 实现说明](./talent-interview实现说明.md)（Sprint 完成后补充）

---

## 一、验收标准（Definition of Done）

完成以下场景即视为 Sprint 成功：

1. HR 在 **简历详情页** 或 **面试管理页** 创建一场面试（选面试官、时间、方式）
2. 面试官登录 → **我的面试列表** 能看到待进行任务
3. 面试官进入 **面试详情** → 看到 AI 匹配分、优劣势、建议面试题（来自已有 `/api/ai/match/*`）
4. 面试官 **提交评价**（综合分 + 结论 + 文字评价）
5. HR 在 **面试管理列表** 看到状态变为「已完成」、显示评分
6. （可选）安排面试后，候选人简历 `screen_status` 自动变为「面试中」

---

## 二、现有基础（不用重做）

| 已有 | 路径 |
|------|------|
| 表结构 | `docs/sql/talent_interview_schema.sql` |
| 实体 / Mapper | `Interview`、`InterviewEvaluation` |
| 只读 Service | `InterviewServiceImpl.getById`、`listByApplicationId` |
| 内部 API | `GET /api/interview/internal/{id}/brief`、`/by-application` |
| 网关路由 | `/api/interview/**`、`/internal/interview/**` |
| Job Feign 骨架 | `JobFeignClient.applicationById` |
| 前端 UI（Mock） | `InterviewManagementView`、`InterviewListView`、`InterviewDetailView` |
| AI 数据 | `api/ai.ts` 已有 matchScore、advantages、suggestedQuestions、dimensionScores |
| 简历详情 | `HrResumeDetail.applicationId` 已有，可直接用于安排面试 |

---

## 三、分期计划

| 阶段 | 工期建议 | 交付 |
|------|----------|------|
| **2-A 后端 CRUD** | 2～3 天 | 安排 / 列表 / 详情 / 取消 / 评价 API |
| **2-B 跨服务联动** | 1 天 | 校验投递单、阶段同步、面试官列表 |
| **2-C 前端联调** | 2 天 | 3 页面 + 简历详情弹窗 + `api/interview.ts` |
| **2-D 联调验收** | 0.5 天 | 端到端测试 + 文档 |

---

## 四、后端 API 设计

> 统一前缀：`/api/interview`（经网关，需 JWT）  
> 角色：Header `X-User-Role` = `HR` / `INTERVIEWER`

### 4.1 HR 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/interview/hr/schedule` | 安排面试 |
| GET | `/api/interview/hr/page` | HR 分页列表（关键词、状态、日期范围） |
| GET | `/api/interview/hr/stats` | 统计：今日/本周/待安排/已完成（供顶栏卡片） |
| GET | `/api/interview/hr/{id}` | 面试详情（含评价） |
| PUT | `/api/interview/hr/{id}/cancel` | 取消面试 |
| GET | `/api/interview/hr/by-application` | 按投递单查面试列表 |

**POST `/api/interview/hr/schedule` Body：**

```json
{
  "applicationId": 10,
  "interviewerId": 5,
  "roundType": 2,
  "roundNo": 1,
  "interviewMode": 1,
  "scheduledStart": "2026-06-15T14:00:00",
  "scheduledEnd": "2026-06-15T15:00:00",
  "meetingUrl": "https://meet.example.com/xxx",
  "location": null
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `applicationId` | ✅ | 投递单 ID |
| `interviewerId` | ✅ | 面试官 userId |
| `roundType` | ✅ | 1-AI初筛 2-业务初试 3-复试 4-HR面 5-终面 6-交叉面 7-作品评审 |
| `interviewMode` | ✅ | 1-视频 2-现场 3-线上评审 |
| `scheduledStart` | ✅ | ISO 日期时间 |
| `scheduledEnd` | 可选 | |
| `meetingUrl` / `location` | 按 mode | 视频填链接，现场填地址 |

**成功返回 `data`：**

```json
{
  "interviewId": 1,
  "status": 1,
  "candidateName": "张三",
  "jobTitle": "高级前端工程师",
  "interviewerName": "李工",
  "scheduledStart": "2026-06-15T14:00:00"
}
```

**业务规则：**

1. Feign 调 `job.applicationById` 校验投递存在且 `status=进行中`
2. Feign 调 auth 查面试官姓名（见 2-B）
3. 从投递单快照 `candidateName`、`jobTitle`、`jobId`、`candidateId`
4. 新建 `interview`，`status=1`（待进行），`createdBy` = 当前 HR
5. 同一 `applicationId` + `roundNo` 不可重复安排（409）

---

### 4.2 面试官接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/interview/my/page` | 我的面试列表（仅 `interviewer_id = 当前用户`） |
| GET | `/api/interview/my/stats` | 今日/本周/已完成/平均分 |
| GET | `/api/interview/my/{id}` | 面试详情（校验归属） |
| POST | `/api/interview/my/{id}/evaluation` | 提交评价 |
| PUT | `/api/interview/my/{id}/complete` | 标记完成（若评价已提交可合并到 evaluation 接口） |

**POST `/api/interview/my/{id}/evaluation` Body：**

```json
{
  "overallScore": 85.5,
  "conclusion": 1,
  "comment": "技术扎实，沟通清晰，建议通过。",
  "dimensionScores": {
    "skill": 90,
    "communication": 80,
    "logic": 85,
    "teamwork": 88,
    "learning": 90,
    "stability": 75
  }
}
```

| `conclusion` | 含义 |
|--------------|------|
| 1 | 通过 |
| 2 | 待定 |
| 3 | 不通过 |

**业务规则：**

1. 仅 `interviewer_id == X-User-Id` 可提交
2. 面试 `status` 必须为 1（待进行）
3. 写入 `interview_evaluation`；更新 `interview.total_score`、`interview.status=2`（已完成）
4. 同一面试仅允许一条有效评价（重复提交 → 409 或 upsert，团队择一）

---

### 4.3 辅助接口（auth，小增量）

HR 安排面试需选面试官，auth 暂无 HR 可用列表，建议新增：

| 方法 | 路径 | 角色 |
|------|------|------|
| GET | `/api/auth/hr/interviewers` | HR |

返回：`[{ id, nickname, account }]`，`user_type=3` 且 `status=正常`

或在 auth 增加 internal：`GET /api/auth/internal/user/brief?userId=` 供 interview Feign 查姓名。

---

### 4.4 状态码对照（前后端统一）

**interview.status**

| 值 | 中文 | 前端 tag |
|----|------|----------|
| 1 | 待进行 | 待进行 / 待面试 |
| 2 | 已完成 | 已完成 |
| 3 | 待安排 | 待安排 |
| 4 | 已取消 | 已取消 |

**roundType 显示名**

| 值 | 显示 |
|----|------|
| 2 | 业务初试 |
| 3 | 业务复试 |
| 4 | HR初面 |
| 5 | 终面 |
| 7 | 作品评审 |

**interviewMode 显示名**

| 值 | 显示 |
|----|------|
| 1 | 视频面试 |
| 2 | 现场面试 |
| 3 | 线上评审 |

---

## 五、后端文件清单（按实现顺序）

### Sprint 2-A：核心 CRUD

| # | 文件 | 动作 |
|---|------|------|
| 1 | `constant/InterviewConstants.java` | 补充 roundType、interviewMode 常量与 label 方法 |
| 2 | `dto/InterviewScheduleRequest.java` | 新建 |
| 3 | `dto/InterviewEvaluationRequest.java` | 新建 |
| 4 | `dto/InterviewPageQuery.java` | 新建（status、keyword、dateFrom、dateTo） |
| 5 | `vo/InterviewDetailVO.java` | 新建（含 evaluation） |
| 6 | `vo/InterviewListVO.java` | 新建 |
| 7 | `vo/InterviewStatsVO.java` | 新建 |
| 8 | `service/InterviewEvaluationService.java` | 新建 |
| 9 | `service/impl/InterviewEvaluationServiceImpl.java` | 新建 |
| 10 | `service/InterviewService.java` | 扩展 schedule / cancel / page / stats / detail |
| 11 | `service/impl/InterviewServiceImpl.java` | 实现上述方法 |
| 12 | `controller/InterviewHrController.java` | 新建，`/api/interview/hr/**` |
| 13 | `controller/InterviewMyController.java` | 新建，`/api/interview/my/**` |
| 14 | `config/InterviewWebConfig.java` 或复用现有 | 读取 `X-User-Id` / `X-User-Role` 工具类 |

### Sprint 2-B：跨服务

| # | 文件 | 动作 |
|---|------|------|
| 15 | `feign/AuthFeignClient.java` | 新建：查用户 brief / 面试官列表 |
| 16 | `feign/JobFeignClient.java` | 已有，schedule 时校验 application |
| 17 | `talent-auth/.../AuthController` 或新 Controller | 新增 `/api/auth/hr/interviewers` |
| 18 | `InterviewServiceImpl.schedule` | 安排成功后可选：Feign job `sync-by-screen-status`（screenStatus=2） |

### Sprint 2-C：无需改库

表结构已满足 MVP，**本 Sprint 不改 DDL**。

---

## 六、与 job / resume 阶段联动（2-B）

安排面试成功后（可选但推荐）：

```
interview.schedule 成功
  → Feign job: POST /api/job/internal/application/sync-by-screen-status
      { resumeId, candidateId, screenStatus: 2, operatorId, operatorName }
  → resume screen_status = 面试中
  → job_application.current_stage = 3 (HR初面)
```

评价结论为「不通过」时 **本 Sprint 不自动淘汰**（HR 仍在简历详情手动点「淘汰」），避免规则过重。

---

## 七、前端任务清单

### 7.1 新建 API 层

**文件：** `talent-ai-front/src/api/interview.ts`

```typescript
// 需实现的函数（示例签名）
export function scheduleInterview(data: InterviewSchedulePayload)
export function fetchHrInterviewPage(params: HrInterviewQuery)
export function fetchHrInterviewStats()
export function fetchHrInterviewDetail(id: number)
export function cancelInterview(id: number)
export function fetchMyInterviewPage(params: MyInterviewQuery)
export function fetchMyInterviewStats()
export function fetchMyInterviewDetail(id: number)
export function submitInterviewEvaluation(id: number, data: EvaluationPayload)
export function fetchInterviewers()  // 调 auth HR 接口
```

补充常量文件（可选）：`constants/interview.ts` — status / roundType / mode 映射

---

### 7.2 页面改造

| 文件 | 改动 |
|------|------|
| `views/hr/InterviewManagementView.vue` | 去掉 Mock；接 page/stats；「安排面试」弹窗表单 |
| `views/hr/ResumeDetailView.vue` | 「安排面试」按钮 → 弹窗（预填 applicationId、candidateName、jobTitle） |
| `views/interviewer/InterviewListView.vue` | 接 my/page、my/stats；卡片展示 AI 匹配分（需 detail 或列表带 applicationId 再调 ai） |
| `views/interviewer/InterviewDetailView.vue` | 路由带 `?id=`；接 my/{id}；接 `fetchAiMatchByApplication`；评价表单提交 |
| `router/index.ts` | 面试官 detail 改为 `/interviewer/detail?id=` |

**InterviewDetailView 数据来源：**

| UI 区块 | API |
|---------|-----|
| 候选人/岗位/时间 | `GET /api/interview/my/{id}` |
| AI 综合分、雷达图 | `fetchAiMatchByApplication(applicationId)` + `parseDimensionScores` |
| 建议面试题 | `parseJsonStringArray(aiMatch.suggestedQuestions)` |
| 优劣势标签 | `parseJsonStringArray(advantages/disadvantages)` |
| 评价提交 | `POST /api/interview/my/{id}/evaluation` |

**本 Sprint 仍 Mock 的部分（可保留）：**

- `AIModeView.vue`（AI 面试官模式）
- HR 面试管理「日历视图」可先做简化版（按日期分组列表），不必上 FullCalendar

---

## 八、联调测试用例

| # | 步骤 | 期望 |
|---|------|------|
| 1 | admin 创建面试官账号（userType=3） | 登录成功，路由 `/interviewer` |
| 2 | 候选人投递 + AI 匹配完成 | `ai_match_record.match_status=2` |
| 3 | HR 简历详情 → 进入面试 → 安排面试 | `interview` 表新增，`status=1` |
| 4 | 面试官登录 → 列表 | 看到刚安排的记录 |
| 5 | 进入面试详情 | AI 分、建议题展示 |
| 6 | 提交评价 conclusion=1 | `interview.status=2`，`total_score` 有值 |
| 7 | HR 面试管理列表刷新 | 状态「已完成」，显示评分 |
| 8 | 取消待进行面试 | `status=4`，面试官列表不再显示为待进行 |

**PowerShell 冒烟（直连 8085，调试时可绕过网关）：**

```powershell
# 需替换 JWT 与 body
Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:8080/api/interview/hr/page?page=1&size=10" `
  -Headers @{ Authorization = "Bearer <HR_JWT>" }
```

---

## 九、任务拆分（可贴到看板）

### 后端

- [ ] **INT-1** DTO/VO + Constants 扩展
- [ ] **INT-2** InterviewService.schedule + 校验 application
- [ ] **INT-3** InterviewHrController（schedule / page / stats / detail / cancel）
- [ ] **INT-4** InterviewEvaluationService + submit
- [ ] **INT-5** InterviewMyController（my/page / detail / evaluation）
- [ ] **INT-6** AuthFeign + HR 面试官列表接口
- [ ] **INT-7** 安排成功后 sync screen_status（可选）
- [ ] **INT-8** 单元/接口自测

### 前端

- [ ] **INT-F1** 新建 `api/interview.ts`
- [ ] **INT-F2** InterviewManagementView 列表 + 统计 + 安排弹窗
- [ ] **INT-F3** ResumeDetailView 安排面试弹窗
- [ ] **INT-F4** InterviewListView 接 API
- [ ] **INT-F5** InterviewDetailView 接 API + AI 区块 + 评价表单
- [ ] **INT-F6** 路由 query.id 与错误态/loading

### 文档

- [ ] **INT-D1** 补充 `docs/talent-interview实现说明.md`
- [ ] **INT-D2** 部署指南附录增加 interview 服务说明（若需要）

---

## 十、明确不做（留下一 Sprint）

| 功能 | 原因 |
|------|------|
| AI 面试官实时对话 `AIModeView` | 需新 LLM 会话 API |
| 独立「一键生成面试题」 | 先用 match 的 `suggestedQuestions` |
| 人才库 / Offer | 依赖面试结果沉淀 |
| HR 工作台统计 | 可 Sprint3 做轻量聚合 |
| 候选人端「查看面试安排」 | MVP 未在精简.md 强调 |
| 多轮面试自动流转 | 手动安排下一轮即可 |

---

## 十一、建议开发顺序（一人串行）

```
Day1  INT-1 ~ INT-3  → Postman 能 schedule + HR 列表
Day2  INT-4 ~ INT-5  → Postman 能提交评价
Day3  INT-6 ~ INT-7  → 面试官列表 + 阶段同步
Day4  INT-F1 ~ INT-F3 → HR 端页面通
Day5  INT-F4 ~ INT-F6 → 面试官端页面通
Day6  联调 + INT-D1   → 验收 + 文档
```

---

## 十二、风险提示

| 风险 | 应对 |
|------|------|
| HR 无面试官列表 API | Sprint 2-B 必须先做 auth 小接口 |
| `applicationId` 为空（仅上传简历未投递） | 安排面试按钮 disabled + 提示「需先投递」 |
| 时区 / 日期格式 | 前后端统一 ISO-8601，`LocalDateTime` |
| 网关未放行新路径 | 已有 `/api/interview/**` 路由，一般无需改 gateway |

---

**下一步**：从 **INT-1 + INT-2** 开始，我可以按该清单直接帮你写 `InterviewHrController` 和 `schedule` 实现。
