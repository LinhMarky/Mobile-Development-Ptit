
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
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_deletion_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `reason` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `failure_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `completed_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_deletion_requests_1` (`user_id`),
  KEY `ix_account_deletion_requests_1` (`status`,`created_at`),
  CONSTRAINT `fk_account_deletion_requests_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AccountDeletionRequest - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_lifecycle_locks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_lifecycle_locks_1` (`user_id`),
  CONSTRAINT `fk_account_lifecycle_locks_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AccountLifecycleLock - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `amenities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_amenities_1` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amenity - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `actor_id` bigint DEFAULT NULL,
  `actor_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `detail` json DEFAULT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_audit_logs_1` (`target_type`,`target_id`,`created_at`),
  KEY `fk_audit_logs_actor_id` (`actor_id`),
  CONSTRAINT `fk_audit_logs_actor_id` FOREIGN KEY (`actor_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AuditLog - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_cases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `booking_id` bigint NOT NULL,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `opened_by` bigint DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN',
  `decision` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_refund_vnd` decimal(18,0) DEFAULT NULL,
  `host_retained_vnd` decimal(18,0) DEFAULT NULL,
  `resolved_by` bigint DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `resolution_note` text COLLATE utf8mb4_unicode_ci,
  `resolved_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `open_booking_id` bigint GENERATED ALWAYS AS (if((`status` in (_utf8mb4'OPEN',_utf8mb4'IN_REVIEW')),`booking_id`,NULL)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_cases_1` (`open_booking_id`),
  KEY `ix_booking_cases_1` (`booking_id`,`status`),
  KEY `fk_booking_cases_opened_by` (`opened_by`),
  KEY `fk_booking_cases_resolved_by` (`resolved_by`),
  CONSTRAINT `fk_booking_cases_booking_id` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_booking_cases_opened_by` FOREIGN KEY (`opened_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_booking_cases_resolved_by` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BookingCase - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_status_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `booking_id` bigint NOT NULL,
  `from_status` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `to_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_id` bigint DEFAULT NULL,
  `actor_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_booking_status_history_1` (`booking_id`,`created_at`),
  KEY `fk_booking_status_history_actor_id` (`actor_id`),
  CONSTRAINT `fk_booking_status_history_actor_id` FOREIGN KEY (`actor_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_booking_status_history_booking_id` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BookingStatusHistory - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
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
  `terms_snapshot` json NOT NULL,
  `snapshot_schema_version` int NOT NULL DEFAULT '1',
  `request_expires_at` datetime(3) NOT NULL,
  `hold_expires_at` datetime(3) DEFAULT NULL,
  `handover_due_at` datetime(3) DEFAULT NULL,
  `allocated_payment_id` bigint DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `desired_move_in` date DEFAULT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_terms_accepted_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_handover_confirmed_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `host_handover_confirmed_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `completed_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `open_room_id` bigint GENERATED ALWAYS AS (if((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'CONFIRMED')),`room_id`,NULL)) STORED,
  `active_room_id` bigint GENERATED ALWAYS AS (if((`status` in (_utf8mb4'APPROVED',_utf8mb4'CONFIRMED')),`room_id`,NULL)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bookings_1` (`tenant_id`,`open_room_id`),
  UNIQUE KEY `uk_bookings_2` (`active_room_id`),
  UNIQUE KEY `uk_bookings_3` (`allocated_payment_id`),
  KEY `ix_bookings_1` (`tenant_id`,`created_at`),
  KEY `ix_bookings_2` (`host_id`,`created_at`),
  KEY `ix_bookings_3` (`status`,`request_expires_at`),
  KEY `ix_bookings_4` (`status`,`hold_expires_at`),
  KEY `ix_bookings_5` (`status`,`handover_due_at`),
  KEY `fk_bookings_room_id` (`room_id`),
  KEY `fk_bookings_listing_id` (`listing_id`),
  CONSTRAINT `fk_bookings_allocated_payment_id` FOREIGN KEY (`allocated_payment_id`) REFERENCES `payments` (`id`),
  CONSTRAINT `fk_bookings_host_id` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_bookings_listing_id` FOREIGN KEY (`listing_id`) REFERENCES `listings` (`id`),
  CONSTRAINT `fk_bookings_room_id` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_bookings_tenant_id` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Booking - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
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
  `tenant_read_sequence` bigint NOT NULL DEFAULT '0',
  `tenant_read_at` datetime(3) DEFAULT NULL,
  `host_read_sequence` bigint NOT NULL DEFAULT '0',
  `host_read_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_room_tenant` (`room_id`,`tenant_id`),
  KEY `idx_conversation_tenant` (`tenant_id`,`last_message_at`,`id`),
  KEY `idx_conversation_host` (`host_id`,`last_message_at`,`id`),
  CONSTRAINT `fk_conversation_host` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_conversation_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_conversation_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_conversation_read_bounds` CHECK (((`tenant_read_sequence` between 0 and `sequence`) and (`host_read_sequence` between 0 and `sequence`))),
  CONSTRAINT `ck_conversation_members` CHECK ((`tenant_id` <> `host_id`)),
  CONSTRAINT `ck_conversation_sequence` CHECK ((`sequence` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Two-party room conversations, sequence and per-member read positions';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  `token` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `platform` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ANDROID',
  `installation_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `device_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active_installation_id` varchar(36) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (if(`active`,`installation_id`,NULL)) STORED,
  `active_token_hash` varchar(64) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (if(`active`,sha2(`token`,256),NULL)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_tokens_1` (`user_id`,`installation_id`),
  UNIQUE KEY `uk_device_tokens_2` (`active_installation_id`),
  UNIQUE KEY `uk_device_tokens_3` (`active_token_hash`),
  KEY `ix_device_tokens_1` (`user_id`,`active`),
  CONSTRAINT `fk_device_tokens_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DeviceToken - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Flyway migration history and checksums. Managed by Flyway only.';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `idempotency_keys` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `idempotency_key` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PROCESSING',
  `expires_at` datetime(3) NOT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `response_content_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `response_location` text COLLATE utf8mb4_unicode_ci,
  `response_status_code` int DEFAULT NULL,
  `response_body` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotency_keys_1` (`idempotency_key`),
  KEY `ix_idempotency_keys_1` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IdempotencyKeyEntity - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `listing_fees` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `listing_id` bigint NOT NULL,
  `fee_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fee_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FIXED',
  `amount_vnd` decimal(18,0) NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `unit_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_listing_fees_1` (`listing_id`,`sort_order`),
  CONSTRAINT `fk_listing_fees_listing_id` FOREIGN KEY (`listing_id`) REFERENCES `listings` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ListingFee - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Public listings, moderation, publication deadlines and terms version.';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File metadata and object-storage references; file bytes are outside MySQL.';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Durable chat messages; client UUID provides retry deduplication.';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `transaction_push` tinyint(1) NOT NULL DEFAULT '1',
  `transaction_email` tinyint(1) NOT NULL DEFAULT '1',
  `chat_push` tinyint(1) NOT NULL DEFAULT '1',
  `recommendation_push` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_preferences_1` (`user_id`),
  CONSTRAINT `fk_notification_preferences_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_preferences_booleans` CHECK (((`transaction_push` in (0,1)) and (`transaction_email` in (0,1)) and (`chat_push` in (0,1)) and (`recommendation_push` in (0,1))))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NotificationPreference - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  `notification_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ref_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ref_id` bigint DEFAULT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `body` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `ix_notifications_1` (`user_id`,`created_at`,`id`),
  KEY `ix_notifications_2` (`user_id`,`is_read`,`created_at`,`id`),
  CONSTRAINT `fk_notifications_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Notification - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `one_time_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `token_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `purpose` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `used` tinyint(1) NOT NULL DEFAULT '0',
  `expires_at` datetime(3) NOT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_one_time_tokens_1` (`token_hash`),
  KEY `ix_one_time_tokens_1` (`expires_at`),
  KEY `ix_one_time_tokens_2` (`user_id`),
  CONSTRAINT `fk_one_time_tokens_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OneTimeToken - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_webhook_receipts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `provider` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_id` bigint DEFAULT NULL,
  `payload_hash` binary(32) NOT NULL,
  `outcome` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processed_at` datetime(3) NOT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_webhook_receipts_1` (`provider`,`event_id`),
  KEY `fk_payment_webhook_receipts_payment_id` (`payment_id`),
  CONSTRAINT `fk_payment_webhook_receipts_payment_id` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='WebhookReceipt - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `booking_id` bigint NOT NULL,
  `payer_id` bigint NOT NULL,
  `provider` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MOCK_SANDBOX',
  `provider_payment_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount_vnd` decimal(18,0) NOT NULL,
  `currency` char(3) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VND',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `expires_at` datetime(3) NOT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `succeeded_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `failure_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_test` tinyint(1) NOT NULL DEFAULT '1',
  `pending_booking_id` bigint GENERATED ALWAYS AS (if((`status` = _utf8mb4'PENDING'),`booking_id`,NULL)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payments_1` (`provider_payment_id`),
  UNIQUE KEY `uk_payments_2` (`pending_booking_id`),
  KEY `ix_payments_1` (`booking_id`,`created_at`),
  KEY `ix_payments_2` (`status`,`expires_at`),
  KEY `fk_payments_payer_id` (`payer_id`),
  CONSTRAINT `fk_payments_booking_id` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_payments_payer_id` FOREIGN KEY (`payer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_payment_is_test` CHECK ((`is_test` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Payment - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `push_deliveries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `notification_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `attempts` int NOT NULL DEFAULT '0',
  `next_attempt_at` datetime(3) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `last_error` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_push_deliveries_1` (`notification_id`,`device_id`),
  KEY `ix_push_deliveries_1` (`status`,`next_attempt_at`,`id`),
  KEY `ix_push_deliveries_2` (`created_at`),
  KEY `fk_push_deliveries_device_id` (`device_id`),
  CONSTRAINT `fk_push_deliveries_device_id` FOREIGN KEY (`device_id`) REFERENCES `device_tokens` (`id`),
  CONSTRAINT `fk_push_deliveries_notification_id` FOREIGN KEY (`notification_id`) REFERENCES `notifications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PushDelivery - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `token_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `installation_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime(3) NOT NULL,
  `revoked` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `device_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_tokens_1` (`token_hash`),
  KEY `ix_refresh_tokens_1` (`expires_at`),
  KEY `ix_refresh_tokens_2` (`user_id`),
  CONSTRAINT `fk_refresh_tokens_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RefreshToken - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refunds` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `payment_id` bigint NOT NULL,
  `provider_refund_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount_vnd` decimal(18,0) NOT NULL,
  `refund_reason` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `attempt_count` int NOT NULL DEFAULT '0',
  `next_attempt_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `last_error_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `completed_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refunds_1` (`payment_id`),
  UNIQUE KEY `uk_refunds_2` (`provider_refund_id`),
  KEY `ix_refunds_1` (`status`,`next_attempt_at`),
  CONSTRAINT `fk_refunds_payment_id` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Refund - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `reporter_id` bigint NOT NULL,
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `reason_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `resolved_by` bigint DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `description` text COLLATE utf8mb4_unicode_ci,
  `resolution_note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resolved_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_reports_1` (`status`,`created_at`),
  KEY `ix_reports_2` (`target_type`,`target_id`),
  KEY `fk_reports_reporter_id` (`reporter_id`),
  KEY `fk_reports_resolved_by` (`resolved_by`),
  CONSTRAINT `fk_reports_reporter_id` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_reports_resolved_by` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `booking_id` bigint NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `visible` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `comment` text COLLATE utf8mb4_unicode_ci,
  `hide_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reviews_1` (`booking_id`),
  KEY `ix_reviews_1` (`room_id`,`visible`,`created_at`),
  KEY `fk_reviews_reviewer_id` (`reviewer_id`),
  CONSTRAINT `fk_reviews_booking_id` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_reviews_reviewer_id` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_reviews_room_id` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `ck_reviews_1` CHECK ((`rating` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Review - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_amenities` (
  `room_id` bigint NOT NULL,
  `amenity_id` bigint NOT NULL,
  PRIMARY KEY (`room_id`,`amenity_id`),
  KEY `fk_room_amenities_amenity` (`amenity_id`),
  CONSTRAINT `fk_room_amenities_amenity` FOREIGN KEY (`amenity_id`) REFERENCES `amenities` (`id`),
  CONSTRAINT `fk_room_amenities_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Amenities assigned to rooms. amenity_id references an AMENITY catalog item.';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rooms, owner, location, rental terms and availability.';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_name` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (`user_id`,`role_name`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_user_roles_name` CHECK ((`role_name` in (_utf8mb4'ROLE_TENANT',_utf8mb4'ROLE_HOST',_utf8mb4'ROLE_ADMIN')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Roles granted to users. role_id references a ROLE account record.';
/*!40101 SET character_set_client = @saved_cs_client */;
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
  `suspend_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_email` (`email`),
  KEY `idx_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User accounts and profiles. Sessions and devices are in account_records.';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `viewing_slots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `room_id` bigint NOT NULL,
  `host_id` bigint NOT NULL,
  `start_at` datetime(3) NOT NULL,
  `end_at` datetime(3) NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `ix_viewing_slots_1` (`room_id`,`status`,`start_at`),
  KEY `ix_viewing_slots_2` (`host_id`,`start_at`),
  CONSTRAINT `fk_viewing_slots_host_id` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_viewing_slots_room_id` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `ck_viewing_slots_1` CHECK ((`end_at` > `start_at`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ViewingSlot - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `viewings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT '0',
  `viewing_slot_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `host_id` bigint NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REQUESTED',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `completed_at` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_viewings_1` (`tenant_id`,`created_at`),
  KEY `ix_viewings_2` (`host_id`,`created_at`),
  KEY `ix_viewings_3` (`viewing_slot_id`,`status`),
  KEY `fk_viewings_room_id` (`room_id`),
  CONSTRAINT `fk_viewings_host_id` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_viewings_room_id` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_viewings_tenant_id` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_viewings_viewing_slot_id` FOREIGN KEY (`viewing_slot_id`) REFERENCES `viewing_slots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Viewing - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wishlist_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wishlist_items_1` (`user_id`,`room_id`),
  KEY `ix_wishlist_items_1` (`user_id`,`created_at`),
  KEY `fk_wishlist_items_room_id` (`room_id`),
  CONSTRAINT `fk_wishlist_items_room_id` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_wishlist_items_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='WishlistItem - direct business storage';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

