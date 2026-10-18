-- 新系统统一使用 weiran-region 的地区主键；旧赛区数据不迁移。
-- 旧账号与作品地区清空，避免旧 legacy_id 偶然对应到新 region.id 后展示错误地区。
ALTER TABLE cqt_portal_accounts
    CHANGE COLUMN city_legacy_id region_id BIGINT NULL COMMENT '地区（region.id）';
UPDATE cqt_portal_accounts SET region_id = NULL;

ALTER TABLE cqt_entries
    CHANGE COLUMN region_legacy_id region_id BIGINT NULL COMMENT '地区（region.id）';
UPDATE cqt_entries SET region_id = NULL;

-- 旧地区范围无法沿用；移除受旧地区限制的评委范围，需在新地区下重新配置。
DELETE FROM cqt_reviewer_scopes WHERE region_id <> 0;

DROP TABLE cqt_regions;
