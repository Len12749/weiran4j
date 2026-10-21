---
title: "系统配置共享分组改造调研"
status: "draft"
updated_at: "2026-10-20"
---

# Explore

## 调研范围

| 模块 | 目录/文件 | 为什么看它 |
|---|---|---|
| `weiran-system-*` | platform config / dict 的 domain、application、infrastructure、adapter | 评估领域校验、存储与接口改动点 |
| 数据库 | `weiran-system-infrastructure/.../db/migration/platform/**` | 确认既有表结构与迁移版本 |
| `web` | `web/src/pages/system/configs/**`、`hooks/queries/configs.ts` | 评估页面结构和请求缓存 |
| 文档 | `weiran4j/docs/01-架构与接口契约.md`、`openspec/state/bizs/sys_config.md` | 同步公开契约和现状 |

## 现有实现

| 能力 | 位置 | 现状 |
|---|---|---|
| 配置组目录 | `weiran4j/weiran-system/weiran-system-domain/src/main/java/com/weiran/platform/domain/config/ConfigGroupDefinition.java:9` | `general`、`user` 在代码中固定定义 |
| 配置分组校验 | `weiran4j/weiran-system/weiran-system-domain/src/main/java/com/weiran/platform/domain/config/SystemConfig.java:60` | 新建/更新配置只接受上述固定目录 |
| 配置组接口 | `weiran4j/weiran-system/weiran-system-application/src/main/java/com/weiran/platform/application/config/ConfigApplicationService.java:49` | `/groups` 返回领域内置目录 |
| 配置表 | `weiran4j/weiran-system/weiran-system-infrastructure/src/main/resources/db/migration/platform/V202609260101__platform_init_schema.sql:40` | `sys_config.group_key` 存储分组编码；没有通用组表 |
| 配置页 | `web/src/pages/system/configs/ConfigsPage.tsx:286` | 以 Tabs 展示组，右侧动态配置表单 |
| 双栏组件 | `web/src/components/NavListPanel.tsx:1` | 字典管理已经采用导航列表主从布局 |
| 字典分组删除 | `web/src/pages/system/dicts/DictsPage.tsx` | 左侧条目展示创建时间、内置标签，操作菜单按内置状态禁用删除 |
| 字典持久化 | `weiran4j/weiran-system/weiran-system-infrastructure/src/main/java/com/weiran/platform/infrastructure/persistence/MybatisDictRepository.java:22` | 字典主记录存于 `sys_dict`，字典项存于 `sys_dict_item`；两者通过 ID 关联 |

## 可复用点

| 可复用对象 | 位置 | 复用方式 | 需要改造吗 |
|---|---|---|---|
| `NavListPanel` / `NavListItem` | `web/src/components/NavListPanel.tsx` | 配置组列表与字典页保持一致的左侧选择交互 | 否 |
| 配置动态字段表单 | `web/src/pages/system/configs/ConfigsPage.tsx` | 保留并嵌入右侧内容区 | 是：从 Tabs 改为当前组面板 |
| 配置项读写 API | `ConfigController`、`ConfigApplicationService` | 保持现有配置项 API，变更分组目录来源 | 是 |

## 真实约束

| 约束 | 证据位置 | 对设计的影响 |
|---|---|---|
| 已合入 Flyway 迁移不可修改 | `openspec/rules/enforced/constitution.md:CP-7` | 新增迁移创建 `sys_group` 并写入组记录 |
| 持久化类型只在 infrastructure | `openspec/rules/enforced/constitution.md:CP-3` | application 通过领域端口访问组数据 |
| 不建外键，关联由应用保证 | `V202609260101__platform_init_schema.sql:2` | `sys_config.group_key` 按编码关联并筛 `config` 类型；迁移后 `sys_dict_item.group_id` 按 ID 关联 `dict` 类型 |
| 前端共享类型手动同步 | `weiran4j/docs/01-架构与接口契约.md:286` | 更新 `web/src/types/api.ts` 与契约文档 |

