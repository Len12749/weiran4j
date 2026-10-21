---
status: "active"
---

# system-sms-sender Specification

## Purpose
本能力长期负责为业务模块提供统一、可替换且安全的短信发送入口，使业务模块不必直接依赖具体短信厂商 SDK；验证码生成、校验和发送频率等策略由具体业务模块负责。

## Requirements

### Requirement: [FR-001] 业务模块短信服务契约

`weiran-system-api` **MUST** 提供 Java 短信发送契约，允许调用方提交手机号、短信模板 CODE 与模板参数，并获得可判定的发送结果。system **MUST NOT** 因此新增公开 HTTP 短信发送接口或承载业务验证码生命周期。

#### Scenario: 业务模块调用短信服务
- **WHEN** 业务模块通过 `weiran-system-api` 提交手机号、模板 CODE 和参数
- **THEN** system 返回成功、未配置、服务商频控、手机号非法或其它失败之一
- **判据**:system-api 契约测试确认请求类型和结果类型均不依赖 Spring、Web 或阿里云 SDK。

#### Scenario: 不暴露验证码 HTTP 入口
- **WHEN** 本能力完成装配
- **THEN** 短信发送只作为 Java 服务提供，不存在新增的短信 Controller 路由
- **判据**:应用上下文不注册短信发送 Controller，接口文档无新增短信 HTTP 路径。

### Requirement: [FR-002] 短信发送模式配置

system **MUST** 支持 `disabled`、`local` 与 `aliyun` 三种短信模式，默认模式为 `disabled`；`disabled` 模式调用发送服务时 **MUST** 返回 `NOT_CONFIGURED` 且不得调用外部服务；`local` 模式 **MUST** 将短信模板代码与参数写入应用日志且不得调用外部服务，并返回 `LOGGED`。

#### Scenario: 默认关闭短信发送
- **WHEN** 未配置 `WEIRAN_SMS_MODE`
- **THEN** 短信模式为 `disabled`，业务调用得到 `NOT_CONFIGURED`
- **判据**:默认配置上下文的发送结果为 `NOT_CONFIGURED`，假网关调用次数为 0。

#### Scenario: 仅记录短信内容
- **WHEN** 短信模式为 `local` 且调用方提交模板消息
- **THEN** 服务记录模板代码和模板参数、手机号脱敏，返回 `LOGGED` 且不调用短信供应商
- **判据**:捕获日志包含模板代码和模板参数、不含完整手机号，sender 装配为 local 实现。

#### Scenario: 阿里云凭据缺失
- **WHEN** 短信模式为 `aliyun` 且 AccessKey ID 或 AccessKey Secret 缺失
- **THEN** 应用启动失败，异常只列出缺失的环境变量名，不包含配置值
- **判据**:配置校验测试逐项移除必填凭据，并断言异常不含测试用密钥。

### Requirement: [FR-003] 阿里云 SendSms 请求

`aliyun` 模式 **MUST** 使用阿里云 `SendSms` API 发送调用方提供的模板消息；请求 **MUST** 使用系统配置中的签名名称与固定端点，并将调用方模板参数序列化为模板参数 JSON。

短信配置 **MUST** 通过独立的「短信配置」菜单进入与系统配置共用的页面实现，并复用 `sys_config` 存储。配置键遵循 `{range}.{group}.{value}`，短信的 `range=sms`；`sys_group.group_key=config_sms` 是独立短信配置域，其中 `sms.general.sign_name` 位于 `general`（通用）配置组，`sms.system_tpl.captcha_local` 和 `sms.system_tpl.captcha_aliyun` 位于 `system_tpl`（系统模板）配置组。页面必须复用系统配置的分组导航、配置项表单和管理操作，并按配置域隔离数据；手机号登录按当前发送模式读取对应验证码模板，阿里云发送读取短信签名。`local` 验证码模板 **MUST** 包含 `${code}`，发送时以实际验证码替换占位符。

