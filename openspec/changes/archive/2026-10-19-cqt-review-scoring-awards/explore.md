---
title: "后台评审、评分与评奖现实校验"
status: "draft"
updated_at: "2026-10-16"
---

# Explore

## 调研范围

当前仅产出 L1 文档，不编写业务代码、tasks 或测试，不把未运行验证记为通过。已读取 interview.md，未决歧义为无；用户已确认独立综合分、平均汇总、区间评奖无名额，以及中戏作品级、渠道/后台参赛人级评分。

扫描顺序为模块目录与迁移目录清单 → rg 定位符号 → 下列十个文件定点精读 → 最后反查确定改动的符号。旧项目资料属于只读参考，不是本仓可部署实现；治理文件不计入十个业务源码精读点。

| 模块 | 目录 | 调研目的 |
|---|---|---|
| weiran-framework | auth/error/page/persistence | 复用认证、权限、统一错误与审计，保持框架不改 |
| weiran-system-api | user | 复用后台用户的查询与启用选项，不跨层读取 sys_user |
| weiran-cqt 五层 | entry/competition、autoconfigure、迁移目录 | 识别已有作品/评审对象及评审评分评奖空缺 |
| web | pages/cqt、hooks、动态菜单解析 | 复用列表、页签及表单协议，菜单配对注册 |
| 旧项目 | fastapi_backend/app/routes/review.py、awards.py | 对照用户确认的实际规则，识别不迁入的隐式行为 |

## 现有实现

以下路径中的 `CQT` 代表 `weiran4j/weiran-cqt`；`LEGACY` 代表 `/Users/duoli/Projects/qidian-zhongxi/常青藤20260929/fastapi_backend/app/routes`。

| 精读序号 | 位置（file:line） | 现状与证据 |
|---|---|---|
| 1 | `CQT/weiran-cqt-infrastructure/src/main/resources/db/migration/cqt/V202610051001__cqt_entries.sql:5,31,106–115` | 已有作品、人员、参赛人和评审对象；scoring_scope 与 target_type 均支持 ENTRY/PARTICIPANT；作品级唯一约束与人员级唯一约束已具备。没有评分、汇总、评奖表 |
| 2 | `CQT/weiran-cqt-infrastructure/src/main/java/com/weiran/cqt/infrastructure/persistence/MybatisEntryRepository.java:126–130,205–211` | 创建作品固定 scoring_scope=ENTRY；仅实现 insertEntryTarget；作品来源由现有写入约定固定为 zhongxi，不等同于报名账号来源 |
| 3 | `CQT/weiran-cqt-application/src/main/java/com/weiran/cqt/application/entry/SignupApplicationService.java:165–181` | 人员来源使用 account.getSourceDatabase，最终无条件创建作品级对象；不能以某一成员来源反推整个团体的作品来源 |
| 4 | `CQT/weiran-cqt-application/src/main/java/com/weiran/cqt/application/entry/SchoolImportEntryWriter.java:155–189` | 学校批量导入复用同一作品与参赛人端口，末尾也只创建 ENTRY target；新能力必须覆盖该入口而不破坏指纹幂等 |
| 5 | `CQT/weiran-cqt-domain/src/main/java/com/weiran/cqt/domain/entry/EntryRepository.java:18–24,38–62,64–77` | 仓储端口已有作品/参赛人/唯一键及归属查询；需要新增人员对象准备与初审操作端口，不能把 Mapper 泄露给应用层 |
| 6 | `weiran4j/weiran-system/weiran-system-api/src/main/java/com/weiran/system/api/user/UserService.java:11–14,35–36` | get/page/options 可读取用户与启用选项；评委账号继续复用后台用户，不另存评委密码、不跨层 JOIN sys_user |
| 7 | `CQT/weiran-cqt-adapter/src/main/java/com/weiran/cqt/adapter/web/NewsAdminController.java:49–91` | 后台 CRUD 已有 RequiresPermission/OperationLog 模式，错误使用 BizException；可作为控制器接入范式 |
| 8 | `web/src/hooks/usePageTabs.ts:4–16` | closeCurrentAndGo 绑定发起页面 pathname，可用于评分详情独立页签及成功保存后返回/刷新任务列表 |
| 9 | `LEGACY/review.py:20–39,41–170,171–355,357–550,589–675` | 旧阶段、维度、范围、分配、本人评分、平均汇总、修订快照与晋级实现；综合分独立，维度评语必填，平均 HALF_UP 两位。自动分配缺人静默跳过；汇总隐式同步奖项不应迁入 |
| 10 | `LEGACY/awards.py:37–64,66–210,362` | 区间规则、预览、批次确认与撤销；quota 实际固定 null。存在更新同一 awards 行可能串扰旧批次的风险，新设计需快照及批次隔离 |

