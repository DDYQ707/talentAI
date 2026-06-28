# AI 知识库维护指南

本目录存放 RAG 知识库的 **Markdown 种子文档**。`talent-ai-agent` 会在首次启动且数据库为空时，自动加载本目录下所有 `.md` 文件（**不含本 README**），切分并向量化后写入 MySQL。

---

## 一、目录与内置文档

```
talent-ai-agent/src/main/resources/knowledge/
├── README.md                          # 本说明（不会入库）
├── 01-process-recruitment-flow.md     # 标准招聘流程
├── 02-faq-screen-status.md            # 简历筛选状态说明
├── 03-faq-match-score.md              # AI 匹配分解读
├── 04-faq-assistant-guide.md          # AI 助手使用指南
├── 05-policy-offer-approval.md        # Offer 审批制度
├── 06-policy-data-privacy.md          # 数据隐私与权限制度
├── 07-process-interview-flow.md       # 面试流程详解
├── 08-faq-ai-resume-parse.md          # AI 简历解析功能说明
├── 09-faq-ai-interview-question.md    # AI 面试题生成功能说明
├── 10-faq-interview-note.md           # 面试笔记与AI评估
├── 11-faq-resume-quality-score.md     # 简历质量评分说明
├── 12-faq-talent-profile.md           # AI 人才画像
├── 13-process-hr-workbench.md         # HR 工作台操作指南
├── 14-jd-template-java-developer.md   # JD模板：Java
├── 15-jd-template-product-manager.md  # JD模板：产品经理
└── 16-jd-template-frontend-developer.md # JD模板：前端
├── 17-faq-candidate-guide.md          # 候选人端使用指南
└── 18-policy-elimination-reason.md    # 候选人淘汰原因
```

| 文件 | title | category |
|------|-------|----------|
| `01-process-recruitment-flow.md` | 标准招聘流程 | `process` |
| `02-faq-screen-status.md` | 简历筛选状态说明 | `faq` |
| `03-faq-match-score.md` | AI 人岗匹配分解读 | `faq` |
| `04-faq-assistant-guide.md` | AI 招聘助手使用指南 | `faq` |
| `05-policy-offer-approval.md` | Offer 审批制度 | `policy` |
| `06-policy-data-privacy.md` | 数据隐私与权限制度 | `policy` |
| `07-process-interview-flow.md` | 面试流程详解 | `process` |
| `08-faq-ai-resume-parse.md` | AI 简历解析功能说明 | `faq` |
| `09-faq-ai-interview-question.md` | AI 面试题生成 | `faq` |
| `10-faq-interview-note.md` | 面试笔记与AI评估 | `faq` |
| `11-faq-resume-quality-score.md` | 简历质量评分说明 | `faq` |
| `12-faq-talent-profile.md` | AI 人才画像 | `faq` |
| `13-process-hr-workbench.md` | HR 工作台操作指南 | `process` |
| `14-jd-template-java-developer.md` | JD模板：Java开发 | `jd_template` |
| `15-jd-template-product-manager.md` | JD模板：产品经理 | `jd_template` |
| `16-jd-template-frontend-developer.md` | JD模板：前端开发 | `jd_template` |
| `17-faq-candidate-guide.md` | 候选人端使用指南 | `faq` |
| `18-policy-elimination-reason.md` | 候选人淘汰原因 | `policy` |

数据库表（`talent_ai_db`）：

- `ai_knowledge_doc` — 文档全文
- `ai_knowledge_chunk` — 切分片段 + 向量（`embedding_json`）

建表脚本：`docs/sql/20260622_ai_knowledge_rag_patch.sql`

---

## 二、Markdown 文件格式

每个知识文件 **必须** 包含 YAML front matter + 正文：

```markdown
---
title: 简历筛选状态说明
category: faq
---

简历筛选状态枚举：

- 1 待初筛：候选人已投递，等待 HR 审核。
- 2 面试中：已通过初筛并进入面试环节。
```

### 字段说明

| 字段 | 必填 | 说明 |
|------|------|------|
| `title` | 是 | 文档标题，检索结果中会展示 |
| `category` | 是 | 文档分类，见下表 |

### category 取值

| 值 | 说明 |
|----|------|
| `process` | 招聘流程 |
| `faq` | 常见问题 |
| `policy` | 制度规范 |
| `jd_template` | JD 模板 |

### 格式注意

- `---` 必须单独占一行
- `title:`、`category:` 冒号后要有空格
- 本 `README.md` 不会被导入（代码已跳过）
- 建议文件名加数字前缀（如 `05-xxx.md`）以控制启动导入顺序

---

## 三、两种导入方式（重要）

| 方式 | 何时使用 | 条件 |
|------|----------|------|
| **A. 启动自动导入** | 首次部署、本地空库 | `ai_knowledge_doc` 表为空 |
| **B. API 手动导入** | 日常改内容、新增文档 | 随时可用 |

