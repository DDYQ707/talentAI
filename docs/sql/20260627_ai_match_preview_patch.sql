-- =============================================================================
-- 预览匹配补丁：允许 ai_match_record.application_id 为空（未投递岗位的简历-岗位预估匹配）
-- 库：talent_ai_db
-- 执行：mysql --host=127.0.0.1 --port=3306 -uroot -p < docs/sql/20260627_ai_match_preview_patch.sql
-- =============================================================================

USE `talent_ai_db`;

ALTER TABLE `ai_match_record`
  MODIFY COLUMN `application_id` BIGINT UNSIGNED NULL COMMENT '投递ID（预览匹配可为空）';
