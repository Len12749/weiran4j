---
title: "CQT 站点配置后台表单技术设计"
status: "draft"
updated_at: "2026-10-15"
approved_by: "用户"
approved_at: "2026-10-15"
---

# Design

## Context

- 需求来源：`interview.md`。用户确认 `guanyuwomen` 与 `user_agreement` 必须拆成独立配置。
- 现有实现调研：`explore.md`。
- 关键约束：前台按 ident 取最大 id 行；配置可能含 HTML；管理必须保存原始 `contents`；不新增表、不改前台键集合。

## 宪法对照

| 原则 | 判定 | 说明 |
|---|---|---|
| CP-1 领域层不依赖框架 | ☑ 符合 | 管理命令、字段映射和配置标识保持纯 Java；领域层不引入 Spring/MyBatis 类型。 |
| CP-2 依赖方向单向向内 | ☑ 符合 | Controller 调 application，application 使用 domain 端口，infrastructure 实现端口。 |
| CP-3 持久化类型不跨层 | ☑ 符合 | `*DO` 与 Mapper 只在 infrastructure；API DTO 不暴露持久化类型。 |
| CP-4 版本号只有一个来源 | ☑ 符合 | 前端 Tiptap 依赖版本写在 `web/package.json`，解析版本固定在 `pnpm-lock.yaml`。 |
| CP-5 质量规则只在 build-logic 里配置 | ☑ 符合 | 不修改质量规则或模块豁免。 |
| CP-6 豁免必须最小且带理由 | ☐ 不涉及 | 不需要质量规则豁免。 |
| CP-7 表结构只经 Flyway 迁移变更，已发布的迁移永不修改 | ☑ 符合 | 不改表结构或旧迁移；新增迁移只做一次性的 ident 2→71 内容拆分及菜单/权限种子。 |
| CP-8 密码只用 BCrypt，令牌吊销只走 token_version | ☐ 不涉及 | 不修改认证、密码或令牌生命周期。 |
| CP-9 凭据不进版本库、不进日志 | ☑ 符合 | 字段是公开站点文案，不含凭据；操作日志沿用框架规则记录请求体，脱敏敏感字段并截断到 4096 字符。 |
| CP-10 认证失败不泄露账号存在性 | ☐ 不涉及 | 不增加认证入口。 |
| CP-11 错误码归属决定 HTTP 状态，不靠 Controller 判断 | ☑ 符合 | Controller 不转换业务错误；认证和权限由框架，异常由统一处理器映射。 |
| CP-12 依赖只能业务 → 基座 → 框架，反向与同层横向禁止 | ☑ 符合 | 只扩展 CQT，不依赖其他业务模块或基座内部模块。 |
| CP-13 业务只经基座的 weiran-system-api 接触基座 | ☑ 符合 | 页面授权走框架 `@RequiresPermission`；不访问 `sys_*` 表或基座内部 API。 |
| CP-14 错误码按号段分配 | ☐ 不涉及 | 复用通用错误码，不新增业务错误码。 |
| CP-15 权限码、菜单 id 与 Flyway 模块段按层隔离 | ☑ 符合 | 使用 `cqt:` 权限码、CQT 菜单号段 1000–1999，迁移放 `db/migration/cqt/`。 |

## Architecture

管理页面使用 CQT 专用 API 直接编辑 `cqt_setting` 的映射值。管理读取返回配置目录中的键、中文名称、ident 与 raw contents；提交完整配置集后，应用层校验键集合和 ident 映射，仓储按 ident 更新最大 id 行，缺行时插入。公开 GET 对富文本键保留 HTML，其他值按原规则清洗。图片使用带 `cqt:site-config:update` 权限的后台 multipart 接口，经现有 `FileUploadService` 写入配置好的 CQT 存储。

## Data Flow

1. 管理员打开 CQT 站点配置页面，前端调用受保护的 `GET /api/cqt/site-config`。
2. 管理接口按固定键目录读取 ident 2–71 相关项，返回 raw contents；不通过公开 GET 取值。
3. 管理员修改表单并提交 `PUT /api/cqt/site-config`，请求只包含登记的键和值。
4. 应用层拒绝缺失/未知键或身份映射不一致的数据；对每个 ident 更新最大 id 行，缺行才插入。
5. 富文本图片可通过工具栏选择或从剪贴板粘贴；两者都上传至 CQT 存储后，将公开 URL 插入 HTML。粘贴处理只拦截图片文件，普通文本继续走 Tiptap 默认处理。图片上传仅允许 jpg/jpeg/png/gif。
6. 保存动作记录 `@OperationLog`；随后公开 GET 对富文本键返回 HTML，其它值执行原清洗规则。

