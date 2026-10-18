---
status: "active"
---

# cqt-competition Specification

## Purpose
本能力长期负责常青藤的赛事基础配置：赛事（即「届」）及其报名时间窗与省 / 国赛报名模式、一级与二级赛项（沿用旧系统编号）、按赛事与赛项适用的组别，
以及前台报名表单读取这些配置的接口口径。单库存放多届，历届数据并存，是否可报名由赛事状态与报名时间窗决定。

## Requirements

### Requirement: [FR-001] 赛事存储与报名开放判定

赛事 **MUST** 存放在 `cqt_competitions`（列同 FastAPI 2026 库最终结构，另加 `national_entry_mode`：`SHARED` 省国赛共用作品，默认；`SEPARATE` 省国赛分别报名）。
赛事可报名 **MUST** 同时满足：`status=1`；`registration_start` 为空或当前时间不早于它；`registration_end` 为空或当前时间不晚于它。
不满足时报名 **MUST** 分别提示「所选赛事尚未启用」「赛事报名尚未开始」「赛事报名已经结束」。

#### Scenario: 报名时间窗
- **WHEN** 赛事 `status=1`、报名时间为明天到后天；以及 `status=0`；以及报名截止于昨天
- **THEN** 分别判为「赛事报名尚未开始」「所选赛事尚未启用」「赛事报名已经结束」
- **判据**:领域单测用可控时间断言三种提示，并断言窗口内判为可报名

### Requirement: [FR-002] 前台赛事与赛项读取

`competitionlists`（`GET`/`POST /api-web/competcategory/competitionlists`，公开）**MUST** 返回 `[{id, legacy_id, name, edition, year, description, status}]`，按 `year`、`id` 倒序，
可按 `status`（精确）与 `name`（包含）过滤。
`getsecondcat`（`GET /api-web/auth/getsecondcat?id=<赛事ID>`，公开）在赛事不存在或 `status≠1` 时 **MUST** 返回空数组，否则返回启用的一级赛项；
`secondcategory`（`GET`/`POST /api-web/competcategory/secondcategory?id=<一级赛项编号>`，公开）**MUST** 返回该一级赛项下启用的二级赛项。
赛项项 **MUST** 为 `{id: legacy_id, pid: parent_legacy_id, name, sort: sort_order, status, fujian: attachment_required}`，按 `sort_order`、`id` 升序。

#### Scenario: 赛事列表过滤
- **WHEN** 存在 2026（status=2）与 2027（status=1）两个赛事，分别不带条件与带 `status=1` 请求
- **THEN** 前者 2027 在前、共两条；后者只有 2027
- **判据**:集成测试断言

#### Scenario: 未启用赛事无可报赛项
- **WHEN** 对 `status=0` 的赛事请求 `getsecondcat`
- **THEN** `data` 为空数组
- **判据**:集成测试断言

#### Scenario: 二级赛项
- **WHEN** 一级赛项 1 下有启用的二级赛项 11 与停用的 12，请求 `secondcategory?id=1`
- **THEN** 只返回 11，字段为 `id,pid,name,sort,status,fujian`
- **判据**:集成测试断言

### Requirement: [FR-003] 组别

组别 **MUST** 存放在 `cqt_competition_groups`，按赛事配置；一级 / 二级赛项编号为 0 表示对该层级通用。
`groups`（`GET /api-web/competcategory/groups?competition_id&firstcatid&secondcatid`，公开）缺 `competition_id` 或 `firstcatid` 时 **MUST** 返回空数组，
否则 **MUST** 返回该赛事启用的、一级赛项为 0 或等于 `firstcatid`、二级赛项为 0 或等于 `secondcatid` 的组别 `[{id, name, sort: id}]`，按 `id` 升序。

#### Scenario: 通用组别与赛项专属组别
- **WHEN** 赛事有通用组别「初中组」（0/0）与仅一级赛项 2 适用的「专业组」（2/0）；分别以 `firstcatid=1` 与 `firstcatid=2` 请求
- **THEN** 前者只有「初中组」，后者两个都有
- **判据**:集成测试断言