## 影响面

### 普通改动

| 文件/模块 | 动作 |
|---|---|
| `weiran-system-domain` | 字典主记录模型扩展为通用组类型；配置有效性校验改为由应用层验证组类型 |
| `weiran-system-application` | 字典查询限定 `dict` 组，配置组查询限定 `config` 组，保存前校验分组 |
| `weiran-system-infrastructure` | `sys_dict` 迁为 `sys_group`，合并主表 Mapper/实体；追加迁移并保留字典项关联 |
| `weiran-system-adapter` | 组响应契约保持 `key/label` 或扩展排序字段 |
| `web/src/pages/system/configs` | Tabs 改为 NavListPanel + 右侧内容 |
| API/状态文档 | 更新分组结构与页面布局描述 |

### 共享层命中 ⚠️

#### 桶文件 / 注册表

| ID | 文件 | 冲突原因 | 是否命中 · 预计改动 |
|---|---|---|---|
| SL-1 | `weiran4j/settings.gradle.kts` | 模块自动发现配置 | 未命中；不新增模块 |
| SL-2 | `weiran4j/weiran-dependencies/build.gradle.kts` | 依赖版本约束 | 未命中；不新增依赖 |
| SL-3 | `weiran4j/weiran-app/build.gradle.kts` | 应用依赖聚合 | 未命中；模块不变 |
| SL-4 | Flyway 种子菜单 | 菜单序号及配套路由 | 未命中；页面路径和菜单不变 |
| SL-5 | `web/src/pages/**` 与页面注册表 | 页面文件/菜单配对 | 未命中新增路由；改现有页面，不改 component |

#### 跨模块契约

| ID | 文件 | 冲突原因 | 是否命中 · 预计改动 |
|---|---|---|---|
| SL-6 | `weiran-framework/.../CommonErrors.java` | 跨模块错误码基座 | 未命中；复用通用错误码 |
| SL-7 | `weiran-framework/.../{page,response}/*` | 通用响应与分页契约 | 未命中；分组接口不改变分页/包络 |

#### 序号型资源

| 资源 | 命中情况 |
|---|---|
| Flyway 迁移版本 | 命中；新增 platform 模块唯一版本迁移，实施前确认全仓未占用 |
| `sys_menu` 菜单 id | 未命中；无菜单变更 |

### 本次不会碰的目录

- `weiran-framework/`、`weiran-app/`、`settings.gradle.kts`、`weiran-dependencies/`
- `web/src/App.tsx`、菜单种子；字典页面与 API 路径保持，`sys_dict_item` 表保留但关联列改名
- 登录、权限、站点公开配置和媒体库

## 对 interview 的反向修正

| 原结论 | 现实情况 | 修正后 | 是否已同步回 interview.md |
|---|---|---|---|
| 原先按配置另建分组表 | 用户明确 `sys_dict` 与 `sys_group` 合并 | 原字典记录迁为 `type=dict`；配置组为 `type=config`，字典项仍关联组 ID | ☑ |

## 风险预警

| 风险 | 触发条件 | 建议应对 |
|---|---|---|
| 字典数据或字典项关联丢失 | 改表时遗漏既有 ID 或关联值 | 将主表改名为 `sys_group`，将 `dict_id` 改名为 `group_id` 并原样保留值 |
| 既有配置组无法匹配新表 | 数据迁移遗漏旧组键 | 插入两个 config 组并保证 `sys_config.group_key` 值不变 |
| 领域对象依赖仓储查询 | 在纯领域模型内验证数据库组 | 将存在性/类型校验放在 application，领域保持纯 Java |
| 双栏窄屏高度不足 | 小屏同时堆叠两栏 | 沿用字典页响应式样式与窄屏滚动上限 |

## Gate

- [x] 共享层命中已完整列出
- [x] 与 interview 的冲突项已回写
- [x] 可复用点已确认
