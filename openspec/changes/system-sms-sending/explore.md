---
title: "system 短信发送能力现状调研"
status: "draft"
updated_at: "2026-10-19"
---

# Explore

> **L1 · 现实校验**。带着 `interview.md` 去读代码；本 change 只迁移可复用发送能力，不迁移 CQT 账号验证码业务。

## 调研范围

| 模块 | 目录/文件 | 为什么看它 |
|---|---|---|
| system API / domain | `weiran4j/weiran-system/weiran-system-api`、`.../weiran-system-domain` | 确认下游模块可用的服务契约与领域端口位置 |
| system application / infrastructure | `.../weiran-system-application`、`.../weiran-system-infrastructure` | 确认自动配置、属性绑定、第三方适配和依赖声明方式 |
| 应用配置与文档 | `weiran4j/weiran-app/src/main/resources/application.yml`、`weiran4j/.env.example`、`weiran4j/docs/01-架构与接口契约.md`、`weiran4j/docs/02-部署.md` | 确认配置映射、部署说明和唯一契约文档 |
| 参考实现 | `/Users/duoli/Projects/qidian-zhongxi/mono4j/weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/java/com/weiran/cqt/infrastructure/sms` | 复用官方 SDK 调用方式与发送结果处理；区分 CQT 业务逻辑 |

## 现有实现

| 能力 | 位置 | 现状 |
|---|---|---|
| system 可复用服务契约 | `weiran-system-api/.../media/MediaService.java:9` | system API 已有供模块间调用的 Java 服务接口；本仓没有短信服务契约。 |
| system 领域端口 | `weiran-system-domain/.../storage/FileStorage.java:5` | 外部设施能力通过 domain 接口隔离；短信发送可采用相同依赖方向。 |
| system 自动配置 | `weiran-system-application/.../autoconfigure/SystemApplicationAutoConfiguration.java:28`、`weiran-system-infrastructure/.../autoconfigure/SystemInfrastructureAutoConfiguration.java:46` | 应用服务由 application 自动配置导入，基础设施实现由 infrastructure 自动配置条件注册。 |
| 当前 OSS 能力 | `weiran-system-infrastructure/.../storage/OssFileStorage.java:12` | OSS 客户端只为 system 图片文件的存储/删除使用，不能直接作为短信依赖或实现。 |
| 参考项目 SDK 调用 | `mono4j/weiran4j/weiran-cqt/weiran-cqt-infrastructure/.../sms/SdkAliyunSmsGateway.java:11` | 使用官方 `dysmsapi20170525` SDK；SendSms 请求设置手机号、签名、模板与模板参数，并显式关闭自动重试、设置超时。 |
| 参考项目发送结果 | `mono4j/.../sms/AliyunSmsSender.java:14` | `OK`、服务商频控、非法号码、其他失败分别处理；手机号脱敏，日志不含验证码和 AccessKey。 |
| 参考项目配置装配 | `mono4j/.../autoconfigure/CqtInfrastructureAutoConfiguration.java:103` | `mode=aliyun` 时才校验配置并创建发送器；本项目应采用 `weiran.system.sms` 与 `WEIRAN_SYSTEM_SMS_*` 命名。 |
| 当前部署配置 | `weiran4j/weiran-app/src/main/resources/application.yml:41`、`weiran4j/.env.example:40` | 已有 OSS 环境变量段；短信配置没有占位符或说明。 |

## 可复用点

| 可复用对象 | 位置 | 复用方式 | 需要改造吗 |
|---|---|---|---|
| 阿里云 SDK 调用结构 | `mono4j/.../sms/SdkAliyunSmsGateway.java` | infrastructure 内部隔离 SDK 客户端、超时和请求构造 | 是：模板 CODE/参数应由调用方传入，不能绑定 CQT 验证码配置 |
| 结果分类与敏感日志原则 | `mono4j/.../sms/AliyunSmsSender.java` | 复用服务商返回码分类、手机号掩码和不自动重试原则 | 是：改用本项目 system 配置与中立命名 |
| system API + domain port + application service 分层 | `weiran-system-api/.../media/MediaService.java:9`、`weiran-system-domain/.../storage/FileStorage.java:5` | 以 API 契约供业务调用，application 负责调用领域端口，infrastructure 提供阿里云实现 | 是：短信能力是发送接口，不创建 HTTP Controller 或持久化表 |

## 真实约束

