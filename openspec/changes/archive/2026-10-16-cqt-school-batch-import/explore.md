---
title: "CQT 学校批量导入代码现状调研"
status: "draft"
updated_at: "2026-10-16"
---

# Explore

## 调研范围

| 模块 | 目录/文件 | 为什么看它 |
|---|---|---|
| `weiran-framework` | portal 响应与异常机制（由 CQT portal 入口复用） | 确认前台 HTTP/body 错误口径；本次不改框架 |
| `weiran-cqt` | portal adapter、entry domain/application/infrastructure、file storage、CQT 迁移 | 学校导入接口、作品写入、异步任务与文件读取落在此业务模块 |
| `uniapp` | `pages/my/my.vue`、`main.js`、`util/upload.js` | 对齐现有页面正在调用的请求和上传流程 |
| `web` | 无 | 本 change 不增加后台管理页 |

## 现有实现

| 能力 | 位置 | 现状 |
|---|---|---|
| uniapp API 路径改写 | `uniapp/main.js:106-150` | 页面旧路径 `/api/...` 自动改为 `/api-web/...`；保持页面现有 URL 即可接 CQT portal controller |
| Excel/ZIP 文件上传 | `uniapp/util/upload.js:1-38` | 文件先传 `/api-web/local-files/upload`，得到本服务存储 URL；当前组件名 `uploadOss` 已不代表实际 OSS 直传 |
| 模板与防重复检查 | `uniapp/pages/my/my.vue:1645-1652,2097-2113` | 调用 `/api/auth/getexinfo`、`/api/competition/checksignup`，后端无对应 controller |
| 学校作品列表 | `uniapp/pages/my/my.vue:1934-1950,2129-2138` | 调用 `/api/competcategory/schoolproductlists`，数据被列表和获奖区域复用；本次只返回作品，不实现奖项评价 |
| 上传登记与确认 | `uniapp/pages/my/my.vue:1813-1851,2578-2670` | ZIP/Excel 上传后分别登记 URL，再由 `confirmImportoss` 确认；请求含客户端 `userid`，不得信任该值作为身份 |
| 任务列表与明细 | `uniapp/pages/my/my.vue:2429-2450,2507-2530` | `getTaskResult` 期待 `data.data` 数组；`getTaskDetail` 期待 `results[]` 及逐行成功/失败字段 |
| 不可达进度代码 | `uniapp/pages/my/my.vue:1366-1370,2144-2172,2261-2290` | `job_id` 初始化为空且未赋值，没有调用轮询方法的入口；不作为当前实际请求契约 |
| 现有报名写入 | `weiran4j/weiran-cqt/weiran-cqt-application/src/main/java/com/weiran/cqt/application/entry/SignupApplicationService.java:79-185` | 个人/团体校验、赛事窗口、附件、组别、参赛唯一键及事务写入集中在此；但入口按当前账号成员构建，不能直接拿来批量导入任意学生 |
| 报名端口 | `weiran4j/weiran-cqt/weiran-cqt-domain/src/main/java/com/weiran/cqt/domain/entry/EntryRepository.java:10-68` | 支持作品、参赛人、唯一键和评审对象写入；没有批量导入幂等指纹或学校作品查询端口 |
| 我的作品列表 | `weiran4j/weiran-cqt/weiran-cqt-application/src/main/java/com/weiran/cqt/application/entry/MyEntriesApplicationService.java:25-64` | 只按当前账号作为参赛人查询；学校批导需要按来源学校账号关联的作品列表 |
| 文件存储 | `weiran4j/weiran-cqt/weiran-cqt-domain/src/main/java/com/weiran/cqt/domain/file/FileStorage.java:7-26`、`weiran4j/weiran-cqt/weiran-cqt-adapter/src/main/java/com/weiran/cqt/adapter/portal/LocalFilesController.java:11-40` | 目前只定义写入与 URL 归属判定，无安全读取已上传 Excel/ZIP 的端口 |
| 表结构 | `weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/resources/db/migration/cqt/V202610051001__cqt_entries.sql:1-107` | 已有作品、人员、参赛人、唯一键、评审对象与序列；作品含 `import_fingerprint`，可用于幂等写入；无导入任务表 |
| 需求来源 | `openspec/changes/archive/2026-10-04-cqt-signup/proposal.md:34-43` | 学校批量导入、异步任务及国赛作品提交被明确留作后续切片 |

