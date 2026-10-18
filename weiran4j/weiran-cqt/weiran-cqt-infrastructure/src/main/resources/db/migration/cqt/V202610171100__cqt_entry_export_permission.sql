INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort)
VALUES (1054,1050,'导出作品清单','button',NULL,NULL,'cqt:entry-review:export',NULL,1,1054);
INSERT INTO sys_role_menu(role_id,menu_id)
SELECT id,1054 FROM sys_role WHERE code='super_admin';
