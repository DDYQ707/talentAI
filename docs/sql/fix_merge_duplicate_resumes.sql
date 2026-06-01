-- 一次性修复：合并同一 candidate_id 的重复简历（张明 candidate_id=4，张三 candidate_id=7）
-- 执行前请备份 talent_resume_db
-- 执行后重启 talent-resume，刷新 HR 简历管理页

USE `talent_resume_db`;

-- ========== 候选人 4（张明）：保留 resume_id=7（有附件），合并 resume_id=6 的在线内容 ==========
SET @keep4 := 7;
SET @from4 := 6;

-- 在线内容：6 -> 7（仅当 7 为空时插入）
INSERT INTO resume_education (resume_id, school_name, major, degree, start_date, end_date, description, sort_order, is_deleted)
SELECT @keep4, school_name, major, degree, start_date, end_date, description, sort_order, 0
FROM resume_education e
WHERE e.resume_id = @from4 AND e.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM resume_education x WHERE x.resume_id = @keep4 AND x.is_deleted = 0);

INSERT INTO resume_work_experience (resume_id, company_name, job_title, start_date, end_date, job_description, sort_order, is_deleted)
SELECT @keep4, company_name, job_title, start_date, end_date, job_description, sort_order, 0
FROM resume_work_experience w
WHERE w.resume_id = @from4 AND w.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM resume_work_experience x WHERE x.resume_id = @keep4 AND x.is_deleted = 0);

INSERT INTO resume_skill (resume_id, skill_name, proficiency_level, sort_order, is_deleted)
SELECT @keep4, skill_name, proficiency_level, sort_order, 0
FROM resume_skill s
WHERE s.resume_id = @from4 AND s.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM resume_skill x WHERE x.resume_id = @keep4 AND x.is_deleted = 0);

UPDATE resume SET summary = (SELECT summary FROM (SELECT summary FROM resume WHERE id = @from4) t),
    resume_name = COALESCE((SELECT resume_name FROM resume WHERE id = @from4), resume_name)
WHERE id = @keep4 AND (summary IS NULL OR summary = '');

-- 软删重复主表
UPDATE resume SET is_deleted = 1 WHERE candidate_id = 4 AND id <> @keep4;

-- ========== 候选人 7（张三）：保留有附件且 updated_at 最新的一条（示例：resume_id=3 简历模板.pdf）==========
-- 请按实际数据调整 @keep7；下面取该候选人最新附件简历
SET @keep7 := (
  SELECT r.id FROM resume r
  INNER JOIN resume_attachment a ON a.resume_id = r.id AND a.is_deleted = 0
  WHERE r.candidate_id = 7 AND r.is_deleted = 0
  ORDER BY r.updated_at DESC, a.created_at DESC
  LIMIT 1
);

-- 将其他简历的在线内容合并到 @keep7（有内容的 resume_id=5）
INSERT INTO resume_education (resume_id, school_name, major, degree, start_date, end_date, description, sort_order, is_deleted)
SELECT @keep7, school_name, major, degree, start_date, end_date, description, sort_order, 0
FROM resume_education e
WHERE e.resume_id = 5 AND e.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM resume_education x WHERE x.resume_id = @keep7 AND x.is_deleted = 0);

INSERT INTO resume_work_experience (resume_id, company_name, job_title, start_date, end_date, job_description, sort_order, is_deleted)
SELECT @keep7, company_name, job_title, start_date, end_date, job_description, sort_order, 0
FROM resume_work_experience w
WHERE w.resume_id = 5 AND w.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM resume_work_experience x WHERE x.resume_id = @keep7 AND x.is_deleted = 0);

INSERT INTO resume_skill (resume_id, skill_name, proficiency_level, sort_order, is_deleted)
SELECT @keep7, skill_name, proficiency_level, sort_order, 0
FROM resume_skill s
WHERE s.resume_id = 5 AND s.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM resume_skill x WHERE x.resume_id = @keep7 AND x.is_deleted = 0);

UPDATE resume SET is_deleted = 1 WHERE candidate_id = 7 AND id <> @keep7;

SELECT candidate_id, id, resume_name, is_deleted FROM resume WHERE candidate_id IN (4, 7) ORDER BY candidate_id, id;
