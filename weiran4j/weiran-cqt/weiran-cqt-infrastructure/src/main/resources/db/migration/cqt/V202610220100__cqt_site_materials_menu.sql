-- 站点资料统一归类；保留页面路径、权限及隐藏新建／编辑路由。
INSERT INTO sys_menu (id, parent_id, title, type, path, component, icon, permission, sort, visible, keep_alive)
VALUES (1140, 0, '站点资料', 'directory', '/cqt/site-materials', NULL, 'BookOpen', NULL, 20, 1, 0);

UPDATE sys_menu
SET parent_id = 1140
WHERE id IN (1000, 1010, 1020, 1030, 1040) AND type = 'menu';

-- 已拥有任一子页面的角色也登记目录，供角色授权页展示完整层级。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT granted.role_id, 1140
FROM (
    SELECT DISTINCT role_id
    FROM sys_role_menu
    WHERE menu_id IN (1000, 1010, 1020, 1030, 1040)
) AS granted;
