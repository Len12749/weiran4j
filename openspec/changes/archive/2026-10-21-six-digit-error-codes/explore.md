---
title: "六位错误码现状调研"
status: "draft"
updated_at: "2026-10-21"
---

# Explore

## 调研范围

| 模块 | 目录/文件 | 原因 |
|---|---|---|
| 框架 | `weiran4j/weiran-framework/src/main/java/com/weiran/framework/error/` | 统一错误码契约与通用码 |
| system、region | 两模块的 `*-api` 与认证应用服务 | 专属错误定义与抛出位置 |
| web | `web/src/utils/request.ts`、`web/src/hooks/queries/identities.ts` | 按码判断会话和 SSO 提示 |
| 规格与现状 | `openspec/specs/`、`openspec/state/bizs/` | 旧五位码的当前规格与现状说明 |

## 现有实现

| 能力 | 位置 | 现状 |
|---|---|---|
| 统一码 | `weiran4j/weiran-framework/src/main/java/com/weiran/framework/error/ErrorCode.java:9` | `code()` 返回 int，`httpStatus()` 独立返回 HTTP 状态 |
| 通用码 | `weiran4j/weiran-framework/src/main/java/com/weiran/framework/error/CommonErrors.java:9` | 单一枚举供全部模块复用 |
| 系统认证 | `weiran4j/weiran-system/weiran-system-api/src/main/java/com/weiran/system/api/auth/SystemAuthErrors.java:6` | 专属认证枚举，含凭据、外部身份、手机号场景 |
| 地区 | `weiran4j/weiran-region/weiran-region-api/src/main/java/com/weiran/region/api/region/RegionErrors.java:5` | 专属区划错误枚举 |
| 响应 | `weiran4j/weiran-framework/src/main/java/com/weiran/framework/web/ErrorResponse.java:19` | 失败响应直接序列化数值码 |
| 前端 | `web/src/utils/request.ts:7` | 未登录与凭据错误分开判断，避免误清会话 |

## 可复用点

| 对象 | 位置 | 用法 | 改造 |
|---|---|---|---|
| `ErrorCode` 与枚举 | `weiran-framework`、模块 `*-api` | 保留接口、按服务号替换枚举值 | 不改接口类型 |
| `ErrorResponse` | `weiran-framework/web` | 继续序列化数字码 | 不改字段形状 |
| `ApiError` | `web/src/utils/request.ts` | 继续按数值码判断 | 同步常量 |

## 真实约束

| 约束 | 证据 | 影响 |
|---|---|---|
| 成功响应仍为数字 0 | `weiran4j/weiran-framework/src/main/java/com/weiran/framework/response/ApiResponse.java:22` | 不改前端成功判断 |
| 号段不得撞码 | `weiran4j/docs/business-modules.md:5` | 通用 10、系统 11、地区 12，各自资源 00/01 |
| 主规格须经 change 归档更新 | `openspec/rules/enforced/project.md`、`openspec/check.mjs` | 用 delta spec，不能直接改主规格 |

## 影响面

### 普通改动

| 文件/模块 | 动作 |
|---|---|
| system、region 的错误枚举与调用处 | 改造 |
| 前端请求、SSO 提示与断言 | 改造 |
| 接口契约、现状文档与主规格 | 同步 |

### 共享层命中

| ID | 判定 | 原因与动作 |
|---|---|---|
| SL-1 | 未命中 | 不改模块发现 |
| SL-2 | 未命中 | 不改 BOM |
| SL-3 | 未命中 | 不改应用依赖 |
| SL-4 | 未命中 | 不新增菜单或迁移 |
| SL-5 | 未命中 | 不新增页面 |
| SL-6 | 命中，Layer 0 | 改 `CommonErrors`；冻结码值后同步所有消费者 |
| SL-7 | 未命中 | 响应字段与类型保持不变 |
| Flyway 版本与菜单 ID | 未命中 | 不增表结构、种子数据或菜单 |

跨多个模块读取的 `ErrorCode`、`CommonErrors` 及号段登记表均归 Layer 0，串行确定映射后修改消费者。

## 对 interview 的反向修正

最终三个服务号均无前导零，数字 `code` 可保留六位；已写回 interview。

## 不会碰的目录

不改 `build-logic/`、`weiran-dependencies/`、历史 Flyway 迁移、归档 change 和决策记录。

## 风险预警

按 HTTP 前缀区间判断错误的前端代码必须改为比较 HTTP 状态或具体业务码；`media-library.tsx` 已发现此类判断。

## Gate

- [x] 共享层命中已完整列出
- [x] 与 interview 的冲突项已回写
- [x] 可复用点已确认
