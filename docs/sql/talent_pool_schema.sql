-- =============================================================================
-- talent-talent-pool 服务独立库 DDL
-- 库名：talent_pool_db
-- 微服务：talent-talent-pool（规划中，当前后端为骨架模块）
-- 主要表：talent_pool_record、talent_tag、talent_pool_tag
--
-- 执行（项目根目录）：
--   mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_pool_schema.sql
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `talent_pool_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `talent_pool_db`;

DROP TABLE IF EXISTS `talent_pool_tag`;
DROP TABLE IF EXISTS `talent_tag`;
DROP TABLE IF EXISTS `talent_pool_record`;

CREATE TABLE `talent_pool_record` (
  `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `candidate_id`            BIGINT UNSIGNED NOT NULL COMMENT '候选人ID（逻辑外键->auth库）',
  `candidate_name`          VARCHAR(64)     NOT NULL COMMENT '候选人姓名快照',
  `current_title`           VARCHAR(128)    DEFAULT NULL COMMENT '当前职位快照',
  `resume_id`               BIGINT UNSIGNED DEFAULT NULL COMMENT '简历ID（逻辑外键->resume库）',
  `source_application_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '来源投递ID（逻辑外键->job库）',
  `talent_category`         TINYINT         DEFAULT NULL COMMENT '人才分类',
  `job_seeking_status`      TINYINT         DEFAULT NULL COMMENT '求职状态',
  `match_score`             TINYINT         DEFAULT NULL COMMENT '归档时匹配分',
  `current_company`         VARCHAR(128)    DEFAULT NULL COMMENT '当前公司',
  `is_saved`                TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否收藏',
  `archive_reason`          VARCHAR(256)    DEFAULT NULL COMMENT '归档原因',
  `archived_by`             BIGINT UNSIGNED DEFAULT NULL COMMENT '操作HR',
  `archived_at`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  `created_at`              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`              TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_talent_category` (`talent_category`),
  KEY `idx_match_score` (`match_score`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人才库记录表';

CREATE TABLE `talent_tag` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag_name`      VARCHAR(64)     NOT NULL COMMENT '标签名',
  `tag_type`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1-技能 2-领域 3-自定义',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人才标签字典表';

CREATE TABLE `talent_pool_tag` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pool_record_id`  BIGINT UNSIGNED NOT NULL COMMENT '人才库记录ID',
  `tag_id`          BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pool_tag` (`pool_record_id`, `tag_id`),
  CONSTRAINT `fk_pt_pool` FOREIGN KEY (`pool_record_id`) REFERENCES `talent_pool_record` (`id`),
  CONSTRAINT `fk_pt_tag` FOREIGN KEY (`tag_id`) REFERENCES `talent_tag` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人才库标签关联表';

SET FOREIGN_KEY_CHECKS = 1;
