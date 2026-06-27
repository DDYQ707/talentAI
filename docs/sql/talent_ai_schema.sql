-- =============================================================================
-- TalentAI 智能招聘与人才画像分析系统
-- MySQL 8.0 DDL  v1.1  【全量参考版】
-- 字符集: utf8mb4 | 引擎: InnoDB
--
-- ⚠️ 本地/队友初始化请使用按微服务拆分的可执行脚本（含 CREATE DATABASE）：
--     docs/sql/README.md
--     docs/sql/talent_auth_schema.sql
--     docs/sql/talent_job_schema.sql
--     docs/sql/talent_resume_schema.sql
--     docs/sql/talent_interview_schema.sql
--     docs/sql/talent_pool_schema.sql
--     docs/sql/talent_ai_agent_schema.sql
--
-- 本文件用途：设计文档 / ER 总览（单库视角，不含 CREATE DATABASE）
--
-- 微服务说明：
--   - MVP 可单库部署；生产按服务拆库（talent_auth_db / talent_job_db 等）
--   - 跨服务仅保留逻辑外键（存 ID），不建物理 FOREIGN KEY
--   - 展示用名称通过冗余快照字段写入（publisher_name 等），避免跨库 JOIN
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 组织与权限
-- -----------------------------------------------------------------------------

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
  `user_type`       TINYINT         NOT NULL COMMENT '门户分流：1-候选人 2-HR 3-面试官 4-管理员（与RBAC正交，见设计文档§1.6）',
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

-- -----------------------------------------------------------------------------
-- 2. 岗位与投递
-- -----------------------------------------------------------------------------

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招聘岗位表（talent-job库）';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递申请表（talent-job库）';

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

-- -----------------------------------------------------------------------------
-- 3. 简历
-- -----------------------------------------------------------------------------

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历主表（talent-resume库）';

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
  KEY `idx_company_name` (`company_name`),
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
  KEY `idx_skill_name` (`skill_name`),
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

-- -----------------------------------------------------------------------------
-- 4. 面试
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS `interview_evaluation`;
DROP TABLE IF EXISTS `interview`;

CREATE TABLE `interview` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `application_id`    BIGINT UNSIGNED NOT NULL COMMENT '投递ID',
  `job_id`            BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  `candidate_id`      BIGINT UNSIGNED NOT NULL COMMENT '候选人ID（逻辑外键）',
  `candidate_name`    VARCHAR(64)     NOT NULL COMMENT '候选人姓名快照',
  `job_title`           VARCHAR(128)    NOT NULL COMMENT '应聘岗位名称快照',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试安排表（talent-interview库）';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试评价表（talent-interview库）';

-- -----------------------------------------------------------------------------
-- 5. Offer
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS `offer_approval`;
DROP TABLE IF EXISTS `offer`;

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
  `expire_at`         DATETIME        DEFAULT NULL COMMENT 'Offer失效截止时间，超时未接受自动作废',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer表（talent-job库）';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer审批记录表（talent-job库）';

-- -----------------------------------------------------------------------------
-- 6. 人才库
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS `talent_pool_tag`;
DROP TABLE IF EXISTS `talent_tag`;
DROP TABLE IF EXISTS `talent_pool_record`;

