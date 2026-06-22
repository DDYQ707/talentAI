-- =============================================================================
-- 在线简历解析补丁：ai_parse_task.attachment_id 允许 NULL
--
-- 背景：parseSource=online 时无附件，插入 ai_parse_task 若 attachment_id NOT NULL 会 500
--
-- 执行：
--   mysql -h localhost -P 3306 -uroot -p < docs/sql/20260622_ai_parse_task_online_patch.sql
-- =============================================================================

USE `talent_ai_db`;

ALTER TABLE `ai_parse_task`
  MODIFY COLUMN `attachment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '附件ID（在线简历可为空）';
