-- 已有 talent_resume_db 仅含 resume / resume_attachment 时，执行本脚本补充在线简历子表

USE `talent_resume_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `resume_education` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resume_id`     BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
  `school_name`   VARCHAR(128)    NOT NULL COMMENT '学校名称',
  `major`         VARCHAR(64)     DEFAULT NULL COMMENT '专业',
  `degree`        TINYINT         DEFAULT NULL COMMENT '学历',
  `start_date`    DATE            DEFAULT NULL COMMENT '开始日期',
  `end_date`      DATE            DEFAULT NULL COMMENT '结束日期',
  `description`   VARCHAR(512)    DEFAULT NULL COMMENT '补充说明',
  `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_edu_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教育经历表';

CREATE TABLE IF NOT EXISTS `resume_work_experience` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resume_id`         BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
  `company_name`      VARCHAR(128)    NOT NULL COMMENT '公司名称',
  `job_title`         VARCHAR(64)     NOT NULL COMMENT '职位',
  `start_date`        DATE            DEFAULT NULL COMMENT '开始日期',
  `end_date`          DATE            DEFAULT NULL COMMENT '结束日期',
  `job_description`   TEXT            COMMENT '工作描述',
  `sort_order`        INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_work_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作经历表';

CREATE TABLE IF NOT EXISTS `resume_skill` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resume_id`           BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
  `skill_name`          VARCHAR(64)     NOT NULL COMMENT '技能名称',
  `proficiency_level`   TINYINT         DEFAULT NULL COMMENT '熟练度0-100',
  `sort_order`          INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_skill_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能表';

SET FOREIGN_KEY_CHECKS = 1;
