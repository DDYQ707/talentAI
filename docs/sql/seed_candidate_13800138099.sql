-- =============================================================================
-- 候选人演示数据：手机号 13800138099（仅注册账号 → 完善档案 + 在线简历）
-- 执行前请先确认该手机号已在 sys_user 中注册（user_type=1）
--
-- 库说明（与 application.yml 一致）：
--   talent_auth_db  → sys_user、candidate_profile
--   talent_resume_db → resume、resume_education、resume_work_experience、resume_skill
--
-- 使用方式：在 MySQL 客户端依次执行，或整文件 source。
-- 若用户 id 不是自动推断，可先执行：
--   SELECT id, username, phone FROM talent_auth_db.sys_user WHERE phone = '13800138099';
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. talent_auth_db：用户展示信息 + 候选人档案（可投递、档案完整）
-- -----------------------------------------------------------------------------
USE `talent_auth_db`;

SET @uid := (
  SELECT `id` FROM `sys_user`
  WHERE `phone` = '13800138099' AND `is_deleted` = 0
  LIMIT 1
);

-- 未找到用户时下面语句会无效，请先注册该手机号
SELECT IF(@uid IS NULL, '错误：未找到 phone=13800138099 的用户，请先在前端注册', CONCAT('将写入 user_id=', @uid)) AS check_user;

UPDATE `sys_user`
SET
  `nickname`   = '张明',
  `email`      = COALESCE(NULLIF(TRIM(`email`), ''), 'zhangming.demo@example.com'),
  `updated_at` = NOW()
WHERE `id` = @uid AND @uid IS NOT NULL;

UPDATE `candidate_profile`
SET
  `real_name`            = '张明',
  `gender`               = 1,
  `birth_date`           = '1995-08-15',
  `current_title`        = '高级前端工程师',
  `work_years`           = 6.5,
  `highest_edu`          = 2,
  `city`                 = '北京',
  `job_seeking_status`   = 2,
  `resume_completeness`  = 92,
  `ai_score`             = 86,
  `updated_at`           = NOW(),
  `is_deleted`           = 0
WHERE `user_id` = @uid AND @uid IS NOT NULL;

-- 注册时若未生成 candidate_profile，则插入一条
INSERT INTO `candidate_profile` (
  `user_id`, `real_name`, `gender`, `birth_date`, `current_title`, `work_years`,
  `highest_edu`, `city`, `job_seeking_status`, `resume_completeness`, `ai_score`, `is_deleted`
)
SELECT
  @uid, '张明', 1, '1995-08-15', '高级前端工程师', 6.5,
  2, '北京', 2, 92, 86, 0
FROM DUAL
WHERE @uid IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `candidate_profile` WHERE `user_id` = @uid AND `is_deleted` = 0
  );

-- -----------------------------------------------------------------------------
-- 2. talent_resume_db：在线简历（教育 / 工作 / 技能，便于「我的简历」展示）
-- -----------------------------------------------------------------------------
USE `talent_resume_db`;

-- 清理该候选人已有简历子数据（可重复执行）
DELETE rs FROM `resume_skill` rs
INNER JOIN `resume` r ON r.`id` = rs.`resume_id`
WHERE r.`candidate_id` = @uid;

DELETE rw FROM `resume_work_experience` rw
INNER JOIN `resume` r ON r.`id` = rw.`resume_id`
WHERE r.`candidate_id` = @uid;

DELETE re FROM `resume_education` re
INNER JOIN `resume` r ON r.`id` = re.`resume_id`
WHERE r.`candidate_id` = @uid;

DELETE ra FROM `resume_attachment` ra
INNER JOIN `resume` r ON r.`id` = ra.`resume_id`
WHERE r.`candidate_id` = @uid;

DELETE FROM `resume` WHERE `candidate_id` = @uid;

INSERT INTO `resume` (
  `candidate_id`, `resume_name`, `is_default`, `summary`, `parse_status`, `screen_status`, `is_deleted`
) VALUES (
  @uid,
  '张明-前端工程师简历',
  1,
  '6年+前端开发经验，擅长 Vue3 / TypeScript 工程化与性能优化；主导过招聘类 B 端与 C 端产品，熟悉组件库设计、CI 与可观测性。求职方向：高级前端 / 前端架构，期望 base 北京。',
  2,
  1,
  0
);

SET @resume_id := LAST_INSERT_ID();