> **改 `.md` 文件不会自动更新数据库**（除非表为空后重启服务）。

---

## 方式 A：启动时自动导入（仅空库）

### 步骤

1. 确认知识库为空：

```sql
USE talent_ai_db;
SELECT COUNT(*) FROM ai_knowledge_doc;
-- 结果为 0 时，启动才会自动导入
```

2. 编辑本目录下的 `.md` 文件

3. 确保 `talent-ai-agent` 进程已配置环境变量 `DASHSCOPE_API_KEY`（向量化需要）

4. **重启** `talent-ai-agent`

5. 查看启动日志，应出现类似：

```
Knowledge base empty, seeding 4 markdown documents...
Knowledge indexed, docId=1, title=标准招聘流程, chunks=...
```

若已有数据，则会跳过：

```
Knowledge base already initialized, skip seed
```

6. 验证入库：

```sql
SELECT id, title, category, source_path FROM ai_knowledge_doc;
SELECT doc_id, chunk_index, LEFT(content, 50) FROM ai_knowledge_chunk;
```

---

## 方式 B：API 手动导入（日常推荐）

### 前置条件

1. 已启动：`talent-gateway`（8080）、`talent-auth`、`talent-ai-agent`（8084）
2. 使用 **HR 或 Admin 账号**登录（接口校验 `X-User-Role`）
3. 已配置 `DASHSCOPE_API_KEY`

### 接口一览

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/api/ai/knowledge/docs` | 查看已入库文档列表 |
| POST | `/api/ai/knowledge/import` | 导入一篇文档（切分 + 向量化） |
| POST | `/api/ai/knowledge/{docId}/reindex` | 对已有文档重新切分并向量化 |

网关地址：`http://localhost:8080`

### 获取 Token

1. 使用 HR 账号登录前端
2. 浏览器 F12 → Application → Local Storage → 复制 `token`
3. 或在 Network 面板复制任意请求的 `Authorization: Bearer xxx`

---

### B1. 查看现有文档

```http
GET http://localhost:8080/api/ai/knowledge/docs
Authorization: Bearer <你的JWT>
```

响应示例：

```json
{
  "code": 200,
  "data": [
    {
      "id": 2,
      "title": "简历筛选状态说明",
      "category": "faq",
      "chunkCount": 1,
      "sourcePath": "classpath:knowledge/02-faq-screen-status.md"
    }
  ]
}
```

记下需要操作的 `id`。

---

### B2. 修改已有 FAQ

当前逻辑说明：

- **改 `.md` 文件** → 不会自动同步到已有数据库记录
- **`reindex`** → 仅对 **数据库里已有正文** 重新切分、向量化（**不会**重新读取 `.md`）
- **`import`** → **总是新增**一篇文档

#### 推荐做法一：开发环境，从 Markdown 全量刷新

```sql
USE talent_ai_db;
DELETE FROM ai_knowledge_chunk;
DELETE FROM ai_knowledge_doc;
```

然后 **重启 `talent-ai-agent`**，会自动重新读取本目录全部 `.md` 并导入。

#### 推荐做法二：不删库，手动更新某一篇

1. 编辑对应 `.md`，复制 `---` 下方的正文
2. 若库里已有同名旧文档，建议先删除（避免检索命中重复内容）：

```sql
SELECT id, title FROM ai_knowledge_doc WHERE title = '简历筛选状态说明';
-- 假设旧文档 id = 2
DELETE FROM ai_knowledge_chunk WHERE doc_id = 2;
DELETE FROM ai_knowledge_doc WHERE id = 2;
```

3. 调用 import 导入最新内容：

```http
POST http://localhost:8080/api/ai/knowledge/import
Authorization: Bearer <你的JWT>
Content-Type: application/json

{
  "title": "简历筛选状态说明",
  "category": "faq",
  "content": "简历筛选状态枚举：\n\n- 1 待初筛：候选人已投递，等待 HR 审核。\n- 2 面试中：已通过初筛并进入面试环节。\n- 3 已录用：候选人已被标记为录用。\n- 4 已淘汰：候选人未通过当前流程。",
  "sourcePath": "manual/02-faq-screen-status.md"
}
```

---

### B3. 新增一篇 FAQ

**步骤 1：** 在本目录新建文件，例如 `05-policy-offer-approval.md`：

```markdown
---
title: Offer 审批制度
category: policy
---

Offer 发放流程：

1. HR 发起 Offer，填写薪资与入职日期。
2. HRBP 一级审批。
3. 部门负责人二级审批。
4. 审批通过后发送候选人。
```

**步骤 2：** 导入（二选一）

