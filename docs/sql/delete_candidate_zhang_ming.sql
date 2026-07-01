-- =============================================================================
-- 删除候选人「张明」（phone=13800138099, user_id 自动推断）的业务数据
-- 保留 sys_user 登录账号，便于重新注册/投递联调
-- =============================================================================

SET NAMES utf8mb4;

USE `talent_auth_db`;

SET @uid := (
  SELECT `id` FROM `sys_user`
  WHERE `phone` = '13800138099' AND `is_deleted` = 0
  LIMIT 1
);

SELECT IF(@uid IS NULL, '错误：未找到 phone=13800138099 的用户', CONCAT('将删除 user_id=', @uid, ' 的业务数据')) AS check_user;

-- 收集关联 id
SET @resume_id := (
  SELECT `id` FROM `talent_resume_db`.`resume`
  WHERE `candidate_id` = @uid AND `is_deleted` = 0
  ORDER BY `is_default` DESC, `id` DESC
  LIMIT 1
);

-- -----------------------------------------------------------------------------
-- 1. talent_interview_db（先删面试题，再删评价与面试）
-- -----------------------------------------------------------------------------
USE `talent_ai_db`;
SET FOREIGN_KEY_CHECKS = 0;

DELETE q FROM `ai_interview_question` q
INNER JOIN `talent_interview_db`.`interview` i ON i.`id` = q.`interview_id`
WHERE i.`candidate_id` = @uid;

USE `talent_interview_db`;

DELETE ev FROM `interview_evaluation` ev
INNER JOIN `interview` i ON i.`id` = ev.`interview_id`
WHERE i.`candidate_id` = @uid;

DELETE FROM `interview` WHERE `candidate_id` = @uid;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 2. talent_ai_db
-- -----------------------------------------------------------------------------
USE `talent_ai_db`;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `ai_talent_profile` WHERE `candidate_id` = @uid;

DELETE FROM `ai_match_record` WHERE `resume_id` = @resume_id;

DELETE pr FROM `ai_resume_parse_result` pr
INNER JOIN `ai_parse_task` t ON t.`id` = pr.`task_id`
WHERE t.`resume_id` = @resume_id OR t.`candidate_id` = @uid;

DELETE FROM `ai_parse_task` WHERE `resume_id` = @resume_id OR `candidate_id` = @uid;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 3. talent_job_db
-- -----------------------------------------------------------------------------
USE `talent_job_db`;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `application_stage_log`
WHERE `application_id` IN (
  SELECT `id` FROM `job_application` WHERE `candidate_id` = @uid
);

DELETE FROM `job_application` WHERE `candidate_id` = @uid;

-- 重算岗位投递数
UPDATE `job_post` jp
SET `applied_count` = (
  SELECT COUNT(*) FROM `job_application` ja
  WHERE ja.`job_id` = jp.`id` AND ja.`is_deleted` = 0
),
`updated_at` = NOW()
WHERE jp.`is_deleted` = 0;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 4. talent_resume_db
-- -----------------------------------------------------------------------------
USE `talent_resume_db`;
SET FOREIGN_KEY_CHECKS = 0;

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

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 5. talent_auth_db：重置档案（保留账号）
-- -----------------------------------------------------------------------------
USE `talent_auth_db`;

UPDATE `candidate_profile`
SET
  `current_title`       = NULL,
  `work_years`          = NULL,
  `highest_edu`         = NULL,
  `city`                = NULL,
  `job_seeking_status`  = 1,
  `resume_completeness` = 0,
  `ai_score`            = NULL,
  `updated_at`          = NOW()
WHERE `user_id` = @uid AND `is_deleted` = 0;

DELETE n FROM `sys_notification` n
WHERE n.`user_id` = @uid;

-- -----------------------------------------------------------------------------
-- 6. 校验
-- -----------------------------------------------------------------------------
SELECT 'resume' AS item, COUNT(*) AS cnt
FROM `talent_resume_db`.`resume` WHERE `candidate_id` = @uid
UNION ALL
SELECT 'application', COUNT(*)
FROM `talent_job_db`.`job_application` WHERE `candidate_id` = @uid
UNION ALL
SELECT 'interview', COUNT(*)
FROM `talent_interview_db`.`interview` WHERE `candidate_id` = @uid
UNION ALL
SELECT 'ai_profile', COUNT(*)
FROM `talent_ai_db`.`ai_talent_profile` WHERE `candidate_id` = @uid;

SELECT 'user_kept' AS item, `id`, `phone`, `nickname`
FROM `sys_user` WHERE `id` = @uid;
