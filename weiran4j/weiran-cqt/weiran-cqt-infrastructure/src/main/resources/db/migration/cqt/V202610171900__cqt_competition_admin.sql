ALTER TABLE cqt_competitions ADD COLUMN version bigint NOT NULL DEFAULT 0;

INSERT INTO sys_menu(id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES
(1130,1055,'赛事管理','menu','/cqt/competitions','cqt/competition/CompetitionsPage','cqt:competition:list','Calendar',1,0),
(1131,1055,'新增赛事','menu','/cqt/competitions/create','cqt/competition/CompetitionCreatePage','cqt:competition:create','Calendar',0,0),
(1132,1055,'编辑赛事','menu','/cqt/competitions/edit/:id','cqt/competition/CompetitionEditPage','cqt:competition:update','Calendar',0,0),
(1133,1130,'查询赛事','button',NULL,NULL,'cqt:competition:list',NULL,1,1),
(1134,1131,'新增赛事','button',NULL,NULL,'cqt:competition:create',NULL,1,2),
(1135,1132,'编辑赛事','button',NULL,NULL,'cqt:competition:update',NULL,1,3),
(1136,1130,'启停赛事','button',NULL,NULL,'cqt:competition:status',NULL,1,4);
INSERT INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.id BETWEEN 1130 AND 1136 WHERE r.code='super_admin';
