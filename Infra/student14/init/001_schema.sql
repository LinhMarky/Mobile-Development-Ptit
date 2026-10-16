-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: homely_student14
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
-- Table structure for table `auth_tokens`
--

DROP TABLE IF EXISTS `auth_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  `purpose` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_hash` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `installation_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expires_at` datetime(3) NOT NULL,
  `consumed_at` datetime(3) DEFAULT NULL COMMENT 'Đã dùng hoặc đã thu hồi; NULL là chưa dùng',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_tokens_hash` (`token_hash`),
  KEY `ix_auth_tokens_user` (`user_id`,`purpose`,`consumed_at`),
  KEY `ix_auth_tokens_expiry` (`expires_at`),
  CONSTRAINT `fk_auth_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_auth_tokens_hash` CHECK ((char_length(`token_hash`) = 64)),
  CONSTRAINT `ck_auth_tokens_purpose` CHECK ((`purpose` in (_utf8mb4'REFRESH',_utf8mb4'EMAIL_VERIFICATION',_utf8mb4'PASSWORD_RESET'))),
  CONSTRAINT `ck_auth_tokens_refresh` CHECK (((`purpose` <> _utf8mb4'REFRESH') or ((`installation_id` is not null) and (`device_name` is not null) and (char_length(trim(`installation_id`)) > 0) and (char_length(trim(`device_name`)) > 0)))),
  CONSTRAINT `ck_auth_tokens_time` CHECK (((`expires_at` > `created_at`) and ((`consumed_at` is null) or (`consumed_at` >= `created_at`))))
) ENGINE=InnoDB AUTO_INCREMENT=186 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token xác thực có mục đích và hạn dùng, chỉ lưu SHA-256';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bookings`
--

