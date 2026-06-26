-- =============================================================================
-- 面试官 AI 笔记表
-- =============================================================================

SET NAMES utf8mb4;
USE `talent_ai_db`;

CREATE TABLE IF NOT EXISTS `ai_interview_note` (
  `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `interview_id`            BIGINT UNSIGNED NOT NULL COMMENT '面试ID（逻辑外键->interview库）',
  `interviewer_id`          BIGINT UNSIGNED NOT NULL COMMENT '面试官用户ID',
  `note_content`            TEXT            DEFAULT NULL COMMENT '面试过程笔记',
  `ai_summary`              TEXT            DEFAULT NULL COMMENT 'AI生成的评估摘要草稿',
  `ai_suggested_score`      TINYINT         DEFAULT NULL COMMENT 'AI建议综合分0-100',
  `ai_suggested_conclusion` TINYINT         DEFAULT NULL COMMENT '1-通过 2-待定 3-不通过',
  `ai_dimension_scores`     JSON            DEFAULT NULL COMMENT 'AI建议维度分',
  `ai_highlights`           JSON            DEFAULT NULL COMMENT 'AI捕捉的关键信号',
  `model_id`                BIGINT UNSIGNED DEFAULT NULL COMMENT '生成模型',
  `created_at`              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`              TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interview_interviewer` (`interview_id`, `interviewer_id`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_interviewer_id` (`interviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试官AI笔记表';
