---
title: "通用站点富文本与图片库调研"
status: "draft"
updated_at: "2026-10-17"
---

# Explore

## 调研范围

| 模块 | 调研内容 | 结论 |
|---|---|---|
| system 后端 | API、domain、application、infrastructure、adapter | 由 system 模块承载站点配置、文件存储与媒体资产能力 |
| 后台 web | 站点配置页、编辑器、页面请求 | 复用现有 Tiptap 编辑交互；新增 system 页面与页面本地请求 |
| 下游参考 `mono4j` | 站点配置富文本与图片上传 | 参考工具栏、HTML 输出、剪贴板多图行为；不迁移新闻管理 |
| OpenSpec | system 站点配置、文件存储、媒体库能力 | 本 change 创建 system 命名的新能力规格 |

## 现状与可复用点

| 能力 | 现状 | 本 change 的处理 |
|---|---|---|
| 富文本编辑 | 站点介绍与协议字段已使用 Tiptap，HTML 原样保存；剪贴板图片可上传，文件选择暂只取第一张 | 继续复用编辑器；文件选择支持多选，图片按选择顺序插入 |
| 图片上传 | 现有上传服务校验图片类型与大小，生成随机对象名并写入本地或 OSS；不保存可搜索的图片元数据 | 在 system 模块提供上传服务，统一登记 `sys_media` 资产 |
| 文件存储 | 现有存储抽象支持上传、URL 归属判断和读取，没有删除接口 | system 文件存储实现本地/OSS 删除，并校验对象归属和本地根路径 |
| 站点配置 | 站点配置需要保存富文本原文并提供管理读写 | 新增 `sys_site_config` 持久化；保存与删除使用同一批配置行锁协调 |
| 新闻 | 参考项目包含新闻管理与读取能力 | 明确不迁入新闻后台或前台新闻接口 |

## 设计约束

| 约束 | 对设计的影响 |
|---|---|
| 本 change 的接口位于 system 命名空间 | API 使用 `/api/system/**`，权限码使用 `system:` 前缀 |
| 媒体资产使用系统表命名 | 图片资产表为 `sys_media`；站点配置表为 `sys_site_config` |
| 持久化类型留在 infrastructure | `*DO`、Mapper 与 SQL 仅在 infrastructure；domain 与 application 不暴露 ORM 类型 |
| 表结构通过新增 Flyway 迁移 | 新增 system 迁移；不修改已合入的迁移脚本 |
| 页面由菜单 component 驱动 | 图片库 component 与 `web/src/pages/system/media-library.tsx` 对应 |
| 图片 URL 是随机对象名；旧对象没有资产记录 | 只管理新上传并登记的图片，不扫描或伪造历史记录 |
| 删除必须保护站点配置引用 | 删除前检查 `sys_site_config.contents`；保存和删除事务以 ident 顺序锁定配置行 |
| 统一分页与错误语义由框架提供 | 复用框架分页类型和通用业务异常，不新增框架契约 |

## 影响面

| 文件/模块 | 预计动作 |
|---|---|
| `weiran-system-api` | 新增媒体库请求、响应与应用服务契约 |
| `weiran-system-domain` | 新增媒体资产模型与存储端口 |
| `weiran-system-application` | 新增配置、上传、查询、删除用例及引用协调 |
| `weiran-system-infrastructure` | 新增实体、Mapper、迁移及本地/OSS 存储实现 |
| `weiran-system-adapter` | 提供 `/api/system/**` 管理接口与权限校验 |
| `web/src/pages/system/**` | 新增图片库页面与请求；站点配置编辑器支持多图上传 |
| `openspec/state/bizs/` | 登记 `sys_media` 表现状 |

## 序号型资源

- 实现前核对 system 模块 Flyway 版本号及 `sys_menu.id`；新迁移使用唯一时间戳，菜单 id 使用 system 菜单号段。
- 图片库菜单 component 必须与页面路径逐字匹配。

## 风险

| 风险 | 建议应对 |
|---|---|
| 上传后未保存到配置的图片仍留在存储中 | 允许在图片库人工删除；不做自动清理 |
| HTML 实体编码或代理改写绕过 URL 引用检查 | 先规范化 URL；无法确认安全时拒绝删除 |
| 引用检查与保存并发 | 两类事务按 ident 升序锁定相同的 `sys_site_config` 行 |

## Gate

- [x] 共享层影响已列出
- [x] 编辑器与文件存储复用边界已确认
- [x] 新闻管理与读取已排除
