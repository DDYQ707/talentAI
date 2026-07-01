-- =============================================================================
-- talent_job_db：Offer 表（与 Offer.java / OfferApproval.java 实体对齐）
-- 若库中尚无 offer 表，执行本脚本；已有旧版表请先备份再执行 V2.1_offer_table_patch.sql
-- =============================================================================

USE `talent_job_db`;

CREATE TABLE IF NOT EXISTS `offer` (
  `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `offer_no`              VARCHAR(32)     NOT NULL COMMENT 'Offer编号',
  `application_id`      BIGINT UNSIGNED NOT NULL COMMENT '投递ID',
  `job_id`                BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  `job_title`             VARCHAR(128)    NOT NULL COMMENT '岗位名称快照',
  `candidate_id`          BIGINT UNSIGNED NOT NULL COMMENT '候选人ID',
  `candidate_name`        VARCHAR(64)     NOT NULL COMMENT '候选人姓名快照',
  `dept_id`               BIGINT UNSIGNED DEFAULT NULL COMMENT '部门ID',
  `dept_name`             VARCHAR(64)     DEFAULT NULL COMMENT '部门名称快照',
  `base_salary`           DECIMAL(12,2)   DEFAULT NULL COMMENT '月薪',
  `annual_salary`         DECIMAL(12,2)   DEFAULT NULL COMMENT '年薪总包',
  `bonus`                 DECIMAL(12,2)   DEFAULT NULL COMMENT '年终奖',
  `salary_remark`         VARCHAR(255)    DEFAULT NULL COMMENT '薪资备注',
  `position_level`        VARCHAR(64)     DEFAULT NULL COMMENT '职级',
  `expected_onboard_date` DATE            DEFAULT NULL COMMENT '预计入职日期',
  `probation_months`      INT             DEFAULT NULL COMMENT '试用期月数',
  `status`                TINYINT         NOT NULL DEFAULT 1 COMMENT 'Offer状态',
  `hr_id`                 BIGINT UNSIGNED DEFAULT NULL COMMENT '发起HR ID',
  `hr_name`               VARCHAR(64)     DEFAULT NULL COMMENT '发起HR姓名',
  `remark`                VARCHAR(512)    DEFAULT NULL COMMENT '备注',
  `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`            TINYINT(1)      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offer_no` (`offer_no`),
  UNIQUE KEY `uk_application_id` (`application_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer表';

CREATE TABLE IF NOT EXISTS `offer_approval` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `offer_id`     BIGINT UNSIGNED NOT NULL COMMENT 'Offer ID',
  `seq`          INT             NOT NULL DEFAULT 1 COMMENT '审批顺序',
  `approver_id`  BIGINT UNSIGNED NOT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(64)    DEFAULT NULL COMMENT '审批人姓名',
  `status`       TINYINT         NOT NULL COMMENT '1-待审批 2-已通过 3-已拒绝',
  `comment`      VARCHAR(512)    DEFAULT NULL COMMENT '审批意见',
  `approved_at`  DATETIME        DEFAULT NULL COMMENT '审批时间',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`   TINYINT(1)      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_offer_id` (`offer_id`),
  KEY `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer审批节点表';
