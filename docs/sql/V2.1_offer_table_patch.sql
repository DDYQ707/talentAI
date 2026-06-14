-- Offer 及审批表结构同步补丁 (由 B1 模块扩展业务产生)

-- 1. 解除陈旧的物理外键与非空约束
ALTER TABLE `offer` DROP FOREIGN KEY `fk_offer_app`;
ALTER TABLE `offer` DROP FOREIGN KEY `fk_offer_job`;
ALTER TABLE `offer` 
MODIFY COLUMN `created_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人HR ID',
MODIFY COLUMN `created_by_name` VARCHAR(64) DEFAULT NULL COMMENT '创建人姓名快照';

-- 2. 补齐业务层所需的新增字段
ALTER TABLE `offer`
ADD COLUMN `annual_salary` DECIMAL(10,2) COMMENT '年薪总包',
ADD COLUMN `bonus` DECIMAL(10,2) COMMENT '年终奖',
ADD COLUMN `salary_remark` VARCHAR(255) COMMENT '薪资备注',
ADD COLUMN `position_level` VARCHAR(64) COMMENT '职级',
ADD COLUMN `expected_onboard_date` DATE COMMENT '预计入职日期',
ADD COLUMN `probation_months` INT COMMENT '试用期月数',
ADD COLUMN `hr_id` BIGINT UNSIGNED COMMENT 'HR ID',
ADD COLUMN `hr_name` VARCHAR(64) COMMENT 'HR姓名',
ADD COLUMN `remark` VARCHAR(512) COMMENT 'Offer备注';

-- 3. 修正审批表命名与逻辑不一致问题
ALTER TABLE `offer_approval`
ADD COLUMN `seq` INT NOT NULL DEFAULT 1 COMMENT '审批顺序',
CHANGE COLUMN `approval_status` `status` TINYINT NOT NULL COMMENT '1-待审批 2-通过 3-驳回',
CHANGE COLUMN `approval_comment` `comment` VARCHAR(512) DEFAULT NULL COMMENT '审批意见';