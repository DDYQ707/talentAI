-- =============================================================================
-- 简历质量评分表（候选人端 AI 简历评分，与人岗匹配分独立）
-- 执行：mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/20260623_ai_resume_quality_score.sql
-- =============================================================================

SET NAMES utf8mb4;
USE `talent_ai_db`;

CREATE TABLE IF NOT EXISTS `ai_resume_quality_score` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resume_id`         BIGINT UNSIGNED NOT NULL COMMENT '简历ID（逻辑外键->resume库）',
  `candidate_id`      BIGINT UNSIGNED NOT NULL COMMENT '候选人用户ID',
  `parse_task_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '关联解析任务ID',
  `quality_score`     TINYINT         NOT NULL DEFAULT 0 COMMENT '简历质量分0-100',
  `summary`           VARCHAR(512)    DEFAULT NULL COMMENT 'AI综合评价',
  `strengths`         JSON            DEFAULT NULL COMMENT '优势标签',
  `weaknesses`        JSON            DEFAULT NULL COMMENT '不足项',
  `suggestions`       JSON            DEFAULT NULL COMMENT '优化建议',
  `dimension_scores`  JSON            DEFAULT NULL COMMENT '维度分：完整度/清晰度/亮点等',
  `model_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '模型ID',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_parse_task_id` (`parse_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历质量评分记录';
