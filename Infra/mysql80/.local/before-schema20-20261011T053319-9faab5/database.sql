-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: roomly_local80
-- ------------------------------------------------------
-- Server version	8.0.45

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
-- Table structure for table `account_records`
--

DROP TABLE IF EXISTS `account_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_type` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `version` int NOT NULL DEFAULT '0',
  `role_name` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_token_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_user_id` bigint DEFAULT NULL,
  `refresh_installation_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_device_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_expires_at` datetime(3) DEFAULT NULL,
  `refresh_revoked` tinyint(1) DEFAULT '0',
  `refresh_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `verify_token_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `verify_user_id` bigint DEFAULT NULL,
  `verify_purpose` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `verify_used` tinyint(1) DEFAULT '0',
  `verify_expires_at` datetime(3) DEFAULT NULL,
  `verify_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `device_user_id` bigint DEFAULT NULL,
  `device_token` text COLLATE utf8mb4_unicode_ci,
  `device_platform` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'ANDROID',
  `device_device_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_installation_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_active` tinyint(1) DEFAULT '1',
  `device_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `device_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `device_active_installation` varchar(36) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (if((`record_type` = _ascii'DEVICE_TOKEN'),if(`device_active`,`device_installation_id`,NULL),NULL)) STORED,
  `device_active_token_hash` varchar(64) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (if((`record_type` = _ascii'DEVICE_TOKEN'),if(`device_active`,sha2(`device_token`,256),NULL),NULL)) STORED,
  `preference_user_id` bigint DEFAULT NULL,
  `preference_transaction_push` tinyint(1) DEFAULT '1',
  `preference_transaction_email` tinyint(1) DEFAULT '1',
  `preference_chat_push` tinyint(1) DEFAULT '1',
  `preference_recommendation_push` tinyint(1) DEFAULT '1',
  `preference_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `preference_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deletion_user_id` bigint DEFAULT NULL,
  `deletion_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `deletion_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `deletion_failure_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deletion_completed_at` datetime(3) DEFAULT NULL,
  `deletion_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `deletion_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `lifecycle_user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_records_typed_id` (`id`,`record_type`),
  UNIQUE KEY `uk_account_records_1` (`role_name`),
  UNIQUE KEY `uk_account_records_4` (`refresh_token_hash`),
  UNIQUE KEY `uk_account_records_7` (`verify_token_hash`),
  UNIQUE KEY `uk_account_records_9` (`device_active_installation`),
  UNIQUE KEY `uk_account_records_10` (`device_active_token_hash`),
  UNIQUE KEY `uk_account_records_11` (`device_user_id`,`device_installation_id`),
  UNIQUE KEY `uk_account_records_12` (`preference_user_id`),
  UNIQUE KEY `uk_account_records_14` (`deletion_user_id`),
  UNIQUE KEY `uk_account_records_15` (`lifecycle_user_id`),
  KEY `ix_account_records_2` (`record_type`,`refresh_expires_at`),
  KEY `ix_account_records_3` (`record_type`,`refresh_user_id`),
  KEY `ix_account_records_5` (`record_type`,`verify_expires_at`),
  KEY `ix_account_records_6` (`record_type`,`verify_user_id`),
  KEY `ix_account_records_8` (`record_type`,`device_user_id`,`device_active`),
  KEY `ix_account_records_13` (`record_type`,`deletion_status`),
  KEY `fk_refresh_user_id` (`refresh_user_id`),
  KEY `fk_verify_user_id` (`verify_user_id`),
  CONSTRAINT `fk_deletion_user_id` FOREIGN KEY (`deletion_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_device_user_id` FOREIGN KEY (`device_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_lifecycle_user_id` FOREIGN KEY (`lifecycle_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_preference_user_id` FOREIGN KEY (`preference_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_refresh_user_id` FOREIGN KEY (`refresh_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_verify_user_id` FOREIGN KEY (`verify_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_account_records_kind` CHECK ((`record_type` in (_ascii'ROLE',_ascii'REFRESH_TOKEN',_ascii'ONE_TIME_TOKEN',_ascii'DEVICE_TOKEN',_ascii'PREFERENCES',_ascii'DELETION_REQUEST',_ascii'LIFECYCLE_LOCK'))),
  CONSTRAINT `ck_deletion_required` CHECK (((`record_type` <> _ascii'DELETION_REQUEST') or ((`deletion_user_id` is not null) and (`deletion_reason` is not null) and (`deletion_status` is not null)))),
  CONSTRAINT `ck_device_required` CHECK (((`record_type` <> _ascii'DEVICE_TOKEN') or ((`device_user_id` is not null) and (`device_token` is not null) and (`device_platform` is not null) and (`device_device_name` is not null) and (`device_installation_id` is not null) and (`device_active` is not null)))),
  CONSTRAINT `ck_lifecycle_required` CHECK (((`record_type` <> _ascii'LIFECYCLE_LOCK') or (`lifecycle_user_id` is not null))),
  CONSTRAINT `ck_preference_required` CHECK (((`record_type` <> _ascii'PREFERENCES') or ((`preference_user_id` is not null) and (`preference_transaction_push` is not null) and (`preference_transaction_email` is not null) and (`preference_chat_push` is not null) and (`preference_recommendation_push` is not null)))),
  CONSTRAINT `ck_refresh_required` CHECK (((`record_type` <> _ascii'REFRESH_TOKEN') or ((`refresh_token_hash` is not null) and (`refresh_user_id` is not null) and (`refresh_installation_id` is not null) and (`refresh_device_name` is not null) and (`refresh_expires_at` is not null) and (`refresh_revoked` is not null)))),
  CONSTRAINT `ck_role_required` CHECK (((`record_type` <> _ascii'ROLE') or (`role_name` is not null))),
  CONSTRAINT `ck_verify_required` CHECK (((`record_type` <> _ascii'ONE_TIME_TOKEN') or ((`verify_token_hash` is not null) and (`verify_user_id` is not null) and (`verify_purpose` is not null) and (`verify_used` is not null) and (`verify_expires_at` is not null))))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_records`
