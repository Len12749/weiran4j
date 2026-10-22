-- 旧库站点配置首次导入到 sys_config。须先启动应用完成 Flyway 迁移。
-- 已在后台编辑过的非空配置值不会被覆盖；生产库中文编码按原值保留。
-- 执行：mysql -h <host> -u <user> -p <目标库> < scripts/biz/import/cqt_setting.sql
CREATE TEMPORARY TABLE cqt_site_import_map (config_name VARCHAR(64) PRIMARY KEY, ident INT NOT NULL);
INSERT INTO cqt_site_import_map (config_name, ident) VALUES
('guanyuwomen', 2), ('user_agreement', 71), ('yinsixieyi', 3), ('about_us', 4),
('dizhi', 7), ('shouji', 8), ('weixin', 9), ('youxiang', 10),
('gongsijieshao', 15), ('hezuohuoban', 16), ('dasaijieshao', 17),
('mianzexieyi', 18), ('shouhoufuwu', 19), ('shangwuhezuo', 20),
('gongzuoshijian', 21), ('dasaizhangcheng', 22), ('gongzhonghao', 23),
('certificate_visibility', 67), ('sheng_certificate_visibility', 68),
('guo_certificate_visibility', 70), ('teacher_org_certificate_competition_id', 69),
('shouyeimage', 66);

UPDATE sys_config AS target
JOIN cqt_site_import_map AS definition
  ON target.config_key = CONCAT('system.cqt_site.', definition.config_name)
LEFT JOIN (
  SELECT legacy.ident, legacy.contents
  FROM cqtxj2026.sc_setting AS legacy
  JOIN (SELECT ident, MAX(id) AS latest_id FROM cqtxj2026.sc_setting GROUP BY ident) AS latest
    ON latest.ident = legacy.ident AND latest.latest_id = legacy.id
) AS source ON source.ident = definition.ident
LEFT JOIN (
  SELECT legacy.contents
  FROM cqtxj2026.sc_setting AS legacy
  WHERE legacy.ident = 2
  ORDER BY legacy.id DESC LIMIT 1
) AS agreement_fallback ON definition.config_name = 'user_agreement'
SET target.config_value = COALESCE(source.contents,
  IF(definition.config_name = 'user_agreement', agreement_fallback.contents, NULL), '')
WHERE target.config_value = '';

DROP TEMPORARY TABLE cqt_site_import_map;
