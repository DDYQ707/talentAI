-- talent_admin_db + talent_job_risk.job_post_id patch
-- Docker MySQL: Get-Content docs/sql/20260628_job_risk_job_post_id_patch.sql -Encoding UTF8 -Raw | docker exec -i talent-mysql mysql -uroot -proot123

CREATE DATABASE IF NOT EXISTS talent_admin_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE talent_admin_db;

CREATE TABLE IF NOT EXISTS talent_job_risk (
  id BIGINT NOT NULL AUTO_INCREMENT,
  job_title VARCHAR(128) NOT NULL,
  company_name VARCHAR(128) DEFAULT NULL,
  publisher_id BIGINT DEFAULT NULL,
  salary_min INT DEFAULT NULL,
  salary_max INT DEFAULT NULL,
  description TEXT,
  status TINYINT NOT NULL DEFAULT 0,
  risk_keywords VARCHAR(512) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  taken_down_at DATETIME DEFAULT NULL,
  taken_down_by BIGINT DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'talent_admin_db'
    AND TABLE_NAME = 'talent_job_risk'
    AND COLUMN_NAME = 'job_post_id'
);

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE talent_job_risk ADD COLUMN job_post_id BIGINT NULL COMMENT ''关联 talent-job 岗位 ID'' AFTER publisher_id',
  'SELECT ''job_post_id already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
