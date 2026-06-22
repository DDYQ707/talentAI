-- =============================================================================
-- 重置招聘岗位：删除原有岗位及关联投递数据，新建 10 个标准岗位（各招 2 人）
--
-- 执行（项目根目录，按实际密码修改）：
--   mysql -h localhost -P 3306 -uroot -p < docs/sql/reset_seed_job_posts.sql
--
-- 说明：
--   - 会清空 job_application、application_stage_log、job_post
--   - 若环境已建 offer 表，请取消下方 offer 相关 DELETE 注释
--   - 发布人默认取 talent_auth_db 中第一个 HR 账号（user_type=2）
--   - 若部门不存在，会自动写入 5 个标准部门
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 0. 解析 HR 发布人
-- -----------------------------------------------------------------------------
USE `talent_auth_db`;

SET @hr_id := (
  SELECT `id` FROM `sys_user`
  WHERE `user_type` = 2 AND `is_deleted` = 0
  ORDER BY `id`
  LIMIT 1
);

SET @hr_name := COALESCE(
  (SELECT NULLIF(TRIM(`nickname`), '') FROM `sys_user` WHERE `id` = @hr_id),
  (SELECT NULLIF(TRIM(`username`), '') FROM `sys_user` WHERE `id` = @hr_id),
  'HR'
);

SELECT IF(@hr_id IS NULL, '警告：未找到 HR 账号，请先创建 user_type=2 的用户', CONCAT('发布人 user_id=', @hr_id, ' name=', @hr_name)) AS check_hr;

-- -----------------------------------------------------------------------------
-- 1. 标准部门（不存在则插入）
-- -----------------------------------------------------------------------------
INSERT INTO `sys_department` (`id`, `parent_id`, `dept_name`, `dept_code`, `sort_order`, `status`, `is_deleted`)
VALUES
  (1, 0, '技术研发部', 'TECH',      1, 1, 0),
  (2, 0, '产品部',     'PRODUCT',   2, 1, 0),
  (3, 0, '数据智能部', 'DATA_AI',   3, 1, 0),
  (4, 0, '设计部',     'DESIGN',    4, 1, 0),
  (5, 0, '人力资源部', 'HR',        5, 1, 0)
ON DUPLICATE KEY UPDATE
  `dept_name`   = VALUES(`dept_name`),
  `dept_code`   = VALUES(`dept_code`),
  `sort_order`  = VALUES(`sort_order`),
  `status`      = VALUES(`status`),
  `is_deleted`  = 0,
  `updated_at`  = NOW();

-- -----------------------------------------------------------------------------
-- 2. 清空岗位及投递关联数据
-- -----------------------------------------------------------------------------
USE `talent_job_db`;
SET FOREIGN_KEY_CHECKS = 0;

-- 若已建 offer 表，取消下面两行注释
-- DELETE FROM `offer_approval`;
-- DELETE FROM `offer`;

DELETE FROM `application_stage_log`;
DELETE FROM `job_application`;
DELETE FROM `job_post`;

ALTER TABLE `job_post` AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 3. 插入 10 个标准岗位（headcount = 2，status = 1 招聘中）
-- -----------------------------------------------------------------------------
-- education_requirement: 1-大专 2-本科 3-硕士 4-博士
-- employment_type: 1-全职  priority: 1-高 2-中 3-低

