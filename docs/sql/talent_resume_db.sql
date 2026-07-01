-- =============================================================================
-- talent_resume_db  结构 + 种子数据（全量快照）
-- 导出时间: 2026-07-01
-- 微服务: talent-resume  端口: 8083
-- 说明: 由 mysqldump 从本机 MySQL 3306 导出，含 CREATE DATABASE / 表结构 / 数据
-- 导入: mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 < docs/sql/talent_resume_db.sql
-- 注意: 请勿用 PowerShell Get-Content 管道导入（易中文乱码），请用上述重定向或 mysql source
-- =============================================================================
-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: talent_resume_db
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
-- Current Database: `talent_resume_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `talent_resume_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `talent_resume_db`;

--
-- Table structure for table `resume`
--

DROP TABLE IF EXISTS `resume`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `candidate_id` bigint unsigned NOT NULL COMMENT '?????ID?????->auth??',
  `resume_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '??????',
  `summary` text COLLATE utf8mb4_unicode_ci COMMENT '????',
  `parse_status` tinyint NOT NULL DEFAULT '0' COMMENT '0-??? 1-??? 2-?? 3-??',
  `screen_status` tinyint NOT NULL DEFAULT '1' COMMENT '1-??? 2-??? 3-??? 4-???',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_screen_status` (`screen_status`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resume`
--

LOCK TABLES `resume` WRITE;
/*!40000 ALTER TABLE `resume` DISABLE KEYS */;
INSERT INTO `resume` VALUES (8,19,'刘洋-全栈开发工程师.pdf',1,'4年全栈开发经验，精通React + Node.js技术栈，有多个从0到1的项目完整交付经验。具备良好的前后端架构设计能力，善于技术选型和团队协作。',2,5,'2026-07-01 15:51:19','2026-07-01 16:02:29',0),(9,20,'吴芳-算法工程师.pdf',1,'3年算法工程师经验，专注于推荐系统和NLP方向。精通深度学习框架PyTorch和TensorFlow，有大规模推荐系统落地经验。发表过2篇顶会论文。',2,1,'2026-07-01 15:51:59','2026-07-01 15:52:35',0),(10,21,'周杰-DevOps运维工程师.pdf',1,'8年运维及DevOps经验，精通Kubernetes和Docker容器化技术，有大规模集群管理经验（500+节点）。主导过传统IDC向云原生架构的迁移，推动CI/CD流水线建设，发布效率提升5倍。',2,1,'2026-07-01 15:52:35','2026-07-01 15:53:14',0),(11,22,'孙婷-软件测试工程师.pdf',1,'5年软件测试经验，精通功能测试、接口测试和性能测试。熟悉Selenium、JMeter等主流测试工具，有完整的CI /CD自动化测试流水线搭建经验。具备ISTQB认证。',2,1,'2026-07-01 15:53:20','2026-07-01 15:53:56',0),(12,23,'张伟_Java后端开发工程师.pdf',1,'4年Java后端开发经验，熟练掌握Spring Boot、Spring Cloud微服务架构，参与过日活百万级电商平台的架构设计与开发。具备良好的系统设计能力和问题解决能力。',2,3,'2026-07-01 16:04:27','2026-07-01 16:10:34',0),(13,24,'李娜_前端开发工程师.pdf',1,'3年前端开发经验，精通React和Vue技术栈，有大型B端管理系统和C端H5页面开发经验。注重代码质量和用户体验，善于性能优化。',2,1,'2026-07-01 18:25:41','2026-07-01 18:26:06',0),(14,25,'王强_数据分析师.pdf',1,'5年数据分析经验，精通SQL和Python数据分析，擅长用户行为分析、业务指标体系建设。主导过千万级用户平台的增长分析项目，通过数据驱动业务增长30%。',2,2,'2026-07-01 18:26:32','2026-07-01 18:30:45',0),(15,26,'赵敏-ui设计师.pdf',1,'4年UI/UX设计经验，擅长B端后台和C端移动应用设计。熟练使用Figma、Sketch等设计工具，具备完整的设计规范搭建经验。注重用户体验，善于通过用户研究驱动设计决策。',2,1,'2026-07-01 18:27:21','2026-07-01 18:27:39',0),(18,30,'陈磊-产品经理简历',1,'6年互联网产品经验，主导过从0到1的SaaS与企业协作类产品设计，累计服务企业客户500+。擅长B端产品规划、需求分析、数据驱动迭代与跨部门项目推进，持有PMP与NPDP认证。求职方向：高级产品经理 / 产品负责人，期望 base 广州或深圳。',2,1,'2026-07-01 18:40:29','2026-07-01 18:42:04',0);
/*!40000 ALTER TABLE `resume` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resume_attachment`
--

DROP TABLE IF EXISTS `resume_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_attachment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `resume_id` bigint unsigned NOT NULL COMMENT '??ID',
  `file_name` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '?????',
  `file_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '??????',
  `bucket_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MinIO??',
  `object_key` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MinIO???',
  `file_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '??URL',
  `upload_status` tinyint NOT NULL DEFAULT '1' COMMENT '1-?? 2-??',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_attach_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resume_attachment`
--

LOCK TABLES `resume_attachment` WRITE;
/*!40000 ALTER TABLE `resume_attachment` DISABLE KEYS */;
INSERT INTO `resume_attachment` VALUES (3,8,'刘洋-全栈开发工程师.pdf','pdf',94387,'talent-resumes','20260701/c19/093ad712-887a-40d8-99b6-51b0a120696e-刘洋-全栈开发工程师.pdf','http://127.0.0.1:9000/talent-resumes/20260701/c19/093ad712-887a-40d8-99b6-51b0a120696e-刘洋-全栈开发工程师.pdf',1,'2026-07-01 15:51:19','2026-07-01 15:51:19',0),(4,9,'吴芳-算法工程师.pdf','pdf',101280,'talent-resumes','20260701/c20/1f062b14-f5f8-4d71-86b9-2d96019c9e48-吴芳-算法工程师.pdf','http://127.0.0.1:9000/talent-resumes/20260701/c20/1f062b14-f5f8-4d71-86b9-2d96019c9e48-吴芳-算法工程师.pdf',1,'2026-07-01 15:51:59','2026-07-01 15:51:59',0),(5,10,'周杰-DevOps运维工程师.pdf','pdf',100956,'talent-resumes','20260701/c21/b36cff2c-8dcb-44c0-b321-ff4f37131e26-周杰-DevOps运维工程师.pdf','http://127.0.0.1:9000/talent-resumes/20260701/c21/b36cff2c-8dcb-44c0-b321-ff4f37131e26-周杰-DevOps运维工程师.pdf',1,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(6,11,'孙婷-软件测试工程师.pdf','pdf',98342,'talent-resumes','20260701/c22/62b44ae0-4708-4cb0-9d71-fda17feeb398-孙婷-软件测试工程师.pdf','http://127.0.0.1:9000/talent-resumes/20260701/c22/62b44ae0-4708-4cb0-9d71-fda17feeb398-孙婷-软件测试工程师.pdf',1,'2026-07-01 15:53:20','2026-07-01 15:53:20',0),(7,12,'张伟_Java后端开发工程师.pdf','pdf',122491,'talent-resumes','20260701/c23/f60f6f1d-7e9f-4c82-9a0c-6a7a19361da1-张伟_Java后端开发工程师.pdf','http://127.0.0.1:9000/talent-resumes/20260701/c23/f60f6f1d-7e9f-4c82-9a0c-6a7a19361da1-张伟_Java后端开发工程师.pdf',1,'2026-07-01 16:04:27','2026-07-01 16:04:27',0),(8,13,'李娜_前端开发工程师.pdf','pdf',98926,'talent-resumes','20260701/c24/96ebbd73-d663-4c76-a33c-13eb14a7d8e9-李娜_前端开发工程师.pdf','http://127.0.0.1:9000/talent-resumes/20260701/c24/96ebbd73-d663-4c76-a33c-13eb14a7d8e9-李娜_前端开发工程师.pdf',1,'2026-07-01 18:25:41','2026-07-01 18:25:41',0),(9,14,'王强_数据分析师.pdf','pdf',102631,'talent-resumes','20260701/c25/e04b6a31-4fbb-4abb-95a3-d274a6b0a3cf-王强_数据分析师.pdf','http://127.0.0.1:9000/talent-resumes/20260701/c25/e04b6a31-4fbb-4abb-95a3-d274a6b0a3cf-王强_数据分析师.pdf',1,'2026-07-01 18:26:33','2026-07-01 18:26:33',0),(10,15,'赵敏-ui设计师.pdf','pdf',108900,'talent-resumes','20260701/c26/e5740ae1-276c-4cb5-8aa0-5e6125fca122-赵敏-ui设计师.pdf','http://127.0.0.1:9000/talent-resumes/20260701/c26/e5740ae1-276c-4cb5-8aa0-5e6125fca122-赵敏-ui设计师.pdf',1,'2026-07-01 18:27:21','2026-07-01 18:27:21',0);
/*!40000 ALTER TABLE `resume_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resume_certificate`
--

DROP TABLE IF EXISTS `resume_certificate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_certificate` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resume_id` bigint unsigned NOT NULL COMMENT '简历ID',
  `cert_type` tinyint NOT NULL DEFAULT '1' COMMENT '1-证书 2-荣誉 3-职称',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '名称',
  `issuer` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '颁发/发证单位',
  `issue_date` date DEFAULT NULL COMMENT '获得时间',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '补充说明',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_certificate_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=69 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='证书与荣誉表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resume_certificate`
--

LOCK TABLES `resume_certificate` WRITE;
/*!40000 ALTER TABLE `resume_certificate` DISABLE KEYS */;
INSERT INTO `resume_certificate` VALUES (36,8,1,'AWS Certified Developer',NULL,NULL,NULL,0,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(37,8,1,'软考中级-软件设计师',NULL,NULL,NULL,1,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(38,8,1,'AWS Certified Developer',NULL,NULL,NULL,0,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(39,8,1,'软考中级-软件设计师',NULL,NULL,NULL,1,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(40,9,1,'深度学习专项课程（吴恩达）',NULL,NULL,NULL,0,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(41,9,1,'Kaggle竞赛银牌',NULL,NULL,NULL,1,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(42,9,1,'深度学习专项课程（吴恩达）',NULL,NULL,NULL,0,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(43,9,1,'Kaggle竞赛银牌',NULL,NULL,NULL,1,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(44,10,1,'CKA（Kubernetes管理员认证）',NULL,NULL,NULL,0,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(45,10,1,'AWS Solutions Architect',NULL,NULL,NULL,1,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(46,10,1,'CKA（Kubernetes管理员认证）',NULL,NULL,NULL,0,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(47,10,1,'AWS Solutions Architect',NULL,NULL,NULL,1,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(48,11,1,'ISTQB高级测试工程师',NULL,NULL,NULL,0,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(49,11,1,'软考高级-系统分析师',NULL,NULL,NULL,1,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(50,11,1,'ISTQB高级测试工程师',NULL,NULL,NULL,0,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(51,11,1,'软考高级-系统分析师',NULL,NULL,NULL,1,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(52,12,1,'Oracle Java SE 8 认证',NULL,NULL,NULL,0,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(53,12,1,'阿里云ACP认证',NULL,NULL,NULL,1,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(54,12,1,'Oracle Java SE 8 认证',NULL,NULL,NULL,0,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(55,12,1,'阿里云ACP认证',NULL,NULL,NULL,1,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(56,13,1,'软考中级-软件设计师',NULL,NULL,NULL,0,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(57,13,1,'软考中级-软件设计师',NULL,NULL,NULL,0,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(58,14,1,'CDA数据分析师认证',NULL,NULL,NULL,0,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(59,14,1,'Tableau Desktop Specialist',NULL,NULL,NULL,1,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(60,14,1,'CDA数据分析师认证',NULL,NULL,NULL,0,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(61,14,1,'Tableau Desktop Specialist',NULL,NULL,NULL,1,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(62,15,1,'Adobe认证设计师',NULL,NULL,NULL,0,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(63,16,1,'OCP（Oracle认证专家）',NULL,NULL,NULL,0,'2026-07-01 18:28:43','2026-07-01 18:29:25',1),(64,16,1,'MySQL DBA认证',NULL,NULL,NULL,1,'2026-07-01 18:28:43','2026-07-01 18:29:25',1),(65,16,1,'TiDB认证专家',NULL,NULL,NULL,2,'2026-07-01 18:28:43','2026-07-01 18:29:25',1),(66,16,1,'OCP（Oracle认证专家）',NULL,NULL,NULL,0,'2026-07-01 18:29:25','2026-07-01 18:29:25',0),(67,16,1,'MySQL DBA认证',NULL,NULL,NULL,1,'2026-07-01 18:29:25','2026-07-01 18:29:25',0),(68,16,1,'TiDB认证专家',NULL,NULL,NULL,2,'2026-07-01 18:29:25','2026-07-01 18:29:25',0);
/*!40000 ALTER TABLE `resume_certificate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resume_education`
--

DROP TABLE IF EXISTS `resume_education`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_education` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `resume_id` bigint unsigned NOT NULL COMMENT '??ID',
  `school_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `major` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '??',
  `degree` tinyint DEFAULT NULL COMMENT '??',
  `start_date` date DEFAULT NULL COMMENT '????',
  `end_date` date DEFAULT NULL COMMENT '????',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '????',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '??',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_edu_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resume_education`
--

LOCK TABLES `resume_education` WRITE;
/*!40000 ALTER TABLE `resume_education` DISABLE KEYS */;
INSERT INTO `resume_education` VALUES (5,8,'南京大学','软件工程',2,'2016-09-01','2020-06-01',NULL,0,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(6,8,'南京大学','软件工程',2,'2016-09-01','2020-06-01',NULL,0,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(7,9,'浙江大学','计算机科学与技术（AI方向）',3,'2019-09-01','2021-06-01',NULL,0,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(8,9,'武汉大学','计算机科学与技术',2,'2015-09-01','2019-06-01',NULL,1,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(9,9,'浙江大学','计算机科学与技术（AI方向）',3,'2019-09-01','2021-06-01',NULL,0,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(10,9,'武汉大学','计算机科学与技术',2,'2015-09-01','2019-06-01',NULL,1,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(11,10,'北京邮电大学','网络工程',2,'2012-09-01','2016-06-01',NULL,0,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(12,10,'北京邮电大学','网络工程',2,'2012-09-01','2016-06-01',NULL,0,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(13,11,'电子科技大学','软件工程',2,'2015-09-01','2019-06-01',NULL,0,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(14,11,'电子科技大学','软件工程',2,'2015-09-01','2019-06-01',NULL,0,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(15,12,'华中科技大学','软件工程',2,'2016-09-01','2020-06-01',NULL,0,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(16,12,'华中科技大学','软件工程',2,'2016-09-01','2020-06-01',NULL,0,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(17,13,'深圳大学','计算机科学与技术',2,'2017-09-01','2021-06-01',NULL,0,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(18,13,'深圳大学','计算机科学与技术',2,'2017-09-01','2021-06-01',NULL,0,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(19,14,'复旦大学','统计学',3,'2017-09-01','2019-06-01',NULL,0,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(20,14,'华东师范大学','数学与应用数学',2,'2013-09-01','2017-06-01',NULL,1,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(21,14,'复旦大学','统计学',3,'2017-09-01','2019-06-01',NULL,0,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(22,14,'华东师范大学','数学与应用数学',2,'2013-09-01','2017-06-01',NULL,1,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(23,15,'中国美术学院','视觉传达设计',2,'2016-09-01','2020-06-01',NULL,0,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(28,18,'中山大学','工商管理（MBA）',3,'2017-09-01','2019-06-30','研究方向：产品战略与组织管理；参与校企联合创新课题，完成 SaaS 商业模式案例分析',0,'2026-07-01 18:40:29','2026-07-01 18:40:29',0),(29,18,'华南理工大学','信息管理与信息系统',2,'2013-09-01','2017-06-30','校级优秀毕业生；主修信息系统分析与设计、数据库、项目管理',1,'2026-07-01 18:40:29','2026-07-01 18:40:29',0);
/*!40000 ALTER TABLE `resume_education` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resume_project`
--

DROP TABLE IF EXISTS `resume_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_project` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resume_id` bigint unsigned NOT NULL COMMENT '简历ID',
  `project_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目名称',
  `role` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '担任角色',
  `tech_stack` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '技术栈/工具',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '项目描述',
  `link_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '链接/GitHub',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  CONSTRAINT `fk_project_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目经历表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resume_project`
--

LOCK TABLES `resume_project` WRITE;
/*!40000 ALTER TABLE `resume_project` DISABLE KEYS */;
INSERT INTO `resume_project` VALUES (36,8,'在线协作白板',NULL,NULL,NULL,NULL,'基于WebSocket + Canvas实现多人实时协作，支持画笔、文本、图片等工具，月活用户2000+。',NULL,0,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(37,8,'微服务管理平台',NULL,NULL,NULL,NULL,'使用React + Go开发，集成服务注册、配置管理、链路追踪等功能，已开源在GitHub获得500+ Star。',NULL,1,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(38,8,'在线协作白板',NULL,NULL,NULL,NULL,'基于WebSocket + Canvas实现多人实时协作，支持画笔、文本、图片等工具，月活用户2000+。',NULL,0,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(39,8,'微服务管理平台',NULL,NULL,NULL,NULL,'使用React + Go开发，集成服务注册、配置管理、链路追踪等功能，已开源在GitHub获得500+ Star。',NULL,1,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(40,9,'多目标推荐排序模型',NULL,NULL,NULL,NULL,'设计基于MMOE的多目标模型，同时优化点击率、完播率和互动率，线上效果显著。',NULL,0,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(41,9,'文本分类系统',NULL,NULL,NULL,NULL,'基于BERT微调实现新闻文本多标签分类，在公开数据集上达到SOTA效果，已发表论文。',NULL,1,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(42,9,'多目标推荐排序模型',NULL,NULL,NULL,NULL,'设计基于MMOE的多目标模型，同时优化点击率、完播率和互动率，线上效果显著。',NULL,0,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(43,9,'文本分类系统',NULL,NULL,NULL,NULL,'基于BERT微调实现新闻文本多标签分类，在公开数据集上达到SOTA效果，已发表论文。',NULL,1,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(44,10,'混合云管理平台',NULL,NULL,NULL,NULL,'基于Terraform实现多云资源统一编排，支持阿里云、AWS等，管理资源实例2000+。',NULL,0,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(45,10,'全链路监控体系',NULL,NULL,NULL,NULL,'基于Prometheus + Grafana + ELK搭建，覆盖基础设施、应用、业务三层监控，告警准确率95%。',NULL,1,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(46,10,'混合云管理平台',NULL,NULL,NULL,NULL,'基于Terraform实现多云资源统一编排，支持阿里云、AWS等，管理资源实例2000+。',NULL,0,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(47,10,'全链路监控体系',NULL,NULL,NULL,NULL,'基于Prometheus + Grafana + ELK搭建，覆盖基础设施、应用、业务三层监控，告警准确率95%。',NULL,1,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(48,11,'自动化测试平台',NULL,NULL,NULL,NULL,'基于Jenkins + Docker搭建持续测试平台，支持定时执行、结果通知、报告生成，覆盖5个产品线。',NULL,0,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(49,11,'性能测试体系',NULL,NULL,NULL,NULL,'建立性能测试基线和监控指标，实现性能回归自动化，提前发现性能劣化。',NULL,1,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(50,11,'自动化测试平台',NULL,NULL,NULL,NULL,'基于Jenkins + Docker搭建持续测试平台，支持定时执行、结果通知、报告生成，覆盖5个产品线。',NULL,0,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(51,11,'性能测试体系',NULL,NULL,NULL,NULL,'建立性能测试基线和监控指标，实现性能回归自动化，提前发现性能劣化。',NULL,1,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(52,12,'电商订单中台',NULL,NULL,NULL,NULL,'主导微服务拆分，采用DDD领域驱动设计，实现订单创建、支付回调、退款等核心流程，日均处理订单量50万+。',NULL,0,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(53,12,'统一配置中心',NULL,NULL,NULL,NULL,'基于Spring Cloud Config + Bus搭建配置中心，实现配置动态刷新，减少因配置变更导致的发布次数60%。',NULL,1,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(54,12,'电商订单中台',NULL,NULL,NULL,NULL,'主导微服务拆分，采用DDD领域驱动设计，实现订单创建、支付回调、退款等核心流程，日均处理订单量50万+。',NULL,0,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(55,12,'统一配置中心',NULL,NULL,NULL,NULL,'基于Spring Cloud Config + Bus搭建配置中心，实现配置动态刷新，减少因配置变更导致的发布次数60%。',NULL,1,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(56,13,'企业级组件库',NULL,NULL,NULL,NULL,'独立开发20+业务组件，支持主题定制和国际化，NPM周下载量500+。',NULL,0,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(57,13,'低代码平台',NULL,NULL,NULL,NULL,'参与可视化搭建引擎开发，实现拖拽生成页面，提升运营页面开发效率3倍。',NULL,1,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(58,13,'企业级组件库',NULL,NULL,NULL,NULL,'独立开发20+业务组件，支持主题定制和国际化，NPM周下载量500+。',NULL,0,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(59,13,'低代码平台',NULL,NULL,NULL,NULL,'参与可视化搭建引擎开发，实现拖拽生成页面，提升运营页面开发效率3倍。',NULL,1,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(60,14,'用户画像系统',NULL,NULL,NULL,NULL,'基于Hive + Spark构建用户标签体系，涵盖人口属性、行为偏好等200+标签，支撑精准营销。',NULL,0,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(61,14,'自动化报表平台',NULL,NULL,NULL,NULL,'使用Python + Airflow搭建自动化数据管道，将人工报表时间从2天缩短至2小时。',NULL,1,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(62,14,'用户画像系统',NULL,NULL,NULL,NULL,'基于Hive + Spark构建用户标签体系，涵盖人口属性、行为偏好等200+标签，支撑精准营销。',NULL,0,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(63,14,'自动化报表平台',NULL,NULL,NULL,NULL,'使用Python + Airflow搭建自动化数据管道，将人工报表时间从2天缩短至2小时。',NULL,1,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(64,15,'Ant Design Pro 设计规范',NULL,NULL,NULL,NULL,'参与企业级中后台设计规范的制定，覆盖表单、表格、图表等50+组件的交互规范。',NULL,0,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(65,15,'音乐社交App概念设计',NULL,NULL,NULL,NULL,'独立完成从用户调研到高保真原型的全流程设计，获得站酷首页推荐。',NULL,1,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(66,16,'数据库自动化运维平台',NULL,NULL,NULL,NULL,'基于Python + Django开发，集成SQL审核、自动工单、监控告警等功能，DBA日常工单处理效率提升60%。',NULL,0,'2026-07-01 18:28:43','2026-07-01 18:29:25',1),(67,16,'数据迁移工具',NULL,NULL,NULL,NULL,'开发异构数据库迁移工具，支持Oracle到MySQL、MySQL到TiDB的数据同步，数据一致性校验通过率100%。',NULL,1,'2026-07-01 18:28:43','2026-07-01 18:29:25',1),(68,16,'数据库自动化运维平台',NULL,NULL,NULL,NULL,'基于Python + Django开发，集成SQL审核、自动工单、监控告警等功能，DBA日常工单处理效率提升60%。',NULL,0,'2026-07-01 18:29:25','2026-07-01 18:29:25',0),(69,16,'数据迁移工具',NULL,NULL,NULL,NULL,'开发异构数据库迁移工具，支持Oracle到MySQL、MySQL到TiDB的数据同步，数据一致性校验通过率100%。',NULL,1,'2026-07-01 18:29:25','2026-07-01 18:29:25',0);
/*!40000 ALTER TABLE `resume_project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resume_skill`
--

DROP TABLE IF EXISTS `resume_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_skill` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `resume_id` bigint unsigned NOT NULL COMMENT '??ID',
  `skill_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `proficiency_level` tinyint DEFAULT NULL COMMENT '???0-100',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '??',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_skill_name` (`skill_name`),
  CONSTRAINT `fk_skill_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=243 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='???';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resume_skill`
--

LOCK TABLES `resume_skill` WRITE;
/*!40000 ALTER TABLE `resume_skill` DISABLE KEYS */;
INSERT INTO `resume_skill` VALUES (59,8,'React',NULL,0,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(60,8,'Node.js',NULL,1,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(61,8,'TypeScript',NULL,2,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(62,8,'Python',NULL,3,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(63,8,'PostgreSQL',NULL,4,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(64,8,'MongoDB',NULL,5,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(65,8,'GraphQL',NULL,6,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(66,8,'AWS',NULL,7,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(67,8,'Docker',NULL,8,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(68,8,'Redis',NULL,9,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(69,8,'React',NULL,0,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(70,8,'Node.js',NULL,1,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(71,8,'TypeScript',NULL,2,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(72,8,'Python',NULL,3,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(73,8,'PostgreSQL',NULL,4,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(74,8,'MongoDB',NULL,5,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(75,8,'GraphQL',NULL,6,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(76,8,'AWS',NULL,7,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(77,8,'Docker',NULL,8,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(78,8,'Redis',NULL,9,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(79,9,'Python',NULL,0,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(80,9,'PyTorch',NULL,1,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(81,9,'TensorFlow',NULL,2,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(82,9,'Spark ML',NULL,3,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(83,9,'推荐系统',NULL,4,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(84,9,'NLP',NULL,5,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(85,9,'深度学习',NULL,6,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(86,9,'特征工程',NULL,7,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(87,9,'AB实验',NULL,8,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(88,9,'SQL',NULL,9,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(89,9,'Python',NULL,0,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(90,9,'PyTorch',NULL,1,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(91,9,'TensorFlow',NULL,2,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(92,9,'Spark ML',NULL,3,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(93,9,'推荐系统',NULL,4,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(94,9,'NLP',NULL,5,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(95,9,'深度学习',NULL,6,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(96,9,'特征工程',NULL,7,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(97,9,'AB实验',NULL,8,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(98,9,'SQL',NULL,9,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(99,10,'Kubernetes',NULL,0,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(100,10,'Docker',NULL,1,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(101,10,'Jenkins',NULL,2,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(102,10,'Terraform',NULL,3,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(103,10,'Ansible',NULL,4,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(104,10,'Prometheus',NULL,5,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(105,10,'Grafana',NULL,6,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(106,10,'ELK',NULL,7,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(107,10,'Nginx',NULL,8,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(108,10,'Shell',NULL,9,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(109,10,'Kubernetes',NULL,0,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(110,10,'Docker',NULL,1,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(111,10,'Jenkins',NULL,2,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(112,10,'Terraform',NULL,3,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(113,10,'Ansible',NULL,4,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(114,10,'Prometheus',NULL,5,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(115,10,'Grafana',NULL,6,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(116,10,'ELK',NULL,7,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(117,10,'Nginx',NULL,8,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(118,10,'Shell',NULL,9,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(119,11,'Python',NULL,0,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(120,11,'Selenium',NULL,1,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(121,11,'JMeter',NULL,2,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(122,11,'Postman',NULL,3,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(123,11,'Jenkins',NULL,4,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(124,11,'Pytest',NULL,5,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(125,11,'Allure',NULL,6,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(126,11,'SQL',NULL,7,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(127,11,'Linux',NULL,8,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(128,11,'Docker',NULL,9,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(129,11,'Python',NULL,0,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(130,11,'Selenium',NULL,1,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(131,11,'JMeter',NULL,2,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(132,11,'Postman',NULL,3,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(133,11,'Jenkins',NULL,4,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(134,11,'Pytest',NULL,5,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(135,11,'Allure',NULL,6,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(136,11,'SQL',NULL,7,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(137,11,'Linux',NULL,8,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(138,11,'Docker',NULL,9,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(139,12,'Java',NULL,0,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(140,12,'Spring Boot',NULL,1,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(141,12,'Spring Cloud',NULL,2,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(142,12,'MySQL',NULL,3,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(143,12,'Redis',NULL,4,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(144,12,'RabbitMQ',NULL,5,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(145,12,'Docker',NULL,6,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(146,12,'K8s',NULL,7,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(147,12,'MyBatis',NULL,8,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(148,12,'Linux',NULL,9,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(149,12,'Java',NULL,0,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(150,12,'Spring Boot',NULL,1,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(151,12,'Spring Cloud',NULL,2,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(152,12,'MySQL',NULL,3,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(153,12,'Redis',NULL,4,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(154,12,'RabbitMQ',NULL,5,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(155,12,'Docker',NULL,6,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(156,12,'K8s',NULL,7,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(157,12,'MyBatis',NULL,8,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(158,12,'Linux',NULL,9,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(159,13,'React',NULL,0,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(160,13,'Vue.js',NULL,1,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(161,13,'TypeScript',NULL,2,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(162,13,'Webpack',NULL,3,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(163,13,'Vite',NULL,4,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(164,13,'Node.js',NULL,5,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(165,13,'CSS3',NULL,6,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(166,13,'Ant Design',NULL,7,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(167,13,'Element UI',NULL,8,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(168,13,'Git',NULL,9,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(169,13,'React',NULL,0,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(170,13,'Vue.js',NULL,1,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(171,13,'TypeScript',NULL,2,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(172,13,'Webpack',NULL,3,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(173,13,'Vite',NULL,4,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(174,13,'Node.js',NULL,5,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(175,13,'CSS3',NULL,6,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(176,13,'Ant Design',NULL,7,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(177,13,'Element UI',NULL,8,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(178,13,'Git',NULL,9,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(179,14,'Python',NULL,0,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(180,14,'SQL',NULL,1,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(181,14,'Tableau',NULL,2,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(182,14,'Power BI',NULL,3,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(183,14,'Excel',NULL,4,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(184,14,'Spark',NULL,5,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(185,14,'Hive',NULL,6,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(186,14,'AB测试',NULL,7,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(187,14,'统计学',NULL,8,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(188,14,'机器学习',NULL,9,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(189,14,'Python',NULL,0,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(190,14,'SQL',NULL,1,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(191,14,'Tableau',NULL,2,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(192,14,'Power BI',NULL,3,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(193,14,'Excel',NULL,4,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(194,14,'Spark',NULL,5,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(195,14,'Hive',NULL,6,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(196,14,'AB测试',NULL,7,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(197,14,'统计学',NULL,8,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(198,14,'机器学习',NULL,9,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(199,15,'Figma',NULL,0,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(200,15,'Sketch',NULL,1,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(201,15,'Adobe XD',NULL,2,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(202,15,'Photoshop',NULL,3,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(203,15,'Illustrator',NULL,4,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(204,15,'Axure',NULL,5,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(205,15,'Principle',NULL,6,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(206,15,'用户研究',NULL,7,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(207,15,'设计系统',NULL,8,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(208,15,'交互设计',NULL,9,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(236,18,'Axure RP / 墨刀',90,0,'2026-07-01 18:40:29','2026-07-01 18:40:29',0),(237,18,'PRD撰写 / 竞品分析',92,1,'2026-07-01 18:40:29','2026-07-01 18:40:29',0),(238,18,'JIRA / 敏捷开发',88,2,'2026-07-01 18:40:29','2026-07-01 18:40:29',0),(239,18,'SQL / 数据分析',85,3,'2026-07-01 18:40:29','2026-07-01 18:40:29',0),(240,18,'用户调研 / 用户增长',86,4,'2026-07-01 18:40:29','2026-07-01 18:40:29',0),(241,18,'项目管理',90,5,'2026-07-01 18:40:29','2026-07-01 18:40:29',0),(242,18,'Figma',78,6,'2026-07-01 18:40:29','2026-07-01 18:40:29',0);
/*!40000 ALTER TABLE `resume_skill` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resume_work_experience`
--

DROP TABLE IF EXISTS `resume_work_experience`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_work_experience` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '??',
  `resume_id` bigint unsigned NOT NULL COMMENT '??ID',
  `company_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `job_title` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??',
  `experience_type` tinyint NOT NULL DEFAULT '1' COMMENT '1-?? 2-?? 3-??',
  `start_date` date DEFAULT NULL COMMENT '????',
  `end_date` date DEFAULT NULL COMMENT '????',
  `job_description` text COLLATE utf8mb4_unicode_ci COMMENT '????',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '??',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '????',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_company_name` (`company_name`),
  CONSTRAINT `fk_work_resume` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resume_work_experience`
--

LOCK TABLES `resume_work_experience` WRITE;
/*!40000 ALTER TABLE `resume_work_experience` DISABLE KEYS */;
INSERT INTO `resume_work_experience` VALUES (8,8,'小米','全栈开发工程师',1,NULL,NULL,'负责小米IoT平台管理后台的全栈开发，前端使用React + Ant Design，后端使用Node.js + PostgreSQL；开发设备管理、OTA升级等核心功能。',0,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(9,8,'苏宁易购','全栈开发工程师',1,NULL,NULL,'参与电商后台管理系统开发，使用Vue.js + Python FastAPI技术栈；独立负责商品管理和库存管理模块，支撑日均百万级商品数据。',1,'2026-07-01 15:51:49','2026-07-01 15:51:51',1),(10,8,'小米','全栈开发工程师',1,'2022-09-01',NULL,'负责小米IoT平台管理后台的全栈开发，前端使用React + Ant Design，后端使用Node.js + PostgreSQL；开发设备管理、OTA升级等核心功能。',0,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(11,8,'苏宁易购','全栈开发工程师',1,'2020-07-01','2022-08-01','参与电商后台管理系统开发，使用Vue.js + Python FastAPI技术栈；独立负责商品管理和库存管理模块，支撑日均百万级商品数据。',1,'2026-07-01 15:51:51','2026-07-01 15:51:51',0),(12,9,'快手','算法工程师',1,'2023-04-01',NULL,'负责快手短视频推荐算法优化，设计多目标排序模型，用户观看时长提升15%；引入多模态特征，冷启动场景下新内容曝光率提升30%。',0,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(13,9,'网易云音乐','算法工程师',1,'2021-07-01','2023-03-01','参与音乐推荐系统开发，基于协同过滤+内容特征构建召回和排序模型；通过特征交叉和模型融合，推荐点击率提升12%。',1,'2026-07-01 15:52:31','2026-07-01 15:52:35',1),(14,9,'快手','算法工程师',1,NULL,NULL,'负责快手短视频推荐算法优化，设计多目标排序模型，用户观看时长提升15%；引入多模态特征，冷启动场景下新内容曝光率提升30%。',0,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(15,9,'网易云音乐','算法工程师',1,NULL,NULL,'参与音乐推荐系统开发，基于协同过滤+内容特征构建召回和排序模型；通过特征交叉和模型融合，推荐点击率提升12%。',1,'2026-07-01 15:52:35','2026-07-01 15:52:35',0),(16,10,'京东','DevOps架构师',1,NULL,NULL,'负责京东云原生基础设施平台建设，管理500+节点K8s集群；设计GitOps发布流程，实现多环境一键部署和回滚，发布频率从周级提升至日级。',0,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(17,10,'百度','运维工程师',1,NULL,NULL,'负责百度搜索广告系统的运维保障，保障系统99.99%可用性；开发自动化运维工具，将日常运维操作效率提升70%。',1,'2026-07-01 15:53:03','2026-07-01 15:53:13',1),(18,10,'京东','DevOps架构师',1,NULL,NULL,'负责京东云原生基础设施平台建设，管理500+节点K8s集群；设计GitOps发布流程，实现多环境一键部署和回滚，发布频率从周级提升至日级。',0,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(19,10,'百度','运维工程师',1,NULL,NULL,'负责百度搜索广告系统的运维保障，保障系统99.99%可用性；开发自动化运维工具，将日常运维操作效率提升70%。',1,'2026-07-01 15:53:13','2026-07-01 15:53:13',0),(20,11,'华为','高级测试工程师',1,NULL,NULL,'负责华为云某PaaS平台的测试工作，搭建自动化测试框架，用例自动化覆盖率达75%；编写性能测试脚本，定位并推动解决内存泄漏问题，系统稳定性从99.5%提升至99.95%。',0,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(21,11,'中兴通讯','测试工程师',1,NULL,NULL,'参与5G核心网网元测试，设计测试用例2000+；使用Robot Framework搭建自动化回归测试，每次版本回归时间从3天缩短至4小时。',1,'2026-07-01 15:53:52','2026-07-01 15:53:55',1),(22,11,'华为','高级测试工程师',1,'2022-05-01',NULL,'负责华为云某PaaS平台的测试工作，搭建自动化测试框架，用例自动化覆盖率达75%；编写性能测试脚本，定位并推动解决 内存泄漏问题，系统稳定性从99.5%提升至99.95%。',0,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(23,11,'中兴通讯','测试工程师',1,'2019-08-01','2022-04-01','参与5G核心网网元测试，设计测试用例2000+；使用Robot Framework搭建自动化回归测试，每次版本回归时间从3天缩短至4小时。',1,'2026-07-01 15:53:55','2026-07-01 15:53:55',0),(24,12,'字节跳动','高级Java开发工程师',1,NULL,NULL,'负责电商中台订单系统的微服务化改造，将单体应用拆分为10+微服务，系统QPS从2000提升至8000；主导设计分布式事务方案，保证订单数据最终一致性；优化数据库慢查询，核心接口响应时间从500ms降至80ms。',0,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(25,12,'美团','Java开发工程师',1,NULL,NULL,'参与商家平台后端开发，负责商家入驻、资质审核模块的设计与实现；使用Redis实现分布式锁解决并发问题；编写单元测试，代码覆盖率达到85%以上。',1,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(26,12,'软通动力','初级Java开发工程师',1,NULL,NULL,'参与某银行信贷管理系统的开发，负责报表模块的后端接口开发；使用Spring Boot + MyBatis技术栈完成功能迭代。',2,'2026-07-01 16:04:55','2026-07-01 16:05:13',1),(27,12,'字节跳动','高级Java开发工程师',1,NULL,NULL,'负责电商中台订单系统的微服务化改造，将单体应用拆分为10+微服务，系统QPS从2000提升至8000；主导设计分布式事务方案，保证订单数据最终一致性；优化数据库慢查询，核心接口响应时间从500ms降至80ms。',0,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(28,12,'美团','Java开发工程师',1,NULL,NULL,'参与商家平台后端开发，负责商家入驻、资质审核模块的设计与实现；使用Redis实现分布式锁解决并发问题；编写单元测试，代码覆盖率达到85%以上。',1,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(29,12,'软通动力','初级Java开发工程师',1,NULL,NULL,'参与某银行信贷管理系统的开发，负责报表模块的后端接口开发；使用Spring Boot + MyBatis技术栈完成功能迭代。',2,'2026-07-01 16:05:13','2026-07-01 16:05:13',0),(30,13,'腾讯','前端开发工程师',1,NULL,NULL,'负责腾讯云控制台前端开发，使用React + TypeScript重构旧版页面，首屏加载时间减少40%；封装通用组件库，被团队10+项目复用。',0,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(31,13,'金蝶软件','前端开发工程师',1,NULL,NULL,'参与ERP系统前端开发，基于Vue3 + Element Plus实现采购、库存管理等核心模块；使用ECharts实现数据可视化大屏，支撑管理层决策。',1,'2026-07-01 18:26:05','2026-07-01 18:26:06',1),(32,13,'腾讯','前端开发工程师',1,NULL,NULL,'负责腾讯云控制台前端开发，使用React + TypeScript重构旧版页面，首屏加载时间减少40%；封装通用组件库，被团队10+项目复用。',0,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(33,13,'金蝶软件','前端开发工程师',1,NULL,NULL,'参与ERP系统前端开发，基于Vue3 + Element Plus实现采购、库存管理等核心模块；使用ECharts实现数据可视化大屏，支撑管理层决策。',1,'2026-07-01 18:26:06','2026-07-01 18:26:06',0),(34,14,'拼多多','高级数据分析师',1,NULL,NULL,'负责首页推荐流的数据分析，通过AB实验优化推荐策略，点击率提升18%；搭建用户分层模型，精细化运营后用户留存率提升12%。',0,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(35,14,'携程','数据分析师',1,NULL,NULL,'负责酒店业务线数据分析，建立核心指标体系，产出周报/月报驱动业务决策；通过漏斗分析定位转化瓶颈，优化后支付转化率提升8%。',1,'2026-07-01 18:27:00','2026-07-01 18:27:01',1),(36,14,'拼多多','高级数据分析师',1,NULL,NULL,'负责首页推荐流的数据分析，通过AB实验优化推荐策略，点击率提升18%；搭建用户分层模型，精细化运营后用户留存率提升12%。',0,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(37,14,'携程','数据分析师',1,NULL,NULL,'负责酒店业务线数据分析，建立核心指标体系，产出周报/月报驱动业务决策；通过漏斗分析定位转化瓶颈，优化后支付转化率提升8%。',1,'2026-07-01 18:27:01','2026-07-01 18:27:01',0),(38,15,'阿里巴巴','UI设计师',1,'2022-08-01',NULL,'负责淘宝商家后台改版设计，主导设计规范升级，组件复用率提升至80%；通过用户访谈和可用性测试，优化核心操作流程，操作效率提升25%。',0,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(39,15,'网易','UI设计师',1,'2020-07-01','2022-07-01','参与网易云音乐App界面设计，负责播放页和个人中心改版；输出交互原型和高保真设计稿，与开发紧密协作保证设计还原度95%以上。',1,'2026-07-01 18:27:39','2026-07-01 18:27:39',0),(47,18,'字节跳动','高级产品经理',1,'2023-01-01',NULL,'负责飞书企业管理后台产品规划，从0到1搭建组织架构、权限管理、审计日志等核心功能。\n- 协调研发、设计、测试团队，按季度交付产品迭代，保障大客户定制需求落地；\n- 主导「飞书管理后台」从需求调研到上线全流程，上线3个月内服务企业数突破1000家，客户满意度92%；\n- 建立需求优先级评审机制与埋点看板，支撑权限模块使用率提升约28%。',0,'2026-07-01 18:40:29','2026-07-01 18:40:29',0),(48,18,'金山办公','产品经理',1,'2019-07-01','2022-12-31','主导WPS云文档协作功能的产品设计，DAU从50万增长至200万。\n- 通过用户调研和数据分析优化协作体验，NPS从35提升至52；\n- 负责「WPS协作模块增长」项目，通过A/B实验验证产品假设，协作功能周活跃用户占比从15%提升至38%；\n- 输出PRD与交互规范，推动评论、@提醒、版本历史等能力上线。',1,'2026-07-01 18:40:29','2026-07-01 18:40:29',0),(49,18,'某互联网教育公司','产品实习生',1,'2018-07-01','2019-06-30','参与学习管理后台需求梳理与原型设计，协助完成3个版本迭代与用户访谈纪要整理。',2,'2026-07-01 18:40:29','2026-07-01 18:40:29',0);
/*!40000 ALTER TABLE `resume_work_experience` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'talent_resume_db'
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
