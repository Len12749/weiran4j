---
title: "通用站点富文本与图片管理任务"
status: "draft"
updated_at: "2026-10-17"
---

# Tasks

## 0. 准备

- [x] 0.1 核实 system 菜单 id 与 Flyway 版本号，确认无重复（菜单 12、13；按钮 121–125；迁移 V202610170001）

## 1. 数据层 `weiran-system-infrastructure`

- [x] 1.1 新增 system 迁移，创建 `sys_site_config`、`sys_media`，并登记菜单与权限数据（system-site-config/FR-001、system-media-library/FR-001）

## 2. 领域与 API 层 `weiran-system-domain` / `weiran-system-api`

- [x] 2.1 定义媒体资产模型、存储删除端口及站点配置/图片库请求响应类型（system-file-storage/FR-001、system-site-config/FR-001、system-media-library/FR-001）

## 3. 应用与基础设施层 `weiran-system-application` / `weiran-system-infrastructure`

- [x] 3.1 实现 system 文件存储上传、读取和安全删除；实现媒体资产登记、查询、引用检查与删除（system-file-storage/FR-001、system-media-library/FR-001、system-media-library/FR-002、system-media-library/FR-004）
- [x] 3.2 实现站点配置读写、富文本原样保存及配置行锁协调（system-site-config/FR-001、system-media-library/FR-004）

## 4. 适配层 `weiran-system-adapter`

- [x] 4.1 提供 `/api/system/site-config`、`/api/system/media/images` 与 `/api/system/media-library/**` 接口，接入 `system:` 权限及操作日志（system-site-config/FR-001、system-media-library/FR-001、system-media-library/FR-002、system-media-library/FR-003、system-media-library/FR-004）

## 5. 前端 `web`

- [x] 5.1 实现站点配置富文本多图上传插入及 system 图片库的批量上传、查询、预览、复制链接和引用保护删除交互（system-site-config/FR-001、system-media-library/FR-002、system-media-library/FR-003、system-media-library/FR-004）

## 测试覆盖

- 文件存储配置、本地路径越界保护、OSS 上传/删除。
- 图片类型和大小校验、资产持久化、分页搜索、未引用可删、已引用拒删。
- 配置保存与图片删除并发时的配置行锁协调。
- 编辑器多图粘贴与选择、图片库逐项成功/失败及重试。
- 接口权限、操作日志、Flyway 迁移与页面菜单加载。
