-- 将 user_agreement 从 ident=2 拆到 ident=71，初始内容沿用当前公开读取值。
-- 如果 ident=2 缺行则不臆造配置；如果 ident=71 已存在则保留现值。
INSERT INTO cqt_setting (`ident`, `name`, `contents`, `created_at`, `updated_at`, `deleted_at`, `key`)
SELECT 71, source_row.`name`, source_row.`contents`, source_row.`created_at`, source_row.`updated_at`,
       source_row.`deleted_at`, 'user_agreement'
FROM (
    SELECT `name`, `contents`, `created_at`, `updated_at`, `deleted_at`
    FROM cqt_setting
    WHERE `ident` = 2
    ORDER BY `id` DESC
    LIMIT 1
) AS source_row
WHERE NOT EXISTS (SELECT 1 FROM cqt_setting WHERE `ident` = 71);

-- 菜单与权限：CQT 菜单 id 号段 1000–1999。
INSERT INTO sys_menu (id, parent_id, title, type, path, component, icon, permission, sort)
SELECT 1000, 0, '站点配置', 'menu', '/cqt/site-config', 'cqt/site-config', 'SlidersHorizontal',
       'cqt:site-config:list', 20
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1000);

INSERT INTO sys_menu (id, parent_id, title, type, permission, sort)
SELECT 1001, 1000, '查看站点配置', 'button', 'cqt:site-config:list', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1001);

INSERT INTO sys_menu (id, parent_id, title, type, permission, sort)
SELECT 1002, 1000, '修改站点配置', 'button', 'cqt:site-config:update', 2
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1002);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id.id
FROM (SELECT 1000 AS id UNION ALL SELECT 1001 UNION ALL SELECT 1002) AS menu_id;
