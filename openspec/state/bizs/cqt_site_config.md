# 常青藤站点配置（存于 sys_config）

> 事实源：`SiteConfigApplicationService`、`SiteConfig`、`V202610221804__cqt_migrate_site_config.sql`、`scripts/biz/import/cqt_setting.sql`。

## 0. 概要

| 项 | 值 |
| --- | --- |
| 存储 | `sys_config` 中 `system.cqt_site.*` 的 22 项；`cqt_setting` 已删除 |
| 后台 | 系统管理 › 系统配置 › 常青藤站点 |
| 前台 | `GET /api-web/product/getconfig`，保留旧 22 键响应 |
| 权限 | `system:config:list/update`，图片上传用 `system:media-library:upload` |

## 1. 列表

系统配置页按 `cqt_site` 分组展示，不另设业务页面。

## 2. 字段与表单

11 个站点介绍及协议字段为富文本；`gongzhonghao`、`shouyeimage` 为单图；其他字段为文本。配置值在 `sys_config.config_value TEXT` 中保存，最长由旧表 TEXT 容量决定。

## 3. 动作

前台仍返回固定 22 键；富文本返回 HTML，普通文本去标签和首尾空白，空值为 null，公众号为数组。后台通过系统配置 API 编辑；`weiran-cqt` 只通过 `weiran-system-api` 读取配置。

## 4. 用到的公共组件

`SystemConfigGroupForm`、`ConfigRichTextEditor`、`ConfigImageUpload`。

## 5. 说明与建议

Flyway `V202610221804` 从旧表同一 ident 的最大 id 行复制 22 项后删除表和原菜单。迁移后执行旧库补导脚本时只填写空配置值，不覆盖已有编辑。历史 Flyway 仍保留以通过校验。

## 6. 已知问题汇总

- **#01 ❓ P1 原库导出文件可能存在双重编码乱码**：`cqtxj2026.sql` 里的中文有双重编码现象，生产源库是否同样乱码尚未确认。症状：补导后站点文案乱码；导入脚本保留源值，不擅自转码。

## 7. changelog

**2026-10-22**
- 将站点配置迁入系统配置页面和 `sys_config`，删除旧表、专用页面与管理接口；前台 22 键协议保持。

**2026-10-18**
- 原专用表单支持单图上传与历史回显。

**2026-10-15**
- 原专用表单支持富文本及协议独立编辑。

**2026-10-02**
- 初建旧表与 `getconfig` 接口。