结构扫描与符号索引确认：当前 Java 没有 review/award 的业务实现；后台无初审、评委、评分、汇总、评奖页面。现状文件 `openspec/state/bizs/cqt_entries.md` 的 #02 明确省/国奖恒为空；`cqt_competitions.md` 的 #02 明确赛事尚无后台管理。已有成绩查询草案已撤回，未新增对应表或接口。

## 可复用点

| 可复用对象 | 位置 | 复用方式 | 是否改造 |
|---|---|---|---|
| 作品/成员/评审对象 | 上表精读 1–5 | 复用现有主键、来源三元身份、目标唯一键和导入指纹；增加准备/初审用例 | 是，不能再假定全为 ENTRY |
| 赛事/赛项/组别/阶段范围 | `CQT` 既有 competition 子域，迁移 `V202610051000__cqt_competitions.sql` | 用现有赛事和赛项身份，复用 BOTH/PROVINCIAL/NATIONAL 作品阶段约束 | 扩展评审配置，不新建 Gradle 模块 |
| 后台身份与用户查询 | 上表精读 6–7 | CurrentUser、RequiresPermission、OperationLog；用户只经 system-api 获取 | 否；业务仅保存 userId 及必要展示快照 |
| 标准列表能力 | `openspec/rules/advisory/components.md` 与 components.biz.md 的 PageContainer/SearchToolbar/ColumnSettings/TableActions/useTableDefaults | 搜索/重置图标、列设置、刷新、权限过滤 | 直接复用，不改通用组件协议 |
| 页签机制 | 上表精读 8 | 菜单驱动的详情页、保存后返回并刷新 | 直接复用，不硬编码 App 路由 |
| 统一分页/错误/空安全 | framework 纯 Java 契约及当前 JSpecify 包约定 | PageQuery/PageResult、BizException、NullMarked/Nullable | 直接复用，业务错误优先通用码 |

## 真实约束

| 约束 | 证据 | 对设计的影响 |
|---|---|---|
| 业务只能在 weiran-cqt 内扩展 | AGENTS.biz.md；constitution CP-12/13 | review/award 子域沿用五层，不能新增业务模块；不引用 system-domain/infrastructure |
| 学校导入和普通报名都仅创建 ENTRY | 精读 2–4 | 新增“准备评审对象”幂等对账，不能只修页面显示或只覆盖一个报名入口 |
| 人员来源不等于作品来源 | 精读 2–3 | 最小不新增客户端字段方案：新普通报名及学校导入均由服务端当前账号的sourceDatabase决定作品source_database与scoring_scope，中戏ENTRY、其它PARTICIPANT并创建匹配对象；不从成员反推。历史作品以其已存source_database为权威补对象。需明确MODIFIED cqt-entry-signup FR005并同步writer/契约/state，不把它当成当前已有实现 |
| 已存在历史对象与未来评分引用 | 精读 1 的唯一键及历史导入现状 | 无评分/任务引用时可幂等修正对象；已有评分或批次引用时禁止自动转换，显示阻断原因交管理员复核，防止 ENTRY/PARTICIPANT 双计 |
| 综合分不是维度求和 | 精读 9 | 维度分与评语留档，汇总只取有效评委综合分，BigDecimal 平均 HALF_UP 两位 |
| 旧自动分配静默缺人 | 精读 9 | 设计需预览并显式返回范围不匹配、缺人、重复和已提交不可取消的原因 |
| 旧算分直接改奖 | 精读 9–10 | 汇总与评奖分离；评分变更使相关汇总/批次失效待复核，不能静默覆盖确认奖项 |
| 单库多届 | cqt_competitions.md；AGENTS.biz.md | 每次写入核对 phase、target、entry 属于同一赛事；不得硬编码 phase_id=2/3或靠旧数据库名分流 |
| 权限不等于数据范围 | 精读 9 归属校验；CP-13 | 评委每次读写校验自己的有效 assignment、scope、用户可用状态和阶段开放状态；管理员纠错独立权限 |
| 当前没有完整赛事后台 | cqt_competitions.md#02 | 本切片提供评审所需赛事选择和阶段配置，不顺带重写全量报名/赛项/赛事管理 |

## 影响面

### 普通改动

