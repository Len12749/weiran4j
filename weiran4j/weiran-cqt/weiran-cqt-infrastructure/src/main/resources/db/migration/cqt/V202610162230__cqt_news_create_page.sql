-- Hidden routes are registered by /api/auth/menus and appear only in page tabs.
INSERT INTO sys_menu (id, parent_id, title, type, path, component, icon, permission, sort, visible, keep_alive)
SELECT 1015, 0, '新增大赛动态', 'menu', '/cqt/news/create', 'cqt/NewsCreatePage', 'Newspaper', 'cqt:news:create', 22, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1015);

-- The create button belongs to its hidden page, so future role grants include the route ancestor.
UPDATE sys_menu SET parent_id = 1015 WHERE id = 1012;

-- Preserve the existing create permission assignments.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 1015 FROM sys_role_menu WHERE menu_id = 1012;
