---
title: "CQT 站点配置后台表单现实校验"
status: "draft"
updated_at: "2026-10-15"
---

# Explore

## 调研范围

| 模块 | 目录/文件 | 为什么看它 |
|---|---|---|
| `weiran-cqt` | domain/application/infrastructure/adapter | 现有站点配置读取链、管理写入边界和控制器装配 |
| `weiran-system` | adapter 权限控制器、菜单迁移 | 复用权限注解、操作日志与菜单种子写法 |
| `web` | 配置页、API hooks、page registry | 复用表单、请求与动态页面注册方式 |
| `openspec` | cqt-site-config 能力、`cqt_setting` state | 更新现有契约与现状记录 |

## 现有实现

| 能力 | 位置 | 现状 |
|---|---|---|
| 前台键名与组装 | `weiran4j/weiran-cqt/weiran-cqt-domain/src/main/java/com/weiran/cqt/domain/setting/SiteConfig.java:27` | 22 个前台键映射到 ident；`guanyuwomen` 与 `user_agreement` 目前都指向 2；字符串值去 HTML 标签和首尾空白，公众号返回数组。 |
| 前台读取用例 | `weiran4j/weiran-cqt/weiran-cqt-application/src/main/java/com/weiran/cqt/application/setting/SiteConfigApplicationService.java:19` | 通过仓储读 ident 1–100 并委托领域组装。 |
| 仓储端口 | `weiran4j/weiran-cqt/weiran-cqt-domain/src/main/java/com/weiran/cqt/domain/setting/SettingRepository.java:7` | 当前只有按 ident 范围读取的端口，没有管理查询/保存。 |
| MyBatis 实现 | `weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/java/com/weiran/cqt/infrastructure/persistence/MybatisSettingRepository.java:23` | 升序读取、同 ident 覆盖，最终取最大 id；不能用重复 ident 行实现分离。 |
| 管理表单范例 | `web/src/pages/system/configs/ConfigsPage.tsx:44` | Semi `Form`、表单校验、mutation 与 Toast 可复用；站点配置应采用单独页面和专用 API。 |
| 状态文档 | `openspec/state/bizs/cqt_setting.md:20` | 当前表只供前台读取，没有管理菜单或页面；记录 22 个输出键及清洗行为。 |

## 可复用点

| 可复用对象 | 位置 | 复用方式 | 需要改造吗 |
|---|---|---|---|
| 前台领域组装 | `SiteConfig.build` | 增加 `user_agreement` 的独立 ident 映射，保持清洗行为 | 是：新增编号常量/映射 |
| 现有 Setting 仓储 | `SettingRepository` / `MybatisSettingRepository` | 扩展管理端口读取指定配置并按 ident 更新最高 id 行，不创建重复 ident | 是：补管理读写方法及更新实现 |
| Semi 表单与 API mutation 模式 | `ConfigsPage.tsx`、`web/src/hooks/queries/configs.ts` | 新建独立站点配置页面与专用 query/mutation | 是：不复用通用参数 CRUD |
| 动态页面注册 | `web/src/utils/page-registry.ts` | 页面由 glob 收集，菜单 component 指向对应页面路径 | 否 |
| RBAC 与操作审计 | `@RequiresPermission`、`@OperationLog` | 管理读写接口分别校验 `cqt:site-config:list/update`，保存记操作日志 | 否 |

## 真实约束

| 约束 | 证据位置 | 对设计的影响 |
|---|---|---|
| 同 ident 多行时前台只取最大 id | `MybatisSettingRepository.java:24-34` | 管理保存必须更新最大 id 行或缺行时插入；禁止盲目新增重复行。 |
| ident 范围为 1–100，现有业务映射最大为 70 | `SiteConfig.java:18-22, 59-83` | 给 `user_agreement` 分配空闲编号 71；接口读取范围无需扩大。 |
| 管理须受 `/api/**` 拦截器保护，读写分别需权限 | `openspec/rules/enforced/project.md:174` | 提供受保护 `/api/cqt/site-config` 端点；前端按系统请求层处理 CSRF。 |
| 配置原始内容可能含 HTML，前台会清洗 | `SiteConfig.java:41-49` | 管理读写 raw `contents`；不可从清洗后的公开 GET 回填表单。 |
| 页面路径与菜单 component 必须匹配，并有按钮权限种子 | `openspec/rules/enforced/project.md:70-80` | 新增 CQT Flyway 菜单迁移和对应按钮行；页面使用相同 component 路径。 |
| 当前接口规格声明两键共用 ident 2 | `openspec/specs/cqt-site-config/spec.md:31` | 修改既有 `cqt-site-config` 能力，不另立矛盾能力；新增 ident=71 作为 `user_agreement`。 |
| 初始数据由独立脚本从旧库导入，Flyway 不装载业务配置 | `V202610022200__cqt_setting.sql`、`scripts/biz/import/cqt_setting.sql` | 新迁移需明确只把现有 ident=2 内容复制到新 ident 71，避免老环境丢失当前协议内容；导入脚本也要保证旧库导入时生成新 ident。 |

## 影响面

### 普通改动

