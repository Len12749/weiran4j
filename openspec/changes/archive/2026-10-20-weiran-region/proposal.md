---
title: "新增中国行政区划模块"
owner: "weiran4j"
status: "draft"
created_at: "2026-10-19"
updated_at: "2026-10-19"
---

# Proposal

## Why

- 背景：mono4ts 已提供中国省、市、县行政区划管理和选择能力；weiran4j 目前没有可供业务表单复用的行政区划服务。
- 业务目标：提供独立、可复用的地区业务模块，让管理员维护标准行政区划，其他登录态页面能够选择启用地区。
- 当前问题：各业务若自行维护省市区数据，会出现重复数据、代码口径不一致和级联逻辑分散；没有统一的地区树、查询与导出接口。
- 需求来源：见 `interview.md`；功能边界对照 mono4ts 的 `regions` 实现。

## What Changes

- 新增：`weiran-region` 五层模块，独立管理省、市、县三级行政区划和其权限、数据迁移。
- 新增：启用地区树选择器、地区管理页面、树/平铺/详情/增改删 API、筛选和 CSV 导出。
- 新增：按 `china-division@2.7.0` 固化的 3351 条初始数据；不增加 Java/Node 运行时依赖。
- 复用：Spring 自动配置、统一错误/响应/权限/审计机制，以及现有 Semi UI 列表、树展开、表单和查询缓存组件。
- 下线/不做：不增加地区数据导入、街道/乡镇级别、通用导出中心、XLSX 依赖，也不改用户/部门等业务字段。

## Scope

### In Scope

- 行政区划领域模型、表、父级/层级校验与删除保护。
- 独立业务权限码、菜单/按钮、操作日志和错误码段登记。
- authenticated lookup API、管理接口、级联选择器和管理页面。
- 3351 条固定数据的追加迁移、地区字段 CSV 导出、契约和状态文档。

### Out of Scope

- 不提供行政区划文件上传或批量导入页面；源模块没有这项管理能力。
- 不扩展街道、乡镇、村级数据；初始化范围与源模块一致，为省、市、县三级。
- 不在本 change 将区划字段接入用户、部门或其他业务表；级联选择器作为独立可复用组件交付。
- 不引入通用导出中心或 XLSX 依赖；导出仅实现地区筛选结果的 UTF-8 CSV 下载。

## Capabilities

| 能力 | delta 操作 | 说明 |
|---|---|---|
| `administrative-regions` | ADDED | 管理并查询省、市、县三级区划，提供启用地区级联选择、维护审计与筛选结果导出。 |

## 影响的包

| ID | 包 | 动作 | 影响说明 | Owner |
|---|---|---|---|---|
| PK-1 | `weiran-framework` | 不修改 | 复用既有纯 Java 错误码/分页响应和 Spring 权限、审计能力；地区专属错误码放在自身 API 模块。 | — |
| PK-2 | `weiran-system-*` | 不修改 | 基座保持独立；只通过现有统一菜单/权限机制接入地区菜单，不依赖 system 内部模块。 | — |
| PK-3 | `weiran-app` | 不修改脚本 | settings 自动发现业务模块，BOM 和应用聚合随发现清单自动生成。 | — |
| PK-4 | `web` | 新增页面、查询 hooks、类型与组件 | 管理页面、`RegionSelect`、地区 API hooks、菜单 component 页面目标。 | — |

新增服务端业务包：`weiran4j/weiran-region/weiran-region-{api,domain,application,infrastructure,adapter}`。这些目录齐全后由 Gradle 自动发现；不修改 `settings.gradle.kts`、BOM 与 `weiran-app/build.gradle.kts`。

## 共享层影响(决定能否并行)