--

LOCK TABLES `account_records` WRITE;
/*!40000 ALTER TABLE `account_records` DISABLE KEYS */;
INSERT INTO `account_records` (`id`, `record_type`, `version`, `role_name`, `refresh_token_hash`, `refresh_user_id`, `refresh_installation_id`, `refresh_device_name`, `refresh_expires_at`, `refresh_revoked`, `refresh_created_at`, `verify_token_hash`, `verify_user_id`, `verify_purpose`, `verify_used`, `verify_expires_at`, `verify_created_at`, `device_user_id`, `device_token`, `device_platform`, `device_device_name`, `device_installation_id`, `device_active`, `device_created_at`, `device_updated_at`, `preference_user_id`, `preference_transaction_push`, `preference_transaction_email`, `preference_chat_push`, `preference_recommendation_push`, `preference_created_at`, `preference_updated_at`, `deletion_user_id`, `deletion_reason`, `deletion_status`, `deletion_failure_code`, `deletion_completed_at`, `deletion_created_at`, `deletion_updated_at`, `lifecycle_user_id`) VALUES (1,'ROLE',0,'ROLE_TENANT',NULL,NULL,NULL,NULL,NULL,0,'2026-10-10 22:55:55.300',NULL,NULL,NULL,0,NULL,'2026-10-10 22:55:55.300',NULL,NULL,'ANDROID',NULL,NULL,1,'2026-10-10 22:55:55.300','2026-10-10 22:55:55.300',NULL,1,1,1,1,'2026-10-10 22:55:55.300','2026-10-10 22:55:55.300',NULL,'','PENDING',NULL,NULL,'2026-10-10 22:55:55.300','2026-10-10 22:55:55.300',NULL),(2,'ROLE',0,'ROLE_HOST',NULL,NULL,NULL,NULL,NULL,0,'2026-10-10 22:55:55.300',NULL,NULL,NULL,0,NULL,'2026-10-10 22:55:55.300',NULL,NULL,'ANDROID',NULL,NULL,1,'2026-10-10 22:55:55.300','2026-10-10 22:55:55.300',NULL,1,1,1,1,'2026-10-10 22:55:55.300','2026-10-10 22:55:55.300',NULL,'','PENDING',NULL,NULL,'2026-10-10 22:55:55.300','2026-10-10 22:55:55.300',NULL),(3,'ROLE',0,'ROLE_ADMIN',NULL,NULL,NULL,NULL,NULL,0,'2026-10-10 22:55:55.300',NULL,NULL,NULL,0,NULL,'2026-10-10 22:55:55.300',NULL,NULL,'ANDROID',NULL,NULL,1,'2026-10-10 22:55:55.300','2026-10-10 22:55:55.300',NULL,1,1,1,1,'2026-10-10 22:55:55.300','2026-10-10 22:55:55.300',NULL,'','PENDING',NULL,NULL,'2026-10-10 22:55:55.300','2026-10-10 22:55:55.300',NULL);
/*!40000 ALTER TABLE `account_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `activity_records`
--

DROP TABLE IF EXISTS `activity_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_type` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `version` int NOT NULL DEFAULT '0',
  `notification_user_id` bigint DEFAULT NULL,
  `notification_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notification_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notification_body` text COLLATE utf8mb4_unicode_ci,
  `notification_ref_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notification_ref_id` bigint DEFAULT NULL,
  `notification_is_read` tinyint(1) DEFAULT '0',
  `notification_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `delivery_notification_id` bigint DEFAULT NULL,
  `delivery_device_id` bigint DEFAULT NULL,
  `delivery_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `delivery_attempts` int DEFAULT '0',
  `delivery_next_attempt_at` datetime(3) DEFAULT NULL,
  `delivery_last_error` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `delivery_created_at` datetime(3) DEFAULT NULL,
  `delivery_device_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'DEVICE_TOKEN',
  `delivery_notification_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'NOTIFICATION',
  `audit_actor_id` bigint DEFAULT NULL,
  `audit_actor_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `audit_action` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `audit_target_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `audit_target_id` bigint DEFAULT NULL,
  `audit_detail` json DEFAULT NULL,
  `audit_ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `audit_trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `audit_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `request_idempotency_key` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PROCESSING',
  `request_response_status_code` int DEFAULT NULL,
  `request_response_body` longtext COLLATE utf8mb4_unicode_ci,
  `request_expires_at` datetime(3) DEFAULT NULL,
  `request_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `request_request_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_response_content_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_response_location` text COLLATE utf8mb4_unicode_ci,
  `review_booking_id` bigint DEFAULT NULL,
  `review_reviewer_id` bigint DEFAULT NULL,
  `review_room_id` bigint DEFAULT NULL,
  `review_rating` int DEFAULT NULL,
  `review_comment` text COLLATE utf8mb4_unicode_ci,
  `review_visible` tinyint(1) DEFAULT '1',
  `review_hide_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `review_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `review_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `report_reporter_id` bigint DEFAULT NULL,
  `report_target_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `report_target_id` bigint DEFAULT NULL,
  `report_reason_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `report_description` text COLLATE utf8mb4_unicode_ci,
  `report_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `report_resolved_by` bigint DEFAULT NULL,
  `report_resolution_note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `report_resolved_at` datetime(3) DEFAULT NULL,
  `report_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `report_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_records_typed_id` (`id`,`record_type`),
  UNIQUE KEY `uk_activity_records_5` (`delivery_notification_id`,`delivery_device_id`),
  UNIQUE KEY `uk_activity_records_8` (`request_idempotency_key`),
  UNIQUE KEY `uk_activity_records_11` (`review_booking_id`),
  KEY `ix_activity_records_1` (`record_type`,`notification_user_id`,`notification_is_read`,`notification_created_at`),
  KEY `ix_activity_records_2` (`record_type`,`delivery_device_id`),
  KEY `ix_activity_records_3` (`record_type`,`delivery_status`,`delivery_created_at`,`id`),
  KEY `ix_activity_records_4` (`record_type`,`delivery_status`,`delivery_next_attempt_at`),
  KEY `ix_activity_records_6` (`record_type`,`audit_actor_id`,`audit_created_at`),
  KEY `ix_activity_records_7` (`record_type`,`audit_target_type`,`audit_target_id`,`audit_created_at`),
  KEY `ix_activity_records_9` (`record_type`,`request_status`,`request_expires_at`,`id`),
  KEY `ix_activity_records_10` (`record_type`,`request_expires_at`),
  KEY `ix_activity_records_12` (`record_type`,`review_reviewer_id`),
  KEY `ix_activity_records_13` (`record_type`,`review_room_id`,`review_visible`,`review_created_at`),
  KEY `ix_activity_records_14` (`record_type`,`report_resolved_by`),
  KEY `ix_activity_records_15` (`record_type`,`report_reporter_id`),
  KEY `ix_activity_records_16` (`record_type`,`report_status`),
  KEY `ix_activity_records_17` (`record_type`,`report_target_type`,`report_target_id`),
  KEY `fk_notification_user_id` (`notification_user_id`),
  KEY `fk_delivery_device_id` (`delivery_device_id`,`delivery_device_id_kind`),
  KEY `fk_delivery_notification_id` (`delivery_notification_id`,`delivery_notification_id_kind`),
  KEY `fk_audit_actor_id` (`audit_actor_id`),
  KEY `fk_review_reviewer_id` (`review_reviewer_id`),
  KEY `fk_review_room_id` (`review_room_id`),
  KEY `fk_report_reporter_id` (`report_reporter_id`),
  KEY `fk_report_resolved_by` (`report_resolved_by`),
  CONSTRAINT `fk_audit_actor_id` FOREIGN KEY (`audit_actor_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_delivery_device_id` FOREIGN KEY (`delivery_device_id`, `delivery_device_id_kind`) REFERENCES `account_records` (`id`, `record_type`),
  CONSTRAINT `fk_delivery_notification_id` FOREIGN KEY (`delivery_notification_id`, `delivery_notification_id_kind`) REFERENCES `activity_records` (`id`, `record_type`),
  CONSTRAINT `fk_notification_user_id` FOREIGN KEY (`notification_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_report_reporter_id` FOREIGN KEY (`report_reporter_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_report_resolved_by` FOREIGN KEY (`report_resolved_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_review_reviewer_id` FOREIGN KEY (`review_reviewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_review_room_id` FOREIGN KEY (`review_room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `ck_activity_records_kind` CHECK ((`record_type` in (_ascii'NOTIFICATION',_ascii'PUSH_DELIVERY',_ascii'AUDIT_LOG',_ascii'IDEMPOTENCY',_ascii'REVIEW',_ascii'REPORT'))),
  CONSTRAINT `ck_audit_required` CHECK (((`record_type` <> _ascii'AUDIT_LOG') or ((`audit_actor_type` is not null) and (`audit_action` is not null) and (`audit_target_type` is not null) and (`audit_target_id` is not null)))),
  CONSTRAINT `ck_delivery_device_id_kind` CHECK ((`delivery_device_id_kind` = _ascii'DEVICE_TOKEN')),
  CONSTRAINT `ck_delivery_notification_id_kind` CHECK ((`delivery_notification_id_kind` = _ascii'NOTIFICATION')),
  CONSTRAINT `ck_delivery_required` CHECK (((`record_type` <> _ascii'PUSH_DELIVERY') or ((`delivery_notification_id` is not null) and (`delivery_device_id` is not null) and (`delivery_status` is not null) and (`delivery_attempts` is not null) and (`delivery_next_attempt_at` is not null) and (`delivery_created_at` is not null)))),
  CONSTRAINT `ck_notification_required` CHECK (((`record_type` <> _ascii'NOTIFICATION') or ((`notification_user_id` is not null) and (`notification_type` is not null) and (`notification_title` is not null) and (`notification_is_read` is not null)))),
  CONSTRAINT `ck_report_required` CHECK (((`record_type` <> _ascii'REPORT') or ((`report_reporter_id` is not null) and (`report_target_type` is not null) and (`report_target_id` is not null) and (`report_reason_code` is not null) and (`report_status` is not null)))),
  CONSTRAINT `ck_request_required` CHECK (((`record_type` <> _ascii'IDEMPOTENCY') or ((`request_idempotency_key` is not null) and (`request_status` is not null) and (`request_expires_at` is not null)))),
  CONSTRAINT `ck_review_required` CHECK (((`record_type` <> _ascii'REVIEW') or ((`review_booking_id` is not null) and (`review_reviewer_id` is not null) and (`review_room_id` is not null) and (`review_rating` is not null) and (`review_visible` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activity_records`
--

LOCK TABLES `activity_records` WRITE;
/*!40000 ALTER TABLE `activity_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `activity_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `catalog_items`
--

DROP TABLE IF EXISTS `catalog_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_type` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `amenity_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amenity_icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amenity_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fee_listing_id` bigint DEFAULT NULL,
  `fee_fee_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fee_fee_mode` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'FIXED',
  `fee_amount_vnd` decimal(18,0) DEFAULT NULL,
  `fee_unit_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fee_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fee_sort_order` int DEFAULT '0',
  `wishlist_user_id` bigint DEFAULT NULL,
  `wishlist_room_id` bigint DEFAULT NULL,
  `wishlist_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_items_typed_id` (`id`,`record_type`),
  UNIQUE KEY `uk_catalog_items_1` (`amenity_name`),
  UNIQUE KEY `uk_catalog_items_5` (`wishlist_user_id`,`wishlist_room_id`),
  KEY `ix_catalog_items_2` (`record_type`,`fee_listing_id`),
  KEY `ix_catalog_items_3` (`record_type`,`wishlist_room_id`),
  KEY `ix_catalog_items_4` (`record_type`,`wishlist_user_id`),
  KEY `fk_fee_listing_id` (`fee_listing_id`),
  KEY `fk_wishlist_room_id` (`wishlist_room_id`),
  CONSTRAINT `fk_fee_listing_id` FOREIGN KEY (`fee_listing_id`) REFERENCES `listings` (`id`),
  CONSTRAINT `fk_wishlist_room_id` FOREIGN KEY (`wishlist_room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_wishlist_user_id` FOREIGN KEY (`wishlist_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_amenity_required` CHECK (((`record_type` <> _ascii'AMENITY') or (`amenity_name` is not null))),
  CONSTRAINT `ck_catalog_items_kind` CHECK ((`record_type` in (_ascii'AMENITY',_ascii'LISTING_FEE',_ascii'WISHLIST_ITEM'))),
  CONSTRAINT `ck_fee_required` CHECK (((`record_type` <> _ascii'LISTING_FEE') or ((`fee_listing_id` is not null) and (`fee_fee_code` is not null) and (`fee_fee_mode` is not null) and (`fee_amount_vnd` is not null) and (`fee_sort_order` is not null)))),
  CONSTRAINT `ck_wishlist_required` CHECK (((`record_type` <> _ascii'WISHLIST_ITEM') or ((`wishlist_user_id` is not null) and (`wishlist_room_id` is not null))))
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalog_items`
--

LOCK TABLES `catalog_items` WRITE;
/*!40000 ALTER TABLE `catalog_items` DISABLE KEYS */;
INSERT INTO `catalog_items` VALUES (1,'AMENITY','WiFi','wifi','connectivity',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(2,'AMENITY','Air Conditioning','ac_unit','comfort',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(3,'AMENITY','Washing Machine','local_laundry_service','appliance',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(4,'AMENITY','Refrigerator','kitchen','appliance',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(5,'AMENITY','Water Heater','hot_tub','comfort',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(6,'AMENITY','Parking','local_parking','facility',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(7,'AMENITY','Security Camera','videocam','security',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(8,'AMENITY','Balcony','balcony','feature',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(9,'AMENITY','Kitchen','restaurant','feature',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(10,'AMENITY','Private Bathroom','bathtub','feature',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(11,'AMENITY','Pet Allowed','pets','policy',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(12,'AMENITY','Furnished','chair','feature',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(13,'AMENITY','Elevator','elevator','facility',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(14,'AMENITY','Gym','fitness_center','facility',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303'),(15,'AMENITY','Swimming Pool','pool','facility',NULL,NULL,'FIXED',NULL,NULL,NULL,0,NULL,NULL,'2026-10-10 22:55:55.303');
/*!40000 ALTER TABLE `catalog_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `conversations`
--

DROP TABLE IF EXISTS `conversations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `host_id` bigint NOT NULL,
  `room_title` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_message_at` datetime(3) DEFAULT NULL,
  `last_message_preview` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sequence` bigint NOT NULL DEFAULT '0',
  `version` int NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_room_tenant` (`room_id`,`tenant_id`),
  KEY `idx_conversation_tenant` (`tenant_id`,`last_message_at`,`id`),
  KEY `idx_conversation_host` (`host_id`,`last_message_at`,`id`),
  CONSTRAINT `fk_conversation_host` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_conversation_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_conversation_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_conversation_members` CHECK ((`tenant_id` <> `host_id`)),
  CONSTRAINT `ck_conversation_sequence` CHECK ((`sequence` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `conversations`
--

LOCK TABLES `conversations` WRITE;
/*!40000 ALTER TABLE `conversations` DISABLE KEYS */;
/*!40000 ALTER TABLE `conversations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','create users table','SQL','V1__create_users_table.sql',1826201962,'roomly_local80','2026-10-10 15:10:29',69,1),(2,'2','auth tables','SQL','V2__auth_tables.sql',787361212,'roomly_local80','2026-10-10 15:10:29',493,1),(3,'3','catalog tables','SQL','V3__catalog_tables.sql',1653764284,'roomly_local80','2026-10-10 15:10:30',835,1),(4,'4','idempotency request fingerprint','SQL','V4__idempotency_request_fingerprint.sql',-1836370027,'roomly_local80','2026-10-10 15:10:30',122,1),(5,'5','fix catalog schema','SQL','V5__fix_catalog_schema.sql',2119791417,'roomly_local80','2026-10-10 15:10:31',356,1),(6,'6','add user roles','SQL','V6__add_user_roles.sql',-1553442380,'roomly_local80','2026-10-10 15:10:31',252,1),(7,'7','media tables','SQL','V7__media_tables.sql',1743316116,'roomly_local80','2026-10-10 15:10:31',91,1),(8,'8','interaction tables','SQL','V8__interaction_tables.sql',-2019790549,'roomly_local80','2026-10-10 15:10:31',455,1),(9,'9','booking tables','SQL','V9__booking_tables.sql',-1182686610,'roomly_local80','2026-10-10 15:10:32',931,1),(10,'10','payment tables','SQL','V10__payment_tables.sql',-907760036,'roomly_local80','2026-10-10 15:10:33',654,1),(11,'11','chat tables','SQL','V11__chat_tables.sql',-54311385,'roomly_local80','2026-10-10 15:10:33',278,1),(12,'12','notification admin tables','SQL','V12__notification_admin_tables.sql',-1052808021,'roomly_local80','2026-10-10 15:10:34',184,1),(13,'13','add user suspended','SQL','V13__add_user_suspended.sql',1290904185,'roomly_local80','2026-10-10 15:10:34',178,1),(14,'14','case settlement amounts','SQL','V14__case_settlement_amounts.sql',659611730,'roomly_local80','2026-10-10 15:10:34',118,1),(15,'15','push delivery outbox','SQL','V15__push_delivery_outbox.sql',2105256890,'roomly_local80','2026-10-10 15:10:34',69,1),(16,'16','active device uniqueness','SQL','V16__active_device_uniqueness.sql',-1439506432,'roomly_local80','2026-10-10 15:10:34',278,1),(17,'17','retention indexes','SQL','V17__retention_indexes.sql',-1889667394,'roomly_local80','2026-10-10 15:10:34',62,1),(18,'18','account lifecycle locks','SQL','V18__account_lifecycle_locks.sql',1480430692,'roomly_local80','2026-10-10 15:10:34',45,1),(19,'19','compact fourteen table schema','SQL','V19__compact_fourteen_table_schema.sql',-1974099897,'roomly_local80','2026-10-10 15:55:56',18182,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `listings`
--

DROP TABLE IF EXISTS `listings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `listings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `title` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `description` varchar(5000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `rent_vnd` decimal(18,0) DEFAULT NULL,
  `deposit_vnd` decimal(18,0) DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `terms_version` int NOT NULL DEFAULT '1',
  `published_at` datetime(3) DEFAULT NULL,
  `expires_at` datetime(3) DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_listings_room` (`room_id`),
  KEY `idx_listings_room` (`room_id`),
  KEY `idx_listings_status` (`status`),
  KEY `idx_listings_published` (`published_at`),
  KEY `idx_listings_expires` (`status`,`expires_at`),
  CONSTRAINT `fk_listings_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `listings`
--

LOCK TABLES `listings` WRITE;
/*!40000 ALTER TABLE `listings` DISABLE KEYS */;
/*!40000 ALTER TABLE `listings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `media`
--

DROP TABLE IF EXISTS `media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uploader_id` bigint NOT NULL,
  `purpose` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `resource_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resource_id` bigint DEFAULT NULL,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size_bytes` bigint NOT NULL,
  `storage_key` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_filename` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `width` int DEFAULT NULL,
  `height` int DEFAULT NULL,
  `duration_secs` int DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'READY',
  `display_order` int NOT NULL DEFAULT '0',
  `version` int NOT NULL DEFAULT '0',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `storage_key` (`storage_key`),
  KEY `idx_media_uploader` (`uploader_id`),
  KEY `idx_media_resource` (`resource_type`,`resource_id`,`status`),
  KEY `idx_media_status_created` (`status`,`created_at`),
  KEY `idx_media_purpose` (`purpose`),
  CONSTRAINT `fk_media_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `media`
--

LOCK TABLES `media` WRITE;
/*!40000 ALTER TABLE `media` DISABLE KEYS */;
/*!40000 ALTER TABLE `media` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `client_message_id` varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `sequence` bigint NOT NULL,
  `content_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_client` (`conversation_id`,`client_message_id`),
  UNIQUE KEY `uk_message_sequence` (`conversation_id`,`sequence`),
  KEY `fk_message_sender` (`sender_id`),
  CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`),
  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_message_sequence` CHECK ((`sequence` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `messages`
--

LOCK TABLES `messages` WRITE;
/*!40000 ALTER TABLE `messages` DISABLE KEYS */;
/*!40000 ALTER TABLE `messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `read_markers`
--

DROP TABLE IF EXISTS `read_markers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `read_markers` (
  `conversation_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `last_read_sequence` bigint NOT NULL DEFAULT '0',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`conversation_id`,`user_id`),
  KEY `fk_marker_user` (`user_id`),
  CONSTRAINT `fk_marker_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`),
  CONSTRAINT `fk_marker_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_marker_sequence` CHECK ((`last_read_sequence` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `read_markers`
--

LOCK TABLES `read_markers` WRITE;
/*!40000 ALTER TABLE `read_markers` DISABLE KEYS */;
/*!40000 ALTER TABLE `read_markers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rental_records`
--

DROP TABLE IF EXISTS `rental_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rental_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_type` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `version` int NOT NULL DEFAULT '0',
  `booking_tenant_id` bigint DEFAULT NULL,
  `booking_host_id` bigint DEFAULT NULL,
  `booking_room_id` bigint DEFAULT NULL,
  `booking_listing_id` bigint DEFAULT NULL,
  `booking_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `booking_desired_move_in` date DEFAULT NULL,
  `booking_occupant_count` int DEFAULT '1',
  `booking_rent_vnd` decimal(18,0) DEFAULT NULL,
  `booking_deposit_vnd` decimal(18,0) DEFAULT '0',
  `booking_note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `booking_terms_snapshot` json DEFAULT NULL,
  `booking_snapshot_schema_version` int DEFAULT '1',
  `booking_request_expires_at` datetime(3) DEFAULT NULL,
  `booking_hold_expires_at` datetime(3) DEFAULT NULL,
  `booking_handover_due_at` datetime(3) DEFAULT NULL,
  `booking_tenant_terms_accepted_at` datetime(3) DEFAULT NULL,
  `booking_tenant_handover_confirmed_at` datetime(3) DEFAULT NULL,
  `booking_host_handover_confirmed_at` datetime(3) DEFAULT NULL,
  `booking_allocated_payment_id` bigint DEFAULT NULL,
  `booking_completed_at` datetime(3) DEFAULT NULL,
  `booking_cancelled_at` datetime(3) DEFAULT NULL,
  `booking_last_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `booking_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `booking_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `booking_active_room_id` bigint GENERATED ALWAYS AS (if((`record_type` = _ascii'BOOKING'),(case when (`booking_status` in (_utf8mb4'APPROVED',_utf8mb4'CONFIRMED')) then `booking_room_id` else NULL end),NULL)) STORED,
  `booking_open_room_id` bigint GENERATED ALWAYS AS (if((`record_type` = _ascii'BOOKING'),(case when (`booking_status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'CONFIRMED')) then `booking_room_id` else NULL end),NULL)) STORED,
  `booking_allocated_payment_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PAYMENT',
  `case_booking_id` bigint DEFAULT NULL,
  `case_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `case_opened_by` bigint DEFAULT NULL,
  `case_description` text COLLATE utf8mb4_unicode_ci,
  `case_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'OPEN',
  `case_decision` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `case_resolution_note` text COLLATE utf8mb4_unicode_ci,
  `case_resolved_by` bigint DEFAULT NULL,
  `case_resolved_at` datetime(3) DEFAULT NULL,
  `case_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `case_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `case_open_booking_id` bigint GENERATED ALWAYS AS (if((`record_type` = _ascii'BOOKING_CASE'),(case when (`case_status` in (_utf8mb4'OPEN',_utf8mb4'IN_REVIEW')) then `case_booking_id` else NULL end),NULL)) STORED,
  `case_tenant_refund_vnd` decimal(18,0) DEFAULT NULL,
  `case_host_retained_vnd` decimal(18,0) DEFAULT NULL,
  `case_booking_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'BOOKING',
  `history_booking_id` bigint DEFAULT NULL,
  `history_from_status` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `history_to_status` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `history_actor_id` bigint DEFAULT NULL,
  `history_actor_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `history_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `history_trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `history_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `history_booking_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'BOOKING',
  `slot_room_id` bigint DEFAULT NULL,
  `slot_host_id` bigint DEFAULT NULL,
  `slot_start_at` datetime(3) DEFAULT NULL,
  `slot_end_at` datetime(3) DEFAULT NULL,
  `slot_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'OPEN',
  `slot_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `slot_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `viewing_viewing_slot_id` bigint DEFAULT NULL,
  `viewing_tenant_id` bigint DEFAULT NULL,
  `viewing_room_id` bigint DEFAULT NULL,
  `viewing_host_id` bigint DEFAULT NULL,
  `viewing_status` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT 'REQUESTED',
  `viewing_note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `viewing_cancelled_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `viewing_completed_at` datetime(3) DEFAULT NULL,
  `viewing_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `viewing_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `viewing_viewing_slot_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'VIEWING_SLOT',
  `payment_booking_id` bigint DEFAULT NULL,
  `payment_payer_id` bigint DEFAULT NULL,
  `payment_provider` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'MOCK_SANDBOX',
  `payment_provider_payment_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_amount_vnd` decimal(18,0) DEFAULT NULL,
  `payment_currency` char(3) COLLATE utf8mb4_unicode_ci DEFAULT 'VND',
  `payment_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `payment_expires_at` datetime(3) DEFAULT NULL,
  `payment_succeeded_at` datetime(3) DEFAULT NULL,
  `payment_failure_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_is_test` tinyint(1) DEFAULT '1',
  `payment_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `payment_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `payment_pending_booking_id` bigint GENERATED ALWAYS AS (if((`record_type` = _ascii'PAYMENT'),(case when (`payment_status` = _utf8mb4'PENDING') then `payment_booking_id` else NULL end),NULL)) STORED,
  `payment_booking_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'BOOKING',
  `refund_payment_id` bigint DEFAULT NULL,
  `refund_provider_refund_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refund_amount_vnd` decimal(18,0) DEFAULT NULL,
  `refund_reason` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refund_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `refund_attempt_count` int DEFAULT '0',
  `refund_next_attempt_at` datetime(3) DEFAULT NULL,
  `refund_last_error_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refund_completed_at` datetime(3) DEFAULT NULL,
  `refund_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `refund_updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `refund_payment_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PAYMENT',
  `webhook_provider` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `webhook_event_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `webhook_payment_id` bigint DEFAULT NULL,
  `webhook_payload_hash` binary(32) DEFAULT NULL,
  `webhook_outcome` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `webhook_processed_at` datetime(3) DEFAULT NULL,
  `webhook_created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `webhook_payment_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PAYMENT',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_records_typed_id` (`id`,`record_type`),
  UNIQUE KEY `uk_rental_records_1` (`booking_allocated_payment_id`),
  UNIQUE KEY `uk_rental_records_9` (`booking_active_room_id`),
  UNIQUE KEY `uk_rental_records_10` (`booking_tenant_id`,`booking_open_room_id`),
  UNIQUE KEY `uk_rental_records_14` (`case_open_booking_id`),
  UNIQUE KEY `uk_rental_records_26` (`payment_provider_payment_id`),
  UNIQUE KEY `uk_rental_records_27` (`payment_pending_booking_id`),
  UNIQUE KEY `uk_rental_records_29` (`refund_payment_id`),
  UNIQUE KEY `uk_rental_records_30` (`refund_provider_refund_id`),
  UNIQUE KEY `uk_rental_records_32` (`webhook_provider`,`webhook_event_id`),
  KEY `ix_rental_records_2` (`record_type`,`booking_listing_id`),
  KEY `ix_rental_records_3` (`record_type`,`booking_room_id`),
  KEY `ix_rental_records_4` (`record_type`,`booking_status`,`booking_handover_due_at`),
  KEY `ix_rental_records_5` (`record_type`,`booking_status`,`booking_hold_expires_at`),
  KEY `ix_rental_records_6` (`record_type`,`booking_status`,`booking_request_expires_at`),
  KEY `ix_rental_records_7` (`record_type`,`booking_host_id`,`booking_status`,`booking_created_at`,`id`),
  KEY `ix_rental_records_8` (`record_type`,`booking_tenant_id`,`booking_status`,`booking_created_at`,`id`),
  KEY `ix_rental_records_11` (`record_type`,`case_opened_by`),
  KEY `ix_rental_records_12` (`record_type`,`case_resolved_by`),
  KEY `ix_rental_records_13` (`record_type`,`case_booking_id`,`case_status`),
  KEY `ix_rental_records_15` (`record_type`,`history_actor_id`),
  KEY `ix_rental_records_16` (`record_type`,`history_booking_id`,`history_created_at`),
  KEY `ix_rental_records_17` (`record_type`,`slot_host_id`,`slot_status`),
  KEY `ix_rental_records_18` (`record_type`,`slot_room_id`,`slot_status`,`slot_start_at`),
  KEY `ix_rental_records_19` (`record_type`,`viewing_viewing_slot_id`),
  KEY `ix_rental_records_20` (`record_type`,`viewing_host_id`,`viewing_status`),
  KEY `ix_rental_records_21` (`record_type`,`viewing_room_id`,`viewing_status`),
  KEY `ix_rental_records_22` (`record_type`,`viewing_tenant_id`,`viewing_status`),
  KEY `ix_rental_records_23` (`record_type`,`payment_payer_id`),
  KEY `ix_rental_records_24` (`record_type`,`payment_booking_id`),
  KEY `ix_rental_records_25` (`record_type`,`payment_status`,`payment_expires_at`),
  KEY `ix_rental_records_28` (`record_type`,`refund_status`,`refund_next_attempt_at`),
  KEY `ix_rental_records_31` (`record_type`,`webhook_payment_id`),
  KEY `fk_booking_host_id` (`booking_host_id`),
  KEY `fk_booking_listing_id` (`booking_listing_id`),
  KEY `fk_booking_allocated_payment_id` (`booking_allocated_payment_id`,`booking_allocated_payment_id_kind`),
  KEY `fk_booking_room_id` (`booking_room_id`),
  KEY `fk_case_booking_id` (`case_booking_id`,`case_booking_id_kind`),
  KEY `fk_case_opened_by` (`case_opened_by`),
  KEY `fk_case_resolved_by` (`case_resolved_by`),
  KEY `fk_history_actor_id` (`history_actor_id`),
  KEY `fk_history_booking_id` (`history_booking_id`,`history_booking_id_kind`),
  KEY `fk_slot_host_id` (`slot_host_id`),
  KEY `fk_slot_room_id` (`slot_room_id`),
  KEY `fk_viewing_host_id` (`viewing_host_id`),
  KEY `fk_viewing_room_id` (`viewing_room_id`),
  KEY `fk_viewing_viewing_slot_id` (`viewing_viewing_slot_id`,`viewing_viewing_slot_id_kind`),
  KEY `fk_viewing_tenant_id` (`viewing_tenant_id`),
  KEY `fk_payment_booking_id` (`payment_booking_id`,`payment_booking_id_kind`),
  KEY `fk_payment_payer_id` (`payment_payer_id`),
  KEY `fk_refund_payment_id` (`refund_payment_id`,`refund_payment_id_kind`),
  KEY `fk_webhook_payment_id` (`webhook_payment_id`,`webhook_payment_id_kind`),
  CONSTRAINT `fk_booking_allocated_payment_id` FOREIGN KEY (`booking_allocated_payment_id`, `booking_allocated_payment_id_kind`) REFERENCES `rental_records` (`id`, `record_type`),
  CONSTRAINT `fk_booking_host_id` FOREIGN KEY (`booking_host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_booking_listing_id` FOREIGN KEY (`booking_listing_id`) REFERENCES `listings` (`id`),
  CONSTRAINT `fk_booking_room_id` FOREIGN KEY (`booking_room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_booking_tenant_id` FOREIGN KEY (`booking_tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_case_booking_id` FOREIGN KEY (`case_booking_id`, `case_booking_id_kind`) REFERENCES `rental_records` (`id`, `record_type`),
  CONSTRAINT `fk_case_opened_by` FOREIGN KEY (`case_opened_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_case_resolved_by` FOREIGN KEY (`case_resolved_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_history_actor_id` FOREIGN KEY (`history_actor_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_history_booking_id` FOREIGN KEY (`history_booking_id`, `history_booking_id_kind`) REFERENCES `rental_records` (`id`, `record_type`),
  CONSTRAINT `fk_payment_booking_id` FOREIGN KEY (`payment_booking_id`, `payment_booking_id_kind`) REFERENCES `rental_records` (`id`, `record_type`),
  CONSTRAINT `fk_payment_payer_id` FOREIGN KEY (`payment_payer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_refund_payment_id` FOREIGN KEY (`refund_payment_id`, `refund_payment_id_kind`) REFERENCES `rental_records` (`id`, `record_type`),
  CONSTRAINT `fk_slot_host_id` FOREIGN KEY (`slot_host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_slot_room_id` FOREIGN KEY (`slot_room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_viewing_host_id` FOREIGN KEY (`viewing_host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_viewing_room_id` FOREIGN KEY (`viewing_room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_viewing_tenant_id` FOREIGN KEY (`viewing_tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_viewing_viewing_slot_id` FOREIGN KEY (`viewing_viewing_slot_id`, `viewing_viewing_slot_id_kind`) REFERENCES `rental_records` (`id`, `record_type`),
  CONSTRAINT `fk_webhook_payment_id` FOREIGN KEY (`webhook_payment_id`, `webhook_payment_id_kind`) REFERENCES `rental_records` (`id`, `record_type`),
  CONSTRAINT `ck_booking_allocated_payment_id_kind` CHECK ((`booking_allocated_payment_id_kind` = _ascii'PAYMENT')),
  CONSTRAINT `ck_booking_required` CHECK (((`record_type` <> _ascii'BOOKING') or ((`booking_tenant_id` is not null) and (`booking_host_id` is not null) and (`booking_room_id` is not null) and (`booking_listing_id` is not null) and (`booking_status` is not null) and (`booking_occupant_count` is not null) and (`booking_rent_vnd` is not null) and (`booking_deposit_vnd` is not null) and (`booking_terms_snapshot` is not null) and (`booking_snapshot_schema_version` is not null) and (`booking_request_expires_at` is not null)))),
  CONSTRAINT `ck_case_booking_id_kind` CHECK ((`case_booking_id_kind` = _ascii'BOOKING')),
  CONSTRAINT `ck_case_required` CHECK (((`record_type` <> _ascii'BOOKING_CASE') or ((`case_booking_id` is not null) and (`case_type` is not null) and (`case_description` is not null) and (`case_status` is not null)))),
  CONSTRAINT `ck_history_booking_id_kind` CHECK ((`history_booking_id_kind` = _ascii'BOOKING')),
  CONSTRAINT `ck_history_required` CHECK (((`record_type` <> _ascii'BOOKING_HISTORY') or ((`history_booking_id` is not null) and (`history_to_status` is not null) and (`history_actor_type` is not null) and (`history_trace_id` is not null)))),
  CONSTRAINT `ck_payment_booking_id_kind` CHECK ((`payment_booking_id_kind` = _ascii'BOOKING')),
  CONSTRAINT `ck_payment_required` CHECK (((`record_type` <> _ascii'PAYMENT') or ((`payment_booking_id` is not null) and (`payment_payer_id` is not null) and (`payment_provider` is not null) and (`payment_amount_vnd` is not null) and (`payment_currency` is not null) and (`payment_status` is not null) and (`payment_expires_at` is not null) and (`payment_is_test` is not null)))),
  CONSTRAINT `ck_refund_payment_id_kind` CHECK ((`refund_payment_id_kind` = _ascii'PAYMENT')),
  CONSTRAINT `ck_refund_required` CHECK (((`record_type` <> _ascii'REFUND') or ((`refund_payment_id` is not null) and (`refund_amount_vnd` is not null) and (`refund_status` is not null) and (`refund_attempt_count` is not null)))),
  CONSTRAINT `ck_rental_records_kind` CHECK ((`record_type` in (_ascii'BOOKING',_ascii'BOOKING_CASE',_ascii'BOOKING_HISTORY',_ascii'VIEWING_SLOT',_ascii'VIEWING',_ascii'PAYMENT',_ascii'REFUND',_ascii'WEBHOOK_RECEIPT'))),
  CONSTRAINT `ck_slot_required` CHECK (((`record_type` <> _ascii'VIEWING_SLOT') or ((`slot_room_id` is not null) and (`slot_host_id` is not null) and (`slot_start_at` is not null) and (`slot_end_at` is not null) and (`slot_status` is not null)))),
  CONSTRAINT `ck_viewing_required` CHECK (((`record_type` <> _ascii'VIEWING') or ((`viewing_viewing_slot_id` is not null) and (`viewing_tenant_id` is not null) and (`viewing_room_id` is not null) and (`viewing_host_id` is not null) and (`viewing_status` is not null)))),
  CONSTRAINT `ck_viewing_viewing_slot_id_kind` CHECK ((`viewing_viewing_slot_id_kind` = _ascii'VIEWING_SLOT')),
  CONSTRAINT `ck_webhook_payment_id_kind` CHECK ((`webhook_payment_id_kind` = _ascii'PAYMENT')),
  CONSTRAINT `ck_webhook_required` CHECK (((`record_type` <> _ascii'WEBHOOK_RECEIPT') or ((`webhook_provider` is not null) and (`webhook_event_id` is not null) and (`webhook_payload_hash` is not null) and (`webhook_processed_at` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_records`
--

LOCK TABLES `rental_records` WRITE;
/*!40000 ALTER TABLE `rental_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `rental_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_amenities`
--

DROP TABLE IF EXISTS `room_amenities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_amenities` (
  `room_id` bigint NOT NULL,
  `amenity_id` bigint NOT NULL,
  `amenity_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'AMENITY',
  PRIMARY KEY (`room_id`,`amenity_id`),
  KEY `fk_room_amenities_typed` (`amenity_id`,`amenity_id_kind`),
  CONSTRAINT `fk_room_amenities_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_amenities_typed` FOREIGN KEY (`amenity_id`, `amenity_id_kind`) REFERENCES `catalog_items` (`id`, `record_type`),
  CONSTRAINT `ck_room_amenities_kind` CHECK ((`amenity_id_kind` = _utf8mb4'AMENITY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_amenities`
--

LOCK TABLES `room_amenities` WRITE;
/*!40000 ALTER TABLE `room_amenities` DISABLE KEYS */;
/*!40000 ALTER TABLE `room_amenities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rooms`
--

DROP TABLE IF EXISTS `rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `host_id` bigint NOT NULL,
  `unit_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `room_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `area_m2` decimal(6,1) NOT NULL,
  `max_occupants` int NOT NULL DEFAULT '1',
  `address_line` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `province_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `province_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ward_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ward_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `availability` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE',
  `terms_version` int NOT NULL DEFAULT '1',
  `version` int NOT NULL DEFAULT '0',
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rooms_host_unit` (`host_id`,`unit_code`),
  KEY `idx_rooms_host` (`host_id`),
  KEY `idx_rooms_availability` (`availability`),
  KEY `idx_rooms_location` (`latitude`,`longitude`),
  CONSTRAINT `fk_rooms_host` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rooms`
--

LOCK TABLES `rooms` WRITE;
/*!40000 ALTER TABLE `rooms` DISABLE KEYS */;
/*!40000 ALTER TABLE `rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `role_id_kind` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ROLE',
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `fk_user_roles_typed` (`role_id`,`role_id_kind`),
  CONSTRAINT `fk_user_roles_typed` FOREIGN KEY (`role_id`, `role_id_kind`) REFERENCES `account_records` (`id`, `record_type`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_user_roles_kind` CHECK ((`role_id_kind` = _utf8mb4'ROLE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email_verified` tinyint(1) NOT NULL DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `suspended` tinyint(1) NOT NULL DEFAULT '0',
  `suspend_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_token` text COLLATE utf8mb4_unicode_ci,
  `version` int NOT NULL DEFAULT '0',
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_email` (`email`),
  KEY `idx_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-11 12:33:23