## 跨模块契约变更(DS-1)

不修改 `weiran-framework` 错误码、分页与响应契约；接口仅属于 `weiran-cqt`。

## API Design(DS-2)

| 路由 | 方法 | 所属模块 | 请求关键字段 | 返回关键字段 | 权限点 |
|---|---|---|---|---|---|
| `/api/cqt/site-config` | GET | `weiran-cqt-adapter` | 无 | `items[]: {key, label, ident, contents}` | `cqt:site-config:list` |
| `/api/cqt/site-config` | PUT | `weiran-cqt-adapter` | `items[]: {key, contents}` | 保存成功包络 | `cqt:site-config:update` |
| `/api/cqt/site-config/images` | POST multipart | `weiran-cqt-adapter` | `file` 图片文件 | `{url, name, path, size}` | `cqt:site-config:update` |

- 管理 API 使用框架统一 `{code: 0, message, data}` 包络；前端类型保持数字 code 口径。
- `items` 固定包含前台 22 个键，禁止客户端改写 ident；`user_agreement` 对应 ident 71，`guanyuwomen` 对应 ident 2。
- `contents` 可为 JSON `null`，也可为空字符串；HTML 作为普通原始文本保存。
- 图片上传复用 CQT 文件存储，只允许 `.jpg`、`.jpeg`、`.png`、`.gif`，单张不超过 10 MB，以当前后台用户 ID 作为存储路径归属；不记录二进制请求体操作日志。
- 写请求按既有 Cookie CSRF 或 Bearer 认证方式保护；Controller 标准权限注解，不自行检查用户身份。

## Database Design(DS-3)

| 表 | 动作 | 关键字段 | 索引 | 备注 |
|---|---|---|---|---|
| `cqt_setting` | 不改结构；新增一条 ident 71 初始映射 | `ident=2` 当前最大 id 行的 `contents` 复制到 ident 71 | 无新增索引 | 新 Flyway 脚本按主键最大值生成新行；ident 2 缺行时不造内容。旧库导入 SQL 幂等生成相同初始化值。 |
| `sys_menu` | 追加菜单和按钮权限种子 | 菜单 id 取 CQT 号段未用值；权限 `cqt:site-config:list/update` | 既有约束 | 仅菜单种子由业务迁移登记；不改 system 表结构。 |

不得新增重复配置行来保存普通更新；更新时选择该 ident 最大 id 行。导入脚本重新执行时按确定的配置行主键更新或在缺失时创建，不能不断追加新行。

## 权限与已知缺口(DS-4)

| 项 | 设计 |
|---|---|
| 权限点 | `cqt:site-config:list` 保护 GET，`cqt:site-config:update` 保护 PUT 与图片上传。 |
| 菜单挂载 | 新增后端动态菜单 `component=cqt/site-config`；页面文件路径逐字匹配。按钮权限挂到菜单树并由菜单迁移关联默认超管角色，按现有 CQT seed 约定实现。 |
| 操作审计 | PUT 使用 `@OperationLog(module="站点配置", description="更新站点配置")`；按框架规则记录请求体（脱敏并截断到 4096 字符）。配置字段是公开站点文案，不含凭据。 |
| 数据范围/租户/导出/工作流 | 不适用；站点配置全站共用。 |
| 已知缺口 | 公众号与首页图片路径字段没有独立上传控件，仍可编辑 URL/路径；富文本内图片通过上传按钮插入。 |

## 分层与装配(DS-5)

| 项 | 设计 |
|---|---|
| 依赖方向 | `adapter → application → domain`、`infrastructure → domain`；不新增模块。 |
| 端口 | 在 domain 定义管理读写能力与配置项类型；签名只使用 Java 类型及 JSpecify 空值标记。 |
| 应用用例 | application 编排读取和保存、校验固定键目录及交易边界。 |
| 持久化 | infrastructure 通过 MyBatis-Plus 查 ident 最大 id 并更新/插入；DO/Mapper 不出 infrastructure。 |
| 自动装配 | 在现有 CQT auto-configuration 注册管理服务和 controller；不改 `weiran-app`。 |

## 前端设计(DS-6)

