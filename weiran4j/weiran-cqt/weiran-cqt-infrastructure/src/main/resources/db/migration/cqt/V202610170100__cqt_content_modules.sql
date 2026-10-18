CREATE TABLE cqt_experts (
id bigint NOT NULL AUTO_INCREMENT,
name varchar(255) NOT NULL DEFAULT '',
image varchar(1024) NOT NULL DEFAULT '',
content longtext NOT NULL,
show_on_home tinyint(1) NOT NULL DEFAULT 1,
enabled tinyint(1) NOT NULL DEFAULT 1,
sort int NOT NULL DEFAULT 0,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
created_by bigint DEFAULT NULL,
updated_by bigint DEFAULT NULL,
PRIMARY KEY(id),
KEY idx_enabled_sort(enabled,sort,id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学术评审团';

INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1020','0','学术评审团','menu','/cqt/experts','cqt/experts','UsersRound','cqt:expert:list','1020','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1021','1020','查询','button',NULL,NULL,NULL,'cqt:expert:list','1021','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1022','1025','新增','button',NULL,NULL,NULL,'cqt:expert:create','1022','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1023','1026','编辑','button',NULL,NULL,NULL,'cqt:expert:update','1023','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1024','1020','删除','button',NULL,NULL,NULL,'cqt:expert:delete','1024','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1025','0','新增学术评审团','menu','/cqt/experts/create','cqt/ExpertCreatePage','UsersRound','cqt:expert:create','1025','0','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1026','0','编辑学术评审团','menu','/cqt/experts/edit/:id','cqt/ExpertEditPage','UsersRound','cqt:expert:update','1026','0','0');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT 1,id FROM sys_menu WHERE id BETWEEN 1020 AND 1026;

CREATE TABLE cqt_showcases (
id bigint NOT NULL AUTO_INCREMENT,
title varchar(255) NOT NULL DEFAULT '',
image varchar(1024) NOT NULL DEFAULT '',
category varchar(255) NOT NULL DEFAULT '',
author varchar(255) NOT NULL DEFAULT '',
teacher varchar(255) NOT NULL DEFAULT '',
school varchar(255) NOT NULL DEFAULT '',
description longtext NOT NULL,
content longtext NOT NULL,
attachment_url varchar(1024) NOT NULL DEFAULT '',
attachment_name varchar(255) NOT NULL DEFAULT '',
video_url varchar(1024) NOT NULL DEFAULT '',
enabled tinyint(1) NOT NULL DEFAULT 1,
sort int NOT NULL DEFAULT 0,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
created_by bigint DEFAULT NULL,
updated_by bigint DEFAULT NULL,
PRIMARY KEY(id),
KEY idx_enabled_sort(enabled,sort,id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佳作欣赏';

INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1030','0','佳作欣赏','menu','/cqt/showcases','cqt/showcases','Image','cqt:showcase:list','1030','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1031','1030','查询','button',NULL,NULL,NULL,'cqt:showcase:list','1031','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1032','1035','新增','button',NULL,NULL,NULL,'cqt:showcase:create','1032','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1033','1036','编辑','button',NULL,NULL,NULL,'cqt:showcase:update','1033','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1034','1030','删除','button',NULL,NULL,NULL,'cqt:showcase:delete','1034','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1035','0','新增佳作欣赏','menu','/cqt/showcases/create','cqt/ShowcaseCreatePage','Image','cqt:showcase:create','1035','0','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1036','0','编辑佳作欣赏','menu','/cqt/showcases/edit/:id','cqt/ShowcaseEditPage','Image','cqt:showcase:update','1036','0','0');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT 1,id FROM sys_menu WHERE id BETWEEN 1030 AND 1036;

CREATE TABLE cqt_resources (
id bigint NOT NULL AUTO_INCREMENT,
title varchar(255) NOT NULL DEFAULT '',
category varchar(255) NOT NULL DEFAULT '',
file_url varchar(1024) NOT NULL DEFAULT '',
file_name varchar(255) NOT NULL DEFAULT '',
content longtext NOT NULL,
enabled tinyint(1) NOT NULL DEFAULT 1,
sort int NOT NULL DEFAULT 0,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
created_by bigint DEFAULT NULL,
updated_by bigint DEFAULT NULL,
PRIMARY KEY(id),
KEY idx_enabled_sort(enabled,sort,id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指导资料';

INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1040','0','指导资料','menu','/cqt/resources','cqt/resources','FileText','cqt:resource:list','1040','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1041','1040','查询','button',NULL,NULL,NULL,'cqt:resource:list','1041','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1042','1045','新增','button',NULL,NULL,NULL,'cqt:resource:create','1042','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1043','1046','编辑','button',NULL,NULL,NULL,'cqt:resource:update','1043','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1044','1040','删除','button',NULL,NULL,NULL,'cqt:resource:delete','1044','1','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1045','0','新增指导资料','menu','/cqt/resources/create','cqt/ResourceCreatePage','FileText','cqt:resource:create','1045','0','0');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,icon,permission,sort,visible,keep_alive) VALUES ('1046','0','编辑指导资料','menu','/cqt/resources/edit/:id','cqt/ResourceEditPage','FileText','cqt:resource:update','1046','0','0');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT 1,id FROM sys_menu WHERE id BETWEEN 1040 AND 1046;
