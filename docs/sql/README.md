# 数据库初始化指南

本目录按**微服务拆库**提供 SQL 脚本，与 `talent-ai-backend` 各服务 `application.yml` 中的库名一一对应。

## 库与服务对照

| 脚本文件 | 数据库名 | 微服务 | 端口 |
|----------|----------|--------|------|
| `talent_auth_schema.sql` | `talent_auth_db` | talent-auth | 8081 |
| `talent_job_schema.sql` | `talent_job_db` | talent-job | 8082 |
| `talent_resume_schema.sql` | `talent_resume_db` | talent-resume | 8083 |
| `talent_ai_agent_schema.sql` | `talent_ai_db` | talent-ai-agent | 8084 |
| `talent_interview_schema.sql` | `talent_interview_db` | talent-interview | 8085 |
| `talent_pool_schema.sql` | `talent_pool_db` | talent-talent-pool（规划中） | — |

> `talent-analytics` 无独立业务表，通过 Feign 聚合各服务 API，无需建库。

## 快速初始化（推荐）

MySQL 有两种常见用法，**导入 SQL 的端口须与 `application.yml` 一致**：

| 用法 | 端口 | 密码（示例） | 后端 yml |
|------|------|--------------|----------|
| **本机 MySQL（团队默认）** | 3306 | 各开发者自定（yml 默认 `dyq!`） | 仓库默认，一般不改 |
| **Docker MySQL** | 3307 | `root123` | 需把 JDBC 改为 3307 |

在项目根目录 `talent-ai-system` 下执行（将密码换成你的 root 密码）：

```powershell
# 本机 MySQL 3306
mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_auth_schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_job_schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_resume_schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_interview_schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_pool_schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_ai_agent_schema.sql
```

Docker MySQL（仅在使用容器数据库时）：

```powershell
mysql -h127.0.0.1 -P3307 -uroot -proot123 < docs/sql/talent_auth_schema.sql
# ... 其余文件同理
```

也可在 MySQL 客户端内（已 cd 到项目根目录）：

```sql
SOURCE docs/sql/talent_auth_schema.sql;
SOURCE docs/sql/talent_job_schema.sql;
-- ...
```

或使用 `init_all_schemas.sql`（需在项目根目录启动 mysql，且客户端支持 SOURCE）。

## 可选种子数据

| 文件 | 说明 | 依赖 |
|------|------|------|
| `seed_admin_user.sql` | 管理员 admin / 123456 | 先执行 `talent_auth_schema.sql` |
| `seed_candidate_13800138099.sql` | 演示候选人档案 + 在线简历 | 先注册手机号 13800138099 |

```powershell
mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/seed_admin_user.sql
mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/seed_candidate_13800138099.sql
```

## 文件说明

| 文件 | 用途 |
|------|------|
| `talent_ai_schema.sql` | **全量参考 DDL**（单库视角，不含 CREATE DATABASE），设计文档用 |
| `talent_*_schema.sql` | **按服务拆库的可执行脚本**（含 CREATE DATABASE + 建表） |
| `20260601_ai_sprint1_patch.sql` | 历史增量补丁，字段已合并进 `talent_ai_agent_schema.sql` |
| `init_all_schemas.sql` | 一键 SOURCE 全部拆库脚本 |

## 设计约定

- 跨服务关联字段（如 `candidate_id`、`resume_id`）仅作**逻辑外键**，不跨库建物理 FOREIGN KEY。
- 同库内表（如 `job_application` → `job_post`）保留物理外键，便于本地约束。
- 各脚本可重复执行：先 `DROP TABLE` 再 `CREATE TABLE`（会清空该库业务数据）。

## 验证

```sql
SHOW DATABASES LIKE 'talent_%';

SELECT table_schema, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema LIKE 'talent_%'
GROUP BY table_schema;
```

期望看到 6 个库及对应表数量（auth 约 9 张、job 5 张、resume 5 张、interview 2 张、pool 3 张、ai 约 9 张）。
