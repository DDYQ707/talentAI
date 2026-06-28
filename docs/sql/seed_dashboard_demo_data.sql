-- =============================================================================
-- 数据驾驶舱演示数据初始化
-- 用途：为 RecruitDashboardView 提供可展示的测试数据
--
-- 执行顺序：
--   mysql -h 127.0.0.1 -P 3306 -uroot -p < docs/sql/seed_dashboard_demo_data.sql
--
-- 数据效果（驱动舱显示）：
--   本月投递  12     简历库 15
--   面试完成   4      进行中 3
--   Offer发放  3     接受率 67%
--   本月录用   2
--   招聘趋势 近6月有逐月递增数据
--   漏斗：投递→初筛→面试→Offer→录用
--   部门进度：5个部门各有缺口/在招
-- =============================================================================

SET NAMES utf8mb4;

-- ============================================================
-- 0. 创建 HR 账号（如果不存在）
-- ============================================================
USE `talent_auth_db`;

INSERT INTO `sys_user` (
  `username`, `password_hash`, `nickname`, `user_type`, `status`, `is_deleted`
) 
SELECT 'hr_demo', '$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna', '李HR', 2, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_user` WHERE `username` = 'hr_demo' AND `is_deleted` = 0
);

SET @hr_id := (SELECT `id` FROM `sys_user` WHERE `username` = 'hr_demo' AND `is_deleted` = 0 LIMIT 1);
SET @hr_name := '李HR';

-- ============================================================
-- 1. 清空旧数据
-- ============================================================
USE `talent_job_db`;
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM `application_stage_log`;
DELETE FROM `job_application`;
DELETE FROM `job_post`;
ALTER TABLE `job_post` AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;

USE `talent_interview_db`;
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM `interview_evaluation`;
DELETE FROM `interview`;
SET FOREIGN_KEY_CHECKS = 1;

USE `talent_resume_db`;
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM `resume_skill`;
DELETE FROM `resume_work_experience`;
DELETE FROM `resume_education`;
DELETE FROM `resume_attachment`;
DELETE FROM `resume`;
ALTER TABLE `resume` AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 2. 插入部门
-- ============================================================
USE `talent_auth_db`;
INSERT INTO `sys_department` (`id`, `parent_id`, `dept_name`, `dept_code`, `sort_order`, `status`, `is_deleted`)
VALUES
  (1, 0, '技术研发部', 'TECH',     1, 1, 0),
  (2, 0, '产品部',     'PRODUCT',  2, 1, 0),
  (3, 0, '数据智能部', 'DATA_AI',  3, 1, 0),
  (4, 0, '设计部',     'DESIGN',   4, 1, 0),
  (5, 0, '人力资源部', 'HR',       5, 1, 0)
ON DUPLICATE KEY UPDATE
  `dept_name` = VALUES(`dept_name`),
  `dept_code` = VALUES(`dept_code`),
  `status`    = 1,
  `is_deleted`= 0,
  `updated_at`= NOW();

-- ============================================================
-- 3. 插入 10 个岗位（近半年各月都有发布）
-- ============================================================
USE `talent_job_db`;

INSERT INTO `job_post` (
  `job_code`, `title`, `dept_id`, `dept_name`, `publisher_id`, `publisher_name`,
  `status`, `employment_type`, `work_city`, `is_remote`,
  `salary_min`, `salary_max`, `salary_negotiable`, `headcount`, `priority`,
  `experience_years_min`, `education_requirement`,
  `job_description`, `job_requirements`, `skill_tags`,
  `applied_count`, `matched_count`, `published_at`
) VALUES
('JOB202601', '高级前端工程师', 1, '技术研发部', @hr_id, @hr_name,
 1, 1, '北京', 0, 25000, 40000, 0, 2, 1, 3, 2,
 '负责智能招聘平台前端架构与核心页面开发。',
 '精通Vue3/TypeScript；3年以上前端经验。',
 'Vue3,TypeScript,前端工程化', 5, 2, '2026-01-10 09:00:00'),

('JOB202602', 'Java后端开发工程师', 1, '技术研发部', @hr_id, @hr_name,
 1, 1, '北京', 0, 20000, 35000, 0, 2, 2, 3, 2,
 '参与微服务后端研发，负责岗位、简历、投递等核心模块。',
 '熟悉Spring Boot/Cloud/MyBatis-Plus；3年以上经验。',
 'Java,Spring Boot,MySQL,微服务', 3, 1, '2026-02-15 09:00:00'),

('JOB202603', '全栈开发工程师', 1, '技术研发部', @hr_id, @hr_name,
 1, 1, '上海', 0, 22000, 38000, 0, 2, 2, 2, 2,
 '独立负责招聘系统前后端联调与交付。',
 '掌握Vue3与Java/Spring Boot技术栈。',
 'Vue3,Java,全栈,REST API', 2, 1, '2026-03-01 09:00:00'),

('JOB202604', '产品经理', 2, '产品部', @hr_id, @hr_name,
 1, 1, '北京', 0, 18000, 30000, 0, 2, 2, 3, 2,
 '负责智能招聘平台候选人体验与HR工作台产品设计。',
 '3年以上B端产品经验；熟悉招聘流程。',
 '产品设计,招聘SaaS,需求分析,原型', 4, 3, '2026-03-15 09:00:00'),

('JOB202605', '测试开发工程师', 1, '技术研发部', @hr_id, @hr_name,
 1, 1, '深圳', 0, 15000, 28000, 0, 2, 3, 2, 2,
 '负责招聘系统功能测试与自动化回归体系建设。',
 '2年以上测试经验；熟悉接口测试与自动化脚本。',
 '接口测试,自动化测试,Java', 1, 1, '2026-04-01 09:00:00'),

('JOB202606', '数据分析师', 3, '数据智能部', @hr_id, @hr_name,
 1, 1, '上海', 0, 16000, 28000, 0, 2, 2, 2, 2,
 '围绕招聘漏斗、人岗匹配等展开数据分析。',
 '熟练SQL/Python；2年以上数据分析经验。',
 'SQL,Python,数据分析,BI', 2, 1, '2026-04-15 09:00:00'),

('JOB202607', 'UI/UX设计师', 4, '设计部', @hr_id, @hr_name,
 1, 1, '北京', 0, 15000, 25000, 0, 2, 3, 2, 2,
 '负责招聘平台多端界面设计与交互规范。',
 '精通Figma；2年以上B端设计经验。',
 'Figma,交互设计,B端设计', 1, 0, '2026-05-01 09:00:00'),

('JOB202608', '算法工程师（推荐方向）', 3, '数据智能部', @hr_id, @hr_name,
 1, 1, '北京', 0, 30000, 50000, 0, 2, 1, 3, 3,
 '参与简历解析、人岗匹配、人才画像等AI能力建设。',
 '硕士及以上；3年算法经验；熟悉NLP/推荐系统。',
 'Python,机器学习,推荐系统,LLM', 3, 2, '2026-05-15 09:00:00'),

('JOB202609', 'HRBP', 5, '人力资源部', @hr_id, @hr_name,
 1, 1, '北京', 0, 12000, 20000, 0, 2, 3, 2, 2,
 '支持技术团队招聘与人才发展。',
 '2年以上HR经验；熟悉社招流程。',
 '招聘,HRBP,员工关系', 1, 1, '2026-06-01 09:00:00'),

('JOB202610', '项目经理', 2, '产品部', @hr_id, @hr_name,
 1, 1, '上海', 0, 20000, 35000, 0, 2, 2, 5, 2,
 '统筹智能招聘平台版本规划与跨团队协同。',
 '5年以上项目管理经验；PMP认证优先。',
 '项目管理,敏捷,跨部门协作', 2, 1, '2026-06-10 09:00:00');

SELECT COUNT(*) AS job_count FROM `job_post` WHERE `is_deleted` = 0;

-- ============================================================
-- 4. 创建 5 个候选人和简历
-- ============================================================
USE `talent_auth_db`;

-- 候选人1
INSERT INTO `sys_user` (`username`, `password_hash`, `nickname`, `user_type`, `status`, `is_deleted`)
SELECT 'candidate_01', '$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna', '张明', 1, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'candidate_01' AND `is_deleted` = 0);

SET @uid1 := (SELECT `id` FROM `sys_user` WHERE `username` = 'candidate_01' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `candidate_profile` (`user_id`, `real_name`, `gender`, `birth_date`, `current_title`, `work_years`, `highest_edu`, `city`, `job_seeking_status`, `resume_completeness`, `ai_score`, `is_deleted`)
SELECT @uid1, '张明', 1, '1995-08-15', '高级前端工程师', 6.5, 2, '北京', 2, 92, 86, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `candidate_profile` WHERE `user_id` = @uid1 AND `is_deleted` = 0);

-- 候选人2
INSERT INTO `sys_user` (`username`, `password_hash`, `nickname`, `user_type`, `status`, `is_deleted`)
SELECT 'candidate_02', '$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna', '李娜', 1, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'candidate_02' AND `is_deleted` = 0);

SET @uid2 := (SELECT `id` FROM `sys_user` WHERE `username` = 'candidate_02' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `candidate_profile` (`user_id`, `real_name`, `gender`, `birth_date`, `current_title`, `work_years`, `highest_edu`, `city`, `job_seeking_status`, `resume_completeness`, `ai_score`, `is_deleted`)
SELECT @uid2, '李娜', 2, '1997-03-22', 'Java开发工程师', 4, 2, '北京', 2, 88, 82, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `candidate_profile` WHERE `user_id` = @uid2 AND `is_deleted` = 0);

-- 候选人3
INSERT INTO `sys_user` (`username`, `password_hash`, `nickname`, `user_type`, `status`, `is_deleted`)
SELECT 'candidate_03', '$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna', '王强', 1, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'candidate_03' AND `is_deleted` = 0);

SET @uid3 := (SELECT `id` FROM `sys_user` WHERE `username` = 'candidate_03' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `candidate_profile` (`user_id`, `real_name`, `gender`, `birth_date`, `current_title`, `work_years`, `highest_edu`, `city`, `job_seeking_status`, `resume_completeness`, `ai_score`, `is_deleted`)
SELECT @uid3, '王强', 1, '1992-11-08', '产品经理', 7, 3, '上海', 2, 90, 88, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `candidate_profile` WHERE `user_id` = @uid3 AND `is_deleted` = 0);

-- 候选人4
INSERT INTO `sys_user` (`username`, `password_hash`, `nickname`, `user_type`, `status`, `is_deleted`)
SELECT 'candidate_04', '$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna', '赵婷', 1, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'candidate_04' AND `is_deleted` = 0);

SET @uid4 := (SELECT `id` FROM `sys_user` WHERE `username` = 'candidate_04' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `candidate_profile` (`user_id`, `real_name`, `gender`, `birth_date`, `current_title`, `work_years`, `highest_edu`, `city`, `job_seeking_status`, `resume_completeness`, `ai_score`, `is_deleted`)
SELECT @uid4, '赵婷', 2, '1998-05-20', '数据分析师', 3, 2, '深圳', 2, 85, 79, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `candidate_profile` WHERE `user_id` = @uid4 AND `is_deleted` = 0);

-- 候选人5
INSERT INTO `sys_user` (`username`, `password_hash`, `nickname`, `user_type`, `status`, `is_deleted`)
SELECT 'candidate_05', '$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna', '陈飞', 1, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'candidate_05' AND `is_deleted` = 0);

SET @uid5 := (SELECT `id` FROM `sys_user` WHERE `username` = 'candidate_05' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `candidate_profile` (`user_id`, `real_name`, `gender`, `birth_date`, `current_title`, `work_years`, `highest_edu`, `city`, `job_seeking_status`, `resume_completeness`, `ai_score`, `is_deleted`)
SELECT @uid5, '陈飞', 1, '1994-09-30', '算法工程师', 5, 3, '北京', 2, 93, 91, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `candidate_profile` WHERE `user_id` = @uid5 AND `is_deleted` = 0);

-- ============================================================
-- 5. 插入简历
-- ============================================================
USE `talent_resume_db`;

INSERT INTO `resume` (`candidate_id`, `resume_name`, `is_default`, `summary`, `parse_status`, `screen_status`, `is_deleted`) VALUES
(@uid1, '张明-前端工程师简历', 1, '6年+前端开发经验，擅长Vue3/TypeScript工程化与性能优化。', 2, 1, 0),
(@uid2, '李娜-Java开发简历',   1, '4年Java后端开发经验，熟悉Spring Boot微服务架构。', 2, 1, 0),
(@uid3, '王强-产品经理简历',   1, '7年B端产品经验，熟悉招聘SaaS与AI产品化。',     2, 1, 0),
(@uid4, '赵婷-数据分析师简历', 1, '3年数据分析经验，熟练SQL/Python与BI工具。',      2, 1, 0),
(@uid5, '陈飞-算法工程师简历', 1, '5年算法经验，熟悉推荐系统与NLP。',               2, 1, 0);

SET @r1 := 1; SET @r2 := 2; SET @r3 := 3; SET @r4 := 4; SET @r5 := 5;

INSERT INTO `resume_skill` (`resume_id`, `skill_name`, `proficiency_level`, `sort_order`, `is_deleted`) VALUES
(@r1, 'Vue3/TypeScript', 92, 0, 0),
(@r1, 'React', 75, 1, 0),
(@r2, 'Java', 88, 0, 0),
(@r2, 'Spring Boot', 85, 1, 0),
(@r3, '产品设计', 90, 0, 0),
(@r3, '需求分析', 88, 1, 0),
(@r4, 'SQL', 85, 0, 0),
(@r4, 'Python', 80, 1, 0),
(@r5, 'Python', 92, 0, 0),
(@r5, '机器学习', 90, 1, 0);

SELECT COUNT(*) AS resume_count FROM `resume` WHERE `is_deleted` = 0;

-- ============================================================
-- 6. 插入投递记录（12条，分布近6个月，不同阶段/状态）
-- ============================================================
USE `talent_job_db`;

INSERT INTO `job_application` (
  `application_no`, `job_id`, `job_title`, `candidate_id`, `candidate_name`,
  `resume_id`, `channel`, `current_stage`, `status`, `match_score`, `applied_at`
) VALUES
-- 6月投递（本月）
('APP20260601', 1, '高级前端工程师',         @uid1, '张明', @r1, 1, 4, 1, 85, '2026-06-05 10:00:00'),
('APP20260602', 4, '产品经理',               @uid3, '王强', @r3, 2, 3, 1, 88, '2026-06-08 14:30:00'),
('APP20260603', 8, '算法工程师（推荐方向）', @uid5, '陈飞', @r5, 3, 2, 1, 91, '2026-06-12 09:15:00'),
('APP20260604', 2, 'Java后端开发工程师',     @uid2, '李娜', @r2, 1, 1, 1, 82, '2026-06-18 11:00:00'),

-- 5月投递
('APP20260501', 6, '数据分析师',             @uid4, '赵婷', @r4, 4, 5, 2, 79, '2026-05-10 08:00:00'),
('APP20260502', 7, 'UI/UX设计师',            @uid1, '张明', @r1, 1, 5, 1, 72, '2026-05-15 16:00:00'),

-- 4月投递
('APP20260401', 1, '高级前端工程师',         @uid1, '张明', @r1, 3, 4, 1, 85, '2026-04-20 09:30:00'),
('APP20260402', 5, '测试开发工程师',         @uid4, '赵婷', @r4, 5, 3, 3, 65, '2026-04-22 13:00:00'),

-- 3月投递
('APP20260301', 3, '全栈开发工程师',         @uid2, '李娜', @r2, 1, 5, 2, 84, '2026-03-05 10:00:00'),
('APP20260302', 4, '产品经理',               @uid3, '王强', @r3, 2, 5, 1, 88, '2026-03-18 15:00:00'),

-- 2月投递
('APP20260201', 2, 'Java后端开发工程师',     @uid2, '李娜', @r2, 1, 5, 2, 82, '2026-02-10 09:00:00'),

-- 1月投递
('APP20260101', 1, '高级前端工程师',         @uid5, '陈飞', @r5, 1, 5, 2, 78, '2026-01-15 11:00:00');

SELECT COUNT(*) AS application_count FROM `job_application`;

-- ============================================================
-- 7. 插入面试记录（7条，分布不同月份，不同状态）
-- ============================================================
USE `talent_interview_db`;

-- 需从auth库取HR作为面试官
SET @interviewer_id := (SELECT `id` FROM `talent_auth_db`.`sys_user` WHERE `username` = 'hr_demo' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `interview` (
  `application_id`, `job_id`, `candidate_id`, `candidate_name`,
  `job_title`, `interviewer_id`, `interviewer_name`,
  `round_no`, `round_type`, `interview_mode`,
  `scheduled_start`, `scheduled_end`,
  `status`, `total_score`, `created_by`, `created_by_name`
) VALUES
-- 6月面试
(1, 1,  @uid1, '张明', '高级前端工程师', @interviewer_id, '李HR', 1, 1, 1, '2026-06-15 10:00:00', '2026-06-15 11:00:00', 2, 85.5, @hr_id, @hr_name),
(2, 4,  @uid3, '王强', '产品经理',       @interviewer_id, '李HR', 1, 1, 1, '2026-06-20 14:00:00', '2026-06-20 15:00:00', 2, 88.0, @hr_id, @hr_name),
(3, 8,  @uid5, '陈飞', '算法工程师',     @interviewer_id, '李HR', 1, 2, 1, '2026-06-22 09:00:00', '2026-06-22 10:30:00', 1, NULL, @hr_id, @hr_name),
-- 5月面试
(5, 6,  @uid4, '赵婷', '数据分析师',     @interviewer_id, '李HR', 1, 1, 1, '2026-05-20 10:00:00', '2026-05-20 11:00:00', 2, 82.0, @hr_id, @hr_name),
-- 4月面试
(7, 1,  @uid1, '张明', '高级前端工程师', @interviewer_id, '李HR', 1, 1, 2, '2026-04-25 14:00:00', '2026-04-25 15:00:00', 2, 83.0, @hr_id, @hr_name),
(8, 5,  @uid4, '赵婷', '测试开发工程师', @interviewer_id, '李HR', 1, 1, 1, '2026-04-28 09:00:00', '2026-04-28 10:00:00', 2, 65.0, @hr_id, @hr_name),
-- 3月面试
(9, 3,  @uid2, '李娜', '全栈开发工程师', @interviewer_id, '李HR', 1, 1, 1, '2026-03-15 13:00:00', '2026-03-15 14:00:00', 2, 84.0, @hr_id, @hr_name);

SELECT COUNT(*) AS interview_count FROM `interview`;

-- ============================================================
-- 8. 验证汇总
-- ============================================================
SELECT 'job_post'      AS tbl, COUNT(*) AS cnt FROM `talent_job_db`.`job_post` WHERE `is_deleted` = 0
UNION ALL
SELECT 'resume',             COUNT(*) FROM `talent_resume_db`.`resume` WHERE `is_deleted` = 0
UNION ALL
SELECT 'job_application',    COUNT(*) FROM `talent_job_db`.`job_application`
UNION ALL
SELECT 'interview',          COUNT(*) FROM `talent_interview_db`.`interview`
UNION ALL
SELECT 'candidate_user',     COUNT(*) FROM `talent_auth_db`.`sys_user` WHERE `user_type` = 1 AND `is_deleted` = 0;
