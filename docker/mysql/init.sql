-- Docker 首次启动时自动创建全部业务库（表结构需手动执行 docs/sql/*.sql）
CREATE DATABASE IF NOT EXISTS talent_auth_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS talent_job_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS talent_resume_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS talent_interview_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS talent_pool_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS talent_ai_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;