| 项 | 内容 |
|---|---|
| 页面/路由 | `web/src/pages/cqt/site-config.tsx`，由 page registry glob 自动登记，菜单 `component` 为 `cqt/site-config`。 |
| 菜单挂载 | CQT 新 Flyway 种子提供动态菜单和 list/update 按钮权限。 |
| 数据请求方式 | 新建 `web/src/hooks/queries/cqt-site-config.ts`，使用已有 query client/request helper，GET 加载、PUT 保存后失效缓存。 |
| 复用组件 | Semi UI `Form`、`Tabs`、`Button`、`Toast`、`PageContainer`；Tiptap `StarterKit` 与 `Image` 扩展编辑 HTML。 |
| 页面分组 | 四个 Tab：站点介绍、协议与服务、联系方式、首页素材与证书；字段中文 label 固定映射前台键。 |
| 富文本字段 | `guanyuwomen`、`user_agreement`、`yinsixieyi`、`about_us`、`gongsijieshao`、`hezuohuoban`、`dasaijieshao`、`mianzexieyi`、`shouhoufuwu`、`shangwuhezuo`、`dasaizhangcheng`。 |
| 图片上传 | Tiptap 工具栏选择图片，multipart 调用后台接口，成功后插入返回 URL；失败显示错误且不插入图片。 |
| 图片粘贴 | Tiptap `handlePaste` 识别剪贴板图片文件，复用上传 mutation；只拦截图片粘贴，不改变纯文本粘贴。 |
| 权限控制点 | 菜单由 list 权限控制；保存按钮用 `<Permission code="cqt:site-config:update">`。 |
| 空值 | null 展示为空编辑值；未编辑的 null 仍以 null 保存。富文本公开值原样输出 HTML；其他字段按原有清洗规则。 |

## Observability

- 日志关键字段：使用现有 requestId 与访问日志机制。
- 指标：不新增专用指标。
- 审计：每次成功 PUT 产生操作日志，请求体按框架规则脱敏并截断到 4096 字符；图片上传记录操作元数据，不采集 multipart 二进制。
- 告警 / 排障入口：查看对应 requestId 和「站点配置」操作日志。

## Test Plan

| 层级 | 范围 | 命令 |
|---|---|---|
| 领域单元 | `user_agreement` 独立映射、富文本键 HTML 保留、其它清洗/数组既有行为 | JDK 21 下 CQT domain tests |
| 持久化/应用 | 更新最大 id 行、无行插入、原始内容保留、ident 互不覆盖 | CQT infrastructure/application test task |
| HTTP 集成 | 管理 GET/PUT、图片上传权限/类型、操作日志、公开 GET HTML 保留与清洗 | `./gradlew :weiran-app:test` 中 CQT 集成测试 |
| 前端 | Tab 切换、跨 Tab 保存、富文本 HTML、工具栏上传和剪贴板粘贴图片、普通文本粘贴 | Vitest CQT 页面/编辑器测试 |
| 格式与门禁 | Java 格式及项目质量规则 | `./gradlew spotlessApply` 后 `./gradlew check --no-daemon`，需 JDK 21 与 Docker |

## Rollout Plan

1. 发布前检查 ident 2 和 ident 71 的当前行；Flyway 迁移将 ident 2 最大 id 行内容复制为 ident 71 初始行，菜单与权限同时种入。
2. 发布后在后台分别编辑「关于我们」和「用户协议」，通过公开 GET 验证两者分别生效并保留 HTML；普通文本仍按原规则清洗。
3. 对旧库初始化，修改后的 `scripts/biz/import/cqt_setting.sql` 可重复执行，按 ident 2 最新值初始化 ident 71；已完成独立编辑后不得再用导入脚本覆盖生产数据。

## Rollback Plan

1. revert 锚点：本 change 合并提交；回滚代码与前端页面。
2. Flyway 不执行破坏性删行回滚；ident 71 行保留但旧代码忽略它。若要恢复旧前台共享语义，需另发前向迁移将公开映射改回 ident 2。
3. 回滚期间暂停后台写入；回滚后保留 ident 71 内容供重新发布时恢复。

## Open Questions

- [x] 是否拆分 `guanyuwomen` 与 `user_agreement`：用户确认拆分，ident 71。
- [ ] L3 审阅此设计，重点确认迁移将 ident 2 当前值复制到 ident 71，且独立编辑后不再用导入脚本覆盖生产数据。
