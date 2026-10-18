---
title: "weiran-region 现状调研与影响面"
status: "draft"
updated_at: "2026-10-19"
---

# Explore

## 调研范围

| 模块 | 目录/文件 | 为什么看它 |
|---|---|---|
| 参考 mono4ts | `packages/server/src/services/platform/regions.service.ts`、`routes/platform/regions.ts`、`packages/web/src/components/RegionSelect.tsx`、`packages/server/src/db/seed.ts`、`packages/server/src/lib/export-center/definitions/regions.ts` | 明确地区模型、查询与维护行为、级联选择、数据基线和导出功能。 |
| weiran4j 构建与基座 | `weiran4j/settings.gradle.kts`、`weiran4j/weiran-dependencies/build.gradle.kts`、`weiran4j/weiran-app/build.gradle.kts`、`weiran4j/weiran-system/.../DepartmentController.java` | 确认业务模块自动发现、基座接口约定与装配边界。 |
| weiran4j 前端与契约 | `web/src/components/ColumnSettings.tsx`、`web/src/hooks/useTableDefaults.ts`、`web/src/utils/page-registry.ts`、`web/src/hooks/queries/departments.ts`、`weiran4j/docs/01-架构与接口契约.md` | 确认表格、菜单路由、查询缓存和唯一接口契约的接入方式。 |

## 现有实现

| 能力 | 位置 | 现状 |
|---|---|---|
| 行政区划数据模型与维护 | mono4ts `packages/server/src/db/schema/system.ts:159`、`packages/server/src/services/platform/regions.service.ts:23` | 省、市、县以字符串区划代码关联父节点，支持树/平铺/详情、筛选、增改删；更新防止自引用和后代环，删除拒绝有子级节点。 |
| 数据初始来源 | mono4ts `packages/server/src/db/seed.ts:20`、`:373` | `china-division@2.7.0` 的省/市/县 JSON 快照，分别 31、342、2978 条；以 code 幂等插入。包许可证为 WTFPL。 |
| API 权限与审计 | mono4ts `packages/server/src/routes/platform/regions.ts:28` | `/api/regions` 的树、平铺、详情、增改删接口；源权限为 `system:region:*`，写操作审计。weiran4j 不能照搬该前缀。 |
| 管理页与地区选择器 | mono4ts `packages/web/src/pages/system/regions/RegionsPage.tsx:48`、`packages/web/src/components/RegionSelect.tsx:12` | 管理树表格、筛选、层级编辑、启停、展开折叠和导出；级联选择器只展示启用节点，选中值为区划代码路径。 |
| 源导出 | mono4ts `packages/server/src/lib/export-center/definitions/regions.ts:10` | XLSX 导出所有地区字段，依赖源项目导出中心；weiran4j 没有可复用的导出中心。 |
| 五层模块发现 | `weiran4j/settings.gradle.kts:31`、`weiran4j/weiran-dependencies/build.gradle.kts:25`、`weiran4j/weiran-app/build.gradle.kts:10` | 根目录五层目录齐全后自动 include；BOM 坐标、应用依赖与覆盖率聚合从同一发现清单动态生成，不改这三份构建脚本。 |
| 菜单驱动前端路由 | `web/src/utils/page-registry.ts:4`、`weiran4j/weiran-system/weiran-system-infrastructure/src/main/resources/db/migration/system/V202609260002__system_seed_data.sql:14` | `sys_menu.component` 解析到 `web/src/pages` 下的页面；新增业务页面通过所属模块迁移写菜单项与按钮权限。 |
| 可复用管理页组件 | `web/src/components/ColumnSettings.tsx:199`、`web/src/hooks/useTableDefaults.ts:14`、`web/src/components/StatusTag.tsx:10` | 现有列设置、横向滚动、状态标签和搜索工具栏可复用；树表必须有首列 ID、同级 ID 倒序、弹性内容列。 |
| 登录态地区树 | `weiran4j/weiran-system/weiran-system-adapter/src/main/java/com/weiran/system/adapter/web/DepartmentController.java:24`、`web/src/hooks/queries/departments.ts:11` | 部门树 API 只需登录，供多个页面下拉复用；地区选择器应采用相同模式，不要求调用者拥有地区管理权限。 |

## 可复用点

