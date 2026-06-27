-- =============================================================================
-- 在线简历 P1：项目经历、证书与荣誉、工作经历类型
-- 库：talent_resume_db
-- 执行：mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/20260627_online_resume_p1_patch.sql
-- =============================================================================

USE `talent_resume_db`;

CREATE TABLE IF NOT EXISTS `resume_project` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resume_id`     BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
  `project_name`  VARCHAR(128)    NOT NULL COMMENT '项目名称',
  `role`          VARCHAR(64)     DEFAULT NULL COMMENT '担任角色',
  `tech_stack`    VARCHAR(256)    DEFAULT NULL COMMENT '技术栈/工具',
  `start_date`    DATE            DEFAULT NULL COMMENT '开始日期',
  `end_date`      DATE            DEFAULT NULL COMMENT '结束日期',
  `description`   TEXT            COMMENT '项目描述',
  `link_url`      VARCHAR(512)    DEFAULT NULL COMMENT '链接/GitHub',
  `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_project_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目经历表';

CREATE TABLE IF NOT EXISTS `resume_certificate` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resume_id`     BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
  `cert_type`     TINYINT         NOT NULL DEFAULT 1 COMMENT '1-证书 2-荣誉 3-职称',
  `name`          VARCHAR(128)    NOT NULL COMMENT '名称',
  `issuer`        VARCHAR(128)    DEFAULT NULL COMMENT '颁发/发证单位',
  `issue_date`    DATE            DEFAULT NULL COMMENT '获得时间',
  `description`   VARCHAR(512)    DEFAULT NULL COMMENT '补充说明',
  `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_certificate_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='证书与荣誉表';

-- 工作经历类型：1-全职 2-实习 3-兼职
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'resume_work_experience'
    AND COLUMN_NAME = 'experience_type'
);
SET @ddl = IF(
  @col_exists = 0,
  'ALTER TABLE `resume_work_experience` ADD COLUMN `experience_type` TINYINT NOT NULL DEFAULT 1 COMMENT ''1-全职 2-实习 3-兼职'' AFTER `job_title`',
  'SELECT ''experience_type already exists'' AS msg'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
