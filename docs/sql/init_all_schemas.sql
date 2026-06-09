-- =============================================================================
-- 一键初始化全部业务库（建库 + 建表）
-- 请在项目根目录 talent-ai-system 下执行：
--
--   mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/init_all_schemas.sql
--
-- Docker MySQL 映射端口为 3307 时，将 -P3306 改为 -P3307。
-- Windows PowerShell 若不支持重定向多文件，请逐条执行 README.md 中的命令。
-- =============================================================================

SOURCE docs/sql/talent_auth_schema.sql;
SOURCE docs/sql/talent_job_schema.sql;
SOURCE docs/sql/talent_resume_schema.sql;
SOURCE docs/sql/talent_interview_schema.sql;
SOURCE docs/sql/talent_pool_schema.sql;
SOURCE docs/sql/talent_ai_agent_schema.sql;
