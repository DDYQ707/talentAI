-- =====================================================================
-- talent-admin 广播中心：轮播图 banner + 系统公告 announcement
-- 数据库：talent_admin_db
-- =====================================================================
USE talent_admin_db;

-- ---------------------------------------------------------------------
-- 轮播图 banner
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`      VARCHAR(128) NOT NULL COMMENT '标题',
  `image_url`  VARCHAR(512) NOT NULL COMMENT '图片URL',
  `link_url`   VARCHAR(512) DEFAULT NULL COMMENT '跳转链接',
  `start_time` DATETIME     DEFAULT NULL COMMENT '开始时间',
  `end_time`   DATETIME     DEFAULT NULL COMMENT '结束时间',
  `status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=下线, 1=上线',
  `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图';

INSERT INTO `banner` (`title`, `image_url`, `link_url`, `start_time`, `end_time`, `status`, `sort_order`, `created_at`) VALUES
('春季招聘会火热进行中', 'https://picsum.photos/seed/banner1/1200/400', 'https://www.talent-ai.com/fair/spring', '2026-03-01 00:00:00', '2026-04-30 23:59:59', 1, 1, '2026-02-25 10:00:00'),
('AI 简历优化新功能上线', 'https://picsum.photos/seed/banner2/1200/400', 'https://www.talent-ai.com/feature/ai-resume', '2026-03-10 00:00:00', '2026-06-30 23:59:59', 1, 2, '2026-03-08 09:30:00'),
('企业入驻专享福利', 'https://picsum.photos/seed/banner3/1200/400', 'https://www.talent-ai.com/enterprise/welcome', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 0, 3, '2025-12-28 14:20:00');

-- ---------------------------------------------------------------------
-- 系统公告 announcement
-- level : info / warning / critical
-- target: candidate / hr / all
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`          VARCHAR(128) NOT NULL COMMENT '标题',
  `content`        TEXT         NOT NULL COMMENT '内容',
  `level`          VARCHAR(16)  NOT NULL DEFAULT 'info' COMMENT '级别：info/warning/critical',
  `target`         VARCHAR(16)  NOT NULL DEFAULT 'all' COMMENT '目标人群：candidate/hr/all',
  `broadcasted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已广播：0=否, 1=是',
  `broadcasted_at` DATETIME     DEFAULT NULL COMMENT '广播时间',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告';

INSERT INTO `announcement` (`title`, `content`, `level`, `target`, `broadcasted`, `broadcasted_at`, `created_at`) VALUES
('系统维护通知', '平台将于本周日凌晨 02:00-04:00 进行系统升级维护，期间部分功能可能不可用，敬请谅解。', 'warning', 'all', 0, NULL, '2026-03-15 11:00:00'),
('简历投递功能优化', '我们优化了简历投递流程，现在支持一键投递多个职位，欢迎候选人体验。', 'info', 'candidate', 1, '2026-03-12 10:00:00', '2026-03-11 16:40:00'),
('紧急：账号安全提醒', '近期发现钓鱼网站冒充本平台，请 HR 用户务必通过官方域名登录，谨防信息泄露。', 'critical', 'hr', 0, NULL, '2026-03-18 09:15:00');