| 约束 | 证据位置 | 对设计的影响 |
|---|---|---|
| system 是基座，业务只能依赖 `weiran-system-api` | `openspec/rules/enforced/constitution.md:CP-12–CP-13` | 可调用接口必须放在 system-api；不得让业务依赖 system-infrastructure 或 SDK。 |
| domain/api 不能依赖 Spring、SDK 或持久化实现 | `openspec/rules/enforced/constitution.md:CP-1–CP-3` | SDK 只放 infrastructure；API 请求/响应使用纯 Java 类型。 |
| 密钥和令牌不得入库、日志或异常 | `openspec/rules/enforced/constitution.md:CP-9` | AccessKey 只来自环境变量；校验失败仅列变量名；SDK 异常不能直接回显。 |
| 第三方依赖版本只能集中管理 | `openspec/rules/enforced/constitution.md:CP-4`、`weiran4j/weiran-dependencies/build.gradle.kts:44` | 参考 SDK 版本 4.6.0，应在 BOM 集中约束并由 infrastructure 不带版本引用。 |
| 环境变量需与 application 占位符和 `.env.example` 同步 | `weiran4j/docs/02-部署.md:42`、`weiran4j/.env.example` | 新配置需同时补齐 application.yml、.env.example 和部署文档；`EnvExampleTest` 会检查占位符清单。 |
| Sms SendSms 不幂等，SDK 自动重试可能重复计费 | 官方 [Alibaba Cloud SDK 重试说明](https://www.alibabacloud.com/help/en/sdk/developer-reference/configuration-for-retries)；参考实现 `SdkAliyunSmsGateway.java:33` | 必须显式关闭重试并设置 `maxAttempts=1`；应用层不自动重试。 |
| system 本身没有短信业务状态或验证码表 | 搜索 `weiran-system` 的 API/domain/infrastructure 与 Flyway 目录 | 本 change 不新增表、菜单、HTTP API、验证码生命周期或限流规则；具体业务策略由调用方负责。 |

## 影响面

### 普通改动

| 文件/模块 | 动作 |
|---|---|
| `weiran-system-api` | 新增短信服务契约、请求与结果类型 |
| `weiran-system-domain` | 新增短信发送端口与结果分类 |
| `weiran-system-application` | 新增服务实现并登记自动配置 |
| `weiran-system-infrastructure` | 新增配置属性、阿里云网关、模式条件装配与单元测试 |
| `weiran4j/weiran-app/src/main/resources/application.yml`、`weiran4j/.env.example`、`weiran4j/docs/02-部署.md` | 声明 system 短信运行模式与环境变量 |
| `weiran4j/docs/01-架构与接口契约.md` | 记录业务模块可调用的 system Java 契约及边界 |

### 共享层命中 ⚠️

| 共享层 | 是否命中 · 原因与预计改动 |
|---|---|
| `weiran-dependencies/build.gradle.kts`（SL-2） | 命中：加入阿里云短信 SDK 的集中版本约束；这是构建桶文件，先串行更新。 |
| `weiran-system-api`（跨模块契约） | 命中：业务模块会消费新增服务接口、请求和结果类型；冻结契约后再实现其他层。 |
| `build-logic` | 未命中：不改质量规则或构建约定。 |
| `weiran-framework`（SL-6/SL-7） | 未命中：不增加全局错误码、分页或响应类型。 |
| Flyway 序号资源 / system 菜单种子（SL-4） | 未命中：不新增持久化表或 HTTP 菜单。 |
| `settings.gradle.kts`、`weiran-app/build.gradle.kts`（SL-1/SL-3） | 未命中：不新增 Gradle 模块；沿用现有 system 模块自动配置。 |
| 前端页面注册（SL-4/SL-5） | 未命中：无管理页面、前端 API 或菜单。 |

## 对 interview 的反向修正

| 原结论 | 现实情况 | 修正后 | 是否已同步回 interview.md |
|---|---|---|---|
| system 短信适合暴露 HTTP 验证码接口？ | system 当前采用 Java API 服务供业务模块协作，源项目验证码服务绑定 CQT 账号注册/重置流程。 | 仅新增供业务模块调用的 Java 发送服务；验证码接口与校验留在业务模块。 | 是 |

## 风险预警

| 风险 | 触发条件 | 建议应对 |
|---|---|---|
| 误把 CQT 验证码策略复制到通用基座 | 移植 `SmsApplicationService`、内存验证码存储或账号 Controller | 只移植发送能力；业务模块传目标号码、模板 CODE 和模板参数。 |
| 日志泄露验证码或密钥 | SDK 返回异常或日志直接输出请求参数 | 只记录掩码手机号、SDK 异常类型和不含参数的返回摘要；禁止模板参数与 AccessKey。 |
| 重试产生重复短信 | SDK/应用自动重试 | 请求级自动重试关闭且最大尝试次数为 1。 |

## Gate

- [x] 共享层命中已完整列出(漏一项 = L5 必然冲突)
- [x] 与 interview 的冲突项已回写 `interview.md`
- [x] 可复用点已确认,避免重复造轮子
