---
title: "system 短信发送能力任务清单"
status: "draft"
updated_at: "2026-10-19"
---

# Tasks

> 本文件是需求的权威源，任务按实现依赖排序。

## 0. 准备

- [x] 0.1 核对阿里云 SDK 的传递依赖与版本漂移影响（FR-003、FR-005）

## 1. 共享契约层 `weiran-dependencies`

- [x] 1.1 在统一依赖平台集中约束阿里云短信 SDK 版本（FR-003、FR-005）

## 2. 领域层 `*-domain` / `*-api`

- [x] 2.1 在 system-api 增加短信服务、发送命令和结果契约（FR-001、FR-002、FR-003）
- [x] 2.2 在 system-domain 增加短信发送端口和中立结果分类（FR-001、FR-003）
- [x] 2.3 更新架构与接口契约文档中的 system 短信 Java 服务边界（FR-001、FR-006）

## 3. 应用与基础设施层 `*-application` / `*-infrastructure`

- [x] 3.1 增加 application service 与自动配置，处理未配置和发送结果（FR-001、FR-002、FR-003、FR-005）
- [x] 3.2 增加阿里云 SDK sender、超时/端点配置、返回码映射和敏感日志脱敏（FR-003、FR-004、FR-005）
- [x] 3.3 增加 disabled/log/aliyun 模式装配与启动配置校验（FR-002、FR-006）
- [x] 3.4 同步 application.yml、.env.example 与部署文档中的短信环境变量（FR-006）
- [x] 3.5 增加 log 模式 sender 与 `LOGGED` 稳定结果，保留 disabled 默认值（FR-002、FR-004）
- [x] 3.6 移除短信 endpoint 环境变量，SDK 固定使用阿里云默认公网 endpoint（FR-006）

## 4. 适配层 `*-adapter`

本次不涉及 HTTP Controller。

## 5. 测试

- [x] 5.1 增加短信应用层与基础设施单元测试，覆盖请求/结果映射、SDK 请求、重试配置、脱敏与默认关闭行为（FR-001、FR-002、FR-003、FR-004、FR-005）
- [x] 5.2 增加 log 模式装配、日志内容与结果测试（FR-002、FR-004）

