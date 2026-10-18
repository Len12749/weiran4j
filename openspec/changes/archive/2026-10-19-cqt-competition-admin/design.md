---
title: "赛事管理设计"
status: "approved"
approved_by: "user"
approved_at: "2026-10-17"
approved_date: "2026-10-17"
updated_at: "2026-10-17"
---

# Design

## 目标与审批边界

用户原话为“增加赛事管理”。以下方案依据现状收敛，用户于2026-10-17明确回复“实施”，L3已批准。提供赛事分页、详情、新增、编辑、状态管理；新增编辑使用独立后台Tab。无删除、赛项/组别CRUD、查分、证书生成或自动调度。按已批准方案补齐任务和实施；不新增或运行未单独授权的测试。

## 跨模块契约变更（DS-1）

- 在现有weiran-cqt五层的competition子域扩展管理能力，新增Admin命令/详情/分页类型和领域仓储端口。现有公开CompetitionView及前台接口保持兼容；不扩展framework分页/响应或修改system接口。
- 管理字段：id、legacyId、name、edition、year、description、nationalEntryMode、registrationStart/End、provinceReviewStart/End、nationalReviewStart/End、resultPublishTime、provinceCertificateTemplate、nationalCertificateTemplate、teacherOrgCertificateTemplate、status、version、createdAt/updatedAt。
- id由数据库生成且不可改；legacyId仅展示，后台新建固定null。nationalEntryMode创建时选择SHARED/SEPARATE，创建后不可改变；编辑传入不同值返回409，不转换既有作品或目标。
- 名称trim后1–300字符；届次trim后1–50；年份1900–9999必填。说明可空，按TEXT字节容量在应用边界限制UTF-8不超过65535字节。时间按本地业务时区ISO日期时间传输，每对开始/结束均可空，齐备时开始不得晚于结束；resultPublishTime可空，不强加跨阶段先后顺序。
- 三模板字段为可空URL，长度不超过1000；非空仅允许绝对http/https URL，禁止userinfo、控制字符和携带凭据的地址，不由后端抓取。模板只保存配置，不生成证书。
- 省国评审时间和公布时间仅为计划信息：不自动开放/关闭phase，不触发成绩发布。报名继续只依赖status=1和registration时间窗。

## API Design（DS-2）

所有接口前缀`/api/cqt/competitions`，属于weiran-cqt-adapter；统一成功`{code:0,message,data}`，分页`{list,total,page,pageSize}`。输入空body、非法枚举/时间/URL/长度返回400，不存在404，版本/模式/唯一冲突409；由全局异常处理器输出，不在Controller手写状态。

| 路由 | 方法 | 请求 | 返回 | 权限 | 操作日志 |
| --- | --- | --- | --- | --- | --- |
| 根路径 | GET | page/pageSize、keyword名称包含、year、status精确 | 管理字段分页，year/id倒序 | cqt:competition:list | 否 |
| /{id} | GET | id | 完整管理详情、version | cqt:competition:list | 否 |
| 根路径 | POST | 可编辑字段、nationalEntryMode；不接收id/legacyId/status/version | id、version=0 | cqt:competition:create | 是 |
| /{id} | PUT | 全量可编辑字段、expectedVersion；mode仅允许与原值一致 | id、新version | cqt:competition:update | 是 |
| /{id}/status | POST | status=0/1/2、expectedVersion | id、新version | cqt:competition:status | 是 |

创建默认status=0；普通编辑不修改status，状态走专门命令。同年同届次重复由数据库唯一键兜底，转409。写请求不承诺通用requestKey幂等；前端防重复提交，创建网络不确定时先查询再重试，不能依赖预查避免并发重复。

## Database Design（DS-3）

- 仅追加`weiran-cqt-infrastructure/src/main/resources/db/migration/cqt/V202610171900__cqt_competition_admin.sql`，不得改已执行迁移。
- `cqt_competitions`增加`version bigint NOT NULL DEFAULT 0`；现有历史nullable年届/legacy数据保留，不补假值。保留`uk_competition_year_edition(year,edition)`；后台写入强制非空年届，避免NULL绕过唯一性。编辑历史不完整记录时须补齐年届。
- 新增赛事与五条通用组别在同一事务：小学低年级组、小学中年级组、小学高年级组、初中组、高中组（含中职）；first/second_category_legacy_id均0、status=1。任意写入失败整体回滚；沿用现有组别唯一约束，不复制历史专属组别，不改全局赛项。
- 编辑/状态更新SQL以`id AND version=expectedVersion`为条件，成功version+1；零行重新区分404/409。created_at不可覆盖，updated_at由当前业务时间更新；仅显式更新受管字段，保留其他历史列。
- 模式创建后固定，避免报名或学校worker并发写入时混合BOTH/PROVINCIAL，不新增跨报名链路锁。停用后新请求按既有报名校验拒绝；已通过校验的在途事务可能完成，不宣称追溯撤销。

## 权限与已知缺口（DS-4）

菜单放已有1055“赛事评审”目录首位，图标Calendar已注册。只为现有super_admin种子角色绑定新菜单；不扩大普通角色权限。

| ID | 类型/父 | 路径 | component或权限 |
| --- | --- | --- | --- |
| 1130 | menu/1055，可见 | /cqt/competitions | cqt/competition/CompetitionsPage；cqt:competition:list |
| 1131 | 隐藏menu/1055 | /cqt/competitions/create | cqt/competition/CompetitionCreatePage；cqt:competition:create |
| 1132 | 隐藏menu/1055 | /cqt/competitions/edit/:id | cqt/competition/CompetitionEditPage；cqt:competition:update |
| 1133 | button/1130 | 无 | cqt:competition:list |
| 1134 | button/1131 | 无 | cqt:competition:create |
| 1135 | button/1132 | 无 | cqt:competition:update |
| 1136 | button/1130 | 无 | cqt:competition:status |

