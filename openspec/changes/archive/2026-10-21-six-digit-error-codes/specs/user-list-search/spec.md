## MODIFIED Requirements

### Requirement: [FR-003] 性别过滤

`GET /api/users` **MUST** 接受 `gender` 参数,取值只能是 `male`、`female`、`unknown`,只返回 `gender` 等于该值的用户;
未传或为空白时 **MUST NOT** 按性别过滤。非法取值 **MUST** 返回业务错误 `100001`。

#### Scenario: 按性别过滤
- **WHEN** 请求 `gender=female`
- **THEN** 结果中每个用户的 `gender` 都是 `female`
- **判据**:集成测试断言 `data.list[*].gender` 全部为 `female` 且至少一条

#### Scenario: 未传性别不改变结果
- **WHEN** 请求不带 `gender`
- **THEN** 结果与不支持性别过滤之前一致,包含 `gender=unknown` 以外的用户
- **判据**:`Gender.filterOf(null)` 与 `Gender.filterOf(" ")` 单测返回 `null`;集成测试断言无 `gender` 时结果含 `male` 用户

#### Scenario: 非法性别值
- **WHEN** 请求 `gender=x`
- **THEN** 返回业务错误
- **判据**:HTTP 400,响应 `code=100001`

### Requirement: [FR-004] 时间范围过滤

`GET /api/users` **MUST** 接受 `createdStartTime` / `createdEndTime`(对 `created_at`)与 `lastLoginStartTime` / `lastLoginEndTime`
(对 `last_login_at`),格式 `yyyy-MM-dd HH:mm:ss`,按闭区间过滤;只传一端时按单边过滤。
传了最后登录时间任一端时,从未登录(`last_login_at` 为空)的用户 **MUST NOT** 被返回。格式非法 **MUST** 返回 `100001`。

#### Scenario: 闭区间包含边界
- **WHEN** 用户 C 的 `created_at` 恰为 `T`,请求 `createdStartTime=T&createdEndTime=T`
- **THEN** 结果包含 C
- **判据**:集成测试断言结果 ID 集合含 C

#### Scenario: 单边与空值
- **WHEN** 只传 `lastLoginStartTime`(早于所有登录时间)
- **THEN** 只返回登录过的用户,从未登录的新建用户不在结果中
- **判据**:集成测试断言新建未登录用户的 ID 不在结果中,已登录的 `admin` 在结果中

#### Scenario: 时间格式非法
- **WHEN** 请求 `createdStartTime=2026-13-01`
- **THEN** 返回业务错误
- **判据**:HTTP 400,响应 `code=100001`
