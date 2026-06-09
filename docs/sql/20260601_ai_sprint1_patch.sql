-- talent-ai-agent Sprint 1 增量补丁
-- 基准：docs/sql/talent_ai_schema.sql
-- 目标库：talent_ai_db
--
-- ⚠️ 已合并至 docs/sql/talent_ai_agent_schema.sql（新环境无需再执行本文件）
-- 本文件仅保留给「早期已建库、需增量升级」的旧环境使用。

USE `talent_ai_db`;

-- -----------------------------------------------------------------------------
-- ai_parse_task：补充投递上下文与文件元信息
-- -----------------------------------------------------------------------------
ALTER TABLE `ai_parse_task`
    ADD COLUMN `application_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '投递ID' AFTER `resume_id`,
    ADD COLUMN `candidate_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '候选人ID' AFTER `application_id`,
    ADD COLUMN `raw_text_length` INT DEFAULT 0 COMMENT '抽取文本长度' AFTER `error_message`,
    ADD COLUMN `file_name` VARCHAR(255) DEFAULT NULL COMMENT '文件名' AFTER `raw_text_length`,
    ADD COLUMN `file_type` VARCHAR(32) DEFAULT NULL COMMENT '文件类型' AFTER `file_name`;

ALTER TABLE `ai_parse_task`
    ADD KEY `idx_application_id` (`application_id`),
    ADD KEY `idx_candidate_id` (`candidate_id`);

-- -----------------------------------------------------------------------------
-- ai_resume_parse_result：补充结构化解析扩展字段
-- -----------------------------------------------------------------------------
ALTER TABLE `ai_resume_parse_result`
    ADD COLUMN `project_json` JSON DEFAULT NULL COMMENT '项目经历' AFTER `skills_json`,
    ADD COLUMN `certificate_json` JSON DEFAULT NULL COMMENT '证书' AFTER `project_json`,
    ADD COLUMN `total_years` DECIMAL(4,1) DEFAULT NULL COMMENT '总工作年限' AFTER `certificate_json`,
    ADD COLUMN `industry_keywords` JSON DEFAULT NULL COMMENT '行业关键词' AFTER `total_years`,
    ADD COLUMN `target_position` VARCHAR(128) DEFAULT NULL COMMENT '目标岗位' AFTER `industry_keywords`,
    ADD COLUMN `raw_text_length` INT DEFAULT 0 COMMENT '抽取文本长度' AFTER `target_position`;

-- -----------------------------------------------------------------------------
-- ai_match_record：补充匹配过程与结果扩展字段
-- -----------------------------------------------------------------------------
ALTER TABLE `ai_match_record`
    ADD COLUMN `match_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待处理 1-处理中 2-成功 3-失败' AFTER `match_score`,
    ADD COLUMN `match_level` VARCHAR(32) DEFAULT NULL COMMENT '匹配等级' AFTER `match_status`,
    ADD COLUMN `match_reason` TEXT DEFAULT NULL COMMENT '匹配理由' AFTER `match_level`,
    ADD COLUMN `suggested_questions` JSON DEFAULT NULL COMMENT '建议面试问题' AFTER `disadvantages`,
    ADD COLUMN `error_message` VARCHAR(512) DEFAULT NULL COMMENT '失败原因' AFTER `token_used`,
    ADD COLUMN `started_at` DATETIME DEFAULT NULL COMMENT '开始时间' AFTER `error_message`,
    ADD COLUMN `finished_at` DATETIME DEFAULT NULL COMMENT '完成时间' AFTER `started_at`;

ALTER TABLE `ai_match_record`
    ADD KEY `idx_match_status` (`match_status`);

-- -----------------------------------------------------------------------------
-- ai_model：默认大语言模型（qwen-max）
-- -----------------------------------------------------------------------------
INSERT INTO `ai_model` (`model_code`, `model_name`, `model_type`, `purpose`, `api_endpoint`, `status`)
VALUES ('qwen-max', '通义千问 Qwen-Max', 1, '简历解析与人岗匹配', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 1)
ON DUPLICATE KEY UPDATE
    `model_name`   = VALUES(`model_name`),
    `model_type`   = VALUES(`model_type`),
    `purpose`      = VALUES(`purpose`),
    `api_endpoint` = VALUES(`api_endpoint`),
    `status`       = VALUES(`status`);
