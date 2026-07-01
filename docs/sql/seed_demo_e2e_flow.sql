-- =============================================================================
-- Demo E2E seed: workbench todos, trend, offer, AI insights
--
-- Accounts (password 123456):
--   admin / hr@company.com / interview@company.com / 13800138099
--
-- Run:
--   mysql -h localhost -P 3306 -uroot -p < docs/sql/seed_demo_e2e_flow.sql
-- =============================================================================

SET NAMES utf8mb4;

USE talent_auth_db;
SET @hr_id := (SELECT id FROM sys_user WHERE username = 'hr@company.com' AND is_deleted = 0 LIMIT 1);
SET @interviewer_id := (SELECT id FROM sys_user WHERE username = 'interview@company.com' AND is_deleted = 0 LIMIT 1);
SET @candidate_id := (SELECT id FROM sys_user WHERE phone = '13800138099' AND is_deleted = 0 LIMIT 1);

USE talent_job_db;
SET @app_pending := (SELECT id FROM job_application WHERE application_no = 'APP202606272019541069' AND is_deleted = 0 LIMIT 1);
SET @app_schedule := (SELECT id FROM job_application WHERE application_no = 'APP202606232034233291' AND is_deleted = 0 LIMIT 1);
SET @app_today := (SELECT id FROM job_application WHERE application_no = 'APP202606232047235697' AND is_deleted = 0 LIMIT 1);
SET @resume_pending := (SELECT resume_id FROM job_application WHERE id = @app_pending LIMIT 1);

UPDATE talent_resume_db.resume
SET screen_status = 1, updated_at = NOW()
WHERE id = @resume_pending AND @resume_pending IS NOT NULL;

USE talent_interview_db;
DELETE FROM interview WHERE application_id = @app_schedule AND status = 3;

INSERT INTO interview (
  application_id, job_id, candidate_id, candidate_name, job_title,
  interviewer_id, interviewer_name, round_no, round_type, interview_mode,
  scheduled_start, status, created_by, created_by_name, is_deleted
)
SELECT
  ja.id, ja.job_id, ja.candidate_id, ja.candidate_name, ja.job_title,
  @interviewer_id, 'interviewer-demo', 1, 1, 1,
  DATE_ADD(NOW(), INTERVAL 2 DAY), 3, @hr_id, 'hr-demo', 0
FROM talent_job_db.job_application ja
WHERE ja.id = @app_schedule AND @app_schedule IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM interview i
    WHERE i.application_id = ja.id AND i.status = 3 AND i.is_deleted = 0
  );

UPDATE interview
SET scheduled_start = TIMESTAMP(CURDATE(), '15:00:00'), status = 1, updated_at = NOW()
WHERE application_id = @app_today AND is_deleted = 0;

INSERT INTO interview (
  application_id, job_id, candidate_id, candidate_name, job_title,
  interviewer_id, interviewer_name, round_no, round_type, interview_mode,
  scheduled_start, status, created_by, created_by_name, is_deleted
)
SELECT
  ja.id, ja.job_id, ja.candidate_id, ja.candidate_name, ja.job_title,
  @interviewer_id, 'interviewer-demo', 1, 2, 1,
  TIMESTAMP(CURDATE(), '15:00:00'), 1, @hr_id, 'hr-demo', 0
FROM talent_job_db.job_application ja
WHERE ja.id = @app_today AND @app_today IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM interview i
    WHERE i.application_id = ja.id AND i.status IN (1, 2) AND i.is_deleted = 0
  );

USE talent_job_db;
DELETE FROM offer_approval WHERE offer_id IN (SELECT id FROM offer WHERE offer_no = 'OFR-DEMO-PENDING-001');
DELETE FROM offer WHERE offer_no = 'OFR-DEMO-PENDING-001';

INSERT INTO offer (
  offer_no, application_id, job_id, job_title, candidate_id, candidate_name,
  dept_id, dept_name, base_salary, annual_salary, position_level,
  expected_onboard_date, probation_months, status, hr_id, hr_name, remark, is_deleted
)
SELECT
  'OFR-DEMO-PENDING-001', ja.id, ja.job_id, ja.job_title, ja.candidate_id, ja.candidate_name,
  jp.dept_id, jp.dept_name, 22000.00, 320000.00, 'P5',
  DATE_ADD(CURDATE(), INTERVAL 14 DAY), 3, 3, @hr_id, 'hr-demo', 'demo offer to issue', 0
FROM job_application ja
INNER JOIN job_post jp ON jp.id = ja.job_id
WHERE ja.id = @app_pending AND @app_pending IS NOT NULL;

UPDATE job_application
SET applied_at = NOW(), updated_at = NOW()
WHERE id IN (@app_pending, @app_schedule, @app_today)
  AND applied_at < DATE_FORMAT(CURDATE(), '%Y-%m-01');

UPDATE offer SET created_at = NOW(), updated_at = NOW() WHERE offer_no = 'OFR-DEMO-PENDING-001';

USE talent_ai_db;
SET @job_id := (SELECT job_id FROM talent_job_db.job_application WHERE id = @app_pending LIMIT 1);

DELETE FROM ai_parse_task WHERE file_name = 'demo-e2e-parse';
DELETE FROM ai_match_record WHERE match_reason = 'demo-e2e-match';

INSERT INTO ai_parse_task (
  resume_id, application_id, candidate_id, model_id, task_status,
  raw_text_length, file_name, file_type, started_at, finished_at, is_deleted
)
SELECT @resume_pending, @app_pending, @candidate_id, 1, 2,
  1200, 'demo-e2e-parse', 'online', NOW(), NOW(), 0
FROM DUAL WHERE @resume_pending IS NOT NULL;

INSERT INTO ai_match_record (
  application_id, job_id, resume_id, model_id, match_score, match_status,
  match_level, match_reason, token_used, started_at, finished_at, is_deleted
)
SELECT @app_pending, @job_id, @resume_pending, 1, 88, 2,
  'high', 'demo-e2e-match', 1200, NOW(), NOW(), 0
FROM DUAL
WHERE @app_pending IS NOT NULL AND @job_id IS NOT NULL AND @resume_pending IS NOT NULL;

SELECT 'pending_screen' AS k, COUNT(*) AS v FROM talent_resume_db.resume WHERE screen_status = 1 AND is_deleted = 0;
SELECT 'offers_to_issue' AS k, COUNT(*) AS v FROM talent_job_db.offer WHERE status = 3 AND is_deleted = 0;
SELECT 'to_schedule' AS k, COUNT(*) AS v FROM talent_interview_db.interview WHERE status = 3 AND is_deleted = 0;
SELECT 'today_interview' AS k, COUNT(*) AS v FROM talent_interview_db.interview WHERE status = 1 AND DATE(scheduled_start) = CURDATE() AND is_deleted = 0;
