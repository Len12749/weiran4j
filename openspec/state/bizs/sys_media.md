# sys_media 图片资产

> 当前实现事实，以代码与 system Flyway 迁移为准。

## 0. 概要

| 项 | 值 |
|---|---|
| 表 | `sys_media` |
| 模块 | `weiran-system` |
| 管理页面 | `/system/media-library` |
| 管理接口 | `/api/system/media/images`、`/api/system/media-library` |
| 权限 | `system:media-library:list/upload/delete` |

## 1. 列表

按上传时间倒序分页，可按原始文件名搜索。

## 2. 字段与表单

`id`、`original_name`、`url`、`object_name`、`size`、`uploaded_by`、`created_at`。只收录经本系统上传并成功登记的图片。

## 3. 动作

支持多文件选择、逐文件上传、预览、复制 URL、删除。对象删除在数据库提交后执行，失败可重试。

## 4. 用到的公共组件

Semi UI 图片预览、分页与搜索工具栏、system 文件存储端口。

## 5. 说明与建议

只允许 jpg/jpeg/png/gif，单文件最大 10 MB。本地和 OSS 存储使用同一对象名约束。

## 6. 已知问题汇总

无。

## 7. changelog

**2026-10-17**
- **#01 ✅ P? change `site-media-management` 新增系统图片资产管理**
  图片资产由图片库独立维护，不再与站点内容配置耦合。
