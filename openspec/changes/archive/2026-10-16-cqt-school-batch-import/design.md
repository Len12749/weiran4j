---
title: "CQT 学校作品批量导入技术设计"
status: "draft"
updated_at: "2026-10-16"
approved_by: "用户（对话批准）"
approved_at: "2026-10-16"
---

# Design

> 保持意图粒度：描述行为如何，不写逐文件的实现步骤。实现分工留给 L4 执行计划。

## Context

- 需求来源：`interview.md`；省赛/国赛是作品提交后的评价，本次只创建作品和成员。
- 现有实现调研：`explore.md`。
- 关键约束：CQT portal 的 HTTP 状态恒为 200；业务 code 在 body；学校身份从 portal token 获取；Excel/ZIP 只能读取本服务存储对象；已发布 Flyway 迁移不可修改；新导入任务必须可恢复且重复执行安全。

## 宪法对照

| 原则 | 判定 | 说明 |
|---|---|---|
| CP-1 领域层不依赖框架 | ☑ | 文件、行、任务和导入校验规则在纯 Java 领域/应用契约表达，不把 Spring 或 POI 类型放进 domain/api |
| CP-2 依赖方向单向向内 | ☑ | adapter 调 application，application 编排 domain 端口，infrastructure 实现仓储与文件读取端口 |
| CP-3 持久化类型不跨层 | ☑ | MyBatis DO、Mapper 仅留 infrastructure，任务查询以业务 DTO/领域对象跨层 |
| CP-4 版本号只有一个来源 | ☑ | Apache POI 如被采用，只在下游 `biz-dependencies.gradle.kts` 登记版本，模块不写版本号 |
| CP-5 质量规则只在 build-logic 里配置 | ☐ | 不改构建质量规则 |
| CP-6 豁免必须最小且带理由 | ☐ | 设计不需要质量规则豁免 |
| CP-7 表结构只经 Flyway 迁移变更，已发布的迁移永不修改 | ☑ | 任务/行结果及 `attachment_type=FILE` 使用追加 CQT Flyway 迁移，不改既有脚本 |
| CP-8 密码只用 BCrypt，令牌吊销只走 token_version | ☐ | 不改密码或令牌生命周期 |
| CP-9 凭据不进版本库、不进日志 | ☑ | 不新增凭据；原始行、证件号、电话、token 不写日志 |
| CP-10 认证失败不泄露账号存在性 | ☐ | 不改认证失败语义 |
| CP-11 错误码归属决定 HTTP 状态，不靠 Controller 判断 | ☑ | 复用 CQT 业务错误码与 portal exception advice，不在 Controller 手工构造错误包络 |
| CP-12 依赖只能业务 → 基座 → 框架，反向与同层横向禁止 | ☑ | 只扩展既有 `weiran-cqt`，不新增业务模块或跨模块依赖 |
| CP-13 业务只经基座的 weiran-system-api 接触基座 | ☑ | 学校身份通过现有 CQT portal account 查询/上下文，不直接读取基座持久化表 |
| CP-14 错误码按号段分配，业务不得占用框架与基座的号段 | ☑ | 优先复用通用错误；如确需新业务码，使用 CQT 已登记的 20–39 序号段并先核对空闲码 |
| CP-15 权限码、菜单 id 与 Flyway 模块段按层隔离 | ☑ | 无后台菜单或按钮权限；任务迁移放 CQT 目录，表名使用 `cqt_` 前缀 |

## Architecture

学校在 uniapp 选择赛事并上传 Excel/ZIP。Adapter 校验当前账号为学校类型，登记上传会话并创建持久任务；任务 worker 领取待处理任务，借助受限文件端口读取本服务对象，解析 Excel 表头与连续成员行，逐作品组调用导入用例。每组在一个事务中校验并写入作品、人员、参赛成员、唯一键及既有报名模型要求的作品目标关联；导入过程不创建省赛/国赛评审或奖项记录。任务列表和详情按当前学校归属过滤；错误行和进度从数据库读取，因此可在进程重启后恢复。

解析、ZIP 检查和业务写入之间不传递 MyBatis 类型或 Spring 类型。Excel 行模型保留必要的导入字段用于结果回显，但省赛/国赛奖项列不进入行模型、资格判定或持久化。

## Data Flow

