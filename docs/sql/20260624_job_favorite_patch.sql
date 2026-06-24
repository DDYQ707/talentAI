-- =============================================================================
-- 候选人岗位收藏表
-- 执行：mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/20260624_job_favorite_patch.sql
-- =============================================================================

SET NAMES utf8mb4;
USE `talent_job_db`;

CREATE TABLE IF NOT EXISTS `job_favorite` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `candidate_id`  BIGINT UNSIGNED NOT NULL COMMENT '候选人用户ID（逻辑外键->auth库）',
  `job_id`        BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_candidate_job` (`candidate_id`, `job_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_fav_job` FOREIGN KEY (`job_id`) REFERENCES `job_post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人岗位收藏表';
