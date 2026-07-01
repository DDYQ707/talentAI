# 数据库初始化脚本

本目录已整合为 **7 个库的全量快照**（结构 + 当前种子数据），替代原先分散的 schema / patch / seed 脚本。

## 文件说明

| 文件 | 数据库 | 微服务 | 端口 |
|------|--------|--------|------|
| `talent_auth_db.sql` | talent_auth_db | talent-auth | 8081 |
| `talent_admin_db.sql` | talent_admin_db | talent-admin | 8088 |
| `talent_job_db.sql` | talent_job_db | talent-job | 8082 |
| `talent_resume_db.sql` | talent_resume_db | talent-resume | 8083 |
| `talent_interview_db.sql` | talent_interview_db | talent-interview | 8085 |
| `talent_ai_db.sql` | talent_ai_db | talent-ai-agent | 8084 |
| `talent_pool_db.sql` | talent_pool_db | talent-pool | 8086 |
| `init_all.sql` | 以上全部 | — | 按依赖顺序 SOURCE |

快照导出时间：**2026-07-01**（来源：本机 MySQL `127.0.0.1:3306`）

## 一键初始化（推荐）

在项目根目录 `talent-ai-system` 下执行：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 -e "source docs/sql/init_all.sql"
```

PowerShell 同理（将 `-p` 后换成你的密码，或交互输入）：

```powershell
cd talent-ai-system
mysql -h127.0.0.1 -P3306 -uroot -p你的密码 --default-character-set=utf8mb4 -e "source docs/sql/init_all.sql"
```

## 单库导入

```bash
mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 < docs/sql/talent_auth_db.sql
```

## 演示账号

统一密码：**123456**

| 类型 | 账号示例 |
|------|----------|
| 管理员 | `admin` |
| HR | `hr@company.com` |
| 面试官 | `interview@company.com`、`interview2@company.com`、`interview3@company.com` |
| 候选人（手机号登录） | `13910001001`~`009`、`13500001005`（陈磊）等 |

## 重要说明

1. **中文编码**：请使用 `mysql < file.sql` 或 `mysql -e "source ..."` 导入。**不要**用 `Get-Content file.sql | mysql`（PowerShell 管道易导致中文乱码）。
2. **MinIO 附件**：SQL 仅含 `resume_attachment` 等元数据，`object_key` 指向 MinIO 桶 `talent-resumes`。新环境需单独备份/还原 MinIO，或在前端重新上传简历 PDF。
3. **Docker MySQL（3307）**：将 `-P3306` 和密码改为你 Docker 配置即可，脚本内容相同。
4. **重新导出**：若需更新种子数据，在本机库变更后执行：

```bash
mysqldump -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 --single-transaction --routines --troutines --set-gtid-purged=OFF --databases talent_auth_db --result-file=docs/sql/_export/talent_auth_db.sql
```

（对其余 6 库重复；再用本 README 中的整合流程生成最终文件。）
