-- =============================================================================
-- talent-interview 服务独立库 DDL
-- 库名：talent_interview_db
-- 微服务：talent-interview (8085)
-- 主要表：interview、interview_evaluation
--
-- 执行（项目根目录）：
--   mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_interview_schema.sql
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `talent_interview_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `talent_interview_db`;

DROP TABLE IF EXISTS `interview_evaluation`;
DROP TABLE IF EXISTS `interview`;

CREATE TABLE `interview` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `application_id`    BIGINT UNSIGNED NOT NULL COMMENT '投递ID',
  `job_id`            BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  `candidate_id`      BIGINT UNSIGNED NOT NULL COMMENT '候选人ID（逻辑外键）',
  `candidate_name`    VARCHAR(64)     NOT NULL COMMENT '候选人姓名快照',
  `job_title`         VARCHAR(128)    NOT NULL COMMENT '应聘岗位名称快照',
  `interviewer_id`    BIGINT UNSIGNED NOT NULL COMMENT '面试官ID（逻辑外键）',
  `interviewer_name`  VARCHAR(64)     NOT NULL COMMENT '面试官姓名快照',
  `round_no`          TINYINT         NOT NULL DEFAULT 1 COMMENT '轮次序号',
  `round_type`        TINYINT         NOT NULL COMMENT '轮次类型',
  `interview_mode`    TINYINT         NOT NULL DEFAULT 1 COMMENT '1-视频 2-现场 3-线上评审',
  `scheduled_start`   DATETIME        NOT NULL COMMENT '计划开始时间',
  `scheduled_end`     DATETIME        DEFAULT NULL COMMENT '计划结束时间',
  `meeting_url`       VARCHAR(512)    DEFAULT NULL COMMENT '视频会议链接',
  `location`          VARCHAR(128)    DEFAULT NULL COMMENT '现场地址',
  `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '1-待进行 2-已完成 3-待安排 4-已取消',
  `total_score`       DECIMAL(4,1)    DEFAULT NULL COMMENT '综合得分',
  `created_by`        BIGINT UNSIGNED DEFAULT NULL COMMENT '安排人HR ID（逻辑外键）',
  `created_by_name`   VARCHAR(64)     DEFAULT NULL COMMENT '安排人姓名快照',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_interviewer_id` (`interviewer_id`),
  KEY `idx_scheduled_start` (`scheduled_start`),
  KEY `idx_status` (`status`),
  KEY `idx_job_candidate` (`job_id`, `candidate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试安排表';

CREATE TABLE `interview_evaluation` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `interview_id`      BIGINT UNSIGNED NOT NULL COMMENT '面试ID',
  `evaluator_id`      BIGINT UNSIGNED NOT NULL COMMENT '评价人ID（逻辑外键）',
  `evaluator_name`    VARCHAR(64)     NOT NULL COMMENT '评价人姓名快照',
  `dimension_scores`  JSON            DEFAULT NULL COMMENT '多维评分JSON',
  `overall_score`     DECIMAL(4,1)    DEFAULT NULL COMMENT '综合评分',
  `conclusion`        TINYINT         DEFAULT NULL COMMENT '1-通过 2-待定 3-不通过',
  `comment`           TEXT            COMMENT '文字评价',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_evaluator_id` (`evaluator_id`),
  CONSTRAINT `fk_eval_interview` FOREIGN KEY (`interview_id`) REFERENCES `interview` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试评价表';

SET FOREIGN_KEY_CHECKS = 1;
