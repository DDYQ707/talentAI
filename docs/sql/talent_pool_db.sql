-- =============================================================================
-- talent_pool_db  结构 + 种子数据（全量快照）
-- 导出时间: 2026-07-01
-- 微服务: talent-pool  端口: 8086
-- 说明: 由 mysqldump 从本机 MySQL 3306 导出，含 CREATE DATABASE / 表结构 / 数据
-- 导入: mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 < docs/sql/talent_pool_db.sql
-- 注意: 请勿用 PowerShell Get-Content 管道导入（易中文乱码），请用上述重定向或 mysql source
-- =============================================================================
-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: talent_pool_db
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
-- Current Database: `talent_pool_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `talent_pool_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `talent_pool_db`;

--
-- Table structure for table `talent_pool_record`
--

DROP TABLE IF EXISTS `talent_pool_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `talent_pool_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `candidate_id` bigint unsigned NOT NULL COMMENT '???ID?????->auth??',
  `candidate_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???????',
  `current_title` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '??????',
  `resume_id` bigint unsigned DEFAULT NULL COMMENT '??ID?????->resume??',
  `source_application_id` bigint unsigned DEFAULT NULL COMMENT '????ID?????->job??',
  `source_job_title` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????????',
  `interview_summary` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '??????',
  `talent_category` tinyint DEFAULT NULL COMMENT '????',
  `job_seeking_status` tinyint DEFAULT NULL COMMENT '????',
  `match_score` tinyint DEFAULT NULL COMMENT '??????',
  `current_company` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `is_saved` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  `archive_reason` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `archived_by` bigint unsigned DEFAULT NULL COMMENT '??HR',
  `archived_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_talent_category` (`talent_category`),
  KEY `idx_match_score` (`match_score`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='??????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `talent_pool_record`
--

LOCK TABLES `talent_pool_record` WRITE;
/*!40000 ALTER TABLE `talent_pool_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `talent_pool_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `talent_pool_tag`
--

DROP TABLE IF EXISTS `talent_pool_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `talent_pool_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `pool_record_id` bigint unsigned NOT NULL COMMENT '?????ID',
  `tag_id` bigint unsigned NOT NULL COMMENT '??ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pool_tag` (`pool_record_id`,`tag_id`),
  KEY `fk_pt_tag` (`tag_id`),
  CONSTRAINT `fk_pt_pool` FOREIGN KEY (`pool_record_id`) REFERENCES `talent_pool_record` (`id`),
  CONSTRAINT `fk_pt_tag` FOREIGN KEY (`tag_id`) REFERENCES `talent_tag` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='????????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `talent_pool_tag`
--

LOCK TABLES `talent_pool_tag` WRITE;
/*!40000 ALTER TABLE `talent_pool_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `talent_pool_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `talent_tag`
--

DROP TABLE IF EXISTS `talent_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `talent_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `tag_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???',
  `tag_type` tinyint NOT NULL DEFAULT '1' COMMENT '1-?? 2-?? 3-???',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='???????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `talent_tag`
--

LOCK TABLES `talent_tag` WRITE;
/*!40000 ALTER TABLE `talent_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `talent_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'talent_pool_db'
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
