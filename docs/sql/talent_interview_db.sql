-- =============================================================================
-- talent_interview_db  结构 + 种子数据（全量快照）
-- 导出时间: 2026-07-01
-- 微服务: talent-interview  端口: 8085
-- 说明: 由 mysqldump 从本机 MySQL 3306 导出，含 CREATE DATABASE / 表结构 / 数据
-- 导入: mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 < docs/sql/talent_interview_db.sql
-- 注意: 请勿用 PowerShell Get-Content 管道导入（易中文乱码），请用上述重定向或 mysql source
-- =============================================================================
-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: talent_interview_db
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
-- Current Database: `talent_interview_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `talent_interview_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `talent_interview_db`;

--
-- Table structure for table `interview`
--

DROP TABLE IF EXISTS `interview`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `interview` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `application_id` bigint unsigned NOT NULL COMMENT '??ID',
  `job_id` bigint unsigned NOT NULL COMMENT '??ID',
  `candidate_id` bigint unsigned NOT NULL COMMENT '???ID??????',
  `candidate_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???????',
  `job_title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????????',
  `interviewer_id` bigint unsigned NOT NULL COMMENT '???ID??????',
  `interviewer_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???????',
  `round_no` tinyint NOT NULL DEFAULT '1' COMMENT '????',
  `round_type` tinyint NOT NULL COMMENT '????',
  `interview_mode` tinyint NOT NULL DEFAULT '1' COMMENT '1-?? 2-?? 3-????',
  `scheduled_start` datetime NOT NULL COMMENT '??????',
  `scheduled_end` datetime DEFAULT NULL COMMENT '??????',
  `meeting_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '??????',
  `location` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1-??? 2-??? 3-??? 4-???',
  `total_score` decimal(4,1) DEFAULT NULL COMMENT '????',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '???HR ID??????',
  `created_by_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '???????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_interviewer_id` (`interviewer_id`),
  KEY `idx_scheduled_start` (`scheduled_start`),
  KEY `idx_status` (`status`),
  KEY `idx_job_candidate` (`job_id`,`candidate_id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `interview`
--

LOCK TABLES `interview` WRITE;
/*!40000 ALTER TABLE `interview` DISABLE KEYS */;
INSERT INTO `interview` VALUES (22,38,3,19,'刘洋','全栈开发工程师',18,'面试官03',1,2,1,'2026-07-01 15:55:00',NULL,'33445566',NULL,2,74.0,15,'HR演示','2026-07-01 15:55:13','2026-07-01 15:59:47',0),(23,38,3,19,'刘洋','全栈开发工程师',17,'面试官02',2,4,1,'2026-07-01 16:00:00',NULL,'33445566',NULL,2,81.3,15,'HR演示','2026-07-01 16:00:48','2026-07-01 16:02:29',0),(24,42,2,23,'张伟','Java 后端开发工程师',16,'面试官01',1,2,1,'2026-07-01 17:07:00',NULL,'223344',NULL,2,81.3,15,'HR演示','2026-07-01 16:06:27','2026-07-01 16:08:25',0),(25,44,6,25,'王强','数据分析师',16,'面试官01',1,2,1,'2026-07-01 22:30:00',NULL,'33445566',NULL,1,NULL,15,'HR演示','2026-07-01 18:30:45','2026-07-01 18:30:45',0);
/*!40000 ALTER TABLE `interview` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `interview_evaluation`
--

DROP TABLE IF EXISTS `interview_evaluation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `interview_evaluation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `interview_id` bigint unsigned NOT NULL COMMENT '??ID',
  `evaluator_id` bigint unsigned NOT NULL COMMENT '???ID??????',
  `evaluator_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???????',
  `dimension_scores` json DEFAULT NULL COMMENT '????JSON',
  `overall_score` decimal(4,1) DEFAULT NULL COMMENT '????',
  `conclusion` tinyint DEFAULT NULL COMMENT '1-?? 2-?? 3-???',
  `comment` text COLLATE utf8mb4_unicode_ci COMMENT '????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_evaluator_id` (`evaluator_id`),
  CONSTRAINT `fk_eval_interview` FOREIGN KEY (`interview_id`) REFERENCES `interview` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `interview_evaluation`
--

LOCK TABLES `interview_evaluation` WRITE;
/*!40000 ALTER TABLE `interview_evaluation` DISABLE KEYS */;
INSERT INTO `interview_evaluation` VALUES (1,22,18,'面试官03','{\"专业技能\": 82, \"沟通能力\": 65, \"岗位匹配度\": 75}',74.0,2,'刘洋在技术方面展现出较强的能力，尤其是在项目经验上。但是，在沟通表达方面存在不足，虽然对问题的理解是准确的，但在描述解决方案时不够清晰流畅。此外，他拥有处理高并发场景的经验，但这一部分需要进一步验证以确保与岗位需求相匹配。\n\n关键信号：\n· 项目经验丰富\n· 技术理解深刻\n· 表述不清需改进','2026-07-01 15:59:47','2026-07-01 15:59:47',0),(2,23,17,'面试官02','{\"专业技能\": 85, \"沟通能力\": 78, \"岗位匹配度\": 81}',81.3,1,'候选人刘洋在项目经验方面表现突出，能够清晰地阐述过往项目的挑战及解决方案。沟通表达能力良好但有提升空间，特别是在解释复杂概念时。逻辑思维较强，能够合理规划个人职业发展路径。团队合作经历丰富，展示了良好的解决问题技巧。学习能力强，对于新技术保持高度好奇并愿意投入时间掌握。\n\n关键信号：\n· 量化成果意识强\n· 高并发经验需进一步验证','2026-07-01 16:02:29','2026-07-01 16:02:29',0),(3,24,16,'面试官01','{\"专业技能\": 85, \"沟通能力\": 78, \"岗位匹配度\": 81}',81.3,1,'张伟在项目经验方面表现突出，特别是微服务拆分和分布式事务处理上有着丰富的实战经历。尽管在表述时略显紧张导致说话结巴，但其逻辑清晰、思路明确。对于Spring Cloud相关技术的理解深入，并能结合实际案例进行阐述。不过，在高并发场景下的数据库设计与优化方面提出了较为基础的解决方案，建议进一步考察其具体实施能力。此外，关于Feign的使用经验较少。\n\n关键信号：\n· 量化成果意识强\n· 高并发经验需进一步验证','2026-07-01 16:08:25','2026-07-01 16:08:25',0);
/*!40000 ALTER TABLE `interview_evaluation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'talent_interview_db'
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
