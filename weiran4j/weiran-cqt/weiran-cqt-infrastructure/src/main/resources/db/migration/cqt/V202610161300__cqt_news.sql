CREATE TABLE `cqt_news` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL COMMENT '动态标题',
  `image` varchar(1024) NOT NULL DEFAULT '' COMMENT '首图地址',
  `show_on_home` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否首页展示',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `content` longtext NOT NULL COMMENT '动态正文 HTML',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cqt_news_home_sort` (`show_on_home`, `sort`, `id`),
  KEY `idx_cqt_news_sort` (`sort`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='大赛动态';

INSERT INTO sys_menu (id, parent_id, title, type, path, component, icon, permission, sort)
SELECT 1010, 0, '大赛动态', 'menu', '/cqt/news', 'cqt/news', 'Newspaper', 'cqt:news:list', 21
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1010);

INSERT INTO sys_menu (id, parent_id, title, type, permission, sort)
SELECT 1011, 1010, '查询动态', 'button', 'cqt:news:list', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1011);

INSERT INTO sys_menu (id, parent_id, title, type, permission, sort)
SELECT 1012, 1010, '新增动态', 'button', 'cqt:news:create', 2
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1012);

INSERT INTO sys_menu (id, parent_id, title, type, permission, sort)
SELECT 1013, 1010, '编辑动态', 'button', 'cqt:news:update', 3
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1013);

INSERT INTO sys_menu (id, parent_id, title, type, permission, sort)
SELECT 1014, 1010, '删除动态', 'button', 'cqt:news:delete', 4
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1014);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id.id
FROM (SELECT 1010 AS id UNION ALL SELECT 1011 UNION ALL SELECT 1012 UNION ALL SELECT 1013 UNION ALL SELECT 1014) AS menu_id;