| 类别 | 是否命中 | 说明 |
|---|---|---|
| `settings.gradle.kts` / `weiran-dependencies` BOM | ☐ | 模块坐标自动发现，本次不改构建注册脚本。 |
| `weiran-framework` 的错误码/分页契约 | ☐ | 复用现有 CommonErrors 与分页响应；地区业务错误留在自身 api。 |
| `weiran-app/build.gradle.kts` 依赖聚合 | ☐ | app 依赖与覆盖率模块清单自动生成。 |
| `web/src/App.tsx` / 路由入口 | ☐ | 菜单驱动动态路由，不改 App.tsx。 |
| Flyway 版本与 `sys_menu` 菜单 ID | ☑ | Layer 0 串行确定 `V202610190001`、业务菜单 ID `1000` 段并用模块迁移追加。 |
| `web/src/pages/**` 与菜单 component | ☑ | 新增 `system/regions/RegionsPage.tsx`，菜单 component 必须指向同一路径。 |
| API 契约、TS 类型与契约文档 | ☑ | 新接口在契约登记，前端 `web/src/types/api.ts` 手动同步。 |

## 横切关注点

| 项 | 是否涉及 | 说明 |
|---|---|---|
| CC-1 权限点 (`sys_menu.permission`) | ☑ | 新业务权限使用 `region:list/create/update/delete/export`，不占用 `system:`。 |
| CC-2 菜单（后端 `sys_menu` 驱动） | ☑ | 新地区管理菜单与按钮权限由追加 Flyway 种子创建。 |
| CC-3 数据范围 (`data-scope`) | ☐ | 现有项目尚未引入数据范围；行政区划是共享参考数据，不按部门过滤。 |
| CC-4 多租户隔离 (`tenant`) | ☐ | 项目不是多租户系统。 |
| CC-5 审计日志 (`@OperationLog`) | ☑ | 创建、更新、删除地区写操作记录操作日志。 |
| CC-6 字段脱敏 (`masking`) | ☐ | 地区记录不包含密码、令牌或个人敏感信息。 |
| CC-7 幂等 (`idempotency`) | ☐ | 常规管理 CRUD 不要求跨请求幂等；初始数据由 Flyway 一次性迁移。 |
| CC-8 导出 / 任务中心 | ☑ | `region:export` 权限保护同步 CSV 下载，不建立通用任务中心。 |
| CC-9 工作流绑定 (`workflow-biz-binding`) | ☐ | 项目内暂无工作流审批引擎；地区选择器提供通用表单输入。 |

## Dependencies

- 产品/设计：以 mono4ts 行政区划能力和 weiran4j 的 `list-view.md` 为行为参考。
- 后端：Spring Boot、MyBatis-Plus、Flyway、JDK 21；不新增第三方依赖。
- 前端：React 19、Semi UI、TanStack Query 与现有组件。
- 数据库变更：追加 `weiran-region-infrastructure/.../db/migration/region/V202610190001__region_initial_schema_and_seed.sql`；不修改 system 表结构，菜单只写入既有菜单种子表。
- 运维/配置：无新增环境配置。
- 测试：领域规则单测、基础设施与 Web 集成测试、前端页面/选择器测试及项目 L7 build/test/lint。

## Risks

| 风险 | 触发场景 | 应对方案 |
|---|---|---|
| 行政区划快照有来源与时效边界 | 下次行政区划调整或数据包版本更新 | migration 与数据源固定版本并写入来源、许可证说明；后续通过新迁移修正，不改已发布脚本。 |
| 全量树数据较大 | 管理页面或表单一次加载约 3351 条记录 | 后端限制 lookup 只返回启用树；管理页树默认折叠、按关键词保留祖先，并对真实浏览器首屏和展开交互验证。 |
| 层级调整造成父子关系错误 | 更新 code、parentCode 或 level | 领域服务校验父级存在且为直接上一级，拒绝 code 变更、环引用和有子级删除。 |

## Review Checklist

- [x] 范围和非目标已确认，且与 `interview.md` 一致
- [x] 涉及的包和 Owner 已确认
- [x] 共享层影响已勾选并逐项说明
- [x] 横切关注点已逐项过一遍
- [x] 数据库、接口、菜单、权限、运维配置影响已列出
- [x] 测试与验收策略已确认