| 文件/模块 | 预计动作 |
|---|---|
| `weiran-cqt-api/.../review,award` | 新增初审、阶段、维度、范围、分配、本人评分、汇总复核、晋级、规则与批次契约 |
| `weiran-cqt-domain/.../review,award` | 新增纯领域规则与仓储端口；补评审对象准备能力 |
| `weiran-cqt-application/.../review,award` | 事务用例、权限之外的数据归属校验、版本与状态校验 |
| `weiran-cqt-infrastructure/.../persistence` | 新增实体/Mapper/仓储；扩展现有作品初审和对象准备持久化 |
| `weiran-cqt-adapter/.../web` | 新增后台接口与日志，不新增本次前台公开查询 |
| `weiran-cqt-{application,infrastructure,adapter}/.../autoconfigure` | 自注册新增服务、仓储与控制器，多个单元会读写，需先冻结清单并串行合入 |
| `web/src/pages/cqt/**`、`hooks/queries/cqt-*.ts`、`types/api.ts` | 新增后台页面/查询hooks/类型，使用既有列表和独立页签能力 |
| `weiran4j/docs/01-架构与接口契约.md`、`openspec/specs/**` | 先定接口/字段/权限/状态协议再实现；属于多单元共享输入，先串行冻结 |
| `openspec/state/bizs/cqt_*.md`、`README.biz.md` | 新表逐表现状文档及索引，更新 entries 已知问题状态须以实际实现为准 |

### 共享层命中

以下按 project.md 当前真实 SL 清单逐行判定；CLI 模板的旧 App 静态注册、PHP 与无 Flyway 叙述不适用于本仓，不修改模板本身。

#### 桶文件 / 注册表

| ID | 文件 | 冲突原因 | 是否命中 · 预计改动 |
|---|---|---|---|
| SL-1 | `weiran4j/settings.gradle.kts` | 模块发现为全局入口 | 未命中。五层既有业务模块内加子域，不改 settings，目录自动发现事实保持 |
| SL-2 | `weiran4j/weiran-dependencies/build.gradle.kts` | BOM版本/模块坐标唯一来源 | 未命中。使用现有 Spring/MyBatis/Java BigDecimal，不新增依赖或版本 |
| SL-3 | `weiran4j/weiran-app/build.gradle.kts` | 应用依赖聚合全局入口 | 未命中。应用已依赖 weiran-cqt，不增加 Gradle 模块或依赖；后续验证如需下游IT仅新增Cqt测试文件 |
| SL-4 | `weiran-cqt-infrastructure/.../db/migration/cqt/` 中 sys_menu 种子 | 菜单ID、权限及迁移版本号全局唯一，重复号可能不造成文本冲突 | 命中。新增评审/评分/评奖菜单及隐藏详情路由、按钮权限和角色绑定，全部串行 Layer 0 冻结 |
| SL-5 | `web/src/pages/cqt/**` 与 `web/src/utils/page-registry.ts` 的 component 协议 | 菜单component与页面逐字配对 | 命中协议配对。新增业务页并登记菜单component；page-registry代码不改，App.tsx/AdminLayout.tsx不硬注册路由 |

#### 跨模块契约

| ID | 文件 | 冲突原因 | 是否命中 · 预计改动 |
|---|---|---|---|
| SL-6 | `weiran-framework/.../error/CommonErrors.java` | 全局错误码 | 未命中。优先复用400/403/404/409；确需业务区分时仅在已领取20–39号段内登记，先审契约，不改框架码 |
| SL-7 | `weiran-framework`纯Java page/response等契约及 `web/src/types/api.ts` 手动同步 | 多层共同消费，无自动生成 | framework契约未命中修改，继续复用PageQuery/PageResult/ApiResponse；前端api.ts业务类型命中且为Layer 0共享输入，与weiran4j/docs/01一致冻结 |

#### 序号型资源

| 资源 | 当前事实 | 本次处理 |
|---|---|---|
| Flyway版本 | 存在全局时间戳版本，已执行脚本永不修改；out-of-order不消除撞号 | 预留 `V202610170400__cqt_review_scoring_awards.sql` 于cqt迁移目录；这是规划预留，尚未创建，L3后实现前再查重；表/约束/菜单集中串行产出 |
| 菜单ID/权限 | 业务号段1000–1999，已用内容1020–1046；1050查询草案已彻底撤回且未落迁移，不占用编号 | 采用权限稿1050初审、1055评审目录、1060配置等至1126的规划号段，逐页/按钮ID与component由root设计/契约统一登记；实现前再查重，不私自另取号 |
| 评审阶段与对象ID | 新阶段不能沿用旧全局2/3，目标已有主键/唯一键 | 阶段自增并以competition+code唯一；对象准备走已有目标唯一键，不能手编ID或复制旧固定阶段号 |

#### 构建约定与其它共享输入

