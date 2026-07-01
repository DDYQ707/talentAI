-- =============================================================================
-- talent_auth_db  结构 + 种子数据（全量快照）
-- 导出时间: 2026-07-01
-- 微服务: talent-auth  端口: 8081
-- 说明: 由 mysqldump 从本机 MySQL 3306 导出，含 CREATE DATABASE / 表结构 / 数据
-- 导入: mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 < docs/sql/talent_auth_db.sql
-- 注意: 请勿用 PowerShell Get-Content 管道导入（易中文乱码），请用上述重定向或 mysql source
-- =============================================================================
-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: talent_auth_db
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
-- Current Database: `talent_auth_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `talent_auth_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `talent_auth_db`;

--
-- Table structure for table `auth_verification_code`
--

DROP TABLE IF EXISTS `auth_verification_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_verification_code` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `account` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??????',
  `code` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???',
  `code_type` tinyint NOT NULL DEFAULT '1' COMMENT '1-?? 2-?? 3-????',
  `expire_at` datetime NOT NULL COMMENT '????',
  `is_used` tinyint(1) NOT NULL DEFAULT '0' COMMENT '?????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_account_type` (`account`,`code_type`),
  KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auth_verification_code`
--

LOCK TABLES `auth_verification_code` WRITE;
/*!40000 ALTER TABLE `auth_verification_code` DISABLE KEYS */;
INSERT INTO `auth_verification_code` VALUES (1,'hr@company.com','741628',1,'2026-07-01 18:24:52',1,'2026-07-01 18:19:51','2026-07-01 18:19:51',0);
/*!40000 ALTER TABLE `auth_verification_code` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `candidate_profile`
--

DROP TABLE IF EXISTS `candidate_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `candidate_profile` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `user_id` bigint unsigned NOT NULL COMMENT '?????ID',
  `real_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `gender` tinyint DEFAULT NULL COMMENT '???0-?? 1-? 2-?',
  `birth_date` date DEFAULT NULL COMMENT '????',
  `current_title` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `work_years` decimal(3,1) DEFAULT NULL COMMENT '????',
  `highest_edu` tinyint DEFAULT NULL COMMENT '?????1-?? 2-?? 3-?? 4-??',
  `city` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `job_seeking_status` tinyint DEFAULT NULL COMMENT '?????1-???? 2-???? 3-????',
  `resume_completeness` tinyint NOT NULL DEFAULT '0' COMMENT '?????0-100',
  `ai_score` tinyint DEFAULT NULL COMMENT 'AI????0-100',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_city` (`city`),
  KEY `idx_job_seeking_status` (`job_seeking_status`),
  CONSTRAINT `fk_cp_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='????????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `candidate_profile`
--

LOCK TABLES `candidate_profile` WRITE;
/*!40000 ALTER TABLE `candidate_profile` DISABLE KEYS */;
INSERT INTO `candidate_profile` VALUES (8,19,'刘洋',1,'1994-06-12','全栈开发工程师',5.0,2,'上海',2,25,85,'2026-07-01 15:49:54','2026-07-01 15:52:01',0),(9,20,'吴芳',2,'1993-11-08','算法工程师',4.0,3,'北京',2,25,88,'2026-07-01 15:49:54','2026-07-01 15:52:45',0),(10,21,'周杰',1,'1992-03-25','DevOps运维工程师',6.0,2,'深圳',2,25,88,'2026-07-01 15:49:54','2026-07-01 15:53:21',0),(11,22,'孙婷',2,'1996-09-18','软件测试工程师',3.0,2,'北京',2,25,88,'2026-07-01 15:49:54','2026-07-01 15:54:05',0),(12,23,'张伟',1,'1991-07-03','Java后端开发工程师',7.0,2,'北京',2,25,88,'2026-07-01 15:49:54','2026-07-01 16:05:26',0),(13,24,'李娜',2,'1995-02-14','前端开发工程师',4.0,2,'上海',2,25,88,'2026-07-01 15:49:54','2026-07-01 18:26:11',0),(14,25,'王强',1,'1990-12-01','数据分析师',5.5,2,'上海',2,25,88,'2026-07-01 15:49:54','2026-07-01 18:27:09',0),(15,26,'赵敏',2,'1997-05-20','UI设计师',3.0,2,'北京',2,25,86,'2026-07-01 15:49:54','2026-07-01 18:27:44',0),(16,27,'郑爽',2,'1994-10-30','数据库管理员',6.0,2,'北京',2,25,NULL,'2026-07-01 15:49:54','2026-07-01 18:48:10',0),(25,30,'陈磊',1,'1996-03-12','高级产品经理',6.0,3,'广州',2,93,87,'2026-07-01 18:40:29','2026-07-01 18:42:15',0);
/*!40000 ALTER TABLE `candidate_profile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_department`
--

DROP TABLE IF EXISTS `sys_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_department` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '???ID?0???',
  `dept_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `dept_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '???',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '???0-?? 1-??',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '?????0-? 1-?',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_dept_name` (`dept_name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='???';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_department`
--

LOCK TABLES `sys_department` WRITE;
/*!40000 ALTER TABLE `sys_department` DISABLE KEYS */;
INSERT INTO `sys_department` VALUES (1,0,'技术研发部','TECH',1,1,'2026-07-01 15:38:06','2026-07-01 15:38:06',0),(2,0,'产品部','PRODUCT',2,1,'2026-07-01 15:38:06','2026-07-01 15:38:06',0),(3,0,'数据智能部','DATA_AI',3,1,'2026-07-01 15:38:06','2026-07-01 15:38:06',0),(4,0,'设计部','DESIGN',4,1,'2026-07-01 15:38:06','2026-07-01 15:38:06',0),(5,0,'人力资源部','HR',5,1,'2026-07-01 15:38:06','2026-07-01 15:38:06',0);
/*!40000 ALTER TABLE `sys_department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notification`
--

DROP TABLE IF EXISTS `sys_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `user_id` bigint unsigned NOT NULL COMMENT '???ID',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??',
  `content` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '??',
  `notify_type` tinyint NOT NULL COMMENT '1-?? 2-?? 3-??',
  `biz_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `biz_id` bigint unsigned DEFAULT NULL COMMENT '??ID',
  `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`,`is_read`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_notify_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='???????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notification`
--

LOCK TABLES `sys_notification` WRITE;
/*!40000 ALTER TABLE `sys_notification` DISABLE KEYS */;
INSERT INTO `sys_notification` VALUES (2,19,'投递成功','您已成功投递「全栈开发工程师」，请耐心等待 HR 初筛。',2,'application',38,0,'2026-07-01 15:51:29','2026-07-01 15:51:29',0),(3,20,'投递成功','您已成功投递「算法工程师（推荐方向）」，请耐心等待 HR 初筛。',2,'application',39,0,'2026-07-01 15:52:12','2026-07-01 15:52:12',0),(4,21,'投递成功','您已成功投递「测试开发工程师」，请耐心等待 HR 初筛。',2,'application',40,0,'2026-07-01 15:52:49','2026-07-01 15:52:49',0),(5,22,'投递成功','您已成功投递「测试开发工程师」，请耐心等待 HR 初筛。',2,'application',41,0,'2026-07-01 15:53:32','2026-07-01 15:53:32',0),(6,19,'进入面试环节','您投递的「全栈开发工程师」已进入面试环节，请关注后续安排。',1,'application',38,0,'2026-07-01 15:55:20','2026-07-01 15:55:20',0),(7,19,'进入面试环节','您投递的「全栈开发工程师」已进入面试环节，请关注后续安排。',1,'application',38,0,'2026-07-01 15:55:29','2026-07-01 15:55:29',0),(8,19,'面试安排通知','您投递的「全栈开发工程师」已安排第 1 轮面试，时间：2026-07-01 15:55，线上会议已安排，请准时参加。',1,'interview',22,0,'2026-07-01 15:55:29','2026-07-01 15:55:29',0),(9,16,'面试安排通知','您有一场新的面试安排：候选人 刘洋，岗位「全栈开发工程师」，第 1 轮，时间：2026-07-01 15:55，线上会议已安排，请提前在「面试准备」中查看候选人信息。',1,'interview',22,1,'2026-07-01 15:55:29','2026-07-01 15:57:38',0),(10,18,'面试改派通知','HR 已将一场面试改派给您：候选人 刘洋，岗位「全栈开发工程师」，第 1 轮，时间：2026-07-01 15:55，线上会议已安排，请登录面试官端查看详情。',1,'interview',22,1,'2026-07-01 15:57:19','2026-07-01 15:58:09',0),(11,19,'进入面试环节','您投递的「全栈开发工程师」已进入面试环节，请关注后续安排。',1,'application',38,0,'2026-07-01 16:00:55','2026-07-01 16:00:55',0),(12,19,'面试安排通知','您投递的「全栈开发工程师」已安排第 2 轮面试，时间：2026-07-01 16:00，线上会议已安排，请准时参加。',1,'interview',23,0,'2026-07-01 16:00:55','2026-07-01 16:00:55',0),(13,17,'面试安排通知','您有一场新的面试安排：候选人 刘洋，岗位「全栈开发工程师」，第 2 轮，时间：2026-07-01 16:00，线上会议已安排，请提前在「面试准备」中查看候选人信息。',1,'interview',23,0,'2026-07-01 16:00:55','2026-07-01 16:00:55',0),(14,23,'投递成功','您已成功投递「Java 后端开发工程师」，请耐心等待 HR 初筛。',2,'application',42,0,'2026-07-01 16:04:44','2026-07-01 16:04:44',0),(15,23,'进入面试环节','您投递的「Java 后端开发工程师」已进入面试环节，请关注后续安排。',1,'application',42,0,'2026-07-01 16:06:33','2026-07-01 16:06:33',0),(16,23,'进入面试环节','您投递的「Java 后端开发工程师」已进入面试环节，请关注后续安排。',1,'application',42,0,'2026-07-01 16:06:40','2026-07-01 16:06:40',0),(17,23,'面试安排通知','您投递的「Java 后端开发工程师」已安排第 1 轮面试，时间：2026-07-01 17:07，线上会议已安排，请准时参加。',1,'interview',24,0,'2026-07-01 16:06:40','2026-07-01 16:06:40',0),(18,16,'面试安排通知','您有一场新的面试安排：候选人 张伟，岗位「Java 后端开发工程师」，第 1 轮，时间：2026-07-01 17:07，线上会议已安排，请提前在「面试准备」中查看候选人信息。',1,'interview',24,0,'2026-07-01 16:06:40','2026-07-01 16:06:40',0),(19,23,'面试通过，待确认录用','恭喜！您投递的「Java 后端开发工程师」已通过面试，HR 已向您发放正式 Offer，请在投递记录中查看并确认是否接受。',1,'offer',1,1,'2026-07-01 16:09:38','2026-07-01 16:10:25',0),(20,23,'Offer 已确认','您已接受「Java 后端开发工程师」的 Offer，请按约定时间办理入职手续。',2,'offer',1,0,'2026-07-01 16:10:34','2026-07-01 16:10:34',0),(21,24,'投递成功','您已成功投递「高级前端工程师」，请耐心等待 HR 初筛。',2,'application',43,0,'2026-07-01 18:25:50','2026-07-01 18:25:50',0),(22,25,'投递成功','您已成功投递「数据分析师」，请耐心等待 HR 初筛。',2,'application',44,0,'2026-07-01 18:26:46','2026-07-01 18:26:46',0),(23,27,'投递成功','您已成功投递「数据分析师」，请耐心等待 HR 初筛。',2,'application',45,0,'2026-07-01 18:28:30','2026-07-01 18:28:30',0),(24,25,'进入面试环节','您投递的「数据分析师」已进入面试环节，请关注后续安排。',1,'application',44,0,'2026-07-01 18:30:49','2026-07-01 18:30:49',0),(25,25,'进入面试环节','您投递的「数据分析师」已进入面试环节，请关注后续安排。',1,'application',44,0,'2026-07-01 18:30:54','2026-07-01 18:30:54',0),(26,25,'面试安排通知','您投递的「数据分析师」已安排第 1 轮面试，时间：2026-07-01 22:30，线上会议已安排，请准时参加。',1,'interview',25,0,'2026-07-01 18:30:54','2026-07-01 18:30:54',0),(27,16,'面试安排通知','您有一场新的面试安排：候选人 王强，岗位「数据分析师」，第 1 轮，时间：2026-07-01 22:30，线上会议已安排，请提前在「面试准备」中查看候选人信息。',1,'interview',25,0,'2026-07-01 18:30:54','2026-07-01 18:30:54',0);
/*!40000 ALTER TABLE `sys_notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_permission`
--

DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '???ID',
  `perm_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `perm_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `module_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `perm_type` tinyint NOT NULL DEFAULT '2' COMMENT '1-?? 2-?? 3-API',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '??',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`),
  KEY `idx_module_name` (`module_name`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='???';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_permission`
--

LOCK TABLES `sys_permission` WRITE;
/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` VALUES (1,0,'job:view','查看岗位','招聘管理',2,1,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(2,0,'job:create','发布岗位','招聘管理',2,2,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(3,0,'job:edit','编辑岗位','招聘管理',2,3,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(4,0,'job:close','关闭岗位','招聘管理',2,4,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(5,0,'resume:view','查看简历','简历管理',2,1,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(6,0,'resume:download','下载简历','简历管理',2,2,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(7,0,'resume:tag','标注简历','简历管理',2,3,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(8,0,'resume:delete','删除简历','简历管理',2,4,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(9,0,'ai:screen','AI初筛','AI功能',2,1,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(10,0,'ai:assistant','AI助手','AI功能',2,2,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(11,0,'ai:interview','AI面试官','AI功能',2,3,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(12,0,'ai:report','AI报告导出','AI功能',2,4,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(13,0,'report:view','查看报表','数据报表',2,1,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(14,0,'report:export','导出报表','数据报表',2,2,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(15,0,'report:custom','自定义报表','数据报表',2,3,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(16,0,'sys:user','用户管理','系统设置',2,1,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(17,0,'sys:role','角色管理','系统设置',2,2,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(18,0,'sys:model','AI模型配置','系统设置',2,3,'2026-07-01 16:25:59','2026-07-01 16:25:59',0),(19,0,'sys:audit','系统审计','系统设置',2,4,'2026-07-01 16:25:59','2026-07-01 16:25:59',0);
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `role_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `role_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `description` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '??',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0-?? 1-??',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='???';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'SUPER_ADMIN','超级管理员','系统最高权限',1,1,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(2,'HR_ADMIN','HR管理员','招聘全流程管理',2,1,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(3,'INTERVIEWER','面试官','面试任务与评价',3,1,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(4,'DEPT_MANAGER','部门负责人','部门进展与Offer审批',4,1,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(5,'READONLY','只读用户','仅查看报表',5,1,'2026-07-01 17:35:00','2026-07-01 17:35:00',0);
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `role_id` bigint unsigned NOT NULL COMMENT '??ID',
  `permission_id` bigint unsigned NOT NULL COMMENT '??ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`),
  KEY `fk_rp_perm` (`permission_id`),
  CONSTRAINT `fk_rp_perm` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`),
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='???????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_permission`
--

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
INSERT INTO `sys_role_permission` VALUES (55,1,1,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(56,1,2,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(57,1,3,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(58,1,4,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(59,1,5,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(60,1,6,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(61,1,7,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(62,1,8,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(63,1,9,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(64,1,10,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(65,1,11,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(66,1,12,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(67,1,13,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(68,1,14,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(69,1,15,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(70,1,16,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(71,1,17,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(72,1,18,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(73,1,19,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(86,2,1,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(87,2,2,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(88,2,3,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(89,2,4,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(90,2,5,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(91,2,6,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(92,2,7,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(93,2,9,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(94,2,10,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(95,2,11,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(96,2,13,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(97,2,14,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(98,3,1,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(99,3,5,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(100,3,11,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(101,3,13,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(102,4,1,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(103,4,5,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(104,4,13,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(105,4,14,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(106,4,15,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(107,5,13,'2026-07-01 17:35:00','2026-07-01 17:35:00',0);
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '?????',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '???',
  `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '??',
  `password_hash` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BCrypt????',
  `nickname` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `avatar_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '??URL',
  `user_type` tinyint NOT NULL COMMENT '?????1-??? 2-HR 3-??? 4-???',
  `dept_id` bigint unsigned DEFAULT NULL COMMENT '????ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '?????0-?? 1-?? 2-??',
  `last_login_at` datetime DEFAULT NULL COMMENT '??????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_user_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_department` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (14,'admin',NULL,NULL,'$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','系统管理员',NULL,4,NULL,1,NULL,'2026-07-01 15:35:37','2026-07-01 15:35:37',0),(15,'hr@company.com',NULL,'hr@company.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','HR演示',NULL,2,NULL,1,NULL,'2026-07-01 15:35:37','2026-07-01 15:35:37',0),(16,'interview@company.com',NULL,'interview@company.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','面试官01',NULL,3,NULL,1,NULL,'2026-07-01 15:35:37','2026-07-01 15:35:37',0),(17,'interview2@company.com',NULL,'interview2@company.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','面试官02',NULL,3,NULL,1,NULL,'2026-07-01 15:35:37','2026-07-01 15:35:37',0),(18,'interview3@company.com',NULL,'interview3@company.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','面试官03',NULL,3,NULL,1,NULL,'2026-07-01 15:35:37','2026-07-01 15:35:37',0),(19,'13910001001','13910001001','liuyang.demo@example.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','刘洋',NULL,1,NULL,1,NULL,'2026-07-01 15:49:54','2026-07-01 15:49:54',0),(20,'13910001002','13910001002','wufang.demo@example.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','吴芳',NULL,1,NULL,1,NULL,'2026-07-01 15:49:54','2026-07-01 15:49:54',0),(21,'13910001003','13910001003','zhoujie.demo@example.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','周杰',NULL,1,NULL,1,NULL,'2026-07-01 15:49:54','2026-07-01 15:49:54',0),(22,'13910001004','13910001004','sunting.demo@example.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','孙婷',NULL,1,NULL,1,NULL,'2026-07-01 15:49:54','2026-07-01 15:49:54',0),(23,'13910001005','13910001005','zhangwei.demo@example.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','张伟',NULL,1,NULL,1,NULL,'2026-07-01 15:49:54','2026-07-01 15:49:54',0),(24,'13910001006','13910001006','lina.demo@example.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','李娜',NULL,1,NULL,1,NULL,'2026-07-01 15:49:54','2026-07-01 15:49:54',0),(25,'13910001007','13910001007','wangqiang.demo@example.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','王强',NULL,1,NULL,1,NULL,'2026-07-01 15:49:54','2026-07-01 15:49:54',0),(26,'13910001008','13910001008','zhaomin.demo@example.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','赵敏',NULL,1,NULL,1,NULL,'2026-07-01 15:49:54','2026-07-01 15:49:54',0),(27,'13910001009','13910001009','zhengshuang.demo@example.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','郑爽',NULL,1,NULL,1,NULL,'2026-07-01 15:49:54','2026-07-01 15:49:54',0),(30,'13500001005','13500001005','chenlei@email.com','$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna','陈磊',NULL,1,NULL,1,NULL,'2026-07-01 18:40:29','2026-07-01 18:40:29',0);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `user_id` bigint unsigned NOT NULL COMMENT '??ID',
  `role_id` bigint unsigned NOT NULL COMMENT '??ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='???????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,14,1,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(2,15,2,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(3,16,3,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(4,17,3,'2026-07-01 17:35:00','2026-07-01 17:35:00',0),(5,18,3,'2026-07-01 17:35:00','2026-07-01 17:35:00',0);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'talent_auth_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-01 18:58:02
