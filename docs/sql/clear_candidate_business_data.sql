-- =============================================================================
-- 清空候选人端业务数据（保留候选人登录账号 sys_user）
--
-- 用途：附件与在线简历不一致时，重置简历/投递/AI 解析/匹配/画像等联调数据
-- 建议：执行本脚本后同步清空 MinIO 桶 talent-resumes（见脚本末尾说明）
--
-- 执行示例（本机 MySQL 3306，按实际密码修改）：
--   mysql -h localhost -P 3306 -uroot -p < docs/sql/clear_candidate_business_data.sql
-- Docker MySQL 映射 3307 时：
--   mysql -h localhost -P 3307 -uroot -proot123 < docs/sql/clear_candidate_business_data.sql
--
-- 不删除：HR/面试官账号、岗位 job_post、AI 模型配置、知识库、HR 助手会话
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. talent_ai_db：解析 / 匹配 / 画像 / 面试题
-- -----------------------------------------------------------------------------
USE `talent_ai_db`;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `ai_resume_parse_result`;
DELETE FROM `ai_match_record`;
DELETE FROM `ai_talent_profile`;
DELETE FROM `ai_interview_question`;
DELETE FROM `ai_parse_task`;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 2. talent_interview_db：面试安排与评价
-- -----------------------------------------------------------------------------
USE `talent_interview_db`;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `interview_evaluation`;
DELETE FROM `interview`;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 3. talent_job_db：投递 / Offer / 阶段日志，并重置岗位计数
-- -----------------------------------------------------------------------------
USE `talent_job_db`;
SET FOREIGN_KEY_CHECKS = 0;

-- offer 表若未建则跳过（部分环境仅有 job_application）
DELETE FROM `application_stage_log`;
DELETE FROM `job_application`;

UPDATE `job_post`
SET
  `applied_count`  = 0,
  `matched_count`  = 0,
  `updated_at`     = NOW()
WHERE `is_deleted` = 0;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 4. talent_pool_db：人才库归档（库未建时可跳过）
-- -----------------------------------------------------------------------------
-- USE `talent_pool_db`;
-- SET FOREIGN_KEY_CHECKS = 0;
-- DELETE FROM `talent_pool_tag`;
-- DELETE FROM `talent_pool_record`;
-- SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 5. talent_resume_db：简历主表及子表（含附件元数据）
-- -----------------------------------------------------------------------------
USE `talent_resume_db`;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `resume_skill`;
DELETE FROM `resume_work_experience`;
DELETE FROM `resume_education`;
DELETE FROM `resume_attachment`;
DELETE FROM `resume`;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 6. talent_auth_db：重置候选人档案（不删 sys_user 登录账号）
-- -----------------------------------------------------------------------------
USE `talent_auth_db`;

UPDATE `candidate_profile` cp
INNER JOIN `sys_user` u ON u.`id` = cp.`user_id` AND u.`is_deleted` = 0
SET
  cp.`current_title`       = NULL,
  cp.`work_years`          = NULL,
  cp.`highest_edu`         = NULL,
  cp.`city`                = NULL,
  cp.`job_seeking_status`  = 1,
  cp.`resume_completeness` = 0,
  cp.`ai_score`            = NULL,
  cp.`updated_at`          = NOW()
WHERE u.`user_type` = 1
  AND cp.`is_deleted` = 0;

-- 可选：删除候选人相关通知
DELETE n FROM `sys_notification` n
INNER JOIN `sys_user` u ON u.`id` = n.`user_id` AND u.`is_deleted` = 0
WHERE u.`user_type` = 1;

-- -----------------------------------------------------------------------------
-- 7. 校验
-- -----------------------------------------------------------------------------
SELECT 'resume' AS item, COUNT(*) AS cnt FROM `talent_resume_db`.`resume`
UNION ALL
SELECT 'attachment', COUNT(*) FROM `talent_resume_db`.`resume_attachment`
UNION ALL
SELECT 'application', COUNT(*) FROM `talent_job_db`.`job_application`
UNION ALL
SELECT 'parse_task', COUNT(*) FROM `talent_ai_db`.`ai_parse_task`
UNION ALL
SELECT 'parse_result', COUNT(*) FROM `talent_ai_db`.`ai_resume_parse_result`
UNION ALL
SELECT 'match', COUNT(*) FROM `talent_ai_db`.`ai_match_record`
UNION ALL
SELECT 'profile', COUNT(*) FROM `talent_ai_db`.`ai_talent_profile`
UNION ALL
SELECT 'interview', COUNT(*) FROM `talent_interview_db`.`interview`;
-- UNION ALL SELECT 'pool_record', COUNT(*) FROM `talent_pool_db`.`talent_pool_record`;

SELECT 'candidate_users_kept' AS item, COUNT(*) AS cnt
FROM `talent_auth_db`.`sys_user`
WHERE `user_type` = 1 AND `is_deleted` = 0;

-- =============================================================================
-- MinIO 桶 talent-resumes 建议一并清空（DB 删 attachment 后桶内 PDF 会变成孤儿文件）
--
-- 方式 A — Docker mc（推荐）：
--   docker run --rm --add-host=host.docker.internal:host-gateway minio/mc sh -c "
--     mc alias set local http://host.docker.internal:9000 minioadmin minioadmin &&
--     mc rm --recursive --force local/talent-resumes/ || true &&
--     mc mb local/talent-resumes || true
--   "
--
-- 方式 B — MinIO 控制台：http://localhost:9001 → Buckets → talent-resumes → 全选删除
--
-- 方式 C — 停 MinIO 后删本地卷（会清空所有桶）：
--   docker compose -f docker/docker-compose.yml stop minio
--   删除 docker/minio/data 下 talent-resumes 目录后重启 minio
-- =============================================================================