1. uniapp 通过现有文件上传接口保存 Excel/ZIP；随后调用上传登记接口。服务端校验文件 URL 属于当前存储后端，生成与学校账号绑定的导入会话。
2. 确认导入后创建持久任务并立即返回任务/赛事标识，worker 可按状态和租约领取任务。重复确认命中同一会话时返回同一任务。
3. 解析器按工作表「报名表」表头名称读取数据，跳过空行。附件列为链接时保留链接；附件列为文件时以规范化文件名在 ZIP 条目中唯一匹配，并将匹配文件抽取到当前学校账号独立的受限存储对象，再把该对象 URL 关联到对应作品。读取 ZIP 时不解压到任意路径，限制压缩条目数、单条和累计展开大小，并拒绝绝对路径、`..` 及重复冲突文件名。
4. 个人行各生成一个作品组。团体行根据申报人数 N 消费接下来的 N 条有效成员行；组内赛项、专业、作品名、人数不一致或行数不足时记录组错误，不创建半成品，并继续处理后续可识别组。任务结果为每条 Excel 源数据行保留一条记录；同组成员共享该作品组的成功/失败状态和作品 ID，行回显保留各自成员字段。
5. 每组使用事务完成报名规则校验和作品/成员/唯一键及既有作品目标关联写入；唯一键冲突记为该组失败，不创建省赛/国赛评审或奖项记录。作品 `import_fingerprint` 或导入行唯一关联保证重试不会重复创建。任务更新处理行数、成功/失败数和最终状态。Excel 上传会话创建后两小时失效；未确认的会话标记失败并从待处理检查中排除，确认和 Excel 更新均拒绝已过期会话。
6. 任务列表/详情接口只查当前学校账号拥有的任务；作品列表只返回当前学校导入的作品。响应适配 uniapp 实际读取的字段结构。

## 跨模块契约变更(DS-1)

| 对象 | 文件 | 动作 | 消费方 |
|---|---|---|---|
| 业务接口契约 | `weiran4j/docs/01-架构与接口契约.md` §2.2 下游 CQT 扩展 | 追加 portal 导入 API、任务数据与模板字段规则 | uniapp、CQT adapter |
| 共享框架契约 | `weiran-framework` API | 不变 | 不适用 |

## API Design(DS-2)

所有接口保持 `/api-web/**`、HTTP 200 与 portal 统一响应。请求中携带的 `userid` 不作为授权来源。

| 路由 | 方法 | 所属模块 | 请求关键字段 | 返回关键字段 | 权限点 |
|---|---|---|---|---|---|
| `/api-web/auth/getexinfo` | GET | CQT portal | 当前身份 | 模板下载 URL | 已登录学校账号 |
| `/api-web/competition/checksignup` | GET | CQT portal | `competitionid` | 当前赛事待处理/重复状态 | 已登录学校账号 |
| `/api-web/competcategory/schoolproductlists` | GET | CQT portal | 当前身份、赛事筛选参数 | 本校作品列表 | 已登录学校账号 |
| `/api-web/dramacompetition/uploadAttachmentZiposs` | POST | CQT portal | `oss_zip_url` | `session_id` | 已登录学校账号 |
| `/api-web/dramacompetition/uploadexceloss` | POST | CQT portal | `session_id`,`oss_excel_url` | `excel_id` | 已登录学校账号 |
| `/api-web/dramacompetition/confirmImportoss` | POST | CQT portal | `type`,`competitionid`,`session_id`,`excel_id`,`zip_name`,`excel_name` | `message`,`competitionid` | 已登录学校账号；忽略 `userid` |
| `/api-web/competition/getTaskResult` | POST | CQT portal | `competitionid` | `data` 任务列表 | 已登录学校账号，仅本校 |
| `/api-web/competition/getTaskDetail` | GET | CQT portal | `Competitionid`,`id` | 任务统计与 `results[]` | 已登录学校账号，仅本校 |

- uniapp `main.js` 将页面 `/api/...` 映射为 `/api-web/...`；Controller 依照现有 portal 响应和认证约定实现。
- 不实现未被页面调用的 `job_id` 进度轮询接口。
- 学校作品列表不返回省赛/国赛奖项数据；导入详情不显示、校验或保存奖项字段。

## Database Design(DS-3)

| 表 | 动作 | 关键字段 | 索引 | 备注 |
|---|---|---|---|---|
| `cqt_school_import_tasks` | 新增/追加 | 学校账号、赛事、上传会话、状态、总行数、已处理数、成功数、失败数、上传过期时间、租约/重试时间 | 会话唯一键；学校+赛事+创建时间；状态+可领取时间；上传状态+过期时间 | 承载持久任务与可恢复 worker；过期时间由追加迁移增加 |
| `cqt_school_import_rows` | 新增 | task_id、源行号、作品组起止行、处理状态、作品 ID、错误摘要、必要的逐行结果回显字段 | task_id+行号唯一；task_id+状态 | 每个源数据行一条结果；不存省赛/国赛奖项列；敏感原始列只在授权任务详情按需回显 |

- 表结构变化仅通过 CQT Flyway 追加迁移；除新建任务/行表外，另追加迁移将作品附件 enum 扩展为 `ZIP`、`LINK`、`FILE`，不修改已存在的作品迁移。
- 每个作品组的作品、成员与行结果在同一事务更新；任务领取使用可恢复租约，超时可再次领取。
- 使用现有 `cqt_entries.import_fingerprint` 唯一索引或等价的稳定导入指纹抵御重放；不得以客户端字段做幂等依据。