| 项 | 判定 |
|---|---|
| `weiran4j/build-logic/**` | 未命中，不调整Checkstyle/SpotBugs/Forbidden APIs/覆盖率或编译版本 |
| `weiran-framework/**`与`weiran-system/**` | 未命中修改，直接复用公开API；禁止业务直接读写sys_*和引入反向依赖 |
| `biz-dependencies.gradle.kts`、`turbo.json`、pnpm lockfile | 未命中，不引入新依赖或环境变量 |
| CQT三层AutoConfiguration | 命中，本模块共享注册表，串行 Layer 0 |
| interview、契约、api.ts、对象来源/状态定义 | 两个以上实现单元均会读取，虽非全局构建文件也必须在Layer 0先冻结 |
| L7命令 | JDK21；Java改完先spotlessApply再check，前端lint/build和项目openspec检查按项目要求。当前未获测试执行授权，不能执行或报告测试通过；L3后任务规划须如实记录验证范围 |

### 本次不会碰的目录

- `uniapp/**`：公开查分、证书和前台评审不在本切片。
- `weiran4j/build-logic/**`、`weiran-framework/**`、`weiran-system/**`：不改框架、认证密码、角色权限底座或质量规则。
- 其它独立Gradle模块、部署Docker/CI、根工作区配置与锁文件：不新增依赖或部署形态。
- 既有已执行Flyway脚本、真实本地配置与密钥、生产数据库数据：仅后续追加迁移，不直接评分、颁奖或导入真实数据。
- 既有内容管理/学校导入业务实现：保留本会话已有改动，除共同作品对象准备的必要接点外不重构其流程。
- 兄弟目录旧FastAPI项目：只读规则证据，不修改、不运行迁移、不作为新实现入口。

## 对 interview 的反向修正

| 原结论 | 现实情况 | 修正后 | 是否已同步回 interview.md |
|---|---|---|---|
| 按来源区分ENTRY/PARTICIPANT | 当前新作品固定zhongxi，人员才随账号来源；两个写入入口均仅建ENTRY | 落实已确认规则：新普通报名及学校导入的作品来源取当前账号sourceDatabase，scope/对象随来源生成；历史以已存作品来源为权威幂等补齐，不自动转换已引用对象。同步MODIFIED cqt-entry-signup FR005，不新增客户端来源字段 | 已追加interview真实约束段；确认规则不变，root同步proposal/spec/design |
| 沿用旧评分与评奖规则 | 旧算分隐式改奖、批次可共享可变award行 | 保留用户确认的综合分/平均/区间规则，剥离隐式副作用，使用显式批次确认及失效复核 | 与interview“算分和评奖拆开”一致，无需反向修改 |

## 风险预警

| 风险 | 触发条件 | 应对方向（待设计审批，不等于实施授权） |
|---|---|---|
| 同对象双重计分 | 同作品ENTRY与PARTICIPANT同时ACTIVE | 准备阶段按来源/scoring_scope校验对象集合；有历史引用则阻断修正并给明细 |
| 无声漏分配 | 范围不足、评委禁用、专业/赛区不匹配 | 预览全量覆盖与负载，确认时重验版本，逐项报告原因 |
| 汇总输入漂移 | 评委修改或取消任务与管理员汇总并发 | 规则/评分版本、冻结条件和幂等汇总快照，失效输入显式拒绝 |
| 改分悄悄改奖 | 人工纠错或重算覆盖既有奖项 | 只标记派生批次需复核，重评奖须明确预览/确认，撤销按批次归属 |
| 作用域越权 | 只校验按钮权限、客户端传reviewerId/competitionId | 登录用户决定评委身份；assignment、scope、phase与entry同赛事校验 |
| 规划混入前台查分/证书 | 把旧公开查询顺带搬入 | 本切片仅后台生产结果，公开查询/证书生成另change |
| 共享工作区误覆盖 | 当前含本会话前序未提交改动 | 根据root确认不存在他人change，保留全部改动；同change仅分工写规划文件，不stash、不提交、不并行运行构建 |

## Gate

- [x] 已读取 interview，无未决歧义；现实约束不冒充新增用户决定。
- [x] 共享层SL-1至SL-7、序号资源与构建约定逐行判定。
- [x] 定点精读十个文件，有file:line引用；最后反查insertEntryTarget/insertParticipant、来源及相关调用，覆盖普通报名和学校导入两个入口。
- [x] 可复用点、影响目录与不会碰目录已明确。
- [x] 本文只做L1现实校验，未创建业务代码、tasks或审批字段；后续L3由用户审设计。

## 2026-10-17 导出追加调研

旧Python admin_extended.py的award-update-export.xlsx按entry聚合评分对象，人员级对象被平均合并且缺targetID；新实现不照搬该匹配口径。application已有poi-ooxml，可复用无需加依赖；SchoolImportController已有ResponseEntity<byte[]>附件下载模式。web request强制JSON，业务导出需独立小型二进制下载函数，不改公共request协议。既有ReviewShared可用内部tools扩展接SearchToolbar，新增单独按钮权限无需新页面。
