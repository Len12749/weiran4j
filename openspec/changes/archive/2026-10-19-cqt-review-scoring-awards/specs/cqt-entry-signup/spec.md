## MODIFIED Requirements

### Requirement: [FR-005] 报名写入

校验通过后系统 **MUST** 在一个事务内写入：作品（报名号 `WEB-<赛事ID>-<10 位序号>`，序号取自应用序列 `fastapi_signup_entry`；`entry_type` 个人 `INDIVIDUAL` / 团体 `TEAM`；
`status=ACTIVE`、`legacy_status=1`、`source_database` 取服务端当前账号来源、`scoring_scope` 按来源决定（`zhongxi` 为 `ENTRY`，`qudao`/`houtai` 为 `PARTICIPANT`）、`stage_scope` 按 FR-004、`declared_group_size`=人数、`attachment_type`、赛区、学校编号、指导教师、专业、首位成员组别）；
每位成员一条人员（第一位记账号的 `legacy_user_id`）与一条参赛人（第一位为队长，按顺序排序，记学校与组别快照）；每位成员一条参赛唯一键；`ENTRY` 评分范围创建一条作品级评审对象，`PARTICIPANT` 评分范围为每位成员创建一条关联对应参赛人的人员级评审对象，且不得同时创建有效作品级对象。来源与评分范围 **MUST** 由服务端身份派生，不接受客户端伪造；团体成员的来源不得用于反推作品来源。学校批量导入写入作品时 **MUST** 采用同一来源、评分范围和对象生成规则，保留既有组事务及指纹幂等行为。
成功 **MUST** 返回 `{productid: <作品ID>, entry_no: <报名号>}`。任一步失败 **MUST** 整体回滚。

#### Scenario: 个人报名成功
- **WHEN** 账号对启用中的 `SHARED` 赛事、带合法 ZIP 附件与已配置组别个人报名
- **THEN** `code` 200 返回 `productid` 与形如 `WEB-<赛事ID>-0000000001` 的报名号；库中作品 1、人员 1、参赛人 1（队长）、唯一键 1（`BOTH`）、评审对象 1（中戏为ENTRY，渠道/后台为PARTICIPANT）
- **判据**:集成测试查库断言

#### Scenario: 团体报名成功
- **WHEN** 三人团体报名
- **THEN** 作品 `TEAM`、`declared_group_size=3`，参赛人 3 条且仅第一位为队长，唯一键 3 条；中戏创建1条ENTRY对象，渠道/后台创建3条PARTICIPANT对象且分别关联3位参赛人，作品来源等于登录账号来源
- **判据**:集成测试查库断言

