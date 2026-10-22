-- 一次性跨模块数据搬迁：先复制旧表的最新编号值，再移除旧菜单及表。
-- 仅此迁移脚本访问 sys_*；运行时代码通过 weiran-system-api 读取配置。
INSERT INTO sys_group (name, code, description, type, group_key, sort, status, is_builtin)
VALUES ('常青藤站点', 'cqt_site', '前台站点资料与协议', 'config', 'config_system', 10, 'enabled', 1);

CREATE TEMPORARY TABLE cqt_site_config_migration (
    config_name VARCHAR(64) NOT NULL PRIMARY KEY,
    ident INT NOT NULL,
    label VARCHAR(128) NOT NULL,
    field_type VARCHAR(16) NOT NULL
);

INSERT INTO cqt_site_config_migration (config_name, ident, label, field_type) VALUES
    ('guanyuwomen', 2, '关于我们', 'richtext'),
    ('user_agreement', 71, '用户协议', 'richtext'),
    ('yinsixieyi', 3, '隐私协议', 'richtext'),
    ('about_us', 4, '关于我们（英文）', 'richtext'),
    ('dizhi', 7, '公司地址', 'textarea'),
    ('shouji', 8, '联系电话', 'input'),
    ('weixin', 9, '微信号', 'input'),
    ('youxiang', 10, '联系邮箱', 'input'),
    ('gongsijieshao', 15, '公司介绍', 'richtext'),
    ('hezuohuoban', 16, '合作伙伴', 'richtext'),
    ('dasaijieshao', 17, '大赛介绍', 'richtext'),
    ('mianzexieyi', 18, '免责声明', 'richtext'),
    ('shouhoufuwu', 19, '售后服务', 'richtext'),
    ('shangwuhezuo', 20, '商务合作', 'richtext'),
    ('gongzuoshijian', 21, '工作时间', 'textarea'),
    ('dasaizhangcheng', 22, '大赛章程', 'richtext'),
    ('gongzhonghao', 23, '公众号图片', 'image'),
    ('certificate_visibility', 67, '证书显示设置', 'input'),
    ('sheng_certificate_visibility', 68, '省级证书显示设置', 'input'),
    ('guo_certificate_visibility', 70, '国家级证书显示设置', 'input'),
    ('teacher_org_certificate_competition_id', 69, '教师/机构证书赛事编号', 'input'),
    ('shouyeimage', 66, '首页图片', 'image');

INSERT INTO sys_config (config_key, config_value, config_type, group_key, field_type, required, description, is_builtin)
SELECT CONCAT('system.cqt_site.', definition.config_name), COALESCE(source.contents, ''), 'string',
       'cqt_site', definition.field_type, 0, definition.label, 1
FROM cqt_site_config_migration AS definition
LEFT JOIN (
    SELECT legacy.ident, legacy.contents
    FROM cqt_setting AS legacy
    JOIN (SELECT ident, MAX(id) AS latest_id FROM cqt_setting GROUP BY ident) AS latest
      ON latest.ident = legacy.ident AND latest.latest_id = legacy.id
) AS source ON source.ident = definition.ident;

DROP TEMPORARY TABLE cqt_site_config_migration;

-- 原站点配置授权迁到系统配置；原编辑权限同时可上传配置图片。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT source.role_id, target.menu_id
FROM sys_role_menu AS source
JOIN (
    SELECT 1000 AS source_menu_id, 2 AS menu_id UNION ALL
    SELECT 1000, 8 UNION ALL
    SELECT 1001, 2 UNION ALL
    SELECT 1001, 8 UNION ALL
    SELECT 1002, 2 UNION ALL
    SELECT 1002, 8 UNION ALL
    SELECT 1002, 118 UNION ALL
    SELECT 1002, 13 UNION ALL
    SELECT 1002, 124
) AS target ON target.source_menu_id = source.menu_id;

DELETE FROM sys_role_menu WHERE menu_id IN (1000, 1001, 1002);
DELETE FROM sys_menu WHERE id IN (1001, 1002, 1000);
DROP TABLE cqt_setting;
