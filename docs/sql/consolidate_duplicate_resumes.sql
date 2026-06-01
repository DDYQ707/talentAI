-- 手动合并同一候选人的重复简历（保留最新一条；附件与在线内容会合并到保留记录）
-- 建议在重启 talent-resume 后优先刷新 HR 简历列表，由后端自动合并。
-- 若仍有多条，可执行本脚本前备份数据。

USE `talent_resume_db`;

-- 查看重复
SELECT candidate_id, COUNT(*) AS cnt
FROM resume
WHERE is_deleted = 0
GROUP BY candidate_id
HAVING cnt > 1;

-- 以下逻辑需按业务在应用中执行；推荐重启 talent-resume 后访问 /api/resume/hr/page 触发自动合并。