## 可复用点

| 可复用对象 | 位置 | 复用方式 | 需要改造吗 |
|---|---|---|---|
| Portal 认证与响应 | `weiran-cqt-adapter/.../portal/{PortalController,PortalAccount,PortalResult}.java` | 复用 HTTP 200 + `{code,message,data}`、从 token 解析账号、`@PortalController` | 否；所有接口强制学校账号检查 |
| 赛事、赛项、组别查询 | `weiran-cqt-domain/.../competition/CompetitionRepository.java` | 查询赛事窗口、赛项/专业有效性、配置组别并核对 Excel 值 | 是：导入服务需按表头名称和赛事配置映射 |
| 参赛重复约束与作品表 | `SignupApplicationService`、`EntryRepository`、`cqt_entries` | 沿用参与键规则与写入模型，设置 `import_fingerprint` 保证任务重试幂等 | 是：新增学校批量导入写入用例，不直接循环调用需要个人账号参与人的 `signup()` |
| 文件上传 | `FileUploadService` / `FileStorage` | 继续使用现有上传接口，登记本服务存储 URL | 是：增加受限读取能力；URL 必须先通过 `isStoredUrl`，不允许任意外链抓取 |
| 导入模板 | `openspec/state/files/00-【报名示例】报名表-第二届“常青藤”全国青少年校园戏.xlsx` | 工作表「报名表」第二行为 22 列表头；按列名映射，数据行空白需跳过 | 需新增/配置可部署的模板下载资源；参考文件含示例数据，不能把样例成员数据写入业务数据 |

## 真实约束

| 约束 | 证据位置 | 对设计的影响 |
|---|---|---|
| CQT 仅有一个业务模块，报名/导入放 `match` 领域子包 | `AGENTS.biz.md:12-15` | 新增代码必须放 `weiran-cqt` 五层，不能另起业务模块 |
| portal 接口必须在 `/api-web/**`，未登录 body `code:401`，HTTP 保持 2xx | `AGENTS.biz.md:16-33` | controller 用 `@PortalController`，出错抛 `BizException`，不手写响应码 |
| 学校账号身份由当前 portal token 确定，数据库字段含学校编号/名称 | `Account.java:17-50`、`PortalAccount` | 忽略请求体 `userid`；任务、上传会话和作品查询按账号/学校授权 |
| 报名唯一键是赛事、一级赛项、证件类型、规范化证件号、阶段 | `V202610051001__cqt_entries.sql:88-104` | 同一作品/团体成员以事务处理；重复参与转成逐作品错误，不能绕过唯一约束 |
| `FileStorage` 无读取方法 | `FileStorage.java:7-26` | 必须为异步 worker 增加存储读取端口或等价受控机制，并为本地/OSS 实现测试 |
| 当前实现无导入任务表、worker 或重启恢复 | CQT 全模块检索；`cqt_entries.md#03` | 持久化任务与行明细；避免单纯进程内 `@Async` 导致重启丢失状态 |
| 导入表没有菜单需求；Flyway 版本全仓唯一 | `AGENTS.biz.md:34-35`、`openspec/rules/enforced/project.md:99-105` | 新增 CQT Flyway 迁移前先搜索版本号；不新增 `sys_menu` 资源 |
| Excel 模板手机号在 H 列、学校在 G 列；附件在 O 列 | 用户提供的 Excel；表头 `B2:V2` | 使用表头而非硬编码索引；链接行直接用 URL，文件行在 ZIP 中匹配名称；不读取 U/V 奖项列 |

## 影响面

### 普通改动