#### Scenario: 成功发送短信
- **WHEN** 已配置阿里云凭据与系统配置签名，调用方提交有效手机号、模板 CODE 与参数
- **THEN** SDK 收到对应手机号、签名、模板 CODE 和序列化参数，阿里云返回 `OK` 时结果为 `SENT`
- **判据**:假 SDK 网关断言请求字段逐项匹配，响应 `OK` 映射为 `SENT`。

#### Scenario: 分类阿里云失败
- **WHEN** 阿里云返回服务商频控、非法手机号或其它业务错误
- **THEN** 服务分别返回 `RATE_LIMITED`、`INVALID_NUMBER` 或 `FAILED`
- **判据**:对 `isv.BUSINESS_LIMIT_CONTROL`、`isv.MOBILE_NUMBER_ILLEGAL` 和未知返回码的单测逐项断言对应结果。

#### Scenario: SDK 发送异常
- **WHEN** 已配置的阿里云 sender 在 SDK 发送时抛出异常
- **THEN** 服务返回 `FAILED`，不重试，日志与返回值不包含原始异常消息
- **判据**:模拟含敏感内容的 SDK 异常，断言结果为 `FAILED`、调用一次，且日志/结果中不存在异常消息。

### Requirement: [FR-004] 短信发送安全日志

短信日志 **MUST** 对手机号脱敏，且 **MUST NOT** 记录 AccessKey 或原始 SDK 异常消息。`aliyun` 模式 **MUST NOT** 记录短信模板参数、验证码或完整请求体；显式启用的 `local` 模式 **MUST** 记录替换变量后的短信正文与模板参数以供开发联调，并 **MUST NOT** 调用外部供应商。`local` 模式可能将验证码或个人信息写入应用日志，只应用于受控的开发/测试环境，并由部署方限制日志访问与保留时间。

#### Scenario: 成功和失败发送日志
- **WHEN** 发送成功或收到服务商错误
- **THEN** `aliyun` 日志只包含掩码手机号、必要的厂商返回码/请求号和安全摘要；`local` 日志包含渲染后的短信正文与参数并对手机号脱敏
- **判据**:分别捕获两种模式日志，断言 aliyun 不含测试参数、local 包含测试参数，二者都不含完整手机号或测试密钥。

#### Scenario: SDK 调用抛出异常
- **WHEN** SDK 初始化或发送调用抛异常
- **THEN** 日志和服务结果不包含原始异常消息、AccessKey 或模板参数
- **判据**:以含测试密钥的异常消息触发网关后，捕获日志与返回结果均不含该测试密钥。

### Requirement: [FR-005] 短信请求不自动重试

短信发送 **MUST** 禁止 SDK 自动重试，并将单次发送尝试上限设为 1；system 服务 **MUST NOT** 在 SDK 调用异常或失败结果后自动再次发送。

#### Scenario: SDK 调用只尝试一次
- **WHEN** 阿里云发送请求遇到超时或网络异常
- **THEN** 当前请求不会被 SDK 或 system 应用层自动重试
- **判据**:SDK RuntimeOptions 的 `autoretry` 为 false、`maxAttempts` 为 1，假网关异常时调用次数为 1。

### Requirement: [FR-006] 短信运行配置文档

短信发送模式 **MUST** 使用 `weiran.sms.mode`，仅阿里云凭据配置在 `weiran.sms.aliyun.*` 并通过 `WEIRAN_SMS_*` 环境变量注入；签名与验证码模板通过 `sys_config` 动态读取。application 配置占位符、`.env.example` 与部署说明 **MUST** 保持一致。

#### Scenario: 短信模式部署配置
- **WHEN** 运维人员选择 `disabled`、`local` 或 `aliyun` 模式
- **THEN** 文档列出三种模式的行为与安全影响，并列出 `WEIRAN_SMS_ALIYUN_*` 所需变量及凭据管理要求；endpoint 使用固定默认值，不提供环境变量
- **判据**:`EnvExampleTest` 通过，且部署文档中的短信变量与 application.yml 和 `.env.example` 一致。