CREATE TABLE `talent_pool_record` (
  `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `candidate_id`            BIGINT UNSIGNED NOT NULL COMMENT '候选人ID（逻辑外键）',
  `candidate_name`          VARCHAR(64)     NOT NULL COMMENT '候选人姓名快照',
  `current_title`           VARCHAR(128)    DEFAULT NULL COMMENT '当前职位快照',
  `resume_id`               BIGINT UNSIGNED DEFAULT NULL COMMENT '简历ID（逻辑外键）',
  `source_application_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '来源投递ID',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人才库记录表（talent-pool库）';

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

-- -----------------------------------------------------------------------------
-- 7. AI 相关
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS `ai_chat_message`;
DROP TABLE IF EXISTS `ai_chat_session`;
DROP TABLE IF EXISTS `ai_audit_log`;
DROP TABLE IF EXISTS `ai_talent_profile`;
DROP TABLE IF EXISTS `ai_interview_question`;
DROP TABLE IF EXISTS `ai_match_record`;
DROP TABLE IF EXISTS `ai_resume_parse_result`;
DROP TABLE IF EXISTS `ai_parse_task`;
DROP TABLE IF EXISTS `ai_model`;

CREATE TABLE `ai_model` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_code`      VARCHAR(64)     NOT NULL COMMENT '模型编码',
  `model_name`      VARCHAR(128)    NOT NULL COMMENT '展示名称',
  `model_type`      TINYINT         NOT NULL COMMENT '1-语言 2-嵌入 3-文档解析',
  `purpose`         VARCHAR(128)    DEFAULT NULL COMMENT '使用场景',
  `api_endpoint`    VARCHAR(256)    DEFAULT NULL COMMENT 'API地址',
  `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1-运行中 2-暂停',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_code` (`model_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型配置表';

CREATE TABLE `ai_parse_task` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `attachment_id`   BIGINT UNSIGNED NOT NULL COMMENT '附件ID',
  `resume_id`       BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
  `model_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '模型ID',
  `task_status`     TINYINT         NOT NULL DEFAULT 0 COMMENT '0-待处理 1-处理中 2-成功 3-失败',
  `error_message`   VARCHAR(512)    DEFAULT NULL COMMENT '失败原因',
  `started_at`      DATETIME        DEFAULT NULL COMMENT '开始时间',
  `finished_at`     DATETIME        DEFAULT NULL COMMENT '完成时间',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_attachment_id` (`attachment_id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_task_status` (`task_status`),
  CONSTRAINT `fk_task_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历解析任务表（talent-ai库）';

CREATE TABLE `ai_resume_parse_result` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`         BIGINT UNSIGNED NOT NULL COMMENT '任务ID',
  `resume_id`       BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
  `parsed_json`     JSON            DEFAULT NULL COMMENT '完整解析JSON',
  `basic_info`      JSON            DEFAULT NULL COMMENT '基本信息',
  `education_json`  JSON            DEFAULT NULL COMMENT '教育经历',
  `work_json`       JSON            DEFAULT NULL COMMENT '工作经历',
  `skills_json`     JSON            DEFAULT NULL COMMENT '技能',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_result_task` FOREIGN KEY (`task_id`) REFERENCES `ai_parse_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历解析结果表（talent-ai库）';

CREATE TABLE `ai_match_record` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `application_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '投递ID（预览匹配可为空）',
  `job_id`            BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  `resume_id`         BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
  `model_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '模型ID',
  `match_score`       TINYINT         NOT NULL COMMENT '匹配度0-100',
  `advantages`        JSON            DEFAULT NULL COMMENT '优势列表',
  `disadvantages`     JSON            DEFAULT NULL COMMENT '不足列表',
  `dimension_scores`  JSON            DEFAULT NULL COMMENT '雷达维度分',
  `token_used`        INT             NOT NULL DEFAULT 0 COMMENT 'Token消耗',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_job_resume` (`job_id`, `resume_id`),
  KEY `idx_match_score` (`match_score`),
  CONSTRAINT `fk_match_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人岗匹配记录表（talent-ai库）';

CREATE TABLE `ai_interview_question` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `interview_id`    BIGINT UNSIGNED NOT NULL COMMENT '面试ID',
  `question_text`   TEXT            NOT NULL COMMENT '问题内容',
  `category`        VARCHAR(32)     DEFAULT NULL COMMENT '分类',
  `focus_point`     VARCHAR(256)    DEFAULT NULL COMMENT '考察重点',
  `sort_order`      INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `model_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '生成模型',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_interview_id` (`interview_id`),
  CONSTRAINT `fk_q_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI面试题表（talent-ai库）';

CREATE TABLE `ai_talent_profile` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `candidate_id`      BIGINT UNSIGNED NOT NULL COMMENT '候选人ID',
  `application_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '投递ID',
  `profile_summary`   TEXT            NOT NULL COMMENT '综合画像约200字',
  `profile_tags`      JSON            DEFAULT NULL COMMENT '画像标签',
  `model_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '生成模型',
  `version`           INT             NOT NULL DEFAULT 1 COMMENT '版本号',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_application_id` (`application_id`),
  CONSTRAINT `fk_profile_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI人才画像表（talent-ai库）';

CREATE TABLE `ai_audit_log` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`           BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
  `user_name`         VARCHAR(64)     DEFAULT NULL COMMENT '操作人姓名',
  `role_name`         VARCHAR(64)     DEFAULT NULL COMMENT '角色名称',
  `action_type`       VARCHAR(32)     NOT NULL COMMENT '操作类型',
  `target_type`       VARCHAR(32)     DEFAULT NULL COMMENT '对象类型',
  `target_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '对象ID',
  `target_desc`       VARCHAR(256)    DEFAULT NULL COMMENT '对象描述',
  `model_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '模型ID',
  `token_used`        INT             NOT NULL DEFAULT 0 COMMENT 'Token消耗',
  `result_status`     TINYINT         NOT NULL DEFAULT 1 COMMENT '1-成功 2-失败',
  `risk_level`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1-低 2-高 3-极高',
  `request_payload`   JSON            DEFAULT NULL COMMENT '请求摘要',
  `response_payload`  JSON            DEFAULT NULL COMMENT '响应摘要',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_risk_level` (`risk_level`),
  CONSTRAINT `fk_audit_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI操作审计日志表（talent-ai库）';

-- -----------------------------------------------------------------------------
-- 8. 辅助表
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS `sys_notification`;

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

CREATE TABLE `ai_chat_session` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         BIGINT UNSIGNED NOT NULL COMMENT 'HR用户ID',
  `session_title`   VARCHAR(128)    DEFAULT NULL COMMENT '会话标题',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手会话表（talent-ai库）';

CREATE TABLE `ai_chat_message` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`    BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `role`          TINYINT         NOT NULL COMMENT '1-user 2-assistant',
  `content`       TEXT            NOT NULL COMMENT '消息内容',
  `extra_data`    JSON            DEFAULT NULL COMMENT '附加数据',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_msg_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手消息表';

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 初始化种子数据（可选）
-- -----------------------------------------------------------------------------

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

INSERT INTO `ai_model` (`model_code`, `model_name`, `model_type`, `purpose`, `status`) VALUES
('qwen-max', '通义千问 Qwen-Max', 1, '简历解析/匹配/画像/面试题', 1),
('text-embedding-v2', '向量嵌入模型 v2', 2, '语义检索与匹配', 1),
('cv-parser-pro', 'CV-Parser Pro', 3, 'PDF/Word简历结构化解析', 1);
