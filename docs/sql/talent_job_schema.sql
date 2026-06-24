-- =============================================================================
-- talent-job 服务独立库 DDL
-- 库名：talent_job_db
-- 微服务：talent-job (8082)
-- 主要表：job_post、job_application、application_stage_log、offer、offer_approval
--
-- 执行（项目根目录）：
--   mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_job_schema.sql
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `talent_job_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `talent_job_db`;

DROP TABLE IF EXISTS `offer_approval`;
DROP TABLE IF EXISTS `offer`;
DROP TABLE IF EXISTS `application_stage_log`;
DROP TABLE IF EXISTS `job_application`;
DROP TABLE IF EXISTS `job_post`;

CREATE TABLE `job_post` (
  `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `job_code`                VARCHAR(32)     DEFAULT NULL COMMENT '岗位编号',
  `title`                   VARCHAR(128)    NOT NULL COMMENT '岗位名称',
  `dept_id`                 BIGINT UNSIGNED NOT NULL COMMENT '所属部门ID（逻辑外键->auth库）',
  `dept_name`               VARCHAR(64)     NOT NULL COMMENT '部门名称快照',
  `publisher_id`            BIGINT UNSIGNED NOT NULL COMMENT '发布人HR用户ID（逻辑外键->auth库）',
  `publisher_name`          VARCHAR(64)     NOT NULL COMMENT '发布人姓名快照',
  `status`                  TINYINT         NOT NULL DEFAULT 1 COMMENT '1-招聘中 2-暂停 3-已完成',
  `employment_type`         TINYINT         NOT NULL DEFAULT 1 COMMENT '1-全职 2-兼职 3-实习',
  `work_city`               VARCHAR(32)     DEFAULT NULL COMMENT '工作城市',
  `is_remote`               TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否远程',
  `salary_min`              INT             DEFAULT NULL COMMENT '薪资下限元/月',
  `salary_max`              INT             DEFAULT NULL COMMENT '薪资上限元/月',
  `salary_negotiable`       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否面议',
  `headcount`               INT             NOT NULL DEFAULT 1 COMMENT '招聘人数',
  `priority`                TINYINT         NOT NULL DEFAULT 2 COMMENT '1-高 2-中 3-低',
  `experience_years_min`    TINYINT         DEFAULT NULL COMMENT '最低工作年限',
  `education_requirement`   TINYINT         DEFAULT NULL COMMENT '学历要求',
  `job_description`         TEXT            COMMENT '职位描述JD',
  `job_requirements`        TEXT            COMMENT '任职要求',
  `skill_tags`              VARCHAR(512)    DEFAULT NULL COMMENT '技能标签逗号分隔',
  `applied_count`           INT             NOT NULL DEFAULT 0 COMMENT '投递数',
  `matched_count`           INT             NOT NULL DEFAULT 0 COMMENT '匹配通过数',
  `published_at`            DATETIME        DEFAULT NULL COMMENT '发布时间',
  `closed_at`               DATETIME        DEFAULT NULL COMMENT '关闭时间',
  `created_at`              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`              TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_work_city` (`work_city`),
  KEY `idx_published_at` (`published_at`),
  FULLTEXT KEY `ft_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招聘岗位表';

CREATE TABLE `job_application` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `application_no`    VARCHAR(32)     NOT NULL COMMENT '投递单号',
  `job_id`            BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  `job_title`         VARCHAR(128)    NOT NULL COMMENT '岗位名称快照',
  `candidate_id`      BIGINT UNSIGNED NOT NULL COMMENT '候选人用户ID（逻辑外键->auth库）',
  `candidate_name`    VARCHAR(64)     NOT NULL COMMENT '候选人姓名快照',
  `resume_id`         BIGINT UNSIGNED NOT NULL COMMENT '简历ID（逻辑外键->resume库）',
  `channel`           TINYINT         DEFAULT NULL COMMENT '渠道：1-BOSS 2-猎头 3-内推 4-智联 5-其他',
  `current_stage`     TINYINT         NOT NULL DEFAULT 1 COMMENT '当前招聘阶段',
  `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '1-进行中 2-已录用 3-已淘汰 4-已撤回',
  `match_score`       TINYINT         DEFAULT NULL COMMENT 'AI匹配分0-100',
  `is_starred`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT 'HR标星',
  `applied_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
  `rejected_at`       DATETIME        DEFAULT NULL COMMENT '淘汰时间',
  `hired_at`          DATETIME        DEFAULT NULL COMMENT '录用时间',
  `remark`            VARCHAR(512)    DEFAULT NULL COMMENT 'HR备注',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_status_stage` (`status`, `current_stage`),
  KEY `idx_match_score` (`match_score`),
  KEY `idx_applied_at` (`applied_at`),
  CONSTRAINT `fk_app_job` FOREIGN KEY (`job_id`) REFERENCES `job_post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递申请表';

CREATE TABLE `job_favorite` (
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

CREATE TABLE `application_stage_log` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `application_id`    BIGINT UNSIGNED NOT NULL COMMENT '投递ID',
  `from_stage`        TINYINT         DEFAULT NULL COMMENT '原阶段',
  `to_stage`          TINYINT         NOT NULL COMMENT '新阶段',
  `operator_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID（逻辑外键）',
  `operator_name`     VARCHAR(64)     DEFAULT NULL COMMENT '操作人姓名快照',
  `action_note`       VARCHAR(256)    DEFAULT NULL COMMENT '操作说明',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_stage_app` FOREIGN KEY (`application_id`) REFERENCES `job_application` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递阶段流转日志';

CREATE TABLE `offer` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `offer_no`          VARCHAR(32)     NOT NULL COMMENT 'Offer编号',
  `application_id`    BIGINT UNSIGNED NOT NULL COMMENT '投递ID',
  `job_id`            BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  `candidate_id`      BIGINT UNSIGNED NOT NULL COMMENT '候选人ID（逻辑外键）',
  `candidate_name`    VARCHAR(64)     NOT NULL COMMENT '候选人姓名快照',
  `job_title`         VARCHAR(128)    NOT NULL COMMENT '岗位名称快照',
  `dept_id`           BIGINT UNSIGNED NOT NULL COMMENT '入职部门ID（逻辑外键）',
  `dept_name`         VARCHAR(64)     NOT NULL COMMENT '入职部门名称快照',
  `base_salary`       INT             DEFAULT NULL COMMENT '月薪元',
  `bonus_months`      TINYINT         DEFAULT NULL COMMENT '年终奖月数',
  `start_date`        DATE            DEFAULT NULL COMMENT '预计入职日期',
  `expire_at`         DATETIME        DEFAULT NULL COMMENT 'Offer失效截止时间',
  `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '1-草稿 2-审批中 3-已发送 4-已接受 5-已拒绝 6-已过期',
  `approver_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '当前审批人ID（逻辑外键）',
  `approver_name`     VARCHAR(64)     DEFAULT NULL COMMENT '当前审批人姓名快照',
  `sent_at`           DATETIME        DEFAULT NULL COMMENT '发送时间',
  `accepted_at`       DATETIME        DEFAULT NULL COMMENT '接受时间',
  `expired_at`        DATETIME        DEFAULT NULL COMMENT '实际过期作废时间',
  `created_by`        BIGINT UNSIGNED NOT NULL COMMENT '创建人HR ID（逻辑外键）',
  `created_by_name`   VARCHAR(64)     NOT NULL COMMENT '创建人姓名快照',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offer_no` (`offer_no`),
  UNIQUE KEY `uk_application_id` (`application_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_at` (`expire_at`),
  CONSTRAINT `fk_offer_app` FOREIGN KEY (`application_id`) REFERENCES `job_application` (`id`),
  CONSTRAINT `fk_offer_job` FOREIGN KEY (`job_id`) REFERENCES `job_post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer表';

CREATE TABLE `offer_approval` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `offer_id`          BIGINT UNSIGNED NOT NULL COMMENT 'Offer ID',
  `approver_id`       BIGINT UNSIGNED NOT NULL COMMENT '审批人ID（逻辑外键）',
  `approver_name`     VARCHAR(64)     NOT NULL COMMENT '审批人姓名快照',
  `approval_status`   TINYINT         NOT NULL COMMENT '1-待审批 2-通过 3-驳回',
  `approval_comment`  VARCHAR(512)    DEFAULT NULL COMMENT '审批意见',
  `approved_at`       DATETIME        DEFAULT NULL COMMENT '审批时间',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_offer_id` (`offer_id`),
  KEY `idx_approver_id` (`approver_id`),
  CONSTRAINT `fk_oa_offer` FOREIGN KEY (`offer_id`) REFERENCES `offer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer审批记录表';

SET FOREIGN_KEY_CHECKS = 1;
