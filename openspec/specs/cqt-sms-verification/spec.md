---
status: "active"
---

# cqt-sms-verification Specification

## Purpose
本能力长期负责常青藤前台的短信验证码：给手机号发送验证码的频率与格式限制、验证码的有效期与一次性校验，并调用 system 提供的短信服务；
注册、登录、重置密码等需要证明手机号归属的流程都通过它校验。

## Requirements

### Requirement: [FR-001] 验证码发送

`GET /api-web/n/auth/sendSms?phone=<手机号>` **MUST** 免登录。手机号 **MUST** 为 11 位数字，否则返回 `code` 400「手机号格式不正确」。
系统 **MUST** 生成 6 位数字验证码，并按 `weiran.sms.mode` 使用 system 短信配置的 `sms.system_tpl.captcha_local` 或 `sms.system_tpl.captcha_aliyun` 模板与 system `SmsService` 发送；成功返回 `{"sent":true}`，响应 **MUST NOT** 回显验证码。
同一手机号距上次成功发送不足 60 秒时 **MUST** 返回 `code` 429「发送过于频繁，请稍后再试」且不生成新验证码。
system 短信服务不可用或当前模式的模板为空时 **MUST** 返回 `code` 503「短信服务未配置」。发送失败时 **MUST** 删除本次验证码（允许立即重发），并按 system 返回结果映射：服务商频控 → `code` 429；号码非法 → `code` 400；其它失败 → `code` 503。HTTP 均为 200。
短信发送模式与阿里云凭据由 system 的 `WEIRAN_SMS_*` 设置管理。日志中手机号 **MUST** 只出现掩码；验证码仅可由受控的 system `local` 模式记录，业务接口 **MUST NOT** 回显验证码。

#### Scenario: 手机号格式错误
- **WHEN** 以 `phone=1234` 请求发送
- **THEN** `code` 400，`message` 为「手机号格式不正确」
- **判据**:集成测试断言 `$.code == 400`

#### Scenario: 60 秒内重复发送
- **WHEN** 同一手机号成功发送后立即再次请求
- **THEN** 第二次 `code` 429，`message` 为「发送过于频繁，请稍后再试」
- **判据**:集成测试断言第二次 `$.code == 429`

#### Scenario: system 短信服务或模板未配置
- **WHEN** system 短信模式为 `disabled`，或当前模式对应的系统验证码模板为空时请求发送
- **THEN** `code` 503，`message` 为「短信服务未配置」
- **判据**:应用服务单测断言错误码 `50320`，且不生成验证码

#### Scenario: system 发送失败后可立即重发
- **WHEN** system `SmsService` 返回失败，随后同一号码立即再次请求且发送成功
- **THEN** 第一次 `code` 503「短信发送失败，请稍后再试」，第二次 `code` 200；第一次生成的验证码不可用于校验
- **判据**:应用服务单测（可控的假发送实现）断言两次结果，并断言第一次的验证码校验失败

#### Scenario: system 发送结果映射
- **WHEN** system `SmsService` 分别返回服务商频控与号码非法
- **THEN** 分别抛出错误码 `42920`（前台 429）与 `CommonErrors.BAD_REQUEST`「手机号格式不正确」（前台 400）
- **判据**:应用服务单测断言

### Requirement: [FR-002] 验证码校验

验证码 **MUST** 自发送起 10 分钟内有效；校验成功后 **MUST** 立即作废，同一验证码不能使用第二次；
校验失败（不存在、已过期、不匹配）时调用方 **MUST** 返回 `code` 401「验证码错误」。
同一手机号重新发送后，旧验证码 **MUST** 失效。
同一验证码累计校验失败 5 次后 **MUST** 作废（第 5 次及之后均返回「验证码错误」，需重新发送），防止在有效期内暴力枚举。

#### Scenario: 验证码一次性
- **WHEN** 用同一验证码先后两次完成需要验证码的操作（如两次重置密码）
- **THEN** 第一次成功，第二次 `code` 401「验证码错误」
- **判据**:集成测试断言第二次 `$.code == 401`、`$.message == "验证码错误"`

#### Scenario: 输错 5 次后作废
- **WHEN** 同一手机号的验证码连续输错 5 次后，再输入正确的验证码
- **THEN** 仍返回 `code` 401「验证码错误」
- **判据**:领域单测断言第 6 次用正确验证码校验失败；集成测试以重置密码接口复现

#### Scenario: 验证码过期
- **WHEN** 验证码发送 10 分钟后再校验
- **THEN** 校验失败
- **判据**:领域 / 应用单测用可控时钟断言校验结果为失败

### Requirement: [FR-003] 开发模式

开发环境可将 system 短信模式设为 `local`，由 system 记录渲染后的短信内容以便联调；此模式 **MUST NOT** 用于生产。
CQT 前台响应 **MUST NOT** 回显验证码；短信手机号 **MUST** 以掩码记录。

#### Scenario: 开发环境记录短信
- **WHEN** system 短信模式为 `local` 且模板已配置
- **THEN** system 记录发送参数，CQT 前台只返回 `{"sent":true}`
- **判据**:system 发送器单测与 CQT 集成测试断言

### Requirement: [FR-004] 阿里云短信发送

system 短信模式为 `aliyun` 时 **MUST** 通过阿里云 SendSms 发送模板短信，签名与凭据从 `WEIRAN_SMS_*` 读取，模板 CODE 从系统配置读取。
CQT **MUST NOT** 实现或配置短信服务商发送器，而应将手机号、模板 CODE 和 `{"code":"<验证码>"}` 参数交给 system `SmsService`；system 负责 disabled/local/aliyun 模式、凭据保护与 SDK 交互。
system 返回 `SENT` 或受控开发环境的 `LOGGED` 时，CQT 视为发送成功；CQT 仍独立负责验证码生命周期与手机号冷却。
发送调用 **MUST NOT** 自动重试；AccessKey Secret **MUST NOT** 出现在日志或异常信息中。

#### Scenario: 阿里云配置发送
- **WHEN** system 短信配置完整且阿里云返回成功
- **THEN** system 使用配置签名、模板 CODE 与验证码参数发送，CQT 前台不回显验证码
- **判据**:system 基础设施单测断言 SDK 请求参数，CQT 集成测试断言响应

#### Scenario: 阿里云配置不全
- **WHEN** system 短信模式为 `aliyun` 但凭据或签名缺失
- **THEN** 应用启动失败，错误指出缺少的环境变量名且不泄露 Secret
- **判据**:system 基础设施单测断言

### Requirement: [FR-005] 客户端 IP 发送上限

该历史需求已由统一调用 system 短信服务的调整废止。CQT 不再维护每小时 IP 计数，也不读取 `WEIRAN_CQT_SMS_IP_HOURLY_LIMIT`；发送频控只保留同手机号 60 秒冷却与 system 发送结果中的服务商频控。

#### Scenario: 独立号码不受 CQT IP 计数影响
- **WHEN** 同一客户端连续给多个不同手机号请求验证码，且 system 服务均返回成功
- **THEN** CQT 不因客户端 IP 拒绝这些请求；服务商返回频控时仍映射为 `code` 429
- **判据**:应用服务不接收客户端 IP 或注入 IP 计数器，发送结果映射单测覆盖服务商频控