## 权限与已知缺口(DS-4)

| 项 | 设计 |
|---|---|
| 权限点 | 无后台 RBAC 权限码；每个 portal 接口检查登录状态和学校账号类型，所有查询附带当前账号/学校过滤条件 |
| 菜单挂载 | 不新增后台菜单或管理页面 |
| 操作审计 | 记录任务创建、开始、结束及计数等业务事件；不记录 Excel 原始行、证件号、电话、令牌或文件内容 |
| 幂等 | 同一学校上传会话最多创建一个任务；逐作品组稳定指纹与事务唯一约束保障重试安全 |
| 数据范围 | 任务、行结果和作品列表均按认证学校隔离；请求体 `userid` 不参与数据过滤 |
| 任务中心 | 仅建设 CQT 专用导入任务，不扩展全局任务中心 |

## 分层与装配(DS-5)

| 项 | 设计 |
|---|---|
| 依赖方向 | `adapter → application → domain`，`infrastructure → domain`；不新增 Gradle 模块 |
| 文件读取端口 | 领域/应用定义受限对象读取能力，只接受经 `FileStorage.isStoredUrl` 验证的本服务 URL；Local/OSS 实现不得接受任意 URL |
| 解析器 | Excel/ZIP 解析放在 application 适配边界；若使用 Apache POI，POI 类型不出现在领域和 API 契约中 |
| 存储 | infrastructure 实现任务、行结果仓储及文件读取；MyBatis DO/Mapper 不跨 infrastructure |
| worker | CQT 模块自有定时/异步 worker 扫描可领取任务，采用数据库租约与行级/组级幂等恢复；不依赖进程内唯一状态 |
| 装配 | 沿用 CQT 既有 `@AutoConfiguration` 与 imports；不在 `weiran-app` 添加业务装配代码 |

## 前端设计(DS-6)

| 项 | 内容 |
|---|---|
| 页面/路由 | 复用 `uniapp/pages/my/my.vue` 既有学校导入界面；不新增后台 web 页面 |
| 菜单挂载 | 不适用 |
| 数据请求方式 | 保持现有 uniapp 请求路径和请求方法，由 `main.js` 映射到 `/api-web/**` |
| 结果显示 | 任务列表/详情适配当前字段；成功/失败行显示学校列与联系电话列的正确表头映射 |
| 奖项 | 导入预览、校验、作品列表和任务结果不处理省赛/国赛评价字段 |

## Observability

- 日志关键字段：任务 ID、赛事 ID、处理数量、状态与耗时；不记录成员姓名、证件号、手机号、附件内容或令牌。
- 指标：任务待处理数、处理时长、成功/失败组数、worker 重试次数。
- 审计：记录学校账号 ID 与任务事件，不记录 Excel 行数据；任务详情按学校归属鉴权。
- 告警/排障入口：长期租约超时任务及连续失败任务可从任务详情定位源行号和脱敏错误摘要。

## Test Plan

| 层级 | 范围 | 命令/判据 |
|---|---|---|
| Domain/application | 模板字段映射、个人/团体分组、人数不符、组内字段不符、奖项列忽略、附件匹配及组失败后继续 | JDK 21 `./gradlew :weiran-cqt:weiran-cqt-application:test` 与 domain 测试 |
| Infrastructure | 本地/OSS 受限读取、ZIP 路径/大小限制、任务租约恢复与幂等 | 对应 CQT infrastructure 测试 |
| Portal 集成 | uniapp 实际接口路径/结构、学校账号权限、任务所有权、重复确认、断点恢复、作品成员查库 | `weiran-app` CQT 集成测试 |
| uniapp | 页面模板列与请求兼容 | `pnpm --filter @weiran/uniapp build` |
| 全量门禁 | Java 格式与完整质量检查 | 先 `./gradlew spotlessApply`，再 `./gradlew check`（JDK 21） |

## Rollout Plan

1. 发布追加式 CQT 迁移和后端接口；迁移只新增导入任务/行表。
2. 发布 uniapp H5 并用学校测试账号验证模板下载、少量 Excel/ZIP 导入、任务结果和学校隔离。
3. 生产启用前确认文件存储与反向代理体积/超时配置足以承载现有上传上限；按实际模板逐组核对成功和失败行。

## Rollback Plan

1. revert 锚点：本 change 对应的部署版本或回滚提交；回退前停止导入 worker，避免新旧版本同时领取任务。
2. 数据迁移策略：新增表不做破坏性回滚；已创建的作品保留，任务表保留供审计和排障，不删除或回滚业务作品。
3. 回退应用后关闭 uniapp 导入入口或恢复旧前端；不回滚/删除已经写入的作品和成员。

## Open Questions

- 无；作品导入阶段不涉及省赛/国赛奖项评价。
