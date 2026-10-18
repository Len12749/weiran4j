---
title: "站点配置与媒体库集成及规格核对"
status: "done"
updated_at: "2026-10-18"
---

# Verify

## 一、集成记录（L6）

**并行方式**：单工作区，无并行集成  
**执行单元数**：7 个（E0–E6）

## 输入检查

- [x] E0–E6 执行笔记齐全
- [x] 契约、数据迁移与 system 路由按 plan 冻结
- [x] 自测与最终项目门禁通过

## 冲突清单

无并行冲突。迭代中集成测试发现旧前台 `/uploads/**` 映射与 system 图片映射存在访问路径重叠风险；system 本地图片改用 `/system-uploads/**`，保留 `media/` 对象路径，并由集成测试验证可访问性。

## 重复实现消除

无重复实现。后台站点配置和图片库共用 `/api/system/media/images` 上传接口及 system 文件存储服务。

## 被牺牲的方案

| 放弃的方案 | 来自 | 放弃理由 |
|---|---|---|
| 保留旧站点配置管理接口、页面和菜单 | E0、E5、E6 | 会留下重复的管理路由、权限和编辑入口 |
| system 本地图片使用 `/system-uploads/**` | E5 | 独立前缀标识 system 托管文件，并映射到存储对象路径 |
| disabled storage 返回 500 | E3 | 与部署契约及存储未配置语义不符；改为通用服务不可用错误 50300 |

## 越界修改汇总

| 文件 | 申报单元 | 性质 | 处置 |
|---|---|---|---|
| `weiran4j/weiran-app/src/test/java/com/weiran/app/AuthIT.java` | E1 | 必要连带：system 菜单种子更新 | 保留 |
| `weiran4j/weiran-app/src/main/resources/application.yml` | E5 | 必要连带：配置 system 存储 URL | 保留 |
| `weiran4j/weiran-framework/.../error/{CommonErrors,BizException}.java`、`weiran4j/docs/01-架构与接口契约.md`、`weiran4j/.env.example` | E3 | 必要连带：登记 50300 并说明 system 存储默认路径 | 保留 |
| `web/src/types/api.ts`、`web/src/utils/__tests__/{page-registry,request}.test.ts` | E6 | 必要连带：同步 system API 类型、菜单页与 multipart 路径 | 保留 |
| system、framework 和应用侧上游管理文件 | E1–E6 | 管理功能按通用后台契约归属 system | 按本次明确范围保留；未提交变更 |

## 二、规格一致性（L8）

### 前置条件：L7 硬闸门

| 检查 | 命令 | 结果 | 证据 |
|---|---|---|---|
| `build` | `pnpm --filter @weiran/web build` | ☑ 绿（exit=0） | `evidence/build.log` |
| `test` | `pnpm --filter @weiran/web test` | ☑ 绿（33 文件、278 用例） | `evidence/test.log` |
| `lint` | `pnpm --filter @weiran/web lint` | ☑ 绿（exit=0） | `evidence/lint.log` |
| 后端门禁 | `./gradlew clean check --no-daemon`（JDK 21） | ☑ 绿（exit=0） | `evidence/backend-check.log` |

清理业务实现后重新运行后端与前端门禁；前端首次并行运行出现两个超时/状态竞争失败，单独重跑全绿。证据在最后一处源码改动后生成。

### tasks.md 逐条核对

| tasks.md 条目 | 执行单元 | 实现位置 | 一致 |
|---|---|---|---|
| 0.1 | E0 | system 迁移 `V202610170001`、菜单与按钮号段 | ☑ |
| 1.1 | E1 | system Flyway 创建 `sys_site_config`、`sys_media`、system 菜单与权限 | ☑ |
| 2.1 | E2 | system API/domain 媒体、配置和存储契约 | ☑ |
| 3.1 | E3 | 本地/OSS/disabled 存储、资产登记查询、引用检查、安全删除 | ☑ |
| 3.2 | E4 | 站点配置原始内容读写、ident 初始化与行锁 | ☑ |
| 4.1 | E5 | `/api/system/**` Controller、权限、日志和本地静态访问 | ☑ |
| 5.1 | E6 | system 站点配置富文本编辑器与多图管理页面 | ☑ |

