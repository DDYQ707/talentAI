-- =============================================================================
-- 智能招聘与人才画像分析系统 - 一键初始化全部数据库
-- 导出时间: 2026-07-01
--
-- 用法（在项目根目录 talent-ai-system 下执行）:
--   mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 -e "source docs/sql/init_all.sql"
--
-- 或 CMD/Git Bash 交互式:
--   mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4
--   mysql> source docs/sql/init_all.sql
--
-- 演示账号统一密码: 123456
--   admin / hr@company.com / interview@company.com / 13910001001~009 / 13500001005 等
--
-- MinIO 说明: SQL 不含简历附件文件；resume_attachment.object_key 指向 MinIO 桶 talent-resumes
--   还原后需同步 MinIO 数据，或在前端重新上传简历 PDF
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

SOURCE docs/sql/talent_auth_db.sql;

SOURCE docs/sql/talent_admin_db.sql;

SOURCE docs/sql/talent_job_db.sql;

SOURCE docs/sql/talent_resume_db.sql;

SOURCE docs/sql/talent_interview_db.sql;

SOURCE docs/sql/talent_ai_db.sql;

SOURCE docs/sql/talent_pool_db.sql;

SET FOREIGN_KEY_CHECKS=1;