| 文件/模块 | 动作 |
|---|---|
| `weiran4j/docs/01-架构与接口契约.md` | 修改 `GET /api-web/product/getconfig` 的 ident 映射，登记后台读写接口 |
| `openspec/specs/cqt-site-config/spec.md` | 修改前台映射要求，新增后台管理表单/权限要求 |
| `weiran4j/weiran-cqt` 五层已有模块 | 增加后台配置读取、保存端口/服务/controller/mapper 支持，不新增 Gradle 模块 |
| `web/src/pages/cqt/site-config.tsx`、`web/src/hooks/queries/cqt-site-config.ts`、`web/src/types/api.ts` | 新增独立 form、请求封装、类型 |
| CQT Flyway 目录 | 新增 ident 数据拆分与菜单/权限迁移（版本号待 L4 取号） |
| `scripts/biz/import/cqt_setting.sql` | 导入旧库时为 ident 71 保留旧 ident 2 的起始内容 |
| `openspec/state/bizs/cqt_setting.md`、`README.md` | 更新管理页面、操作与映射现状 |

### 共享层命中 ⚠️

#### 桶文件 / 注册表

| ID | 文件 | 冲突原因 | 是否命中 · 预计改动 |
|---|---|---|---|
| SL-1 | `settings.gradle.kts` | 新模块要改自动发现配置 | 未命中：扩展现有 `weiran-cqt` 五层 |
| SL-2 | `weiran-dependencies/build.gradle.kts` | BOM 全仓单点 | 未命中：无依赖版本变化 |
| SL-3 | `weiran-app/build.gradle.kts` | 应用聚合入口 | 未命中：不新增业务模块或依赖 |
| SL-4 | CQT Flyway 菜单种子 | 菜单 id 全局序号，页面必须有菜单与权限 | 命中：新页面菜单和权限行，L4 Layer 0 先取号并写迁移 |
| SL-5 | `web/src/pages/**` + `page-registry.ts` | 菜单 component 与页面路径需配对 | 命中：新增页面，并保证 component 与文件路径一致；page registry glob 自动收集无需改 |

#### 跨模块契约

| ID | 文件 | 冲突原因 | 是否命中 · 预计改动 |
|---|---|---|---|
| SL-6 | `weiran-framework/.../CommonErrors.java` | 跨模块错误码单点 | 未命中：复用通用错误码，无需新建全局错误 |
| SL-7 | `weiran-framework/.../{PageQuery,PageResult}.java` | 跨模块分页契约 | 未命中：站点配置是整表表单，不使用分页契约 |

#### 序号型资源

| 资源 | 是否命中 · 预计改动 |
|---|---|
| Flyway 版本号 | 命中：在 L4 执行时确定全局未占用的时间戳版本号；只新增迁移，不改旧迁移。 |
| `sys_menu.id` | 命中：在 CQT 登记号段 1000–1999 中选未使用 id，菜单与按钮行使用互不冲突 id。 |
| `user_agreement ident` | 命中：选用 71，现有读取范围已覆盖。 |

## 对 interview 的反向修正

| 原结论 | 现实情况 | 修正后 | 是否已同步回 interview.md |
|---|---|---|---|
| 用户确认两字段独立 | 最大 id 覆盖规则意味着新增重复 ident 行会改变公开读取结果；拆分需独立 ident。老值必须复制，否则切换时内容可能为空。 | ident 2 保留 `guanyuwomen`；ident 71 供 `user_agreement`；迁移及源数据导入都初始化为 ident 2 当前内容。 | 是：澄清记录已要求存量内容保留。 |

## 风险预警

| 风险 | 触发条件 | 建议应对 |
|---|---|---|
| 新 ident 未覆盖旧库导入路径 | 新环境按旧库脚本初始化后 `user_agreement` 无值 | 修改导入 SQL，先导入原始行，再以 ident 2 的最大 id 内容生成/更新 ident 71，确保重复执行幂等 |
| 多行相同 ident 被误增 | 保存接口使用 insert 而不是 update | 保存端口按 ident 查最大 id，存在则更新该行；仅完全无行时才插入 |
| 按钮权限与菜单未绑定 | Flyway 只添加菜单，不给默认管理员角色按钮权限 | 菜单迁移按现有 CQT 种子方式写菜单、按钮权限并关联角色；核查默认超管授权行为 |

## 本次不会碰的目录

不改 `weiran-framework`、`weiran-system`、`web/src/App.tsx`、`web/src/layouts/AdminLayout.tsx`、`web/src/utils/page-registry.ts`、`uniapp/`（如存在）及其他业务模块；不新增模块或表，不改变前台 `getconfig` 的键集合。

### 用户补充要求（2026-10-15）

- 页面需按现有四个配置类别切换 Tab，并为介绍/协议服务类字段提供富文本与图片上传。
- 现有 `/api-web/local-files/upload` 需要 C 端 Portal JWT，后台管理令牌不能调用；因此新增 `/api/cqt/site-config/images`，复用 `FileUploadService`，由框架权限拦截保护，限制图片扩展名和 10 MB 上限。
- 图片与富文本需在 uniapp 可呈现：公开接口对富文本键保留原始 HTML，其它文本保持现有清洗方式。

## Gate

- [x] 共享层命中已完整列出
- [x] 与 interview 的冲突项已回写
- [x] 可复用点已确认
