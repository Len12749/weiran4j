# sys_site_config 系统站点配置

> 当前实现事实，以代码与 system Flyway 迁移为准。

## 0. 概要

| 项 | 值 |
|---|---|
| 表 | `sys_site_config` |
| 模块 | `weiran-system` |
| 管理页面 | `/system/site-config` |
| 管理接口 | `GET/PUT /api/system/site-config` |
| 权限 | `system:site-config:list`、`system:site-config:update` |

## 1. 列表

管理页按固定配置目录分组，不提供分页列表。

## 2. 字段与表单

`id`、`ident`、`config_key`、`contents`、`created_at`、`updated_at`；`contents` 保存原始内容，富文本不清洗。

## 3. 动作

全量保存通用配置目录中的 10 项；每个 ident 更新最大 id 行。管理端读写 `contents` 原始值，不提供公开站点内容读取接口。

## 4. 用到的公共组件

Semi UI Tabs、Tiptap 富文本编辑器、system 图片上传接口。

## 5. 说明与建议

Flyway 首次迁移以通用配置键写入空内容；不从业务表导入数据。配置与图片管理均通过 system 后台接口完成。

## 6. 已知问题汇总

无。

## 7. changelog

**2026-10-17**
- **#01 ✅ P? change `site-media-management` 新增 system 站点配置存储与管理**
  提供通用配置键、原始内容读写和与媒体资产引用检查共用的事务锁。
