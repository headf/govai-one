-- MySQL dump 10.13  Distrib 8.4.9, for Win64 (x86_64)
--
-- Host: localhost    Database: govai_approval
-- ------------------------------------------------------
-- Server version	8.4.9

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
-- Table structure for table `ai_chat_record`
--

DROP TABLE IF EXISTS `ai_chat_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` bigint DEFAULT NULL COMMENT '审批实例ID',
  `template_id` bigint DEFAULT NULL COMMENT 'Prompt模板ID',
  `user_message` text COMMENT '用户消息',
  `ai_response` text COMMENT 'AI响应内容',
  `token_count` int DEFAULT '0' COMMENT 'Token消耗数',
  `response_time_ms` bigint DEFAULT '0' COMMENT '响应耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_record`
--

LOCK TABLES `ai_chat_record` WRITE;
/*!40000 ALTER TABLE `ai_chat_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_prompt_template`
--

DROP TABLE IF EXISTS `ai_prompt_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_prompt_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `scene_type` varchar(32) NOT NULL COMMENT '场景类型（opinion/summary/risk等）',
  `system_prompt` text NOT NULL COMMENT '系统提示词',
  `user_prompt_template` text COMMENT '用户提示词模板',
  `variables` json DEFAULT NULL COMMENT '变量定义（JSON）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_scene_type` (`scene_type`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI Prompt模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_prompt_template`
--

LOCK TABLES `ai_prompt_template` WRITE;
/*!40000 ALTER TABLE `ai_prompt_template` DISABLE KEYS */;
INSERT INTO `ai_prompt_template` VALUES (1,'审核意见生成模板','opinion','你是一个专业的政务服务审批助手。请根据审批事项的具体信息，生成一份专业、规范的审批意见。要求：1. 语言正式、客观；2. 明确给出审批建议（通过/退回）；3. 如有退回，需说明具体原因。','请为以下审批事项生成审批意见：\n事项类型：{matterType}\n事项名称：{matterName}\n申请人：{applicantName}\n申请内容摘要：{contentSummary}\n规则校验结果：{ruleCheckResult}','[\"matterType\", \"matterName\", \"applicantName\", \"contentSummary\", \"ruleCheckResult\"]',1,'2026-09-02 16:36:11'),(2,'政策依据摘要模板','summary','你是一个政务政策分析助手。请根据审批事项类型，检索并总结适用的政策法规依据。要求：1. 引用准确的政策法规名称和条款；2. 简明扼要地解释适用理由；3. 如有多个政策适用，按优先级排列。','请为以下审批事项查找适用的政策依据：\n事项类型：{matterType}\n事项名称：{matterName}\n申请内容：{content}\n关键关键词：{keywords}','[\"matterType\", \"matterName\", \"content\", \"keywords\"]',1,'2026-09-02 16:36:11'),(3,'风险提示生成模板','risk','你是一个政务服务风险预警助手。请根据审批事项的信息，分析潜在的风险点并生成风险提示。要求：1. 识别材料完整性、合规性、金额异常等风险；2. 按风险等级（高/中/低）排序；3. 给出具体的风险防控建议。','请对以下审批事项进行风险分析：\n事项类型：{matterType}\n事项名称：{matterName}\n申请金额：{amount}\n申请人信息：{applicantInfo}\n申请材料清单：{materials}\n规则校验结果：{ruleCheckResult}','[\"matterType\", \"matterName\", \"amount\", \"applicantInfo\", \"materials\", \"ruleCheckResult\"]',1,'2026-09-02 16:36:11');
/*!40000 ALTER TABLE `ai_prompt_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_instance`
--

DROP TABLE IF EXISTS `approval_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '流程模板ID',
  `matter_id` varchar(64) NOT NULL COMMENT '事项ID',
  `applicant_name` varchar(64) DEFAULT NULL COMMENT '申请人姓名',
  `applicant_id` varchar(64) DEFAULT NULL COMMENT '申请人ID',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待审批 APPROVING-审批中 APPROVED-已通过 REJECTED-已退回 SUSPENDED-已挂起 COMPLETED-已完成',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `current_node_id` bigint DEFAULT NULL COMMENT '当前环节ID',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_status` (`status`),
  KEY `idx_submit_time` (`submit_time`),
  KEY `idx_applicant_id` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批实例';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_instance`
--

LOCK TABLES `approval_instance` WRITE;
/*!40000 ALTER TABLE `approval_instance` DISABLE KEYS */;
/*!40000 ALTER TABLE `approval_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_process_node`
--

DROP TABLE IF EXISTS `approval_process_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_process_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '所属模板ID',
  `node_name` varchar(128) NOT NULL COMMENT '环节名称',
  `node_type` tinyint NOT NULL COMMENT '环节类型：0-开始 1-审批 2-规则审核 3-分支 4-结束',
  `assignee_role` varchar(64) DEFAULT NULL COMMENT '审批角色',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序序号',
  `next_node_id` bigint DEFAULT NULL COMMENT '下一环节ID',
  `branch_config` json DEFAULT NULL COMMENT '分支配置（JSON）',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批流程环节';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_process_node`
--

LOCK TABLES `approval_process_node` WRITE;
/*!40000 ALTER TABLE `approval_process_node` DISABLE KEYS */;
INSERT INTO `approval_process_node` VALUES (1,1,'开始',0,NULL,0,2,NULL),(2,1,'材料规则预审',2,'system_auto',1,3,NULL),(3,1,'主管审批',1,'approval_manager',2,NULL,NULL);
/*!40000 ALTER TABLE `approval_process_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_process_template`
--

DROP TABLE IF EXISTS `approval_process_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_process_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `version` varchar(16) NOT NULL DEFAULT '1.0.0' COMMENT '模板版本',
  `matter_type` varchar(64) NOT NULL COMMENT '事项类型',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用 1-启用',
  `node_config` json DEFAULT NULL COMMENT '节点配置（JSON）',
  `description` varchar(512) DEFAULT NULL COMMENT '模板描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_matter_type` (`matter_type`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批流程模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_process_template`
--

LOCK TABLES `approval_process_template` WRITE;
/*!40000 ALTER TABLE `approval_process_template` DISABLE KEYS */;
INSERT INTO `approval_process_template` VALUES (1,'TPL_COMMON_APPROVAL_001','通用行政审批流程','1.0.0','general_administrative',1,'{\"edges\": [{\"to\": \"node_rule_check\", \"from\": \"node_start\"}, {\"to\": \"node_approval\", \"from\": \"node_rule_check\"}, {\"to\": \"node_end\", \"from\": \"node_approval\"}], \"nodes\": [{\"id\": \"node_start\", \"type\": \"START\"}, {\"id\": \"node_rule_check\", \"type\": \"RULE_CHECK\"}, {\"id\": \"node_approval\", \"type\": \"APPROVAL\"}, {\"id\": \"node_end\", \"type\": \"END\"}]}','适用于一般性行政审批事项的通用流程，包含规则预审和人工审批两个核心环节','2026-09-02 16:36:11','2026-09-02 16:36:11'),(2,'ENTERPRISE_REGISTER','新办企业审批流程','1.0','ENTERPRISE_REGISTER',1,NULL,'企业开办一件事审批流程','2026-09-06 20:11:50','2026-09-06 20:11:50');
/*!40000 ALTER TABLE `approval_process_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_rule_item`
--

DROP TABLE IF EXISTS `approval_rule_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_rule_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_set_id` bigint NOT NULL COMMENT '规则集ID',
  `rule_type` varchar(32) NOT NULL COMMENT '规则类型：REQUIRED-必填 COMPARE-比较 REGEX-正则 RANGE-范围 CUSTOM-自定义',
  `field_name` varchar(128) NOT NULL COMMENT '字段名称',
  `operator` varchar(16) NOT NULL COMMENT '比较操作符（>, <, =, !=, contains等）',
  `compare_value` varchar(256) NOT NULL COMMENT '比较值',
  `logic_operator` varchar(8) NOT NULL DEFAULT 'AND' COMMENT '逻辑运算符：AND/OR',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序序号',
  `error_message` varchar(256) DEFAULT NULL COMMENT '校验失败提示信息',
  PRIMARY KEY (`id`),
  KEY `idx_rule_set_id` (`rule_set_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批规则项';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_rule_item`
--

LOCK TABLES `approval_rule_item` WRITE;
/*!40000 ALTER TABLE `approval_rule_item` DISABLE KEYS */;
INSERT INTO `approval_rule_item` VALUES (1,1,'COMPARE','apply_amount','>','0','AND',0,'申请金额必须大于0'),(2,1,'REGEX','material_name','contains','身份证','AND',1,'申请材料必须包含身份证'),(3,1,'RANGE','apply_date','<=','CURRENT_DATE','AND',2,'申请日期不能晚于当前日期');
/*!40000 ALTER TABLE `approval_rule_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_rule_set`
--

DROP TABLE IF EXISTS `approval_rule_set`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_rule_set` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_set_name` varchar(128) NOT NULL COMMENT '规则集名称',
  `matter_type` varchar(64) NOT NULL COMMENT '事项类型',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用 1-启用',
  `logic_type` varchar(8) NOT NULL DEFAULT 'AND' COMMENT '逻辑类型：AND/OR',
  `description` varchar(512) DEFAULT NULL COMMENT '规则集描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_matter_type` (`matter_type`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批规则集';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_rule_set`
--

LOCK TABLES `approval_rule_set` WRITE;
/*!40000 ALTER TABLE `approval_rule_set` DISABLE KEYS */;
INSERT INTO `approval_rule_set` VALUES (1,'通用行政审批校验规则','general_administrative',1,'AND','通用行政审批的基础校验规则集，包含申请金额、申请材料名称、申请日期的基础校验','2026-09-02 16:36:11');
/*!40000 ALTER TABLE `approval_rule_set` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_task`
--

DROP TABLE IF EXISTS `approval_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` bigint NOT NULL COMMENT '审批实例ID',
  `node_id` bigint NOT NULL COMMENT '流程环节ID',
  `assignee_user_id` varchar(64) DEFAULT NULL COMMENT '办理人ID',
  `assignee_user_name` varchar(64) DEFAULT NULL COMMENT '办理人姓名',
  `task_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING-待处理 APPROVED-已通过 REJECTED-已退回 DELEGATED-已委托 SUSPENDED-已挂起 CANCELLED-已取消',
  `approve_result` varchar(32) DEFAULT NULL COMMENT '审批结果：APPROVED-通过 REJECTED-退回',
  `approve_opinion` text COMMENT '审批意见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `handle_time` datetime DEFAULT NULL COMMENT '办理时间',
  `delegate_to` varchar(64) DEFAULT NULL COMMENT '委托人ID',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_assignee_user_id` (`assignee_user_id`),
  KEY `idx_task_status` (`task_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_task`
--

LOCK TABLES `approval_task` WRITE;
/*!40000 ALTER TABLE `approval_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `approval_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` bigint NOT NULL COMMENT '审批实例ID',
  `operator_id` varchar(64) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型（submit/approve/reject/suspend/delegate等）',
  `operation_opinion` text COMMENT '操作意见',
  `before_status` varchar(32) DEFAULT NULL COMMENT '操作前状态(字符串状态码)',
  `after_status` varchar(32) DEFAULT NULL COMMENT '操作后状态(字符串状态码)',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-06 20:21:23