| 文件/模块 | 动作 |
|---|---|
| `weiran4j/docs/01-架构与接口契约.md` | 新增 portal API、任务表及验证规则契约 |
| `weiran4j/weiran-cqt/weiran-cqt-{api,domain,application,infrastructure,adapter}` | 新增导入端口、解析/校验、任务持久化、可恢复 worker、portal endpoints 与 migration |
| `weiran4j/weiran-app/src/test/java/com/weiran/app/Cqt*IT.java` | 覆盖上传登记、导入任务、逐行结果、任务归属及重试幂等 |
| `uniapp/pages/my/my.vue` | 修复结果弹窗联系方式/学校列错位；必要时对齐实际任务字段 |
| `openspec/state/bizs/cqt_entries.md` | 认领并关闭 `#03`；同步新增数据表和动作说明 |
| `weiran4j/weiran-dependencies/biz-dependencies.gradle.kts`（若选用 Apache POI） | 仅在确认增加 Excel 解析依赖时登记下游版本 |

### 共享层命中 ⚠️

| ID | 文件/资源 | 冲突原因 | 是否命中 · 预计改动 |
|---|---|---|---|
| SL-1 | `settings.gradle.kts` | 非业务模块注册单点；CQT 已存在 | 未命中 |
| SL-2 | `weiran-dependencies/build.gradle.kts` | 上游框架 BOM/约束单点；业务依赖走下游独占 BOM | 未命中（若新增 POI，只改 `biz-dependencies.gradle.kts`） |
| SL-3 | `weiran-app/build.gradle.kts` | 应用聚合依赖单点；新增 CQT 层不需改它 | 未命中 |
| SL-4 | `db/migration/cqt/**` | 全仓 Flyway 版本及菜单 id 为序号资源 | 命中：只追加一份 CQT 导入任务迁移；不新增菜单行 |
| SL-5 | `web/src/pages/**` / `page-registry` | 管理后台页面必须与菜单种子配对 | 未命中：无后台页面 |
| SL-6 | `CommonErrors.java` | 跨模块错误码基座刻意冻结 | 未命中：使用 CQT 专属错误码或通用错误码 |
| SL-7 | `weiran-framework/{page,response}` | 全仓共享包络 | 未命中：仅消费现有 Portal 响应契约 |

#### 序号型资源

| 资源 | 是否命中 | 处理 |
|---|---|---|
| Flyway migration version | 是 | 创建前搜索全仓最大/占用版本，使用新的分钟时间戳；已合入脚本不改 |
| `sys_menu.id` | 否 | C 端学校入口，不增加后台菜单 |
| CQT 错误码 `20–39` | 待实现判断 | 能复用现有 `CommonErrors` 时不加新码；确需前端区分才选空闲 CQT 码 |

## 对 interview 的反向修正

| 原结论 | 现实情况 | 修正后 | 是否已同步回 interview.md |
|---|---|---|---|
| 批量导入包含省赛一等奖资格校验 | CQT 当前无奖项查询能力；用户明确省/国赛奖项属于作品之后的评价 | 不做奖项读写或资格校验，模板 U/V 列忽略 | ☑ |
| uniapp `uploadOss` 代表 OSS 直传 | 实际实现调用本服务 `/api-web/local-files/upload` | 后端从本地存储服务读取上传对象，不接收任意外部 URL | ☑ |
| UI 轮询进度/结果接口当前可用 | `job_id` 从未赋值，轮询无调用入口 | 本次以已调用的任务列表/详情接口提供状态与行结果；死代码轮询路径不列为调用契约 | ☑ |

## 风险预警

| 风险 | 触发条件 | 建议应对 |
|---|---|---|
| 任务进程崩溃导致处理中状态滞留 | 应用在处理中重启 | 用数据库可抢占/租约任务机制，worker 定时恢复超时任务，行级写入幂等 |
| ZIP 炸弹或目录穿越 | 上传压缩包有极端展开比、超多条目、`../` 路径 | 限制压缩包字节、条目数、单文件与总展开大小；规范化条目名并拒绝路径越界 |
| 同一身份同时出现在多个作品组 | 多个行组重复证件号 | 每个作品成员写入受唯一键保护；重复行记逐作品失败，其他作品继续 |
| 学校名称与认证账号资料不一致 | 上传者修改模板学校列 | 对照已认证学校名/编号；账号本身无法提供可核验学校时不允许导入 |

## Gate

- [x] 共享层命中已完整列出
- [x] 与 interview 的冲突项已回写 `interview.md`
- [x] 可复用点已确认，避免重复造轮子