| 可复用对象 | 位置 | 复用方式 | 需要改造吗 |
|---|---|---|---|
| `PageContainer`、`SearchToolbar`、`ColumnSettings`、`TableActions`、`StatusTag` | `web/src/components/` | 管理页复用搜索、树展开、状态、列设置和行操作能力。 | 否 |
| `useTableDefaults`、`toQueryString`、React Query 与 `http` | `web/src/hooks/`、`web/src/lib/query.ts`、`web/src/utils/request.ts` | 复用分页/表格行为、请求封装与查询失效模式。 | 否 |
| 后端 `@RequiresPermission`、`@OperationLog`、`CurrentUser` 与统一响应/错误 | `weiran4j/weiran-framework` | 控制地区管理权限、写操作审计和统一错误包络。 | 否；权限码必须使用 `region:`。 |
| `DepartmentNode` 树 DTO 与 `DepartmentTreeSelect` | `weiran4j/weiran-system/weiran-system-api/.../department/DepartmentNode.java`、`web/src/components/DepartmentTreeSelect.tsx` | 仅借鉴树接口和 Semi TreeSelect 受控写法。地区层级与部门不同，不能直接复用部门数据类型或父级校验。 | 是：独立的 `RegionNode`、`RegionSelect` 和级联数据转换。 |

## 真实约束

| 约束 | 证据位置 | 对设计的影响 |
|---|---|---|
| 业务代码只能依赖 `weiran-system-api` 与 `weiran-framework`，不能读写 `sys_*` 表 | `openspec/rules/enforced/constitution.md:91`、`:104` | 新模块独立定义 `region`、自己的模型与权限，不依赖 system 内部层。 |
| 业务模块必须五层齐全；自动发现，无需静态构建脚本登记 | `weiran4j/settings.gradle.kts:31` | 新增 `weiran4j/weiran-region/weiran-region-{api,domain,application,infrastructure,adapter}`。 |
| 业务菜单 ID 从 1000 起；业务权限不能用 `system:`；表名不得以 `sys_` 开头 | `openspec/rules/enforced/constitution.md:166`、`:170`、`weiran4j/docs/business-modules.md:1` | 预留菜单 ID `1000`–`1099`，权限前缀 `region:`，地区表名为 `region`。 |
| 业务错误码序号使用 20–99，每个模块需在号段表登记 | `openspec/rules/enforced/constitution.md:148`、`weiran4j/docs/business-modules.md:1` | 为 `weiran-region` 登记 `20`–`29`；只在错误需要前端区分时定义业务错误码，其余使用 CommonErrors。 |
| Flyway 迁移必须追加，使用全仓唯一时间版本并放在本模块 infrastructure 的 `region/` 目录 | `openspec/rules/enforced/constitution.md:83`、`openspec/rules/enforced/project.md` §一 SL-4 | 计划新增 `V202610190001__region_initial_schema_and_seed.sql`，包含表、菜单、权限和 3351 条快照数据。 |
| 列表页统一要求首列 ID 默认倒序，树表同级按 ID 倒序；必须保留弹性列 | `openspec/rules/advisory/list-view.md:24` | 管理表的同级排序在页面侧转为 ID 倒序；级联选择器仍按地区 sort/code 顺序展示。 |
| API 与菜单/按钮/迁移都必须登记在契约，前端类型手动同步 | `weiran4j/docs/01-架构与接口契约.md:143` | L4 同步接口契约、TypeScript DTO、权限和 Flyway 菜单内容。 |
| 本仓没有通用导出中心 | `web/src` 与 `weiran4j` 全仓索引无 ExportCenter；源定义位于 mono4ts `export-center/definitions/regions.ts` | 不迁移源项目的 XLSX/export-center 基础设施；以 `region:export` 保护可筛选 CSV 响应。 |
| 数据包省级快照仅 31 条，不包括港澳台；原种子无街道/乡镇层级 | mono4ts `china-division@2.7.0/dist/{provinces,cities,areas}.json` | 初始化数据范围与源数据逐条一致，不自行补齐未包含的行政区或扩展级别。 |

## 影响面

### 普通改动

