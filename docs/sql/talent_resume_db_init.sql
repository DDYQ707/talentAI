-- talent-resume 独立库初始化（MinIO 附件上传 MVP）
-- 执行前请确认 MySQL 可访问，并按需修改账号密码

CREATE DATABASE IF NOT EXISTS `talent_resume_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `talent_resume_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `resume_skill`;
DROP TABLE IF EXISTS `resume_work_experience`;
DROP TABLE IF EXISTS `resume_education`;
DROP TABLE IF EXISTS `resume_attachment`;
DROP TABLE IF EXISTS `resume`;

CREATE TABLE `resume` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `candidate_id`    BIGINT UNSIGNED NOT NULL COMMENT '候选人用户ID',
  `resume_name`     VARCHAR(64)     NOT NULL COMMENT '简历名称',
  `is_default`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否默认简历',
  `summary`         TEXT            COMMENT '个人总结',
  `parse_status`    TINYINT         NOT NULL DEFAULT 0 COMMENT '0-未解析 1-解析中 2-成功 3-失败',
  `screen_status`   TINYINT         NOT NULL DEFAULT 1 COMMENT '1-待初筛 2-面试中 3-已录用 4-已淘汰',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_screen_status` (`screen_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历主表';

CREATE TABLE `resume_education` (
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

CREATE TABLE `resume_work_experience` (
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

CREATE TABLE `resume_skill` (
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

CREATE TABLE `resume_attachment` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resume_id`       BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
  `file_name`       VARCHAR(256)    NOT NULL COMMENT '原始文件名',
  `file_type`       VARCHAR(16)     NOT NULL COMMENT '文件类型',
  `file_size`       BIGINT          NOT NULL DEFAULT 0 COMMENT '文件大小字节',
  `bucket_name`     VARCHAR(64)     NOT NULL COMMENT 'MinIO桶名',
  `object_key`      VARCHAR(512)    NOT NULL COMMENT 'MinIO对象键',
  `file_url`        VARCHAR(512)    DEFAULT NULL COMMENT '访问URL',
  `upload_status`   TINYINT         NOT NULL DEFAULT 1 COMMENT '1-成功 2-失败',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_attach_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历附件表';

SET FOREIGN_KEY_CHECKS = 1;
