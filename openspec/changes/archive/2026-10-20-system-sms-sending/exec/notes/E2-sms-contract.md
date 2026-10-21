# E2: system 短信契约

## 完成的 tasks.md 条目

- `2.1`
- `2.2`
- `2.3`

## 改了什么

| 文件 | 动作 | 说明 |
|---|---|---|
| `weiran-system-api/.../api/sms/SmsService.java` | 新增 | 业务模块应用内调用服务。 |
| `weiran-system-api/.../api/sms/SmsSendCommand.java` | 新增 | 手机号、模板 CODE 和不可变模板参数。 |
| `weiran-system-api/.../api/sms/SmsSendResult.java` | 新增 | 暴露稳定发送结果分类。 |
| `weiran-system-domain/.../domain/sms/SmsSender.java` | 新增 | 纯 Java 发送设施端口。 |
| `weiran-system-domain/.../domain/sms/SmsSendOutcome.java` | 新增 | 厂商无关领域结果。 |
| `weiran4j/docs/01-架构与接口契约.md` | 修改 | 记录应用内短信服务边界，无 HTTP 路由。 |

## 为什么这么做

- 关键决策：API/domain 使用纯 Java 类型；domain 端口使用字段签名，不依赖 system-api。
- 考虑过但放弃的方案：让 domain 直接接收 API 命令对象；会形成兄弟层之间的依赖。

## 依赖的契约

- `SmsService.send(SmsSendCommand)` 返回 `SmsSendResult`。
- `SmsSendCommand` 含 `phone`、`templateCode`、`templateParameters`。
- `SmsSender.send(phone, templateCode, templateParameters)` 返回 `SmsSendOutcome`。

## 越界申报

| 文件 | 为什么不得不改 | 性质 | 是否可能影响其他单元 |
|---|---|---|---|
| `weiran4j/docs/01-架构与接口契约.md` | 新增 system Java 服务契约的权威说明 | 必要连带 | E3 只读此边界 |

## 埋的坑 / 遗留

- [ ] 无。

## 自测结果

- 命令：JDK 21 下 system API/domain Java 编译。
- 结果：编译通过；未运行测试套件。
