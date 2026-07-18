-- DAR In and Out Activity Logs Management System
-- Database schema for MySQL (InfinityFree compatible)
-- Import via phpMyAdmin or CLI: mysql -u USER -p DB_NAME < database.sql

SET NAMES utf8mb4;

-- --------------------------------------------------------
-- Table: users
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('admin','user') NOT NULL DEFAULT 'user',
  `approved` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_activity` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_users_approved` (`approved`),
  KEY `idx_users_last_activity` (`last_activity`),
  KEY `idx_users_approved_active` (`approved`, `last_activity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Default admin (password: password; run scripts/create_admin.php for admin123)
INSERT INTO `users` (`username`, `password`, `role`, `approved`) VALUES
('admin', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin', 1);

-- --------------------------------------------------------
-- Table: activity_logs (main records table)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `activity_logs` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `municipality` varchar(128) DEFAULT NULL,
  `lo_claimant` varchar(255) DEFAULT NULL,
  `title_no` varchar(64) DEFAULT NULL,
  `odts_no` varchar(128) DEFAULT NULL,
  `lot_no` varchar(64) DEFAULT NULL,
  `survey_no` varchar(64) DEFAULT NULL,
  `area_has` decimal(10,2) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `transmitted_documents` varchar(255) DEFAULT NULL,
  `route_to` varchar(255) DEFAULT NULL,
  `route_to_user_id` int(11) unsigned DEFAULT NULL,
  `routed_from_user_id` int(11) unsigned DEFAULT NULL,
  `routed_at` datetime DEFAULT NULL,
  `received_by_control_no` varchar(255) DEFAULT NULL,
  `remarks_action_taken` text DEFAULT NULL,
  `work_status` enum('not_finished','finished') NOT NULL DEFAULT 'not_finished',
  `archived_at` datetime DEFAULT NULL,
  `archived_by` int(11) unsigned DEFAULT NULL,
  `created_by` int(11) unsigned DEFAULT NULL,
  `updated_by` int(11) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `archived_at` (`archived_at`),
  KEY `archived_by` (`archived_by`),
  KEY `created_by` (`created_by`),
  KEY `updated_by` (`updated_by`),
  KEY `route_to_user_id` (`route_to_user_id`),
  KEY `routed_from_user_id` (`routed_from_user_id`),
  KEY `idx_al_work_status` (`work_status`),
  KEY `idx_al_municipality` (`municipality`),
  KEY `idx_al_created_at` (`created_at`),
  KEY `idx_al_updated_at` (`updated_at`),
  KEY `idx_al_not_archived` (`archived_at`, `work_status`),
  KEY `idx_al_not_archived_created` (`archived_at`, `work_status`, `created_by`),
  KEY `idx_al_user_scope` (`archived_at`, `created_by`, `route_to_user_id`),
  KEY `idx_al_municipality_archived` (`municipality`, `archived_at`),
  KEY `idx_al_lo_claimant` (`lo_claimant`(64)),
  KEY `idx_al_search` (`lo_claimant`(64), `title_no`, `lot_no`, `received_by_control_no`(64)),
  FULLTEXT KEY `idx_al_ft_search` (`lo_claimant`, `title_no`, `lot_no`, `received_by_control_no`),
  CONSTRAINT `activity_logs_archived_by` FOREIGN KEY (`archived_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `activity_logs_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `activity_logs_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `activity_logs_route_to_user_id` FOREIGN KEY (`route_to_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `activity_logs_routed_from_user_id` FOREIGN KEY (`routed_from_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: audit_log (who did what and when)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(11) unsigned DEFAULT NULL,
  `username` varchar(64) NOT NULL,
  `action` enum('add','edit','delete','archive','restore','login','logout') NOT NULL,
  `table_name` varchar(64) DEFAULT 'activity_logs',
  `record_id` int(11) unsigned DEFAULT NULL,
  `details` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `action` (`action`),
  KEY `created_at` (`created_at`),
  KEY `idx_audit_recent_actions` (`created_at`, `action`),
  KEY `idx_audit_record_id` (`record_id`),
  CONSTRAINT `audit_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: notifications
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(11) unsigned NOT NULL,
  `type` varchar(32) NOT NULL DEFAULT 'route',
  `record_id` int(11) unsigned DEFAULT NULL,
  `sender_id` int(11) unsigned DEFAULT NULL,
  `message` varchar(255) NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notif_user_id` (`user_id`),
  KEY `idx_notif_is_read` (`is_read`),
  KEY `idx_notif_created_at` (`created_at`),
  KEY `idx_notif_record_id` (`record_id`),
  KEY `idx_notif_unread` (`user_id`, `is_read`),
  KEY `idx_notif_user_latest` (`user_id`, `created_at`),
  KEY `idx_notif_sender_id` (`sender_id`),
  CONSTRAINT `notifications_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `notifications_sender_id` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `notifications_record_id` FOREIGN KEY (`record_id`) REFERENCES `activity_logs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
