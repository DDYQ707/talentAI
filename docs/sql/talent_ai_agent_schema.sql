-- =============================================================================
-- talent-ai-agent 服务独立库 DDL
-- 库名：talent_ai_db
-- 微服务：talent-ai-agent (8084)
-- 主要表：ai_model、ai_parse_task、ai_resume_parse_result、ai_match_record 等
--
-- 说明：已合并 20260601_ai_sprint1_patch.sql 中的字段扩展，无需再单独执行补丁。
--
-- 执行（项目根目录）：
--   mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_ai_agent_schema.sql
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `talent_ai_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `talent_ai_db`;

DROP TABLE IF EXISTS `ai_chat_message`;
DROP TABLE IF EXISTS `ai_chat_session`;
DROP TABLE IF EXISTS `ai_audit_log`;
DROP TABLE IF EXISTS `ai_talent_profile`;
DROP TABLE IF EXISTS `ai_interview_question`;
DROP TABLE IF EXISTS `ai_match_record`;
DROP TABLE IF EXISTS `ai_resume_parse_result`;
DROP TABLE IF EXISTS `ai_parse_task`;
DROP TABLE IF EXISTS `ai_model`;

CREATE TABLE `ai_model` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_code`      VARCHAR(64)     NOT NULL COMMENT '模型编码',
  `model_name`      VARCHAR(128)    NOT NULL COMMENT '展示名称',
  `model_type`      TINYINT         NOT NULL COMMENT '1-语言 2-嵌入 3-文档解析',
  `purpose`         VARCHAR(128)    DEFAULT NULL COMMENT '使用场景',
  `api_endpoint`    VARCHAR(256)    DEFAULT NULL COMMENT 'API地址',
  `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1-运行中 2-暂停',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_code` (`model_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型配置表';

CREATE TABLE `ai_parse_task` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `attachment_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '附件ID（在线简历可为空，逻辑外键->resume库）',
  `resume_id`       BIGINT UNSIGNED NOT NULL COMMENT '简历ID（逻辑外键->resume库）',
  `application_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '投递ID（逻辑外键->job库）',
  `candidate_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '候选人ID（逻辑外键->auth库）',
  `model_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '模型ID',
  `task_status`     TINYINT         NOT NULL DEFAULT 0 COMMENT '0-待处理 1-处理中 2-成功 3-失败',
  `error_message`   VARCHAR(512)    DEFAULT NULL COMMENT '失败原因',
  `raw_text_length` INT             DEFAULT 0 COMMENT '抽取文本长度',
  `file_name`       VARCHAR(255)    DEFAULT NULL COMMENT '文件名',
  `file_type`       VARCHAR(32)     DEFAULT NULL COMMENT '文件类型',
  `started_at`      DATETIME        DEFAULT NULL COMMENT '开始时间',
  `finished_at`     DATETIME        DEFAULT NULL COMMENT '完成时间',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_attachment_id` (`attachment_id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_task_status` (`task_status`),
  CONSTRAINT `fk_task_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历解析任务表';

CREATE TABLE `ai_resume_parse_result` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`             BIGINT UNSIGNED NOT NULL COMMENT '任务ID',
  `resume_id`           BIGINT UNSIGNED NOT NULL COMMENT '简历ID（逻辑外键->resume库）',
  `parsed_json`         JSON            DEFAULT NULL COMMENT '完整解析JSON',
  `basic_info`          JSON            DEFAULT NULL COMMENT '基本信息',
  `education_json`      JSON            DEFAULT NULL COMMENT '教育经历',
  `work_json`           JSON            DEFAULT NULL COMMENT '工作经历',
  `skills_json`         JSON            DEFAULT NULL COMMENT '技能',
  `project_json`        JSON            DEFAULT NULL COMMENT '项目经历',
  `certificate_json`    JSON            DEFAULT NULL COMMENT '证书',
  `total_years`         DECIMAL(4,1)    DEFAULT NULL COMMENT '总工作年限',
  `industry_keywords`   JSON            DEFAULT NULL COMMENT '行业关键词',
  `target_position`     VARCHAR(128)    DEFAULT NULL COMMENT '目标岗位',
  `raw_text_length`     INT             DEFAULT 0 COMMENT '抽取文本长度',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_result_task` FOREIGN KEY (`task_id`) REFERENCES `ai_parse_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历解析结果表';

CREATE TABLE `ai_match_record` (
  `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `application_id`        BIGINT UNSIGNED NOT NULL COMMENT '投递ID（逻辑外键->job库）',
  `job_id`                BIGINT UNSIGNED NOT NULL COMMENT '岗位ID（逻辑外键->job库）',
  `resume_id`             BIGINT UNSIGNED NOT NULL COMMENT '简历ID（逻辑外键->resume库）',
  `model_id`              BIGINT UNSIGNED DEFAULT NULL COMMENT '模型ID',
  `match_score`           TINYINT         NOT NULL DEFAULT 0 COMMENT '匹配度0-100',
  `match_status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '0-待处理 1-处理中 2-成功 3-失败',
  `match_level`           VARCHAR(32)     DEFAULT NULL COMMENT '匹配等级',
  `match_reason`          TEXT            DEFAULT NULL COMMENT '匹配理由',
  `advantages`            JSON            DEFAULT NULL COMMENT '优势列表',
  `disadvantages`         JSON            DEFAULT NULL COMMENT '不足列表',
  `suggested_questions`   JSON            DEFAULT NULL COMMENT '建议面试问题',
  `dimension_scores`      JSON            DEFAULT NULL COMMENT '雷达维度分',
  `token_used`            INT             NOT NULL DEFAULT 0 COMMENT 'Token消耗',
  `error_message`         VARCHAR(512)    DEFAULT NULL COMMENT '失败原因',
  `started_at`            DATETIME        DEFAULT NULL COMMENT '开始时间',
  `finished_at`           DATETIME        DEFAULT NULL COMMENT '完成时间',
  `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`            TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_job_resume` (`job_id`, `resume_id`),
  KEY `idx_match_score` (`match_score`),
  KEY `idx_match_status` (`match_status`),
  CONSTRAINT `fk_match_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人岗匹配记录表';

CREATE TABLE `ai_interview_question` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `interview_id`    BIGINT UNSIGNED NOT NULL COMMENT '面试ID（逻辑外键->interview库）',
  `question_text`   TEXT            NOT NULL COMMENT '问题内容',
  `category`        VARCHAR(32)     DEFAULT NULL COMMENT '分类',
  `focus_point`     VARCHAR(256)    DEFAULT NULL COMMENT '考察重点',
  `sort_order`      INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `model_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '生成模型',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_interview_id` (`interview_id`),
  CONSTRAINT `fk_q_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI面试题表';

CREATE TABLE `ai_talent_profile` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `candidate_id`      BIGINT UNSIGNED NOT NULL COMMENT '候选人ID（逻辑外键->auth库）',
  `application_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '投递ID（逻辑外键->job库）',
  `profile_summary`   TEXT            NOT NULL COMMENT '综合画像约200字',
  `profile_tags`      JSON            DEFAULT NULL COMMENT '画像标签',
  `model_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '生成模型',
  `version`           INT             NOT NULL DEFAULT 1 COMMENT '版本号',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_application_id` (`application_id`),
  CONSTRAINT `fk_profile_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI人才画像表';

CREATE TABLE `ai_audit_log` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`           BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID（逻辑外键->auth库）',
  `user_name`         VARCHAR(64)     DEFAULT NULL COMMENT '操作人姓名',
  `role_name`         VARCHAR(64)     DEFAULT NULL COMMENT '角色名称',
  `action_type`       VARCHAR(32)     NOT NULL COMMENT '操作类型',
  `target_type`       VARCHAR(32)     DEFAULT NULL COMMENT '对象类型',
  `target_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '对象ID',
  `target_desc`       VARCHAR(256)    DEFAULT NULL COMMENT '对象描述',
  `model_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '模型ID',
  `token_used`        INT             NOT NULL DEFAULT 0 COMMENT 'Token消耗',
  `result_status`     TINYINT         NOT NULL DEFAULT 1 COMMENT '1-成功 2-失败',
  `risk_level`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1-低 2-高 3-极高',
  `request_payload`   JSON            DEFAULT NULL COMMENT '请求摘要',
  `response_payload`  JSON            DEFAULT NULL COMMENT '响应摘要',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_risk_level` (`risk_level`),
  CONSTRAINT `fk_audit_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI操作审计日志表';

CREATE TABLE `ai_chat_session` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         BIGINT UNSIGNED NOT NULL COMMENT 'HR用户ID（逻辑外键->auth库）',
  `session_title`   VARCHAR(128)    DEFAULT NULL COMMENT '会话标题',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手会话表';

CREATE TABLE `ai_chat_message` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`    BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `role`          TINYINT         NOT NULL COMMENT '1-user 2-assistant',
  `content`       TEXT            NOT NULL COMMENT '消息内容',
  `extra_data`    JSON            DEFAULT NULL COMMENT '附加数据',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_msg_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手消息表';

SET FOREIGN_KEY_CHECKS = 1;

-- 默认 AI 模型
INSERT INTO `ai_model` (`model_code`, `model_name`, `model_type`, `purpose`, `api_endpoint`, `status`) VALUES
('qwen-max', '通义千问 Qwen-Max', 1, '简历解析/匹配/画像/面试题', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 1),
('text-embedding-v2', '向量嵌入模型 v2', 2, '语义检索与匹配', NULL, 1),
('cv-parser-pro', 'CV-Parser Pro', 3, 'PDF/Word简历结构化解析', NULL, 1)
ON DUPLICATE KEY UPDATE
  `model_name`   = VALUES(`model_name`),
  `model_type`   = VALUES(`model_type`),
  `purpose`      = VALUES(`purpose`),
  `api_endpoint` = VALUES(`api_endpoint`),
  `status`       = VALUES(`status`);
