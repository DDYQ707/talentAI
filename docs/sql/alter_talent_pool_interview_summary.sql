-- 人才库：面试结论摘要字段
USE talent_pool_db;

ALTER TABLE talent_pool_record
    ADD COLUMN interview_summary VARCHAR(512) NULL COMMENT '面试评价摘要' AFTER source_job_title;