INSERT INTO `resume_education` (
  `resume_id`, `school_name`, `major`, `degree`, `start_date`, `end_date`, `description`, `sort_order`, `is_deleted`
) VALUES
(
  @resume_id, '北京理工大学', '计算机科学与技术', 2,
  '2013-09-01', '2017-06-30',
  '校级优秀毕业生；ACM 校赛银奖；主修数据结构、操作系统、软件工程',
  0, 0
),
(
  @resume_id, '北京理工大学', '软件工程（辅修）', 2,
  '2014-09-01', '2017-06-30',
  '辅修课程：移动开发、人机交互',
  1, 0
);

INSERT INTO `resume_work_experience` (
  `resume_id`, `company_name`, `job_title`, `start_date`, `end_date`, `job_description`, `sort_order`, `is_deleted`
) VALUES
(
  @resume_id, '某互联网招聘科技公司', '高级前端工程师', '2021-03-01', NULL,
  '负责智能招聘与人才画像分析系统候选人端 / HR 端前端架构。\n'
  '- 基于 Vue3 + Pinia + Vite 搭建移动端与 PC 端双端复用组件体系；\n'
  '- 简历在线编辑、附件投递、MinIO 预签名预览等模块联调落地；\n'
  '- 推动列表虚拟滚动与接口聚合，首屏 LCP 降低约 35%；\n'
  '- 参与 Code Review 与前端规范制定，带教 2 名初级工程师。',
  0, 0
),
(
  @resume_id, '某电商 SaaS 公司', '前端工程师', '2018-07-01', '2021-02-28',
  '参与商家后台与数据看板开发，使用 Vue2 → Vue3 渐进式升级。\n'
  '- 封装图表与表格业务组件 20+，覆盖 80% 列表页场景；\n'
  '- 引入 ESLint + Husky，缺陷率下降；配合后端完成网关统一鉴权改造。',
  1, 0
),
(
  @resume_id, '某软件外包公司', 'Web 开发实习生 → 初级前端', '2017-07-01', '2018-06-30',
  '参与政企门户与 H5 活动页，掌握 HTML/CSS/JS 基础与 jQuery 生态。',
  2, 0
);

INSERT INTO `resume_skill` (
  `resume_id`, `skill_name`, `proficiency_level`, `sort_order`, `is_deleted`
) VALUES
(@resume_id, 'Vue3 / TypeScript', 92, 0, 0),
(@resume_id, 'React', 75, 1, 0),
(@resume_id, 'Vite / Webpack', 88, 2, 0),
(@resume_id, 'Node.js', 70, 3, 0),
(@resume_id, '性能优化', 85, 4, 0),
(@resume_id, '微前端', 72, 5, 0),
(@resume_id, 'MySQL / REST API 联调', 80, 6, 0),
(@resume_id, 'Git / CI', 90, 7, 0);

-- -----------------------------------------------------------------------------
-- 3. 校验
-- -----------------------------------------------------------------------------
SELECT 'auth.user' AS tbl, u.`id`, u.`phone`, u.`nickname`, u.`email`
FROM `talent_auth_db`.`sys_user` u WHERE u.`phone` = '13800138099' AND u.`is_deleted` = 0;

SELECT 'auth.profile' AS tbl, p.*
FROM `talent_auth_db`.`candidate_profile` p
INNER JOIN `talent_auth_db`.`sys_user` u ON u.`id` = p.`user_id`
WHERE u.`phone` = '13800138099' AND p.`is_deleted` = 0;

SELECT 'resume.main' AS tbl, r.*
FROM `talent_resume_db`.`resume` r
INNER JOIN `talent_auth_db`.`sys_user` u ON u.`id` = r.`candidate_id`
WHERE u.`phone` = '13800138099' AND r.`is_deleted` = 0;

SELECT 'resume.edu' AS tbl, e.`school_name`, e.`major`, e.`degree`
FROM `talent_resume_db`.`resume_education` e
WHERE e.`resume_id` = @resume_id AND e.`is_deleted` = 0;

SELECT 'resume.work' AS tbl, w.`company_name`, w.`job_title`
FROM `talent_resume_db`.`resume_work_experience` w
WHERE w.`resume_id` = @resume_id AND w.`is_deleted` = 0;

SELECT 'resume.skill' AS tbl, s.`skill_name`, s.`proficiency_level`
FROM `talent_resume_db`.`resume_skill` s
WHERE s.`resume_id` = @resume_id AND s.`is_deleted` = 0;
