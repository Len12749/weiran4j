INSERT INTO sys_menu (id, parent_id, title, type, path, component, icon, permission, sort, visible, keep_alive)
SELECT 1016, 0, '编辑大赛动态', 'menu', '/cqt/news/edit/:id', 'cqt/NewsEditPage', 'Newspaper', 'cqt:news:update', 23, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1016);

UPDATE sys_menu SET parent_id = 1016 WHERE id = 1013;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 1016 FROM sys_role_menu WHERE menu_id = 1013;
