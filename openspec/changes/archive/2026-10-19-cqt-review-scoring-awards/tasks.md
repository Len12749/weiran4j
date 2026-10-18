---
title: "后台评审评分评奖实施任务"
status: "completed"
updated_at: "2026-10-17"
---

# Tasks

## 0. 准备

- [x] 0.1 冻结后台接口、数据表、状态、来源和菜单契约（cqt-review/FR-001、cqt-review/FR-010、cqt-award/FR-005）

## 1. 数据与菜单

- [x] 1.1 追加评审评分评奖表及菜单迁移（cqt-review/FR-001、cqt-review/FR-003、cqt-award/FR-003）

## 2. 领域/API（TG-2）

- [x] 2.1 实现评审领域规则、来源对象端口和API类型（cqt-review/FR-002、cqt-review/FR-003、cqt-entry-signup/FR-005）
- [x] 2.2 实现区间评奖领域规则与批次端口（cqt-award/FR-001、cqt-award/FR-004）

## 3. 应用与基础设施（TG-3）

- [x] 3.1 实现初审、来源对象准备及两个报名入口修正（cqt-review/FR-001、cqt-review/FR-002、cqt-entry-signup/FR-005）
- [x] 3.2 实现阶段维度、评委范围、分配预览确认（cqt-review/FR-003、cqt-review/FR-004、cqt-review/FR-005）
- [x] 3.3 实现本人评分、修订、CAS及日志隐私（cqt-review/FR-006、cqt-review/FR-010）
- [x] 3.4 实现汇总、纠错、重开失效、晋级及资格关联（cqt-review/FR-007、cqt-review/FR-008、cqt-review/FR-009）
- [x] 3.5 实现评奖规则、预览、确认、替代及撤销（cqt-award/FR-001、cqt-award/FR-002、cqt-award/FR-003、cqt-award/FR-004、cqt-award/FR-005）

## 4. 适配接口（TG-4）

- [x] 4.1 实现后台评审控制器、权限、用户快照和审计（cqt-review/FR-004、cqt-review/FR-010）
- [x] 4.2 实现后台评奖控制器及权限审计（cqt-award/FR-005）

## 5. 后台前端（TG-5）

- [x] 5.1 实现初审、配置、范围、分配与本人评分页面（cqt-review/FR-001、cqt-review/FR-003、cqt-review/FR-004、cqt-review/FR-005、cqt-review/FR-006）
- [x] 5.2 实现成绩晋级与资格、奖项规则与预览批次页面（cqt-review/FR-007、cqt-review/FR-008、cqt-review/FR-009、cqt-award/FR-001、cqt-award/FR-003、cqt-award/FR-004）

## 6. 验证范围

- [x] 6.1 执行编译、格式、静态检查与独立源码复核，记录未执行测试范围（cqt-review/FR-010、cqt-award/FR-005）

## 7. 现状同步

- [x] 7.1 同步业务表状态文档、索引及运行说明（cqt-review/FR-001、cqt-award/FR-003）


## 8. 用户追加：测试与作品导出

- [x] 8.1 实现作品Excel导出领域端口、查询、工作簿及权限接口（cqt-review/FR-011）
- [x] 8.2 实现列表导出入口、筛选弹层、下载与失败处理（cqt-review/FR-011）
- [x] 8.3 执行全量自动化测试并补关键回归，完成最终build/test/lint及独立复核（cqt-review/FR-010、cqt-review/FR-011、cqt-award/FR-005）
- [x] 8.4 更新契约、业务状态、验证及运行证据（cqt-review/FR-011）