DROP TABLE IF EXISTS `bookings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `tenant_id` bigint NOT NULL,
  `host_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `listing_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `occupant_count` int NOT NULL DEFAULT '1',
  `rent_vnd` decimal(18,0) NOT NULL,
  `deposit_vnd` decimal(18,0) NOT NULL DEFAULT '0',
  `terms_snapshot` json NOT NULL COMMENT 'Bản chụp điều khoản tại lúc đặt, không phải kho dữ liệu đa loại',
  `snapshot_schema_version` int NOT NULL DEFAULT '1',
  `request_expires_at` datetime(3) NOT NULL,
  `hold_expires_at` datetime(3) DEFAULT NULL,
  `handover_due_at` datetime(3) DEFAULT NULL,
  `allocated_payment_id` bigint DEFAULT NULL,
  `desired_move_in` date DEFAULT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_terms_accepted_at` datetime(3) DEFAULT NULL,
  `tenant_handover_confirmed_at` datetime(3) DEFAULT NULL,
  `host_handover_confirmed_at` datetime(3) DEFAULT NULL,
  `completed_at` datetime(3) DEFAULT NULL,
  `cancelled_at` datetime(3) DEFAULT NULL,
  `last_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancel_reason_code` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HOST_REJECTED, TENANT_CANCELLED, HOST_CANCELLED hoặc mã hết hạn',
  `open_room_id` bigint GENERATED ALWAYS AS (if((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'CONFIRMED')),`room_id`,NULL)) STORED,
  `active_room_id` bigint GENERATED ALWAYS AS (if((`status` in (_utf8mb4'APPROVED',_utf8mb4'CONFIRMED')),`room_id`,NULL)) STORED,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bookings_tenant_open` (`tenant_id`,`open_room_id`),
  UNIQUE KEY `uk_bookings_active_room` (`active_room_id`),
  UNIQUE KEY `uk_bookings_allocated_payment` (`allocated_payment_id`),
  KEY `ix_bookings_tenant` (`tenant_id`,`created_at`),
  KEY `ix_bookings_host` (`host_id`,`created_at`),
  KEY `ix_bookings_request_expiry` (`status`,`request_expires_at`),
  KEY `ix_bookings_hold_expiry` (`status`,`hold_expires_at`),
  KEY `ix_bookings_handover_due` (`status`,`handover_due_at`),
  KEY `fk_bookings_room` (`room_id`),
  KEY `fk_bookings_listing` (`listing_id`),
  CONSTRAINT `fk_bookings_allocated_payment` FOREIGN KEY (`allocated_payment_id`) REFERENCES `payments` (`id`),
  CONSTRAINT `fk_bookings_host` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_bookings_listing` FOREIGN KEY (`listing_id`) REFERENCES `listings` (`id`),
  CONSTRAINT `fk_bookings_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_bookings_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_bookings_handover` CHECK (((`status` <> _utf8mb4'COMPLETED') or ((`tenant_handover_confirmed_at` is not null) and (`host_handover_confirmed_at` is not null) and (`completed_at` is not null)))),
  CONSTRAINT `ck_bookings_hold` CHECK (((`status` <> _utf8mb4'APPROVED') or (`hold_expires_at` is not null))),
  CONSTRAINT `ck_bookings_money` CHECK (((`rent_vnd` > 0) and (`deposit_vnd` >= 0))),
  CONSTRAINT `ck_bookings_occupants` CHECK ((`occupant_count` between 1 and 10)),
  CONSTRAINT `ck_bookings_participants` CHECK ((`tenant_id` <> `host_id`)),
  CONSTRAINT `ck_bookings_rejection` CHECK (((`cancel_reason_code` is null) or (`cancel_reason_code` <> _utf8mb4'HOST_REJECTED') or ((`status` = _utf8mb4'CANCELLED') and (`last_reason` is not null) and (char_length(trim(`last_reason`)) > 0)))),
  CONSTRAINT `ck_bookings_snapshot` CHECK ((json_type(`terms_snapshot`) = _utf8mb4'OBJECT')),
  CONSTRAINT `ck_bookings_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'CONFIRMED',_utf8mb4'COMPLETED',_utf8mb4'CANCELLED',_utf8mb4'EXPIRED')))
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Đặt thuê, giữ chỗ và bàn giao';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `tenant_read_sequence` bigint NOT NULL DEFAULT '0',
  `tenant_read_at` datetime(3) DEFAULT NULL,
  `host_read_sequence` bigint NOT NULL DEFAULT '0',
  `host_read_at` datetime(3) DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversations_room_tenant` (`room_id`,`tenant_id`),
  KEY `ix_conversations_tenant` (`tenant_id`,`last_message_at`,`id`),
  KEY `ix_conversations_host` (`host_id`,`last_message_at`,`id`),
  CONSTRAINT `fk_conversations_host` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_conversations_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_conversations_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_conversations_members` CHECK ((`tenant_id` <> `host_id`)),
  CONSTRAINT `ck_conversations_read` CHECK (((`tenant_read_sequence` between 0 and `sequence`) and (`host_read_sequence` between 0 and `sequence`))),
  CONSTRAINT `ck_conversations_sequence` CHECK ((`sequence` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hội thoại theo phòng giữa host và tenant';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `fees` json NOT NULL COMMENT 'Mảng phụ phí nhỏ; Service kiểm tra schema, mã, đơn vị và tiền',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `moderation_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Lý do từ chối/đình chỉ hiển thị cho host',
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
  KEY `ix_listings_feed` (`status`,`published_at`,`id`),
  KEY `ix_listings_price` (`status`,`rent_vnd`,`id`),
  KEY `ix_listings_expiry` (`status`,`expires_at`),
  CONSTRAINT `fk_listings_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `ck_listings_fees` CHECK ((json_type(`fees`) = _utf8mb4'ARRAY')),
  CONSTRAINT `ck_listings_money` CHECK ((((`rent_vnd` is null) or (`rent_vnd` > 0)) and ((`deposit_vnd` is null) or (`deposit_vnd` >= 0)))),
  CONSTRAINT `ck_listings_published` CHECK (((`status` <> _utf8mb4'PUBLISHED') or ((`rent_vnd` is not null) and (`deposit_vnd` is not null) and (`published_at` is not null)))),
  CONSTRAINT `ck_listings_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'PENDING_REVIEW',_utf8mb4'REJECTED',_utf8mb4'PUBLISHED',_utf8mb4'HIDDEN',_utf8mb4'SUSPENDED',_utf8mb4'EXPIRED',_utf8mb4'ARCHIVED')))
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tin đăng, giá thuê và duyệt tin';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  UNIQUE KEY `uk_media_storage_key` (`storage_key`),
  KEY `ix_media_resource` (`resource_type`,`resource_id`,`status`,`display_order`),
  KEY `ix_media_uploader` (`uploader_id`),
  CONSTRAINT `fk_media_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_media_size` CHECK (((`file_size_bytes` > 0) and (`display_order` >= 0))),
  CONSTRAINT `ck_media_status` CHECK ((`status` in (_utf8mb4'READY',_utf8mb4'ATTACHED',_utf8mb4'DELETED')))
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Metadata và khóa file trong MinIO, không lưu URL ký tạm thời';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  UNIQUE KEY `uk_messages_client` (`conversation_id`,`client_message_id`),
  UNIQUE KEY `uk_messages_sequence` (`conversation_id`,`sequence`),
  KEY `fk_messages_sender` (`sender_id`),
  CONSTRAINT `fk_messages_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`),
  CONSTRAINT `fk_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_messages_content` CHECK ((char_length(trim(`content`)) > 0)),
  CONSTRAINT `ck_messages_sequence` CHECK ((`sequence` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tin nhắn theo thứ tự trong hội thoại';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  `notification_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `body` text COLLATE utf8mb4_unicode_ci,
  `ref_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ref_id` bigint DEFAULT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `push_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `push_attempts` int NOT NULL DEFAULT '0',
  `push_next_attempt_at` datetime(3) DEFAULT NULL,
  `pushed_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_notifications_user` (`user_id`,`created_at`,`id`),
  KEY `ix_notifications_unread` (`user_id`,`is_read`,`created_at`,`id`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_notifications_read` CHECK ((`is_read` in (0,1))),
  CONSTRAINT `ck_notifications_reference` CHECK ((((`ref_type` is null) and (`ref_id` is null)) or ((`ref_type` is not null) and (`ref_id` is not null) and (`ref_id` > 0))))
) ENGINE=InnoDB AUTO_INCREMENT=287 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Thông báo trong app và đối tượng cần điều hướng';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `booking_id` bigint NOT NULL,
  `payer_id` bigint NOT NULL,
  `provider` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MOCK_SANDBOX',
  `provider_payment_id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `amount_vnd` decimal(18,0) NOT NULL,
  `currency` char(3) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VND',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `expires_at` datetime(3) NOT NULL,
  `succeeded_at` datetime(3) DEFAULT NULL,
  `failure_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_test` tinyint(1) NOT NULL DEFAULT '1',
  `refund_amount_vnd` decimal(18,0) NOT NULL DEFAULT '0',
  `refund_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refund_reason` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refunded_at` datetime(3) DEFAULT NULL,
  `pending_booking_id` bigint GENERATED ALWAYS AS (if((`status` = _utf8mb4'PENDING'),`booking_id`,NULL)) STORED,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payments_provider_id` (`provider_payment_id`),
  UNIQUE KEY `uk_payments_pending_booking` (`pending_booking_id`),
  KEY `ix_payments_booking` (`booking_id`,`created_at`),
  KEY `ix_payments_expiry` (`status`,`expires_at`),
  KEY `fk_payments_payer` (`payer_id`),
  CONSTRAINT `fk_payments_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_payments_payer` FOREIGN KEY (`payer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_payments_money` CHECK (((`amount_vnd` > 0) and (`currency` = _utf8mb4'VND'))),
  CONSTRAINT `ck_payments_provider` CHECK ((`provider` in (_utf8mb4'MOCK_SANDBOX',_utf8mb4'VNPAY',_utf8mb4'MOMO'))),
  CONSTRAINT `ck_payments_refund` CHECK ((((`refund_status` is null) and (`refund_amount_vnd` = 0) and (`refunded_at` is null)) or ((`refund_status` is not null) and (`refund_status` in (_utf8mb4'PENDING',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED')) and (`status` = _utf8mb4'SUCCEEDED') and (`refund_amount_vnd` > 0) and ((`refund_status` <> _utf8mb4'SUCCEEDED') or (`refunded_at` is not null))))),
  CONSTRAINT `ck_payments_refund_amount` CHECK ((`refund_amount_vnd` between 0 and `amount_vnd`)),
  CONSTRAINT `ck_payments_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'EXPIRED'))),
  CONSTRAINT `ck_payments_test` CHECK ((`is_test` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lượt thanh toán cọc và kết quả hoàn tiền sandbox';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reports`
--

DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `reporter_id` bigint DEFAULT NULL COMMENT 'NULL chỉ dành cho case booking do hệ thống mở',
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `booking_id` bigint DEFAULT NULL,
  `reason_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Với booking: chính là CaseType',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN',
  `decision` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_refund_vnd` decimal(18,0) DEFAULT NULL,
  `host_retained_vnd` decimal(18,0) DEFAULT NULL,
  `resolved_by` bigint DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `resolution_note` text COLLATE utf8mb4_unicode_ci,
  `resolved_at` datetime(3) DEFAULT NULL,
  `open_booking_id` bigint GENERATED ALWAYS AS (if(((`target_type` = _utf8mb4'booking') and (`status` in (_utf8mb4'OPEN',_utf8mb4'IN_REVIEW'))),`booking_id`,NULL)) STORED,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reports_open_booking` (`open_booking_id`),
  KEY `ix_reports_admin` (`target_type`,`status`,`created_at`),
  KEY `ix_reports_target` (`target_type`,`target_id`),
  KEY `ix_reports_booking` (`booking_id`,`status`),
  KEY `ix_reports_reporter` (`reporter_id`,`created_at`),
  KEY `fk_reports_resolver` (`resolved_by`),
  CONSTRAINT `fk_reports_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_reports_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_reports_resolver` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_reports_decision` CHECK (((`decision` is null) or (`decision` in (_utf8mb4'REFUND_TENANT',_utf8mb4'FORFEIT_DEPOSIT',_utf8mb4'SPLIT',_utf8mb4'NO_ACTION')))),
  CONSTRAINT `ck_reports_money` CHECK ((((`tenant_refund_vnd` is null) or (`tenant_refund_vnd` >= 0)) and ((`host_retained_vnd` is null) or (`host_retained_vnd` >= 0)))),
  CONSTRAINT `ck_reports_resolution` CHECK (((`status` <> _utf8mb4'RESOLVED') or ((`resolved_by` is not null) and (`resolved_at` is not null) and ((`target_type` <> _utf8mb4'booking') or ((`decision` is not null) and (((`decision` = _utf8mb4'NO_ACTION') and (`tenant_refund_vnd` is null) and (`host_retained_vnd` is null)) or ((`decision` <> _utf8mb4'NO_ACTION') and (`tenant_refund_vnd` is not null) and (`host_retained_vnd` is not null)))))))),
  CONSTRAINT `ck_reports_shape` CHECK ((((`target_type` = _utf8mb4'booking') and (`booking_id` is not null) and (`target_id` = `booking_id`) and (`reason_code` in (_utf8mb4'DEPOSIT_DISPUTE',_utf8mb4'HANDOVER_OVERDUE',_utf8mb4'NO_SHOW',_utf8mb4'OTHER')) and (`description` is not null) and (char_length(trim(`description`)) > 0)) or ((`target_type` <> _utf8mb4'booking') and (`booking_id` is null) and (`reporter_id` is not null) and (`decision` is null) and (`tenant_refund_vnd` is null) and (`host_retained_vnd` is null)))),
  CONSTRAINT `ck_reports_status` CHECK ((`status` in (_utf8mb4'OPEN',_utf8mb4'IN_REVIEW',_utf8mb4'RESOLVED',_utf8mb4'CLOSED'))),
  CONSTRAINT `ck_reports_target` CHECK (((`target_type` in (_utf8mb4'listing',_utf8mb4'review',_utf8mb4'message',_utf8mb4'user',_utf8mb4'booking')) and (`target_id` > 0)))
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Báo cáo nội dung và khiếu nại booking; tách DTO theo mục đích';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `booking_id` bigint NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `comment` text COLLATE utf8mb4_unicode_ci,
  `visible` tinyint(1) NOT NULL DEFAULT '1',
  `hide_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reviews_booking` (`booking_id`),
  KEY `ix_reviews_room` (`room_id`,`visible`,`created_at`),
  KEY `fk_reviews_reviewer` (`reviewer_id`),
  CONSTRAINT `fk_reviews_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_reviews_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_reviews_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `ck_reviews_rating` CHECK ((`rating` between 1 and 5)),
  CONSTRAINT `ck_reviews_visible` CHECK ((`visible` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Đánh giá gắn với booking, có thể ẩn bởi admin';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `description` varchar(5000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
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
  `amenity_ids` json NOT NULL COMMENT 'Mảng ID từ amenities_catalog.json; Service kiểm tra ID và trùng lặp',
  `availability` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE',
  `terms_version` int NOT NULL DEFAULT '1',
  `version` int NOT NULL DEFAULT '0',
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rooms_host_unit` (`host_id`,`unit_code`),
  KEY `ix_rooms_search` (`availability`,`room_type`,`province_code`,`area_m2`),
  CONSTRAINT `fk_rooms_host` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_rooms_amenities` CHECK ((json_type(`amenity_ids`) = _utf8mb4'ARRAY')),
  CONSTRAINT `ck_rooms_area` CHECK ((`area_m2` between 2 and 1000)),
  CONSTRAINT `ck_rooms_availability` CHECK ((`availability` in (_utf8mb4'AVAILABLE',_utf8mb4'HELD',_utf8mb4'RENTED'))),
  CONSTRAINT `ck_rooms_coordinates` CHECK ((((`latitude` is null) and (`longitude` is null)) or ((`latitude` is not null) and (`longitude` is not null) and (`latitude` between -(90) and 90) and (`longitude` between -(180) and 180)))),
  CONSTRAINT `ck_rooms_occupants` CHECK ((`max_occupants` between 1 and 10)),
  CONSTRAINT `ck_rooms_type` CHECK ((`room_type` in (_utf8mb4'SINGLE_ROOM',_utf8mb4'STUDIO',_utf8mb4'MINI_APARTMENT')))
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Phòng vật lý của chủ trọ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt; không lưu mật khẩu gốc',
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email_verified` tinyint(1) NOT NULL DEFAULT '0',
  `is_tenant` tinyint(1) NOT NULL DEFAULT '1',
  `is_host` tinyint(1) NOT NULL DEFAULT '0',
  `is_admin` tinyint(1) NOT NULL DEFAULT '0',
  `fcm_token` text COLLATE utf8mb4_unicode_ci,
  `fcm_installation_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fcm_device_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fcm_token_hash` varchar(64) CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS (sha2(`fcm_token`,256)) STORED,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `suspend_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transaction_push` tinyint(1) NOT NULL DEFAULT '1',
  `transaction_email` tinyint(1) NOT NULL DEFAULT '1',
  `chat_push` tinyint(1) NOT NULL DEFAULT '1',
  `recommendation_push` tinyint(1) NOT NULL DEFAULT '1',
  `version` int NOT NULL DEFAULT '0',
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`),
  UNIQUE KEY `uk_users_fcm_token` (`fcm_token_hash`),
  UNIQUE KEY `uk_users_fcm_installation` (`fcm_installation_id`),
  KEY `ix_users_status` (`status`),
  CONSTRAINT `ck_users_fcm_pair` CHECK ((((`fcm_token` is null) and (`fcm_installation_id` is null)) or ((`fcm_token` is not null) and (`fcm_installation_id` is not null) and (char_length(trim(`fcm_token`)) > 0) and (char_length(trim(`fcm_installation_id`)) > 0)))),
  CONSTRAINT `ck_users_flags` CHECK (((`is_tenant` in (0,1)) and (`is_host` in (0,1)) and (`is_admin` in (0,1)) and (`email_verified` in (0,1)) and (`transaction_push` in (0,1)) and (`transaction_email` in (0,1)) and (`chat_push` in (0,1)) and (`recommendation_push` in (0,1)))),
  CONSTRAINT `ck_users_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'SUSPENDED',_utf8mb4'DELETED')))
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hồ sơ, trạng thái và tùy chọn thông báo';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `viewings`
--

