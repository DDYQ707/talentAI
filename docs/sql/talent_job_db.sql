-- =============================================================================
-- talent_job_db  结构 + 种子数据（全量快照）
-- 导出时间: 2026-07-01
-- 微服务: talent-job  端口: 8082
-- 说明: 由 mysqldump 从本机 MySQL 3306 导出，含 CREATE DATABASE / 表结构 / 数据
-- 导入: mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 < docs/sql/talent_job_db.sql
-- 注意: 请勿用 PowerShell Get-Content 管道导入（易中文乱码），请用上述重定向或 mysql source
-- =============================================================================
-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: talent_job_db
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `talent_job_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `talent_job_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `talent_job_db`;

--
-- Table structure for table `application_stage_log`
--

DROP TABLE IF EXISTS `application_stage_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `application_stage_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `application_id` bigint unsigned NOT NULL COMMENT '??ID',
  `from_stage` tinyint DEFAULT NULL COMMENT '???',
  `to_stage` tinyint NOT NULL COMMENT '???',
  `operator_id` bigint unsigned DEFAULT NULL COMMENT '???ID??????',
  `operator_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '???????',
  `action_note` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_stage_app` FOREIGN KEY (`application_id`) REFERENCES `job_application` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='????????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `application_stage_log`
--

LOCK TABLES `application_stage_log` WRITE;
/*!40000 ALTER TABLE `application_stage_log` DISABLE KEYS */;
INSERT INTO `application_stage_log` VALUES (2,38,NULL,1,19,'刘洋','候选人投递简历','2026-07-01 15:51:29','2026-07-01 15:51:29',0),(3,39,NULL,1,20,'吴芳','候选人投递简历','2026-07-01 15:52:11','2026-07-01 15:52:11',0),(4,40,NULL,1,21,'周杰','候选人投递简历','2026-07-01 15:52:48','2026-07-01 15:52:48',0),(5,41,NULL,1,22,'孙婷','候选人投递简历','2026-07-01 15:53:31','2026-07-01 15:53:31',0),(6,38,1,3,15,'HR演示','HR 标记为面试中：安排面试','2026-07-01 15:55:12','2026-07-01 15:55:12',0),(7,38,3,3,15,'HR演示','HR 标记为面试中：安排面试','2026-07-01 15:55:20','2026-07-01 15:55:20',0),(8,38,3,3,15,'HR演示','HR 标记为面试中：安排面试','2026-07-01 16:00:47','2026-07-01 16:00:47',0),(9,42,NULL,1,23,'张伟','候选人投递简历','2026-07-01 16:04:44','2026-07-01 16:04:44',0),(10,42,1,3,15,'HR演示','HR 标记为面试中：安排面试','2026-07-01 16:06:26','2026-07-01 16:06:26',0),(11,42,3,3,15,'HR演示','HR 标记为面试中：安排面试','2026-07-01 16:06:33','2026-07-01 16:06:33',0),(12,42,3,6,NULL,'系统','Offer 已发放','2026-07-01 16:09:37','2026-07-01 16:09:37',0),(13,42,6,7,23,'张伟','候选人接受 Offer','2026-07-01 16:10:34','2026-07-01 16:10:34',0),(14,43,NULL,1,24,'李娜','候选人投递简历','2026-07-01 18:25:49','2026-07-01 18:25:49',0),(15,44,NULL,1,25,'王强','候选人投递简历','2026-07-01 18:26:45','2026-07-01 18:26:45',0),(16,45,NULL,1,27,'郑爽','候选人投递简历','2026-07-01 18:28:30','2026-07-01 18:28:30',0),(17,44,1,3,15,'HR演示','HR 标记为面试中：安排面试','2026-07-01 18:30:45','2026-07-01 18:30:45',0),(18,44,3,3,15,'HR演示','HR 标记为面试中：安排面试','2026-07-01 18:30:49','2026-07-01 18:30:49',0);
/*!40000 ALTER TABLE `application_stage_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job_application`
--

DROP TABLE IF EXISTS `job_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_application` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `application_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `job_id` bigint unsigned NOT NULL COMMENT '??ID',
  `job_title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??????',
  `candidate_id` bigint unsigned NOT NULL COMMENT '?????ID?????->auth??',
  `candidate_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???????',
  `resume_id` bigint unsigned NOT NULL COMMENT '??ID?????->resume??',
  `channel` tinyint DEFAULT NULL COMMENT '???1-BOSS 2-?? 3-?? 4-?? 5-??',
  `current_stage` tinyint NOT NULL DEFAULT '1' COMMENT '??????',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1-??? 2-??? 3-??? 4-???',
  `match_score` tinyint DEFAULT NULL COMMENT 'AI???0-100',
  `is_starred` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'HR??',
  `applied_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `rejected_at` datetime DEFAULT NULL COMMENT '????',
  `hired_at` datetime DEFAULT NULL COMMENT '????',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HR??',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_status_stage` (`status`,`current_stage`),
  KEY `idx_match_score` (`match_score`),
  KEY `idx_applied_at` (`applied_at`),
  CONSTRAINT `fk_app_job` FOREIGN KEY (`job_id`) REFERENCES `job_post` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_application`
--

LOCK TABLES `job_application` WRITE;
/*!40000 ALTER TABLE `job_application` DISABLE KEYS */;
INSERT INTO `job_application` VALUES (38,'APP202607011551292283',3,'全栈开发工程师',19,'刘洋',8,5,3,1,85,0,'2026-07-01 15:51:29',NULL,NULL,'安排面试','2026-07-01 15:51:29','2026-07-01 16:00:48',0),(39,'APP202607011552117576',8,'算法工程师（推荐方向）',20,'吴芳',9,5,1,1,85,0,'2026-07-01 15:52:12',NULL,NULL,NULL,'2026-07-01 15:52:11','2026-07-01 15:52:11',0),(40,'APP202607011552488700',5,'测试开发工程师',21,'周杰',10,5,1,1,30,0,'2026-07-01 15:52:49',NULL,NULL,NULL,'2026-07-01 15:52:48','2026-07-01 15:52:48',0),(41,'APP202607011553313370',5,'测试开发工程师',22,'孙婷',11,5,1,1,85,0,'2026-07-01 15:53:31',NULL,NULL,NULL,'2026-07-01 15:53:31','2026-07-01 15:53:31',0),(42,'APP202607011604449100',2,'Java 后端开发工程师',23,'张伟',12,5,7,2,90,0,'2026-07-01 16:04:44',NULL,'2026-07-01 16:10:34','安排面试','2026-07-01 16:04:44','2026-07-01 16:10:34',0),(43,'APP202607011825493727',1,'高级前端工程师',24,'李娜',13,5,1,1,85,0,'2026-07-01 18:25:50',NULL,NULL,NULL,'2026-07-01 18:25:49','2026-07-01 18:25:49',0),(44,'APP202607011826456964',6,'数据分析师',25,'王强',14,5,3,1,90,0,'2026-07-01 18:26:46',NULL,NULL,'安排面试','2026-07-01 18:26:45','2026-07-01 18:30:50',0),(45,'APP202607011828301498',6,'数据分析师',27,'郑爽',16,5,1,1,60,0,'2026-07-01 18:28:30',NULL,NULL,NULL,'2026-07-01 18:28:30','2026-07-01 18:28:30',0);
/*!40000 ALTER TABLE `job_application` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job_favorite`
--

DROP TABLE IF EXISTS `job_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_favorite` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `candidate_id` bigint unsigned NOT NULL COMMENT '?????ID?????->auth??',
  `job_id` bigint unsigned NOT NULL COMMENT '??ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_candidate_job` (`candidate_id`,`job_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `fk_fav_job` (`job_id`),
  CONSTRAINT `fk_fav_job` FOREIGN KEY (`job_id`) REFERENCES `job_post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='????????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_favorite`
--

LOCK TABLES `job_favorite` WRITE;
/*!40000 ALTER TABLE `job_favorite` DISABLE KEYS */;
/*!40000 ALTER TABLE `job_favorite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job_post`
--

DROP TABLE IF EXISTS `job_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_post` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `job_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `dept_id` bigint unsigned NOT NULL COMMENT '????ID?????->auth??',
  `dept_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??????',
  `publisher_id` bigint unsigned NOT NULL COMMENT '???HR??ID?????->auth??',
  `publisher_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???????',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1-??? 2-?? 3-???',
  `employment_type` tinyint NOT NULL DEFAULT '1' COMMENT '1-?? 2-?? 3-??',
  `work_city` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `is_remote` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  `salary_min` int DEFAULT NULL COMMENT '?????/?',
  `salary_max` int DEFAULT NULL COMMENT '?????/?',
  `salary_negotiable` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  `headcount` int NOT NULL DEFAULT '1' COMMENT '????',
  `priority` tinyint NOT NULL DEFAULT '2' COMMENT '1-? 2-? 3-?',
  `experience_years_min` tinyint DEFAULT NULL COMMENT '??????',
  `education_requirement` tinyint DEFAULT NULL COMMENT '????',
  `job_description` text COLLATE utf8mb4_unicode_ci COMMENT '????JD',
  `job_requirements` text COLLATE utf8mb4_unicode_ci COMMENT '????',
  `skill_tags` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????????',
  `applied_count` int NOT NULL DEFAULT '0' COMMENT '???',
  `matched_count` int NOT NULL DEFAULT '0' COMMENT '?????',
  `published_at` datetime DEFAULT NULL COMMENT '????',
  `closed_at` datetime DEFAULT NULL COMMENT '????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_work_city` (`work_city`),
  KEY `idx_published_at` (`published_at`),
  FULLTEXT KEY `ft_title` (`title`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_post`
--

LOCK TABLES `job_post` WRITE;
/*!40000 ALTER TABLE `job_post` DISABLE KEYS */;
INSERT INTO `job_post` VALUES (1,'JOB2026062201','高级前端工程师',1,'技术研发部',15,'HR演示',1,1,'北京',0,25000,40000,0,2,1,3,2,'负责智能招聘与人才画像系统候选人端、HR 端前端架构与核心页面开发；参与组件库建设、性能优化与工程规范落地。','1. 3 年及以上前端开发经验，本科及以上学历；\n2. 精通 Vue3、TypeScript，熟悉 Pinia、Vite；\n3. 有 B 端或招聘类产品经验优先；\n4. 具备良好的代码规范与协作意识。','Vue3,TypeScript,前端工程化,性能优化',1,0,'2026-07-01 15:38:06',NULL,'2026-07-01 15:38:06','2026-07-01 18:25:49',0),(2,'JOB2026062202','Java 后端开发工程师',1,'技术研发部',15,'HR演示',1,1,'北京',0,20000,35000,0,2,2,3,2,'参与微服务后端研发，负责岗位、简历、投递、面试等核心业务模块的 API 设计与实现。','1. 3 年及以上 Java 开发经验；\n2. 熟悉 Spring Boot、Spring Cloud、MyBatis-Plus；\n3. 熟悉 MySQL、Redis，了解消息队列；\n4. 有微服务拆分与 Feign 联调经验优先。','Java,Spring Boot,MySQL,微服务',1,0,'2026-07-01 15:38:06',NULL,'2026-07-01 15:38:06','2026-07-01 16:04:44',0),(3,'JOB2026062203','全栈开发工程师',1,'技术研发部',15,'HR演示',1,1,'上海',0,22000,38000,0,2,2,2,2,'独立负责招聘系统部分业务模块的前后端联调与交付，覆盖接口设计、页面实现与基础运维支持。','1. 2 年及以上全栈经验；\n2. 掌握 Vue3 与 Java/Spring Boot 技术栈；\n3. 能独立完成需求分析、开发与联调；\n4. 学习能力强，沟通顺畅。','Vue3,Java,全栈,REST API',1,0,'2026-07-01 15:38:06',NULL,'2026-07-01 15:38:06','2026-07-01 15:51:29',0),(4,'JOB2026062204','产品经理',2,'产品部',15,'HR演示',1,1,'北京',0,18000,30000,0,2,2,3,2,'负责智能招聘平台候选人体验、HR 工作台及 AI 能力产品化设计，推动需求评审与版本迭代。','1. 3 年及以上 B 端或 HR SaaS 产品经验；\n2. 熟悉招聘流程、简历筛选、面试安排等业务场景；\n3. 能输出 PRD、原型并与研发高效协作；\n4. 对 AI 招聘场景有理解者优先。','产品设计,招聘 SaaS,需求分析,原型',0,0,'2026-07-01 15:38:06',NULL,'2026-07-01 15:38:06','2026-07-01 15:38:06',0),(5,'JOB2026062205','测试开发工程师',1,'技术研发部',15,'HR演示',1,1,'深圳',0,15000,28000,0,2,3,2,2,'负责招聘系统功能测试、接口自动化与回归体系建设，保障发版质量与核心链路稳定性。','1. 2 年及以上测试或测开经验；\n2. 熟悉接口测试、Postman/JMeter 等工具；\n3. 了解 Java 或 Python 自动化脚本；\n4. 细心负责，能独立编写测试用例。','接口测试,自动化测试,Java,质量保障',2,0,'2026-07-01 15:38:06',NULL,'2026-07-01 15:38:06','2026-07-01 15:53:31',0),(6,'JOB2026062206','数据分析师',3,'数据智能部',15,'HR演示',1,1,'上海',0,16000,28000,0,2,2,2,2,'围绕招聘漏斗、人岗匹配效果、简历转化等主题开展数据分析，输出可视化报表与业务洞察。','1. 2 年及以上数据分析经验；\n2. 熟练 SQL，掌握 Python 或 Excel 高级分析；\n3. 熟悉 BI 工具，能独立完成专题分析；\n4. 有 HR 或招聘数据经验优先。','SQL,Python,数据分析,BI',2,0,'2026-07-01 15:38:06',NULL,'2026-07-01 15:38:06','2026-07-01 18:28:30',0),(7,'JOB2026062207','UI/UX 设计师',4,'设计部',15,'HR演示',1,1,'北京',0,15000,25000,0,2,3,2,2,'负责招聘平台多端界面设计、交互规范与视觉体系维护，提升候选人投递与 HR 操作体验。','1. 2 年及以上互联网产品设计经验；\n2. 精通 Figma，具备 B 端设计经验；\n3. 理解招聘业务场景与用户路径；\n4. 作品集完整，沟通表达清晰。','Figma,交互设计,B端设计,视觉规范',0,0,'2026-07-01 15:38:06',NULL,'2026-07-01 15:38:06','2026-07-01 15:38:06',0),(8,'JOB2026062208','算法工程师（推荐方向）',3,'数据智能部',15,'HR演示',1,1,'北京',0,30000,50000,0,2,1,3,3,'参与简历解析、人岗匹配、人才画像等 AI 能力建设，优化模型效果与线上指标。','1. 硕士及以上学历，3 年及以上算法经验；\n2. 熟悉 Python、机器学习/NLP 基础；\n3. 有推荐系统、RAG 或 LLM 应用经验优先；\n4. 具备工程落地与效果评估能力。','Python,机器学习,推荐系统,LLM',1,0,'2026-07-01 15:38:06',NULL,'2026-07-01 15:38:06','2026-07-01 15:52:11',0),(9,'JOB2026062209','HRBP',5,'人力资源部',15,'HR演示',1,1,'北京',0,12000,20000,0,2,3,2,2,'支持技术研发团队招聘与人才发展，协调面试安排、Offer 沟通与入职跟进。','1. 2 年及以上 HR 或招聘经验；\n2. 熟悉社招流程与候选人沟通；\n3. 服务意识强，执行力好；\n4. 有互联网或科技行业背景优先。','招聘,HRBP,员工关系,沟通协调',0,0,'2026-07-01 15:38:06',NULL,'2026-07-01 15:38:06','2026-07-01 15:38:06',0),(10,'JOB2026062210','项目经理',2,'产品部',15,'HR演示',1,1,'上海',0,20000,35000,0,2,2,5,2,'统筹智能招聘平台版本规划、跨团队协同与里程碑交付，保障项目按期高质量上线。','1. 5 年及以上项目管理经验；\n2. 熟悉敏捷开发，有互联网产品交付经验；\n3. 具备优秀的沟通、风险识别与推进能力；\n4. PMP 或同类认证优先。','项目管理,敏捷,跨部门协作,交付管理',0,0,'2026-07-01 15:38:06',NULL,'2026-07-01 15:38:06','2026-07-01 15:38:06',0),(11,NULL,'ui设计师',1,'设计部',15,'HR演示',1,1,'杭州',0,25000,40000,0,1,2,NULL,NULL,NULL,NULL,NULL,0,0,'2026-07-01 15:40:18',NULL,'2026-07-01 15:40:17','2026-07-01 15:40:17',0),(13,NULL,'高薪兼职日结',1,'风控演示部',15,'HR演示',2,2,'杭州',0,8000,15000,0,1,1,NULL,NULL,'日结高薪兼职，无需经验，轻松月入过万（演示风控下架岗位）',NULL,NULL,0,0,'2026-06-20 10:00:00','2026-06-25 15:39:03','2026-07-01 17:17:54','2026-07-01 17:17:54',0);
/*!40000 ALTER TABLE `job_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `offer`
--

DROP TABLE IF EXISTS `offer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `offer` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `offer_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Offer??',
  `application_id` bigint unsigned NOT NULL COMMENT '??ID',
  `job_id` bigint unsigned NOT NULL COMMENT '??ID',
  `candidate_id` bigint unsigned NOT NULL COMMENT '???ID??????',
  `candidate_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???????',
  `job_title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??????',
  `dept_id` bigint unsigned NOT NULL COMMENT '????ID??????',
  `dept_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????????',
  `base_salary` int DEFAULT NULL COMMENT '???',
  `bonus_months` tinyint DEFAULT NULL COMMENT '?????',
  `start_date` date DEFAULT NULL COMMENT '??????',
  `expire_at` datetime DEFAULT NULL COMMENT 'Offer??????',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1-?? 2-??? 3-??? 4-??? 5-??? 6-???',
  `approver_id` bigint unsigned DEFAULT NULL COMMENT '?????ID??????',
  `approver_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '?????????',
  `sent_at` datetime DEFAULT NULL COMMENT '????',
  `accepted_at` datetime DEFAULT NULL COMMENT '????',
  `expired_at` datetime DEFAULT NULL COMMENT '????????',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '???HR ID',
  `created_by_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '???????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  `annual_salary` decimal(10,2) DEFAULT NULL COMMENT '????',
  `bonus` decimal(10,2) DEFAULT NULL COMMENT '???',
  `salary_remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `position_level` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '??',
  `expected_onboard_date` date DEFAULT NULL COMMENT '??????',
  `probation_months` int DEFAULT NULL COMMENT '?????',
  `hr_id` bigint unsigned DEFAULT NULL COMMENT 'HR ID',
  `hr_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HR??',
  `remark` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Offer??',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offer_no` (`offer_no`),
  UNIQUE KEY `uk_application_id` (`application_id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_at` (`expire_at`),
  KEY `fk_offer_job` (`job_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer?';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `offer`
--

LOCK TABLES `offer` WRITE;
/*!40000 ALTER TABLE `offer` DISABLE KEYS */;
INSERT INTO `offer` VALUES (1,'OFR-20260701-3597',42,2,23,'张伟','Java 后端开发工程师',1,'技术研发部',25000,NULL,NULL,NULL,6,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-01 16:09:27','2026-07-01 16:09:27',0,500000.00,NULL,NULL,'p3','2026-07-10',3,15,NULL,NULL);
/*!40000 ALTER TABLE `offer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `offer_approval`
--

DROP TABLE IF EXISTS `offer_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `offer_approval` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `offer_id` bigint unsigned NOT NULL COMMENT 'Offer ID',
  `approver_id` bigint unsigned NOT NULL COMMENT '???ID??????',
  `approver_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???????',
  `status` tinyint NOT NULL COMMENT '1-??? 2-?? 3-??',
  `comment` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `approved_at` datetime DEFAULT NULL COMMENT '????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  `seq` int NOT NULL DEFAULT '1' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_offer_id` (`offer_id`),
  KEY `idx_approver_id` (`approver_id`),
  CONSTRAINT `fk_oa_offer` FOREIGN KEY (`offer_id`) REFERENCES `offer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer?????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `offer_approval`
--

LOCK TABLES `offer_approval` WRITE;
/*!40000 ALTER TABLE `offer_approval` DISABLE KEYS */;
/*!40000 ALTER TABLE `offer_approval` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'talent_job_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-01 18:58:03