按钮授权自动补隐藏页面及目录祖先；菜单存在不代替接口权限。所有写方法`@RequiresPermission`和`@OperationLog`；日志只含配置字段，不输出真实密钥/凭据URL。系统暂无部门数据范围和多租户，本功能沿用权限控制，不新增ACL。启停不修改评审、成绩、奖项和报名历史；status=2沿用“已结束”，仍不可报名。

## 分层与装配（DS-5）

- domain定义管理实体、验证规则和端口，api定义命令/视图；不引用Spring/MyBatis类型。
- application组织创建赛事+组别事务、CAS及不可变模式校验；infrastructure复用现有Mapper/DO并扩展字段，管理查询执行数据库分页；adapter处理HTTP边界和权限日志。
- 新服务、仓储和Controller登记到现有三个Cqt AutoConfiguration；不新增Gradle模块或依赖，不改App组件扫描。公开查询仍复用原模型映射，不因AdminDTO新增字段改变旧响应。
- Layer0先冻结管理字段/端口、迁移版本号和菜单1130–1136；三个AutoConfiguration为共享注册文件，实施时单写者。契约落入docs/01后才能编码，前端types手动同步。

## 前端设计（DS-6）

- 三页面文件与上述component逐字配对，不在App或AdminLayout硬注册业务路由；create路径保持精确匹配，edit按id形成独立Tab。
- 列表复用PageContainer、SearchToolbar图标查询/重置、列设置/刷新、TableActions、Permission及Semi Table。查询草稿与生效值分离，无高级筛选；名称/年份/状态均有后端过滤。
- 新增/编辑复用一个业务表单：基本信息、报名时间、省国评审计划、公布计划、三模板URL。显著注明计划不自动调度、模板不生成证书。新增模式可选，编辑模式只读；legacyId只读；状态在列表专门操作。
- 保存成功调用usePageTabs.closeCurrentAndGo(list,true)，关闭发起保存的表单pathname而非异步完成时当前Tab；取消关闭并回列表，失败留页。409保留输入并提示重新加载，不自动覆盖新版本。
- TanStack Query键含分页和已生效筛选；创建/编辑/状态成功失效赛事查询。提交防重复；未知保存结果提示核查列表，不重复自动创建。三模板仅URL配置，不新增模板生成器或上传协议。

## 宪法对照

| 条款 | 结论 | 遵守方式 |
| --- | --- | --- |
| CP-1 | ☑ 符合 | domain无框架依赖 |
| CP-2 | ☑ 符合 | adapter/application/domain单向，infra实现领域端口 |
| CP-3 | ☑ 符合 | DO、Mapper、IPage仅infra |
| CP-4 | ☐ 不涉及 | 无新增第三方依赖及版本 |
| CP-5 | ☑ 符合 | 不改质量规则 |
| CP-6 | ☑ 符合 | 不新增全局豁免，反射映射空值在infra边界处理 |
| CP-7 | ☑ 符合 | 只追加V202610171900，历史迁移不变 |
| CP-8 | ☐ 不涉及 | 不改密码、令牌或吊销 |
| CP-9 | ☑ 符合 | 模板URL拒绝凭据，不写本地密钥或日志 |
| CP-10 | ☐ 不涉及 | 不改认证失败语义 |
| CP-11 | ☑ 符合 | 错误由统一BizException处理，HTTP由错误码决定 |
| CP-12 | ☑ 符合 | 同一CQT模块内扩展，不反向或横向依赖 |
| CP-13 | ☑ 符合 | 不直接操作sys表，菜单仅Flyway种子登记 |
| CP-14 | ☑ 符合 | 复用CommonErrors，不占新增错误号 |
| CP-15 | ☑ 符合 | cqt权限前缀、1130–1136及cqt迁移段 |

## Test Plan

待用户明确授权后验收，当前不新增/运行测试：纯规则覆盖时间/URL/状态/不可变模式；Testcontainers验证创建与五组别事务回滚、同年届并发唯一、CAS/权限/旧公开接口兼容；浏览器验证独立Tab保存关闭、筛选/409保留输入及计划/模板提示。构建如获实施授权须JDK21先spotlessApply再按仓库门禁；不得连接真实业务库写测试数据。

## Rollout / Rollback

回滚代码锚点为HEAD `67b2de5`；保留本会话全部前序dirty，不整库reset/stash，不回退前序功能。先确认迁移版本和菜单号无冲突，L3批准后冻结docs/01及前后端类型，再实施和授权验收。上线应用追加迁移，新version默认0，不自动启用历史赛事。回滚代码保留新增列和菜单数据，不倒退DDL或删除赛事；必要时隐藏管理菜单，原报名查询仍可读取同表。上线后确认菜单component可解析、旧报名可用及新建草稿+五组别完整。

回滚基线为实施前HEAD `67b2de5` 及本change独立文件清单；工作区前序未提交成果必须保留，禁止整库reset/stash。产生独立实施提交后以该提交为revert锚点。


## 2026-10-18 用户追加：配置与赛事单图上传

用户明确要求“配置还有赛事中的输入图片路径/Url的地方均使用单图上传组件”，作为本次界面和上传适配修改授权。站点两图片键、赛事三模板复用SingleImageUpload，原字段不变；赛事新增images上传接口，create/update ANY，复用10MB JPG/PNG/GIF规则及现存FileUploadService。受控字段保留旧值/清空同步，上传互斥保存，保存禁用图片。为使local上传可保存，模板URL校验接受严格/uploads/本地路径，详细规则以docs01追加契约为准。无新迁移/依赖，不启动用户已要求停止的服务；不新增或运行测试。
