-- =============================================================================
-- talent-auth 服务独立库 DDL
-- 库名：talent_auth_db
-- 微服务：talent-auth (8081)
-- 主要表：sys_*、auth_verification_code、candidate_profile、sys_notification
--
-- 执行（项目根目录）：
--   mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/talent_auth_schema.sql
-- Docker 映射端口为 3307 时，将 -P3306 改为 -P3307
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `talent_auth_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `talent_auth_db`;

DROP TABLE IF EXISTS `sys_notification`;
DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_permission`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `candidate_profile`;
DROP TABLE IF EXISTS `auth_verification_code`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_department`;

CREATE TABLE `sys_department` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父部门ID，0为顶级',
  `dept_name`     VARCHAR(64)     NOT NULL COMMENT '部门名称',
  `dept_code`     VARCHAR(32)     DEFAULT NULL COMMENT '部门编码',
  `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '排序号',
  `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_dept_name` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

CREATE TABLE `sys_user` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(64)     DEFAULT NULL COMMENT '登录用户名',
  `phone`           VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
  `email`           VARCHAR(128)    DEFAULT NULL COMMENT '邮箱',
  `password_hash`   VARCHAR(128)    DEFAULT NULL COMMENT 'BCrypt密码哈希',
  `nickname`        VARCHAR(64)     DEFAULT NULL COMMENT '显示昵称',
  `avatar_url`      VARCHAR(512)    DEFAULT NULL COMMENT '头像URL',
  `user_type`       TINYINT         NOT NULL COMMENT '门户分流：1-候选人 2-HR 3-面试官 4-管理员',
  `dept_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '所属部门ID',
  `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '账号状态：0-禁用 1-正常 2-锁定',
  `last_login_at`   DATETIME        DEFAULT NULL COMMENT '最后登录时间',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_user_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_department` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE `sys_role` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code`     VARCHAR(32)     NOT NULL COMMENT '角色编码',
  `role_name`     VARCHAR(64)     NOT NULL COMMENT '角色名称',
  `description`   VARCHAR(256)    DEFAULT NULL COMMENT '角色描述',
  `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `sys_permission` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父权限ID',
  `perm_code`     VARCHAR(64)     NOT NULL COMMENT '权限编码',
  `perm_name`     VARCHAR(64)     NOT NULL COMMENT '权限名称',
  `module_name`   VARCHAR(32)     NOT NULL COMMENT '所属模块',
  `perm_type`     TINYINT         NOT NULL DEFAULT 2 COMMENT '1-目录 2-操作 3-API',
  `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`),
  KEY `idx_module_name` (`module_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE `sys_user_role` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id`       BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE `sys_role_permission` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id`         BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `permission_id`   BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`),
  CONSTRAINT `fk_rp_perm` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

CREATE TABLE `candidate_profile` (
  `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`               BIGINT UNSIGNED NOT NULL COMMENT '候选人用户ID',
  `real_name`             VARCHAR(64)     DEFAULT NULL COMMENT '真实姓名',
  `gender`                TINYINT         DEFAULT NULL COMMENT '性别：0-未知 1-男 2-女',
  `birth_date`            DATE            DEFAULT NULL COMMENT '出生日期',
  `current_title`         VARCHAR(128)    DEFAULT NULL COMMENT '当前职位',
  `work_years`            DECIMAL(3,1)    DEFAULT NULL COMMENT '工作年限',
  `highest_edu`           TINYINT         DEFAULT NULL COMMENT '最高学历：1-大专 2-本科 3-硕士 4-博士',
  `city`                  VARCHAR(32)     DEFAULT NULL COMMENT '所在城市',
  `job_seeking_status`    TINYINT         DEFAULT NULL COMMENT '求职状态：1-在职观望 2-主动求职 3-被动求职',
  `resume_completeness`   TINYINT         NOT NULL DEFAULT 0 COMMENT '简历完整度0-100',
  `ai_score`              TINYINT         DEFAULT NULL COMMENT 'AI简历评分0-100',
  `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`            TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_city` (`city`),
  KEY `idx_job_seeking_status` (`job_seeking_status`),
  CONSTRAINT `fk_cp_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人扩展档案表';

CREATE TABLE `auth_verification_code` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account`       VARCHAR(128)    NOT NULL COMMENT '手机号或邮箱',
  `code`          VARCHAR(8)      NOT NULL COMMENT '验证码',
  `code_type`     TINYINT         NOT NULL DEFAULT 1 COMMENT '1-登录 2-注册 3-重置密码',
  `expire_at`     DATETIME        NOT NULL COMMENT '过期时间',
  `is_used`       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已使用',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_account_type` (`account`, `code_type`),
  KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码表';

CREATE TABLE `sys_notification` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '接收人ID',
  `title`         VARCHAR(128)    NOT NULL COMMENT '标题',
  `content`       VARCHAR(512)    DEFAULT NULL COMMENT '内容',
  `notify_type`   TINYINT         NOT NULL COMMENT '1-待办 2-提醒 3-公告',
  `biz_type`      VARCHAR(32)     DEFAULT NULL COMMENT '业务类型',
  `biz_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '业务ID',
  `is_read`       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已读',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_notify_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知待办表';

SET FOREIGN_KEY_CHECKS = 1;

-- 基础种子数据（部门、角色）
INSERT INTO `sys_department` (`id`, `parent_id`, `dept_name`, `dept_code`, `sort_order`) VALUES
(1, 0, '技术部', 'TECH', 1),
(2, 0, '产品部', 'PRODUCT', 2),
(3, 0, '运营部', 'OPS', 3),
(4, 0, '设计部', 'DESIGN', 4),
(5, 0, '市场部', 'MARKET', 5);

INSERT INTO `sys_role` (`role_code`, `role_name`, `description`, `sort_order`) VALUES
('SUPER_ADMIN', '超级管理员', '系统最高权限', 1),
('HR_ADMIN', 'HR管理员', '招聘全流程管理', 2),
('INTERVIEWER', '面试官', '面试任务与评价', 3),
('DEPT_MANAGER', '部门负责人', '部门进展与Offer审批', 4),
('READONLY', '只读用户', '仅查看报表', 5);
