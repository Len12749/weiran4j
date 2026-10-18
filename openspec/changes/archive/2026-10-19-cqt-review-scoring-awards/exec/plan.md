---
title: "评审评分评奖执行计划"
status: "completed"
updated_at: "2026-10-17"
derived_from: "../tasks.md"
---

# Exec Plan

## 1. 任务映射

| tasks.md条目 | 执行单元ID | 层 | 负责人 |
|---|---|---|---|
| 0.1 | E0 | L0 | root |
| 1.1 | E1 | L0 | executor |
| 2.1 | E2 | L0 | executor |
| 2.2 | E3 | L0 | reviewer |
| 3.1 | E4 | L1 | executor |
| 3.2 | E5 | L1 | executor |
| 3.3 | E6 | L1 | executor |
| 3.4 | E7 | L1 | executor |
| 3.5 | E8 | L1 | reviewer |
| 4.1 | E9 | L1 | executor |
| 4.2 | E10 | L1 | reviewer |
| 5.1 | E11 | L1 | auth_audit |
| 5.2 | E12 | L1 | auth_audit |
| 6.1 | E13 | L1 | root |
| 7.1 | E14 | L1 | root |
| 8.1 | E15 | L1 | reviewer |
| 8.2 | E16 | L1 | auth_audit |
| 8.3 | E17 | L1 | executor / auth_audit |
| 8.4 | E18 | L1 | root |

### 未映射条目

无。

### L7验证范围

基线main@67b2de5，工作区含本会话先前内容和导入改动，全部保留。当前仅授权实现，不新增或运行测试；执行JDK21 spotlessApply/compileJava/compileTestJava及静态规则、前端tsc/eslint/build，记录未执行完整测试，不伪称完整L7门禁通过。

## 2. 依赖图

契约→迁移与领域端口→评审/评奖服务→控制器→前端联调→独立复核与状态文档。无环；评奖消费评审已确认最终成绩与阶段锁，先冻结共享持久化协议。

## 3. 分层执行计划

Layer0先由executor产出迁移及共享模型/事务锁协议；reviewer消费这些协议写award子域，不改review子域或共享注册文件；auth_audit先写前端类型/hooks，待接口冻结后页面对接。

## 4. 契约冻结

| 契约 | 签名/结构 | 定义位置 | 消费方 | 冻结 |
|---|---|---|---|---|
| HTTP/状态/菜单 | design DS2/3/4已批准结构 | docs/01-架构与接口契约.md本次新增节 | executor写review请求/响应；reviewer写award请求/响应；auth_audit读取所有字段 | ☑ |
| preview | token/purpose/operator/phase/inputVersion/hash/expiresAt/rows | 同上及review_previews | 后端两作者分别维护用途；前端只传token不自算分奖 | ☑ |
| 数字响应与分页 | code=0、PageResult | framework现有契约 | 全部使用既有形状 | ☑ |
| 领域事务互锁 | 同赛事phaseId升序锁、inputVersion | 评审基础仓储端口 | executor提供、reviewer消费；禁止并行各造一套锁 | ☑ |

## 5. 并行判据与文件所有权

| ID | 判断 | 本次 |
|---|---|---|
| WT-0 | 不与他人change并行 | 既有dirty均同会话成果，无他人change；保留 |
| WT-1 | 文档可交替 | root写契约/任务/状态 |
| WT-2 | 框架单点不并行 | 不改framework/system/build-logic |
| WT-3 | 单模块分工 | 同一change按文件所有权交替编写，注册/迁移单写者；不并行Gradle |

executor拥有所有review子域、报名对象修正、迁移及CQT三层AutoConfiguration；reviewer拥有award子域，向executor发送所需注册声明；auth_audit拥有web业务页/hooks/types新增段；root拥有docs/openspec状态及运行环境操作。禁止改旧迁移、上游框架、他人文件或执行真实评分颁奖。共享协议需调整先报root。

## 6. 验证归属

不新增/运行测试。后端executor单一Gradle运行者，reviewer不并发构建；前端作者执行tsc/eslint/build。独立交叉复核：auth_audit复核后端，executor/reviewer互审及审前端；root整合证据，不由作者自批自己代码。

## 7. 失败与降级

编译/静态错误由拥有者修复；契约差异先统一协议再继续，不能用占位分支或吞错绕过。单元规模过大可拆交付文件，不能缩减已批准需求。测试未授权如实写验证限制，不伪造全量通过。

## 8. 收尾

各作者将真实实现、检查和未完项写exec/notes；每项只有完整实现才勾任务。root在完整实现后生成implementation-report，记录实际静态检查与运行范围；正式verify所需完整测试未执行，不生成替代性test.log。L9签字及归档不提前执行。

## Gate

- [x] 每task独立映射执行单元
- [x] 共享契约/迁移先行
- [x] Flyway只追加，不涉及PHP共库
- [x] 依赖无环
- [x] 契约输入已冻结
- [x] 无独立测试执行单元
- [x] 文件所有权分离
- [x] 禁止修改范围明确
- [x] 失败降级策略明确
- [x] 未回写获批设计或spec

## 2026-10-17 追加执行单元

| tasks | 执行单元 | 层 | 负责人 |
|---|---|---|---|
| 8.1 | E15 | L1 | reviewer |
| 8.2 | E16 | L1 | auth_audit |
| 8.3 | E17 | L1 | executor唯一Gradle；auth_audit前端 |
| 8.4 | E18 | L1 | root |

用户已授权自动化测试，前文无测试范围仅描述上一轮。本轮Gradle仅executor运行；后端导出独占entryexport文件/三自动注册/新增权限迁移，web导出独占业务页/hook/types；root独占契约及状态。reviewer写导出由executor/root独立审，executor评审回归及改错由reviewer/root独立审。先运行已有测试，再在全部最终源码稳定后重跑门禁，避免用过期日志冒充最终验证。
