# E3: 短信应用与阿里云适配

## 完成的 tasks.md 条目

- `3.1`
- `3.2`
- `3.3`
- `3.4`
- `3.5`
- `3.6`
- `5.1`
- `5.2`

## 改了什么

| 文件 | 动作 | 说明 |
|---|---|---|
| `weiran-system-application/.../application/sms/SmsApplicationService.java` | 新增 | 映射 API 请求与 domain sender 结果。 |
| `SystemApplicationAutoConfiguration.java` | 修改 | 登记短信应用服务。 |
| `weiran-system-infrastructure/.../sms/*` | 新增 | 属性、disabled sender、SDK gateway、发送结果映射与安全日志。 |
| `SystemInfrastructureAutoConfiguration.java` | 修改 | 绑定配置并按模式创建 sender。 |
| `weiran-system-infrastructure/build.gradle.kts` | 修改 | 声明无版本阿里云短信 SDK 依赖。 |
| `weiran-system-application/.../src/test/.../SmsApplicationServiceTest.java` | 新增 | 覆盖请求参数传递与所有结果分类映射。 |
| `weiran-system-infrastructure/.../src/test/.../sms/*Test.java` | 新增 | 覆盖 SDK 请求与返回、自动重试配置、业务码、超时/密钥校验、异常日志脱敏和默认关闭。 |
| `weiran-system-infrastructure/.../sms/LogSmsSender.java` | 新增 | log 模式记录模板代码与参数，手机号掩码，返回 `LOGGED`，不调用供应商。 |
| `weiran-system-infrastructure/.../sms/SystemSmsProperties.java`、`SdkAliyunSmsGateway.java` | 修改 | 移除 endpoint 属性，SDK 固定使用 `dysmsapi.aliyuncs.com`。 |
| `weiran-system-infrastructure/.../src/test/.../LogSmsSenderTest.java`、`autoconfigure/SystemSmsModeTest.java` | 新增 | 覆盖日志正文、返回结果、disabled 默认装配和 log 模式装配。 |
| `weiran-app/src/main/resources/application.yml` | 修改 | 添加环境变量映射与默认值。 |
| `weiran4j/.env.example` | 修改 | 添加空凭据及非敏感默认值。 |
| `weiran4j/docs/02-部署.md` | 修改 | 增加配置表和启用步骤。 |

## 为什么这么做

- 关键决策：默认 `disabled`；`log` 模式用于受控开发/测试，会记录模板代码与参数并返回 `LOGGED`；只有 `aliyun` 模式创建 SDK 客户端；所有厂商异常映射为 `FAILED` 且不记录原始消息；连接和读取超时从配置读取，阿里云 endpoint 固定；每次只尝试一次。
- 考虑过但放弃的方案：在 system 暴露 HTTP 发送或验证码接口；这会把业务验证码策略和外部发送权限放进通用基座。

## 依赖的契约

- 实现 `SmsService.send(SmsSendCommand)`。
- 调用 `SmsSender.send(phone, templateCode, templateParameters)`。
- 结果映射保持 `SENT`、`NOT_CONFIGURED`、`RATE_LIMITED`、`INVALID_NUMBER`、`FAILED`。

## 越界申报

| 文件 | 为什么不得不改 | 性质 | 是否可能影响其他单元 |
|---|---|---|---|
| `weiran4j/docs/02-部署.md` | 新增短信环境变量的部署说明 | 必要连带 | 无 |
| `weiran-app/src/main/resources/application.yml`、`weiran4j/.env.example` | 配置映射需满足部署规范与变量清单校验 | 必要连带 | 无 |

## 埋的坑 / 遗留

- [ ] 未使用真实 AccessKey 发起外部短信；部署启用前仍需配置已审核签名与模板。

## 自测结果

- 命令：JDK 21 下 `./gradlew spotlessApply :weiran-system-application:test :weiran-system-infrastructure:test --no-daemon`；`node openspec/check.mjs --change system-sms-sending`。
- 结果：格式化、短信应用层与基础设施单元测试、OpenSpec 检查均通过。测试使用 mock/fake，不调用真实阿里云服务。
