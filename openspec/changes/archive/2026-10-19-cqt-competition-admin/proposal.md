---
title: "赛事管理"
owner: "user"
status: "draft"
created_at: "2026-10-17"
updated_at: "2026-10-17"
---
# Proposal

## Why

赛事目前依赖seed/运维SQL，新届无法在后台完整配置。用户要求增加赛事管理；需要安全创建草稿与报名可用配置，并保留历史数据和前台协议。

## What Changes

新增分页详情、新增编辑、状态管理；创建同事务五组别，追加version CAS；完整管理既有时间与模板列。复用已有五层、动态菜单、Semi列表及页签。legacy只读且新建null，模式创建后固定。

## Scope

### In Scope

- 赛事配置、CAS/唯一性、权限日志、独立新增编辑Tab，字段见design DS1。

### Out of Scope

- 删除赛事；赛项或组别增删改；已有作品/成绩迁移；模式修改。
- 查分、奖项导入、证书生成、自动阶段调度、定时发布。
- 变更公开报名接口字段、自动创建评审阶段、修改已执行迁移。

## Capabilities

| 能力 | delta 操作 | 说明 |
| --- | --- | --- |
| `cqt-competition` | MODIFIED / ADDED | FR004保留历史导入/seed并增加后台创建；FR005起新增管理规则 |
| `cqt-site-config` | MODIFIED | 站点两图片字段采用单图上传 |

## 影响的包

| ID | 包 | 动作 | 影响说明 | Owner |
| --- | --- | --- | --- | --- |
| PK-1 | framework | 不改 | 复用原错误分页响应 | 无 |
| PK-2 | system五层 | 不改 | 通过注解及菜单迁移复用 | 无 |
| PK-3 | app | 不改 | 不新增业务装配/测试 | root构建 |
| PK-4 | web | 新增 | 三页/hooks/类型 | auth_audit |
| 业务 | cqt五层 | 新增管理 | 全字段持久化与迁移/DI | executor |

## 共享层影响(决定能否并行)

| 类别 | 是否命中 | 说明 |
| --- | --- | --- |
| settings/BOM/build-logic | ☐ | 不新增模块依赖规则 |
| framework契约 | ☐ | 不改响应/错误码 |
| app构建 | ☐ | 无新增依赖 |
| App/AdminLayout | ☐ | 菜单驱动，无硬编码 |
| Flyway/菜单/三DI | ☑ | Layer0冻结V202610171900与1130–1136，后端单写 |
| 业务DTO/TS | ☑ | root先docs01冻结，双方依约 |

## 横切关注点

| 项 | 是否涉及 | 说明 |
| --- | --- | --- |
| CC-1 权限 | ☑ | cqt:competition:list/create/update/status |
| CC-2 菜单 | ☑ | sys_menu动态路由 |
| CC-3 数据范围 | ☐ | 未引入，沿用RBAC |
| CC-4 多租户 | ☐ | 非多租户 |
| CC-5 日志 | ☑ | 写命令OperationLog |
| CC-6 脱敏 | ☑ | 模板输入WRITE_ONLY、URL禁止凭据 |
| CC-7 幂等 | ☑ | CAS/唯一键、防重复；不承诺通用requestKey |
| CC-8 导出任务 | ☐ | 无新增导出/任务 |
| CC-9 工作流 | ☐ | 无审批引擎，计划不调度phase |

## Dependencies / Risks

用户2026-10-17批准设计。root维护docs01并唯一运行构建；前后端按冻结字段配对。追加迁移，无配置凭据；NULL年届绕唯一由后台必填避免。保留既有dirty；测试待单独授权，不新增运行。