### Requirement: [FR-004] 历史导入与新届种子

2026届赛事与赛项 **MUST** 通过独立导入脚本迁入；新一届赛事与标准组别（小学低年级组、小学中年级组、小学高年级组、初中组、高中组（含中职））**MUST** 支持既有种子脚本和后台创建。新赛事默认status=0停用草稿，确认报名配置后显式启用；脚本可重复执行不产生重复行，后台赛事与五组别同事务创建，失败全部回滚。后台创建legacyId为null，不伪造历史编号。

#### Scenario: 种子脚本可重复执行
- **WHEN** 连续执行两次新届种子脚本
- **THEN** 新届赛事1条、通用组别5条
- **判据**:待测试授权后在隔离MySQL执行两次计数

#### Scenario: 后台创建完整草稿
- **WHEN** 管理员创建合法新赛事
- **THEN** status0、legacyId null、version0，并存在五条0/0启用组别；任一组别写入失败无赛事残留
- **判据**:待授权后接口与事务回滚验收

### Requirement: [FR-005] 后台赛事查询与配置

后台 **MUST** 提供权限限定分页、详情、新增、编辑和状态接口，完整字段按docs01赛事管理契约；公开前台响应不变。名称、年届、说明、时间、模板URL须校验，非法400；模板仅无凭据/签名参数的绝对http/https或严格/uploads/本地路径，不抓取URL，不日志输出未校验模板。

#### Scenario: 查询与详情兼容
- **WHEN** 管理员按名称、年份、状态分页并读取历史详情
- **THEN** 返回符合筛选的year/id倒序结果与原始nullable字段，旧前台字段不增删
- **判据**:待授权后比较分页响应、历史详情与公开接口结构

#### Scenario: 配置边界
- **WHEN** 提交非法年届、倒置时间窗或含凭据模板URL
- **THEN** 400且错误不回显URL/说明原文
- **判据**:待授权后HTTP错误及操作日志检查

### Requirement: [FR-006] 标识模式与并发一致性

赛事 **MUST** 使用不可变自增id、只读legacyId；nationalEntryMode创建后固定，编辑不同值409。编辑/状态命令使用expectedVersion CAS，成功version+1，旧版本409；同年同届并发创建依唯一约束至多一条，冲突409。

#### Scenario: 模式与版本保护
- **WHEN** 编辑携旧版本或不同模式
- **THEN** 409且原配置/组别/作品阶段不变
- **判据**:待授权后CAS竞争与数据前后比较

### Requirement: [FR-007] 状态和计划元数据边界

赛事 **MUST** 保留0/1/2状态，编辑不改状态，状态走独立权限命令；停用不删除历史或改变phase/final/award。评审/公布时间仅计划，不自动开关或发布；模板仅配置，不生成证书。

#### Scenario: 停用与计划修改
- **WHEN** 停用赛事并修改评审计划和模板
- **THEN** 后续报名按现有规则拒绝，既有phase状态/成绩/奖项不变，模板原值可读取
- **判据**:待授权后报名接口和业务表状态验收

### Requirement: [FR-008] 管理权限与页签流程

后台 **MUST** 为list/create/update/status分别校验权限并记录写日志；新增编辑由菜单驱动独立Tab，保存成功关闭发起Tab并刷新列表；失败保留输入，409要求重载核查，不提供删除。

#### Scenario: 页签与权限
- **WHEN** 不同记录独立编辑且异步保存期间切Tab，或无权限调用写接口
- **THEN** 成功只关闭发起页并刷新列表；无权限403，失败不关闭表单
- **判据**:待授权后浏览器交互及HTTP权限验收

#### Scenario: 证书模板单图上传
- **WHEN** 管理员新增或编辑赛事的三类模板图片
- **THEN** 单图组件显示历史图、上传后填写返回地址，预览按原图显示，删除清空引用；上传中不得保存
- **判据**:静态复核受控字段和互斥逻辑，后续授权后运行浏览器上传验收
