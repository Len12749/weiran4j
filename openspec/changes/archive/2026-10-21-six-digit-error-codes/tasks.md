---
title: "六位错误码任务"
status: "draft"
updated_at: "2026-10-21"
---

# Tasks

## 1. 共享契约层 `weiran-framework`(TG-1)

- [x] 1.1 通用错误迁到服务号 10，统一响应保持数字类型且 HTTP 状态独立（admin-foundation/FR-001、admin-foundation/FR-003、admin-foundation/FR-004、admin-foundation/FR-006、admin-foundation/FR-007、admin-foundation/FR-009、admin-foundation/FR-011、admin-foundation/FR-013、user-list-search/FR-003、user-list-search/FR-004）

## 2. 领域层 `*-domain` / `*-api`(TG-2)

- [x] 2.1 系统认证专属错误迁到服务号 11，维持认证安全语义（admin-foundation/FR-002、admin-foundation/FR-012、external-identity/FR-002、external-identity/FR-003、external-identity/FR-004、external-identity/FR-005、external-identity/FR-006、external-identity/FR-007、external-identity/FR-008）
- [x] 2.2 地区专属错误迁到服务号 12（administrative-regions/FR-008）

## 3. 应用与基础设施层 `*-application` / `*-infrastructure`(TG-3)

- [x] 3.1 系统认证及外部身份调用处使用对应系统码，保留日志与重定向语义（admin-foundation/FR-002、external-identity/FR-002、external-identity/FR-004、external-identity/FR-005）

## 5. 前端 `web`(TG-5)

- [x] 5.1 按新码识别会话失效、凭据错误和 SSO 错误，移除旧码数值区间判断（admin-foundation/FR-001、admin-foundation/FR-002、external-identity/FR-002、external-identity/FR-004）

## 6. 测试

- [x] 6.1 覆盖码值唯一、服务号、HTTP 响应、前端分支并运行后端和前端检查（admin-foundation/FR-001、administrative-regions/FR-008）

## 7. 文档

- [x] 7.1 同步号段登记、接口契约、现状说明与三份既有能力的 delta spec（admin-foundation/FR-001、administrative-regions/FR-008）
