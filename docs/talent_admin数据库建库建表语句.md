CREATE DATABASE `talent_admin_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 1. 企业资质审核表
CREATE TABLE `talent_enterprise_audit` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `company_name` VARCHAR(100) NOT NULL COMMENT '企业名称',
    `credit_code` VARCHAR(18) NOT NULL COMMENT '统一社会信用代码',
    `legal_person` VARCHAR(20) DEFAULT NULL COMMENT '法定代表人',
    `registered_capital` VARCHAR(50) DEFAULT NULL COMMENT '注册资本',
    `business_scope` TEXT DEFAULT NULL COMMENT '经营范围',
    `license_url` VARCHAR(255) DEFAULT NULL COMMENT '营业执照图片URL',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=待审核, 1=已通过, 2=已驳回',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回理由',
    `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_credit_code` (`credit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业资质审核表';

-- 2. 数据字典类型表
CREATE TABLE `talent_dict_type` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL COMMENT '字典编码，如 job_industry',
    `name` VARCHAR(50) NOT NULL COMMENT '字典名称，如 行业领域',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=启用, 0=禁用',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典类型表';

-- 3. 数据字典键值表
CREATE TABLE `talent_dict_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `dict_type_id` BIGINT NOT NULL COMMENT '关联类型ID',
    `label` VARCHAR(100) NOT NULL COMMENT '字典标签，如 互联网/IT',
    `value` VARCHAR(100) NOT NULL COMMENT '字典键值，如 IT',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=启用, 0=禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type_id` (`dict_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典键值表';

-- 4. 职位风控日志表
CREATE TABLE `talent_job_risk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `job_title` VARCHAR(100) NOT NULL COMMENT '职位名称',
    `company_name` VARCHAR(100) NOT NULL COMMENT '企业名称',
    `publisher_id` BIGINT NOT NULL COMMENT '发布者ID',
    `salary_min` INT DEFAULT NULL,
    `salary_max` INT DEFAULT NULL,
    `description` TEXT COMMENT '职位描述',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=正常, 1=风险预警, 2=已下架',
    `risk_keywords` VARCHAR(255) DEFAULT NULL COMMENT '触发的风险高危词',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `taken_down_at` DATETIME DEFAULT NULL COMMENT '下架时间',
    `taken_down_by` BIGINT DEFAULT NULL COMMENT '下架操作人ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位风控日志表';

USE talent_admin_db;

-- ---------------------------------------------------------------------
-- 轮播图 banner
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
`id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`title`      VARCHAR(128) NOT NULL COMMENT '标题',
`image_url`  VARCHAR(512) NOT NULL COMMENT '图片URL',
`link_url`   VARCHAR(512) DEFAULT NULL COMMENT '跳转链接',
`start_time` DATETIME     DEFAULT NULL COMMENT '开始时间',
`end_time`   DATETIME     DEFAULT NULL COMMENT '结束时间',
`status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=下线, 1=上线',
`sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
`created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图';

INSERT INTO `banner` (`title`, `image_url`, `link_url`, `start_time`, `end_time`, `status`, `sort_order`, `created_at`) VALUES
('春季招聘会火热进行中', 'https://picsum.photos/seed/banner1/1200/400', 'https://www.talent-ai.com/fair/spring', '2026-03-01 00:00:00', '2026-04-30 23:59:59', 1, 1, '2026-02-25 10:00:00'),
('AI 简历优化新功能上线', 'https://picsum.photos/seed/banner2/1200/400', 'https://www.talent-ai.com/feature/ai-resume', '2026-03-10 00:00:00', '2026-06-30 23:59:59', 1, 2, '2026-03-08 09:30:00'),
('企业入驻专享福利', 'https://picsum.photos/seed/banner3/1200/400', 'https://www.talent-ai.com/enterprise/welcome', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 0, 3, '2025-12-28 14:20:00');

-- ---------------------------------------------------------------------
-- 系统公告 announcement
-- level : info / warning / critical
-- target: candidate / hr / all
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
`id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`title`          VARCHAR(128) NOT NULL COMMENT '标题',
`content`        TEXT         NOT NULL COMMENT '内容',
`level`          VARCHAR(16)  NOT NULL DEFAULT 'info' COMMENT '级别：info/warning/critical',
`target`         VARCHAR(16)  NOT NULL DEFAULT 'all' COMMENT '目标人群：candidate/hr/all',
`broadcasted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已广播：0=否, 1=是',
`broadcasted_at` DATETIME     DEFAULT NULL COMMENT '广播时间',
`created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告';

INSERT INTO `announcement` (`title`, `content`, `level`, `target`, `broadcasted`, `broadcasted_at`, `created_at`) VALUES
('系统维护通知', '平台将于本周日凌晨 02:00-04:00 进行系统升级维护，期间部分功能可能不可用，敬请谅解。', 'warning', 'all', 0, NULL, '2026-03-15 11:00:00'),
('简历投递功能优化', '我们优化了简历投递流程，现在支持一键投递多个职位，欢迎候选人体验。', 'info', 'candidate', 1, '2026-03-12 10:00:00', '2026-03-11 16:40:00'),
('紧急：账号安全提醒', '近期发现钓鱼网站冒充本平台，请 HR 用户务必通过官方域名登录，谨防信息泄露。', 'critical', 'hr', 0, NULL, '2026-03-18 09:15:00');
