-- 两小时未完成的 Excel/ZIP 上传会话自动失效，避免中断上传永久阻塞学校新任务。
ALTER TABLE `cqt_school_import_tasks`
  ADD COLUMN `upload_expires_at` datetime DEFAULT NULL AFTER `lease_until`;

UPDATE `cqt_school_import_tasks`
SET `upload_expires_at` = DATE_ADD(`updated_at`, INTERVAL 2 HOUR)
WHERE `status` = 'UPLOADING';

CREATE INDEX `idx_school_import_upload_expiry`
  ON `cqt_school_import_tasks` (`status`, `upload_expires_at`);
