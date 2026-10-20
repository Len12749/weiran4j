---
title: "在 system 基座增加短信发送能力"
owner: "Codex"
status: "draft"
created_at: "2026-10-19"
updated_at: "2026-10-19"
---

# Proposal

## Why

- 背景：参考项目已接入阿里云 SendSms；当前 weiran4j 没有短信服务契约或发送适配器。
- 业务目标：让下游业务模块通过 `weiran-system-api` 调用统一的短信发送服务，不直接依赖阿里云 SDK。
- 当前问题：业务模块若要发送短信，只能自行接 SDK、管理凭据与解析厂商返回码，能力无法在 system 基座复用。
- 需求来源：见 `interview.md`。

## What Changes

- 新增：system 短信 Java 服务契约、领域发送端口、应用层服务与阿里云 infrastructure 适配器。
- 新增：`disabled`、`log` 与 `aliyun` 运行模式、配置检查、环境变量配置及部署说明。
- 改造：集中依赖约束中加入阿里云短信 SDK；system application/infrastructure 自动配置可装配该能力。
- 复用：参考项目的 SendSms SDK 调用结构、服务商结果分类、关闭重试及敏感日志处理原则。
- 不做：不移植 CQT 账号验证码流程，不新增 HTTP 接口、菜单、持久化表或前端页面。

## Scope

### In Scope

- 下游业务通过 system API 提供手机号、模板 CODE 与模板参数，调用短信发送能力。
- `aliyun` 模式使用阿里云官方 SDK，失败分类可由调用方判断。
- AccessKey 仅由环境变量注入；密钥不得进入日志或异常。`aliyun` 不记录模板参数；显式选择 `log` 时为受控开发/测试场景记录模板内容。
- 发送接口不自动重试；发送服务不可用或配置禁用时有明确结果。

### Out of Scope

- 不把源项目 `weiran-cqt` 的账号注册、登录、密码重置业务移入 system。
- 不改变当前项目的认证流程或增加短信登录。

## Capabilities

| 能力 | delta 操作 | 说明 |
|---|---|---|
| `system-sms-sender` | ADDED | system 基座向业务模块提供可配置的阿里云短信发送服务，约束发送契约、配置校验、错误分类和敏感日志。 |

## 影响的包

| ID | 包 | 动作 | 影响说明 | Owner |
|---|---|---|---|---|
| PK-1 | `weiran-framework` | 不涉及 | 不增加跨模块错误码或框架契约。 | — |
| PK-2 | `weiran-system-*` | 新增/改造 | 增加 system-api 契约、domain 端口、application 服务、infrastructure SDK 适配与装配。 | system |
| PK-3 | `weiran-app` | 改造 | 只通过现有 system 自动配置承载部署属性；无模块依赖聚合变更。 | app |
| PK-4 | `web` | 不涉及 | 不增加 HTTP、前端页面、hook 或组件。 | — |

## 共享层影响(决定能否并行)

| 类别 | 是否命中 | 说明 |
|---|---|---|
| `weiran-dependencies` BOM | ☑ | 增加 SDK 版本约束；BOM 为共享构建桶文件，需先串行修改。 |
| `weiran-system-api` 跨模块契约 | ☑ | 下游业务模块依赖新增接口与类型，先冻结签名再实现适配层。 |
| `weiran-framework` 错误码/分页契约 | ☐ | 短信结果在 system-api 自有契约中表达，不需要公共错误码或分页。 |
| `weiran-app/build.gradle.kts` 依赖聚合 | ☐ | 不新增 Gradle 模块，沿用现有依赖。 |
| 前端路由与菜单 | ☐ | 无 HTTP Controller、页面、菜单或权限点。 |

## 横切关注点

| 项 | 是否涉及 | 说明 |
|---|---|---|
| CC-1 权限点 (`sys_menu.permission`) | ☐ | 没有 HTTP 入口；由受信任的业务服务代码调用。 |
| CC-2 菜单 (`sys_menu`) | ☐ | 不新增管理菜单。 |
| CC-3 数据范围 (`data-scope`) | ☐ | 不读取或写入业务数据。 |
| CC-4 多租户隔离 (`tenant`) | ☐ | 本项目不采用多租户；凭据为 system 全局部署配置。 |
| CC-5 审计日志 (`audit-context`) | ☐ | 无用户触发的 HTTP 写接口，不记录操作日志。 |
| CC-6 字段脱敏 (`masking`) | ☑ | 各模式均掩码手机号且不输出 AccessKey；`aliyun` 不输出模板参数，`log` 仅在显式配置后记录模板内容。 |
| CC-7 幂等 (`idempotency`) | ☑ | SendSms 不幂等；SDK 与应用层均不自动重试，调用方需自行决定业务级重发策略。 |
| CC-8 导出 / 任务中心 | ☐ | 不涉及导出或异步任务中心。 |
| CC-9 工作流绑定 | ☐ | 不涉及审批工作流。 |

## Dependencies

- 产品/设计：无前端交互。
- 后端：新增 system Java API 与分层实现。
- 前端：无。
- 数据库变更：无。
- 运维/配置：新增 `WEIRAN_SYSTEM_SMS_*` 环境变量；阿里云控制台需准备签名、短信模板及仅具发送权限的 RAM 子账号。
- 测试：覆盖配置校验、返回码分类、禁用重试、敏感字段日志与自动配置模式选择。

## Risks

| 风险 | 触发场景 | 应对方案 |
|---|---|---|
| 用户重复收到短信 | SDK 或上层在网络超时后自动重试 | SDK 设置 `autoretry=false`、`maxAttempts=1`，system 服务不重试。 |
| 日志中出现验证码或个人信息 | 显式启用 `log` 模式 | 该模式限受控开发/测试环境；部署方限制日志访问和保留时间。所有模式均不输出密钥并掩码手机号。 |
| 通用层误包含验证码业务规则 | 把 CQT 验证码 service/store 一并迁移 | system 只发送由调用方给定的模板消息；验证码生命周期留在业务模块。 |

## Review Checklist

- [x] 范围和非目标已确认,且与 `interview.md` 一致
- [x] 涉及的包和 Owner 已确认
- [x] 共享层影响已勾选(直接决定 L4 分层)
- [x] 横切关注点已逐项过一遍
- [x] 数据库、接口、定时任务、配置、发布影响已列出
- [x] 测试和灰度策略已确认