DROP TABLE IF EXISTS `viewings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `viewings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `room_id` bigint NOT NULL,
  `host_id` bigint NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `start_at` datetime(3) NOT NULL,
  `end_at` datetime(3) NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN',
  `requested_at` datetime(3) DEFAULT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancel_reason_code` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `completed_at` datetime(3) DEFAULT NULL,
  `active_start_at` datetime(3) GENERATED ALWAYS AS (if((`status` in (_utf8mb4'OPEN',_utf8mb4'REQUESTED',_utf8mb4'CONFIRMED')),`start_at`,NULL)) STORED,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_viewings_room_active_start` (`room_id`,`active_start_at`),
  KEY `ix_viewings_available` (`room_id`,`status`,`start_at`),
  KEY `ix_viewings_tenant` (`tenant_id`,`requested_at`),
  KEY `ix_viewings_host` (`host_id`,`start_at`),
  CONSTRAINT `fk_viewings_host` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_viewings_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_viewings_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_viewings_assignment` CHECK ((((`status` = _utf8mb4'OPEN') and (`tenant_id` is null) and (`requested_at` is null)) or ((`status` = _utf8mb4'CANCELLED_BY_HOST') and (`tenant_id` is null) and (`requested_at` is null)) or ((`status` <> _utf8mb4'OPEN') and (`tenant_id` is not null) and (`requested_at` is not null)))),
  CONSTRAINT `ck_viewings_completion` CHECK (((`status` <> _utf8mb4'COMPLETED') or (`completed_at` is not null))),
  CONSTRAINT `ck_viewings_participants` CHECK (((`tenant_id` is null) or (`tenant_id` <> `host_id`))),
  CONSTRAINT `ck_viewings_rejection` CHECK (((`cancel_reason_code` is null) or (`cancel_reason_code` <> _utf8mb4'HOST_REJECTED') or ((`status` = _utf8mb4'CANCELLED_BY_HOST') and (`tenant_id` is not null) and (`cancelled_reason` is not null) and (char_length(trim(`cancelled_reason`)) > 0)))),
  CONSTRAINT `ck_viewings_status` CHECK ((`status` in (_utf8mb4'OPEN',_utf8mb4'REQUESTED',_utf8mb4'CONFIRMED',_utf8mb4'COMPLETED',_utf8mb4'CANCELLED_BY_TENANT',_utf8mb4'CANCELLED_BY_HOST',_utf8mb4'NO_SHOW'))),
  CONSTRAINT `ck_viewings_time` CHECK ((`end_at` > `start_at`))
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Khung giờ mở và cuộc hẹn trong cùng một vòng đời';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wishlist_items`
--

DROP TABLE IF EXISTS `wishlist_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wishlist_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wishlist_user_room` (`user_id`,`room_id`),
  KEY `ix_wishlist_user_created` (`user_id`,`created_at`),
  KEY `fk_wishlist_room` (`room_id`),
  CONSTRAINT `fk_wishlist_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wishlist_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Phòng yêu thích của người dùng';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-15 23:58:28
