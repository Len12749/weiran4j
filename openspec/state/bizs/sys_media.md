# sys_media 文件资源

> 当前实现事实，以代码与 system Flyway 迁移为准。

## 0. 概要

| 项 | 值 |
|---|---|
| 表 | `sys_media` |
| 模块 | `weiran-system` |
| 管理页面 | `/system/media-library` |
| 管理接口 | `/api-backend/system/media/uploadFile`、`/api-backend/system/mediaLibrary/list`、`/api-backend/system/mediaLibrary/delete/{id}` |
| 权限 | `system:media-library:list/upload/delete` |

## 1. 列表

按上传时间与 ID 倒序分页，支持按原始文件名和类型（图片、视频、文档、压缩包）筛选。列表展示 ID、文件、类型、大小、系统配置引用键、上传时间和操作，遵循列表页规范。

## 2. 字段与表单

`id`、`original_name`、`url`、`object_name`、`size`、`uploaded_by`、`created_at`。只收录经本系统上传并成功登记的文件。类型由原始扩展名识别，引用位置从当前 `sys_config.config_value` 中提取文件 URL，并按 `sys_user_backend.avatar` 精确匹配。

## 3. 动作

支持多文件选择、逐文件上传、图片与 PDF 预览、视频页内播放、其他文件下载、复制 URL、删除。系统配置或用户头像仍引用的文件禁止删除；对象删除在数据库提交后执行，失败可重试。

## 4. 用到的公共组件

Semi UI 表格、列设置、操作列与搜索工具栏、system 文件存储端口。

## 5. 说明与建议

允许 jpg/jpeg/png/gif/webp、mp4/webm/mov、pdf/txt/csv、doc/docx、xls/xlsx、ppt/pptx、zip。普通文件最大 10 MB，视频最大 100 MB；上传入口、Spring multipart 与 Nginx 限制均允许该大小。本地和 OSS 存储使用同一对象名约束。浏览器能否解码 MOV 取决于本机支持的编码。

## 6. 已知问题汇总

- **#03 待处理 P2 业务模块引用位置尚未接入资源库**
  业务表保存资源 URL 时，资源库仍可能显示「未发现引用」，管理员可误删业务正在使用的文件。当前从 `sys_config` 与 `sys_user_backend.avatar` 识别引用，后续业务模块需接入资源引用索引或提供引用查询接口。

## 7. changelog

**2026-10-22**
- **#04 ✅ P? 视频预览与文件类型图标**
  资源库和多文件上传支持视频页内播放、PDF 浏览器预览；ZIP、视频、Word、PPT、Excel、PDF 使用对应图标，不可页内预览的文件显示下载操作。多文件拖动时显示插入位置。

- **#02 ✅ P? 文件资源管理**
  图片上传扩为文件上传，图片可预览，其余文件可在浏览器打开或下载；列表显示系统配置键和用户头像引用，并保护引用中的文件不被删除。

**2026-10-17**
- **#01 ✅ P? change `site-media-management` 新增系统图片资产管理**
  图片资产由图片库独立维护，不再与站点内容配置耦合。