| 文件/模块 | 动作 |
|---|---|
| `weiran4j/weiran-region/weiran-region-{api,domain,application,infrastructure,adapter}` | 新增完整五层地区模型、服务、存储、Web API、自动配置与迁移。 |
| `weiran4j/docs/01-架构与接口契约.md`、`weiran4j/docs/business-modules.md` | 新增 API/数据契约与业务号段登记。 |
| `web/src/pages/system/regions/RegionsPage.tsx`、`web/src/components/RegionSelect.tsx`、`web/src/hooks/queries/regions.ts`、`web/src/types/api.ts` | 新增管理页面、级联选择器、查询封装与契约类型。 |
| `openspec/state/bizs/region.md`、`openspec/state/bizs/README.md` | 建立地区表现状说明并加入索引。 |

### 共享层命中 ⚠️

#### 桶文件 / 注册表

| ID | 文件 | 冲突原因 | 是否命中 · 预计改动 |
|---|---|---|---|
| SL-1 | `weiran4j/settings.gradle.kts` | 新模块目录发现；半套五层会导致构建失败 | 未命中：五层齐全后自动发现，不修改设置文件。 |
| SL-2 | `weiran4j/weiran-dependencies/build.gradle.kts` | 动态生成新模块 BOM 坐标 | 未命中：来自自动发现的模块清单，不手工登记坐标。 |
| SL-3 | `weiran4j/weiran-app/build.gradle.kts` | 动态生成 app 装配与覆盖率聚合 | 未命中：读取相同的自动发现清单，不手工加依赖。 |
| SL-4 | `weiran4j/weiran-*/.../db/migration/**` | Flyway 版本为全仓序号型资源，菜单 ID 全局唯一且冲突不会由 Git 识别 | 命中：新增 `region/` 迁移并登记菜单 ID `1000`–`1099`。 |
| SL-5 | `web/src/pages/**` 与菜单 `component` | 路由由数据库菜单驱动，页面路径必须与组件路径一致 | 命中：新增 `system/regions/RegionsPage` 并在同一迁移登记。 |

#### 跨模块契约

| ID | 文件 | 冲突原因 | 是否命中 · 预计改动 |
|---|---|---|---|
| SL-6 | `weiran-framework/.../error/CommonErrors.java` | 全仓错误码基座，新增码必须是跨模块概念 | 未命中：地区专属错误留在 `weiran-region-api`，常见校验与权限失败复用 CommonErrors。 |
| SL-7 | `weiran-framework/.../page/{PageQuery,PageResult}.java`、`web/src/types/api.ts`、`weiran4j/docs/01-架构与接口契约.md` | 前后端 API 和响应契约共用，TS 无自动同步 | 命中契约与 TS DTO：本模块地区接口为新契约，更新 docs 与前端类型；不改通用分页类型。 |

#### 序号型资源

命中 Flyway migration 全局版本号（`V202610190001`）及 `sys_menu` 业务段（`1000`–`1099`）；错误码登记序号段 `20`–`29`。不新增序号文件注册表，不修改构建自动发现配置。

## 对 interview 的反向修正

| 原结论 | 现实情况 | 修正后 | 是否已同步回 interview.md |
|---|---|---|---|
| 新增用户可直接依赖地区树 API 作为通用选择项 | 基座部门树接口只要求登录，源页面级联选择同样不要求地区管理权限 | 新增登录态 `GET /api/regions/lookup` 返回启用树；管理查询及写操作仍分别校验 `region:list/create/update/delete`。 | ☑ |
| 地区导出以当前页还是全部过滤结果为范围未写清 | 源导出定义导出所有地区，管理树无分页 | CSV 导出应用筛选条件并导出所有匹配地区，不受前端树展开状态影响。 | ☑ |

## 风险预警

| 风险 | 触发条件 | 建议应对 |
|---|---|---|
| 初始 SQL 体积较大且第三方行政区划快照有许可与时效问题 | 一次性插入 3351 条数据 | migration 固定来源版本、附来源与许可证说明；将代码作为字符串保存并验证 code 唯一、父级存在及 level 递进。 |
| 使用者把停用父级下的启用子级理解为仍可选 | 级联选择器过滤启用节点时隐藏整个禁用分支 | 选择器仅沿全部启用的祖先链显示地区；编辑管理页仍显示禁用节点并可单独启停。 |
| 地区树全量加载导致过多行渲染 | 数据快照约 3351 行 | 后端一次建树；前端按默认折叠只渲染省市层，搜索命中时保留祖先路径；真浏览器检查响应与展开交互。 |

## Gate

- [x] 共享层命中已完整列出
- [x] 与 interview 的冲突项已回写 `interview.md`
- [x] 可复用点已确认，避免重复造轮子