| 场景 | 操作 |
|------|------|
| 空库 | 清空表后重启，会与其他 `.md` 一起导入 |
| 库中已有数据 | 调用下方 import 接口，仅导入这一篇 |

```http
POST http://localhost:8080/api/ai/knowledge/import
Authorization: Bearer <你的JWT>
Content-Type: application/json

{
  "title": "Offer 审批制度",
  "category": "policy",
  "content": "Offer 发放流程：\n\n1. HR 发起 Offer...\n2. HRBP 一级审批...",
  "sourcePath": "classpath:knowledge/05-policy-offer-approval.md"
}
```

**步骤 3：** 验证

```sql
SELECT id, title, category FROM ai_knowledge_doc ORDER BY id;
```

---

### B4. reindex 什么时候用？

仅当：**数据库里的正文已经正确**，但需要重新切分或重新生成向量（例如之前 Embedding 失败）。

```http
POST http://localhost:8080/api/ai/knowledge/2/reindex
Authorization: Bearer <你的JWT>
```

> `reindex` **不会**重新读取本目录的 `.md` 文件，只处理 `ai_knowledge_doc.content` 字段中的现有内容。

---

## 四、PowerShell 调用示例

```powershell
# 1. 登录获取 token（按实际账号修改）
$login = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"hr账号","password":"密码"}'

$token = $login.data.token

# 2. 查看知识库列表
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/ai/knowledge/docs" `
  -Headers @{ Authorization = "Bearer $token" }

# 3. 导入一篇新知识
$body = @{
  title = "Offer 审批制度"
  category = "policy"
  content = "Offer 发放流程：`n1. HR 发起 Offer`n2. HRBP 审批..."
  sourcePath = "manual/offer-policy.md"
} | ConvertTo-Json -Depth 3

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/ai/knowledge/import" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

---

## 五、导入后如何验证 RAG 生效

1. 打开 HR 端 **AI 助手** 页面
2. 提问测试，例如：
   - 「标准招聘流程是什么？」
   - 「筛选状态 1、2、3、4 分别代表什么？」
   - 「AI 匹配分 60 分是什么意思？」
3. 回答应与 Markdown 内容一致，而不是模型随意编造

也可在 AI 助手中问刚导入的内容，例如：「Offer 审批制度是什么？」

---

## 六、配置项说明

`talent-ai-agent/src/main/resources/application.yml`：

```yaml
ai:
  dashscope:
    embedding-model: text-embedding-v2   # 向量化模型
  knowledge:
    chunk-size: 500      # 每片最大字数
    chunk-overlap: 80    # 片之间重叠字数
    top-k: 5             # 检索返回条数
    min-score: 0.35      # 相似度阈值（低于此分数的片段会被过滤）
```

---

## 七、常见问题

| 问题 | 原因 | 处理 |
|------|------|------|
| 启动没有自动导入 | `ai_knowledge_doc` 已有数据 | 正常跳过；用 API 导入或清空表后重启 |
| import 报错 | 未配置 `DASHSCOPE_API_KEY` | 在 IDEA 运行配置或系统环境变量中配置 |
| import 401/403 | 未登录或非 HR 账号 | 使用 HR / Admin 账号登录 |
| 改了 `.md` 但 AI 回答没变 | 数据库未更新 | 按「B2 修改已有 FAQ」重新 import 或清库重启 |
| 检索不到新内容 | 相似度低于 `min-score` | 提问更贴近文档用词，或临时调低 `ai.knowledge.min-score` |
| 出现重复文档 | 多次 import 未删旧记录 | SQL 删除旧 `doc_id` 对应 chunk 和 doc |

---

## 八、日常维护流程（速查）

```
想改 FAQ
  ├─ 开发/本地：改 .md → 清空 knowledge 表 → 重启 talent-ai-agent
  └─ 已有数据：改 .md → 删旧 doc（可选）→ POST /import

想加 FAQ
  ├─ 新建 05-xxx.md（便于 Git 版本管理）
  └─ POST /import 导入（或清库重启一次性导入全部 .md）

想验证
  ├─ GET /api/ai/knowledge/docs
  ├─ SQL 查 ai_knowledge_doc / ai_knowledge_chunk
  └─ AI 助手页面提问测试
```

---

## 九、相关代码

| 类 / 文件 | 作用 |
|-----------|------|
| `KnowledgeMarkdownLoader.java` | 扫描 `classpath:knowledge/*.md` 并解析 front matter |
| `KnowledgeBootstrapRunner.java` | 空库时自动种子导入 |
| `KnowledgeIngestService.java` | 入库、切分、向量化 |
| `KnowledgeRetrievalService.java` | 相似度检索 |
| `KnowledgeSearchTool.java` | AI 助手 Agent 调用的 RAG 工具 |
| `KnowledgeAdminController.java` | `/api/ai/knowledge/*` 管理接口 |