### design.md 一致性

| 章节 | 设计要求 | 实现情况 | 一致 |
|---|---|---|---|
| 宪法 CP-1–CP-15 | 分层、迁移、错误码、权限和依赖规则 | system 五层实现；迁移追加；存储不可用复用通用 50300；菜单与权限使用 system 号段 | ☑ |
| API Design | system 配置、图片上传、分页与删除接口 | 接口均位于 `/api/system/**`，使用 `system:` 权限和统一响应/操作日志 | ☑ |
| Database Design | `sys_site_config` 与 `sys_media`；资产只登记新上传图片 | 迁移创建两表并以通用键初始化配置，不查询业务表 | ☑ |
| Data Flow | 上传登记、富文本保存、引用保护、提交后删除对象 | 服务与集成测试覆盖上传、引用冲突、解除引用后对象和记录删除 | ☑ |
| DS-4 / DS-6 | system 菜单、编辑器和图片库交互 | 前端页面、组件与权限点均为 system 命名；无新闻管理或读取接口 | ☑ |
| Observability | 写操作审计，不记录文件内容或凭据 | 上传、删除、配置保存接入操作日志；OSS 密钥由环境变量提供 | ☑ |

### specs 验收标准核对

| spec / Scenario | 验证方式 | 通过 |
|---|---|---|
| system-file-storage：disabled 503、OSS 配置校验与流式存储、local/OSS 删除边界 | system application 单测、infrastructure 测试、全量 `check` | ☑ |
| system-media-library FR-001：上传后资产可查，历史对象不伪造记录 | 上传集成测试、仓储测试 | ☑ |
| system-media-library FR-002：按文件名分页搜索 | infrastructure 测试、前端测试 | ☑ |
| system-media-library FR-003：多图逐张上传、失败重试、预览与复制 | 前端页面测试及 API 测试 | ☑ |
| system-media-library FR-004：引用中拒删；解除引用后对象与记录删除；未授权保护 | system 媒体服务与 API 测试 | ☑ |
| system-site-config FR-001：通用配置 Tab、原始 HTML、权限和上传图片 | 后端门禁、前端编辑器测试 | ☑ |
| 富文本选择/粘贴：多图按序插入、普通文本粘贴保持原行为、跨 Tab 保值 | `web/src/pages/system/__tests__/site-config.test.tsx` | ☑ |

### 浏览器页面检查

使用 Chrome 打开 `http://localhost:5373/system/media-library` 时，本机当前连接的后端仍返回旧菜单，前端显示 404；该预览未能检查新页面的实际布局。前端组件测试、构建和 lint 通过；需在运行包含本次迁移的后端后再看真实页面。

### 越界检查

`git diff --name-only` 的跨层改动均在上方越界表申报；system API 与 system 持久化属于本 change 的明确实现范围。未发现未申报越界。

### 不一致项归属判定

| # | 不一致内容 | 判定 | 去向 |
|---|---|---|---|
| 1 | system 托管文件需要独立静态访问前缀 | 实现问题，已修复 | system 使用 `/system-uploads/**`，测试验证 HTTP 访问 |
| 2 | disabled storage 原先映射为 500 | 实现与部署契约不一致，已修复 | 新增通用 `50300`，单测锁定 |

## 遗留问题

- 历史图片对象没有资产记录，不会显示在新图片库中；如需管理，须重新上传登记。该限制符合 design 的迁移策略。
- 本地存储只供开发与测试；部署使用 OSS 并配置相应环境变量。

## 流程反馈

- system 与下游前台应用共用 `/uploads/**` 时，新增静态资源应使用独立访问前缀并验证 URL 到磁盘对象的完整映射。

## 结论

- [x] 集成完成（单工作区、无并行集成）
- [x] L7 三项全绿
- [x] tasks.md 条目全部覆盖
- [x] design/specs 与实现一致
- [x] 无未申报越界
- [x] 不一致项已修复并验证

**判定**：通过，进入 L9 人类审阅 diff。
