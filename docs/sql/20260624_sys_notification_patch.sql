-- =============================================================================
-- 系统通知表（候选人/HR 消息通知）
-- 执行：mysql -h127.0.0.1 -P3306 -uroot -p < docs/sql/20260624_sys_notification_patch.sql
-- =============================================================================

SET NAMES utf8mb4;
USE `talent_auth_db`;

CREATE TABLE IF NOT EXISTS `sys_notification` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '接收人ID',
  `title`         VARCHAR(128)    NOT NULL COMMENT '标题',
  `content`       VARCHAR(512)    DEFAULT NULL COMMENT '内容',
  `notify_type`   TINYINT         NOT NULL COMMENT '1-待办 2-提醒 3-公告',
  `biz_type`      VARCHAR(32)     DEFAULT NULL COMMENT '业务类型',
  `biz_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '业务ID',
  `is_read`       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已读',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_notify_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知待办表';
