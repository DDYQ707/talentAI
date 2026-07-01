-- =============================================================================
-- talent_admin_db  结构 + 种子数据（全量快照）
-- 导出时间: 2026-07-01
-- 微服务: talent-admin  端口: 8088
-- 说明: 由 mysqldump 从本机 MySQL 3306 导出，含 CREATE DATABASE / 表结构 / 数据
-- 导入: mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 < docs/sql/talent_admin_db.sql
-- 注意: 请勿用 PowerShell Get-Content 管道导入（易中文乱码），请用上述重定向或 mysql source
-- =============================================================================
-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: talent_admin_db
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
-- Current Database: `talent_admin_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `talent_admin_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `talent_admin_db`;

--
-- Table structure for table `announcement`
--

DROP TABLE IF EXISTS `announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(128) NOT NULL COMMENT '标题',
  `content` text NOT NULL COMMENT '内容',
  `level` varchar(16) NOT NULL DEFAULT 'info' COMMENT '级别：info/warning/critical',
  `target` varchar(16) NOT NULL DEFAULT 'all' COMMENT '目标人群：candidate/hr/all',
  `broadcasted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已广播：0=否, 1=是',
  `broadcasted_at` datetime DEFAULT NULL COMMENT '广播时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统公告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement`
--

LOCK TABLES `announcement` WRITE;
/*!40000 ALTER TABLE `announcement` DISABLE KEYS */;
INSERT INTO `announcement` VALUES (1,'系统维护通知','平台将于本周日凌晨 02:00-04:00 进行系统升级维护，期间部分功能可能不可用，敬请谅解。','warning','all',1,'2026-06-25 16:32:30','2026-03-15 11:00:00'),(2,'简历投递功能优化','我们优化了简历投递流程，现在支持一键投递多个职位，欢迎候选人体验。','info','candidate',1,'2026-03-12 10:00:00','2026-03-11 16:40:00'),(3,'紧急：账号安全提醒','近期发现钓鱼网站冒充本平台，请 HR 用户务必通过官方域名登录，谨防信息泄露。','critical','hr',0,NULL,'2026-03-18 09:15:00');
/*!40000 ALTER TABLE `announcement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `banner`
--

DROP TABLE IF EXISTS `banner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banner` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(128) NOT NULL COMMENT '标题',
  `image_url` varchar(512) NOT NULL COMMENT '图片URL',
  `link_url` varchar(512) DEFAULT NULL COMMENT '跳转链接',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=下线, 1=上线',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序值，越小越靠前',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='轮播图';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `banner`
--

LOCK TABLES `banner` WRITE;
/*!40000 ALTER TABLE `banner` DISABLE KEYS */;
INSERT INTO `banner` VALUES (1,'春季招聘会火热进行中','https://picsum.photos/seed/banner1/1200/400','https://www.talent-ai.com/fair/spring','2026-03-01 00:00:00','2026-04-30 23:59:59',1,1,'2026-02-25 10:00:00'),(2,'AI 简历优化新功能上线','https://picsum.photos/seed/banner2/1200/400','https://www.talent-ai.com/feature/ai-resume','2026-03-10 00:00:00','2026-06-30 23:59:59',1,2,'2026-03-08 09:30:00'),(3,'企业入驻专享福利','https://picsum.photos/seed/banner3/1200/400','https://www.talent-ai.com/enterprise/welcome','2026-01-01 00:00:00','2026-12-31 23:59:59',0,3,'2025-12-28 14:20:00');
/*!40000 ALTER TABLE `banner` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `talent_dict_item`
--

DROP TABLE IF EXISTS `talent_dict_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `talent_dict_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type_id` bigint NOT NULL COMMENT '关联类型ID',
  `label` varchar(100) NOT NULL COMMENT '字典标签，如 互联网/IT',
  `value` varchar(100) NOT NULL COMMENT '字典键值，如 IT',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用, 0=禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type_id` (`dict_type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据字典键值表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `talent_dict_item`
--

LOCK TABLES `talent_dict_item` WRITE;
/*!40000 ALTER TABLE `talent_dict_item` DISABLE KEYS */;
INSERT INTO `talent_dict_item` VALUES (1,1,'互联网/IT','IT',1,1,'2026-06-25 07:30:39'),(2,1,'金融','FIN',2,1,'2026-06-25 07:30:39'),(3,1,'教育','EDU',3,1,'2026-06-25 15:37:09');
/*!40000 ALTER TABLE `talent_dict_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `talent_dict_type`
--

DROP TABLE IF EXISTS `talent_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `talent_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL COMMENT '字典编码，如 job_industry',
  `name` varchar(50) NOT NULL COMMENT '字典名称，如 行业领域',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用, 0=禁用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `talent_dict_type`
--

LOCK TABLES `talent_dict_type` WRITE;
/*!40000 ALTER TABLE `talent_dict_type` DISABLE KEYS */;
INSERT INTO `talent_dict_type` VALUES (1,'job_industry','行业领域',1,NULL,'2026-06-25 07:30:39'),(2,'edu_level','学历',1,NULL,'2026-06-25 15:37:32');
/*!40000 ALTER TABLE `talent_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `talent_enterprise_audit`
--

DROP TABLE IF EXISTS `talent_enterprise_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `talent_enterprise_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `company_name` varchar(100) NOT NULL COMMENT '企业名称',
  `credit_code` varchar(18) NOT NULL COMMENT '统一社会信用代码',
  `legal_person` varchar(20) DEFAULT NULL COMMENT '法定代表人',
  `registered_capital` varchar(50) DEFAULT NULL COMMENT '注册资本',
  `business_scope` text COMMENT '经营范围',
  `license_url` varchar(255) DEFAULT NULL COMMENT '营业执照图片URL',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=待审核, 1=已通过, 2=已驳回',
  `reject_reason` varchar(255) DEFAULT NULL COMMENT '驳回理由',
  `submitted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_credit_code` (`credit_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业资质审核表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `talent_enterprise_audit`
--

LOCK TABLES `talent_enterprise_audit` WRITE;
/*!40000 ALTER TABLE `talent_enterprise_audit` DISABLE KEYS */;
INSERT INTO `talent_enterprise_audit` VALUES (1,'阿里巴巴科技','91330100MA27XXXX01','张三',NULL,NULL,NULL,1,NULL,'2026-06-25 07:30:39','2026-06-25 15:31:53'),(2,'腾讯计算机','91440300MA5DXXXX02','李四',NULL,NULL,NULL,2,'营业执照模糊不清','2026-06-25 07:30:39','2026-06-25 15:32:46');
/*!40000 ALTER TABLE `talent_enterprise_audit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `talent_job_risk`
--

DROP TABLE IF EXISTS `talent_job_risk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `talent_job_risk` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_title` varchar(100) NOT NULL COMMENT '职位名称',
  `company_name` varchar(100) NOT NULL COMMENT '企业名称',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `job_post_id` bigint DEFAULT NULL COMMENT '?? talent-job ?? ID',
  `salary_min` int DEFAULT NULL,
  `salary_max` int DEFAULT NULL,
  `description` text COMMENT '职位描述',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=正常, 1=风险预警, 2=已下架',
  `risk_keywords` varchar(255) DEFAULT NULL COMMENT '触发的风险高危词',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `taken_down_at` datetime DEFAULT NULL COMMENT '下架时间',
  `taken_down_by` bigint DEFAULT NULL COMMENT '下架操作人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='职位风控日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `talent_job_risk`
--

LOCK TABLES `talent_job_risk` WRITE;
/*!40000 ALTER TABLE `talent_job_risk` DISABLE KEYS */;
INSERT INTO `talent_job_risk` VALUES (2,'高薪兼职日结','智聘科技',15,13,NULL,NULL,NULL,2,'薪资虚高，疑似诈骗','2026-06-25 07:30:39','2026-06-25 15:39:03',14);
/*!40000 ALTER TABLE `talent_job_risk` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'talent_admin_db'
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
