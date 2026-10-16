---
title: "CQT 站点配置后台表单"
owner: "weiran-cqt"
status: "draft"
created_at: "2026-10-15"
updated_at: "2026-10-15"
---

# Proposal

## Why

- 背景：`GET /api-web/product/getconfig` 的站点文案目前只在数据库中维护，没有后台专用表单。
- 业务目标：让管理员在后台集中编辑前台站点配置，并通过现有公开接口立即读取保存结果。
- 当前问题：后台系统配置 CRUD 与前台 ident/key 映射、HTML 清洗规则不一致；直接使用会编辑错项或丢失原始 HTML。
- 需求来源：见 `interview.md`。

## What Changes

- 新增：CQT 站点配置后台独立菜单、权限保护的读写 API 和独立表单页面。
- 改造：扩展现有 `cqt-site-config` 能力，将 `user_agreement` 从 ident 2 拆到 ident 71；既有值复制为初始值。
- 复用：使用现有 `cqt_setting` 表、CQT 文件存储、RBAC/操作日志机制、Semi UI Form 与 Tiptap。
- 改造：后台按类别 Tab 展示；富文本字段保留 HTML 给 uniapp 富文本组件渲染，并通过受权限保护的后台上传接口插入图片。
- 下线/不做：不改变前台键集合与调用方式；不迁移到 `sys_config` 或新增配置表。

## Scope

### In Scope

- 规范并实现后台读取、更新站点配置原始值。
- 对 22 个前台键提供专用表单，区分多行原始 HTML 文案与简单文本/开关/图片路径。
- 为管理菜单和读写权限追加 Flyway 种子；将 `user_agreement` 初始内容从原 ident 2 复制到 ident 71。
- 将配置页改为四个分类 Tab；介绍与协议类字段使用富文本编辑器并支持图片上传。
- 富文本编辑器支持粘贴剪贴板图片，上传成功后插入图片 URL；普通文本粘贴沿用默认行为。
- 为后台管理端新增复用 CQT 文件存储的图片上传接口，受 `cqt:site-config:update` 权限保护。
- 同步契约、规格、状态文档、旧库导入脚本和前端类型。

### Out of Scope

- 不修改 uniapp 页面对 `getconfig` 的调用方式。
- 不将原始配置内容迁移到 `sys_config` 或新增一张配置表。
- 不为公众号/首页图片路径字段新增独立上传控件；它们仍可编辑 URL/路径。富文本内图片通过编辑器上传并插入。

## Capabilities

| 能力 | delta 操作 | 说明 |
|---|---|---|
| `cqt-site-config` | MODIFIED | 扩展现有站点配置能力：独立后台编辑，并将 `user_agreement` 独立映射到 ident 71。 |

## 影响的包

| ID | 包 | 动作 | 影响说明 | Owner |
|---|---|---|---|---|
| PK-1 | `weiran-framework` | 不涉及 | 复用既有响应、权限与操作日志契约 | — |
| PK-2 | `weiran-system-*` | 不涉及 | 复用 RBAC，不改基座 | — |
| PK-3 | `weiran-app` | 不涉及 | 不新增模块依赖 | — |
| PK-4 | `web` | 新增 | 独立页面、query/mutation 与 API 类型 | 前端 |
| CQT | `weiran-cqt` | 改造 | 配置管理用例、持久化与受保护接口 | 后端 |
| 契约 | `weiran4j/docs` + `openspec` | 改造 | 前后台接口与能力规格 | 全栈 |

## 共享层影响(决定能否并行)

| 类别 | 是否命中 | 说明 |
|---|---|---|
| `settings.gradle.kts` / `weiran-dependencies` BOM(新增模块) | ☐ | 在已有 CQT 模块扩展，无依赖或模块变化。 |
| `weiran-framework` 的错误码/分页契约 | ☐ | 复用已有契约，不添加跨模块错误码或分页类型。 |
| `weiran-app/build.gradle.kts` 依赖聚合 | ☐ | 无需新增模块依赖。 |
| 页面菜单种子 + `web/src/pages/**`(SL-4/SL-5) | ☑ | 新增 CQT 菜单/权限迁移与对应页面，必须先分配全局唯一菜单 id。 |

## 横切关注点

| 项 | 是否涉及 | 说明 |
|---|---|---|
| CC-1 权限点 | ☑ | 新增 `cqt:site-config:list` 与 `cqt:site-config:update` 并关联页面菜单。 |
| CC-2 菜单 | ☑ | 后端 `sys_menu` 驱动动态路由，菜单由 Flyway 追加。 |
| CC-3 数据范围 | ☐ | 站点配置为全站共享数据，无数据范围模型。 |
| CC-4 多租户隔离 | ☐ | 系统未引入租户机制，CQT 配置全局生效。 |
| CC-5 审计日志 | ☑ | 保存接口记录 `@OperationLog`；读操作不产生写审计。 |
| CC-6 字段脱敏 | ☐ | 配置内容是公开站点文案，无敏感字段。 |
| CC-7 幂等 | ☑ | 按 ident 更新最大 id 记录；初始化 ident 71 与导入脚本可重复执行。 |
| CC-8 导出 / 任务中心 | ☐ | 本次没有导出或异步任务。 |
| CC-9 工作流绑定 | ☐ | 不涉及审批工作流。 |

## Dependencies

- 产品/设计：用户确认 `guanyuwomen` 与 `user_agreement` 拆成独立配置，后者使用 ident 71。
- 后端：现有 CQT 模块读写 `cqt_setting`；菜单 id 与权限按 CQT 号段登记。
- 前端：复用 Semi UI 表单与系统请求层。
- 数据库变更：仅新增 CQT Flyway 迁移；迁移将 ident 2 最大 id 行内容复制成 ident 71，已有迁移不修改。
- 运维/配置：无新增运行配置；旧库导入脚本须生成 ident 71，确保导入后语义一致。
- 测试：覆盖 raw 值管理读写、权限拒绝、拆分映射、富文本公开 HTML、普通文本清洗与数组行为。

## Risks

| 风险 | 触发场景 | 应对方案 |
|---|---|---|
| 生产迁移复制错误 | ident 2 有多行或无记录 | 复制当前公开读取的最大 id 行；无行时不创建默认值，前台保持 null。 |
| 保存改变公开值 | 写入新增重复 ident 行 | 仓储定位最大 id 行并更新；唯一缺行时才插入。 |
| 菜单/权限种子遗漏 | 菜单存在但用户无按钮权限 | 同一迁移包含菜单、按钮权限及角色关联，并验证 `/api/auth/menus` 和 403。 |

## Review Checklist

- [x] 范围和非目标已确认,且与 `interview.md` 一致
- [x] 涉及的包和 Owner 已确认
- [x] 共享层影响已勾选(直接决定 L4 分层)
- [x] 横切关注点已逐项过一遍
- [x] 数据库、接口、定时任务、配置、发布影响已列出
- [x] 测试和灰度策略已确认