INSERT INTO `job_post` (
  `job_code`, `title`, `dept_id`, `dept_name`, `publisher_id`, `publisher_name`,
  `status`, `employment_type`, `work_city`, `is_remote`,
  `salary_min`, `salary_max`, `salary_negotiable`, `headcount`, `priority`,
  `experience_years_min`, `education_requirement`,
  `job_description`, `job_requirements`, `skill_tags`,
  `applied_count`, `matched_count`, `published_at`, `is_deleted`
) VALUES
(
  'JOB2026062201', '高级前端工程师', 1, '技术研发部', @hr_id, @hr_name,
  1, 1, '北京', 0,
  25000, 40000, 0, 2, 1,
  3, 2,
  '负责智能招聘与人才画像系统候选人端、HR 端前端架构与核心页面开发；参与组件库建设、性能优化与工程规范落地。',
  '1. 3 年及以上前端开发经验，本科及以上学历；\n2. 精通 Vue3、TypeScript，熟悉 Pinia、Vite；\n3. 有 B 端或招聘类产品经验优先；\n4. 具备良好的代码规范与协作意识。',
  'Vue3,TypeScript,前端工程化,性能优化',
  0, 0, NOW(), 0
),
(
  'JOB2026062202', 'Java 后端开发工程师', 1, '技术研发部', @hr_id, @hr_name,
  1, 1, '北京', 0,
  20000, 35000, 0, 2, 2,
  3, 2,
  '参与微服务后端研发，负责岗位、简历、投递、面试等核心业务模块的 API 设计与实现。',
  '1. 3 年及以上 Java 开发经验；\n2. 熟悉 Spring Boot、Spring Cloud、MyBatis-Plus；\n3. 熟悉 MySQL、Redis，了解消息队列；\n4. 有微服务拆分与 Feign 联调经验优先。',
  'Java,Spring Boot,MySQL,微服务',
  0, 0, NOW(), 0
),
(
  'JOB2026062203', '全栈开发工程师', 1, '技术研发部', @hr_id, @hr_name,
  1, 1, '上海', 0,
  22000, 38000, 0, 2, 2,
  2, 2,
  '独立负责招聘系统部分业务模块的前后端联调与交付，覆盖接口设计、页面实现与基础运维支持。',
  '1. 2 年及以上全栈经验；\n2. 掌握 Vue3 与 Java/Spring Boot 技术栈；\n3. 能独立完成需求分析、开发与联调；\n4. 学习能力强，沟通顺畅。',
  'Vue3,Java,全栈,REST API',
  0, 0, NOW(), 0
),
(
  'JOB2026062204', '产品经理', 2, '产品部', @hr_id, @hr_name,
  1, 1, '北京', 0,
  18000, 30000, 0, 2, 2,
  3, 2,
  '负责智能招聘平台候选人体验、HR 工作台及 AI 能力产品化设计，推动需求评审与版本迭代。',
  '1. 3 年及以上 B 端或 HR SaaS 产品经验；\n2. 熟悉招聘流程、简历筛选、面试安排等业务场景；\n3. 能输出 PRD、原型并与研发高效协作；\n4. 对 AI 招聘场景有理解者优先。',
  '产品设计,招聘 SaaS,需求分析,原型',
  0, 0, NOW(), 0
),
(
  'JOB2026062205', '测试开发工程师', 1, '技术研发部', @hr_id, @hr_name,
  1, 1, '深圳', 0,
  15000, 28000, 0, 2, 3,
  2, 2,
  '负责招聘系统功能测试、接口自动化与回归体系建设，保障发版质量与核心链路稳定性。',
  '1. 2 年及以上测试或测开经验；\n2. 熟悉接口测试、Postman/JMeter 等工具；\n3. 了解 Java 或 Python 自动化脚本；\n4. 细心负责，能独立编写测试用例。',
  '接口测试,自动化测试,Java,质量保障',
  0, 0, NOW(), 0
),
(
  'JOB2026062206', '数据分析师', 3, '数据智能部', @hr_id, @hr_name,
  1, 1, '上海', 0,
  16000, 28000, 0, 2, 2,
  2, 2,
  '围绕招聘漏斗、人岗匹配效果、简历转化等主题开展数据分析，输出可视化报表与业务洞察。',
  '1. 2 年及以上数据分析经验；\n2. 熟练 SQL，掌握 Python 或 Excel 高级分析；\n3. 熟悉 BI 工具，能独立完成专题分析；\n4. 有 HR 或招聘数据经验优先。',
  'SQL,Python,数据分析,BI',
  0, 0, NOW(), 0
),
(
  'JOB2026062207', 'UI/UX 设计师', 4, '设计部', @hr_id, @hr_name,
  1, 1, '北京', 0,
  15000, 25000, 0, 2, 3,
  2, 2,
  '负责招聘平台多端界面设计、交互规范与视觉体系维护，提升候选人投递与 HR 操作体验。',
  '1. 2 年及以上互联网产品设计经验；\n2. 精通 Figma，具备 B 端设计经验；\n3. 理解招聘业务场景与用户路径；\n4. 作品集完整，沟通表达清晰。',
  'Figma,交互设计,B端设计,视觉规范',
  0, 0, NOW(), 0
),
(
  'JOB2026062208', '算法工程师（推荐方向）', 3, '数据智能部', @hr_id, @hr_name,
  1, 1, '北京', 0,
  30000, 50000, 0, 2, 1,
  3, 3,
  '参与简历解析、人岗匹配、人才画像等 AI 能力建设，优化模型效果与线上指标。',
  '1. 硕士及以上学历，3 年及以上算法经验；\n2. 熟悉 Python、机器学习/NLP 基础；\n3. 有推荐系统、RAG 或 LLM 应用经验优先；\n4. 具备工程落地与效果评估能力。',
  'Python,机器学习,推荐系统,LLM',
  0, 0, NOW(), 0
),
(
  'JOB2026062209', 'HRBP', 5, '人力资源部', @hr_id, @hr_name,
  1, 1, '北京', 0,
  12000, 20000, 0, 2, 3,
  2, 2,
  '支持技术研发团队招聘与人才发展，协调面试安排、Offer 沟通与入职跟进。',
  '1. 2 年及以上 HR 或招聘经验；\n2. 熟悉社招流程与候选人沟通；\n3. 服务意识强，执行力好；\n4. 有互联网或科技行业背景优先。',
  '招聘,HRBP,员工关系,沟通协调',
  0, 0, NOW(), 0
),
(
  'JOB2026062210', '项目经理', 2, '产品部', @hr_id, @hr_name,
  1, 1, '上海', 0,
  20000, 35000, 0, 2, 2,
  5, 2,
  '统筹智能招聘平台版本规划、跨团队协同与里程碑交付，保障项目按期高质量上线。',
  '1. 5 年及以上项目管理经验；\n2. 熟悉敏捷开发，有互联网产品交付经验；\n3. 具备优秀的沟通、风险识别与推进能力；\n4. PMP 或同类认证优先。',
  '项目管理,敏捷,跨部门协作,交付管理',
  0, 0, NOW(), 0
);

-- -----------------------------------------------------------------------------
-- 4. 校验
-- -----------------------------------------------------------------------------
SELECT
  `id`, `job_code`, `title`, `dept_name`, `work_city`,
  `headcount`, `status`, `salary_min`, `salary_max`, `skill_tags`
FROM `job_post`
WHERE `is_deleted` = 0
ORDER BY `id`;

SELECT COUNT(*) AS total_jobs FROM `job_post` WHERE `is_deleted` = 0;
SELECT SUM(`headcount`) AS total_headcount FROM `job_post` WHERE `is_deleted` = 0;
