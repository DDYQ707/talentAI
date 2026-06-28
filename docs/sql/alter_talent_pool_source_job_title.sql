-- 人才库：来源岗位名称（归档时从投递单快照）
ALTER TABLE talent_pool_record
    ADD COLUMN source_job_title VARCHAR(128) NULL COMMENT '来源岗位名称快照' AFTER source_application_id;
