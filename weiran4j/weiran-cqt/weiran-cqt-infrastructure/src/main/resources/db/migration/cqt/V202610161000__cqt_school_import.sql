-- 学校作品批量导入的上传会话、可恢复任务与作品组逐行结果。
CREATE TABLE `cqt_school_import_tasks` (
  `id` varchar(36) NOT NULL COMMENT '上传会话与导入任务 ID',
  `account_id` bigint unsigned NOT NULL,
  `school_id` varchar(100) DEFAULT NULL,
  `competition_id` bigint unsigned DEFAULT NULL,
  `zip_url` mediumtext,
  `zip_name` varchar(500) DEFAULT NULL,
  `excel_url` mediumtext,
  `excel_name` varchar(500) DEFAULT NULL,
  `import_type` varchar(40) DEFAULT NULL,
  `status` enum('UPLOADING','QUEUED','PROCESSING','COMPLETED','PARTIAL','FAILED') NOT NULL DEFAULT 'UPLOADING',
  `total_rows` int unsigned NOT NULL DEFAULT 0,
  `processed_rows` int unsigned NOT NULL DEFAULT 0,
  `success_count` int unsigned NOT NULL DEFAULT 0,
  `failure_count` int unsigned NOT NULL DEFAULT 0,
  `lease_until` datetime DEFAULT NULL,
  `attempt_count` int unsigned NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_school_import_owner_competition` (`account_id`,`competition_id`,`created_at`),
  KEY `idx_school_import_claim` (`status`,`lease_until`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cqt_school_import_rows` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `task_id` varchar(36) NOT NULL,
  `source_row` int unsigned NOT NULL,
  `last_source_row` int unsigned NOT NULL,
  `entry_fingerprint` char(64) DEFAULT NULL,
  `entry_id` bigint unsigned DEFAULT NULL,
  `status` enum('PENDING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
  `display_json` mediumtext,
  `error_message` varchar(2000) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_import_task_row` (`task_id`,`source_row`),
  KEY `idx_school_import_rows_status` (`task_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
