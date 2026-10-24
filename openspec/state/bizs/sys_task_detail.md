# sys_task_detail 批处理逐项结果

## 0. 概要

| 项 | 值 |
| --- | --- |
| 表 | `sys_task_detail` |
| 模块 | `weiran-system` |
| 演示页面 | `/demo/batch`（仅 local/test） |
| 权限 | `system:batch-job:list` |

## 1. 列表

按 `task_id` 隔离，并先检查任务创建人。预检明细排在处理明细之前，按 `item_no` 分页，可筛选阶段和成功或失败。

## 2. 字段与表单

`task_id`、`stage`、`item_no` 唯一标识一项；`stage` 为 `validating` / `processing`；`item_name` 是行标识或文件路径；`status` 为 `succeeded` / `failed`，`message` 保存预检或处理失败原因。文件级预检错误用 `item_no=0`。

## 3. 动作

预检错误由 `BatchProgress.precheckFailure` 写入，不增加处理计数；处理结果通过 `success/failure` 写入，每项的任务计数和明细在同一数据库事务内提交。重新领取任务时清空旧明细并重置计数。

## 4. 用到的公共组件

`BatchProgress`、`PageQuery`、`PageResult`，前端使用 Semi UI 表格。

## 5. 说明与建议

本表保存处理状态，不代替业务结果表；演示结果见 `demo_batch_item`。接入方式见 `docs/batch-processing.md`。

## 6. 已知问题汇总

无。

## 7. changelog

**2026-10-24**

- **#02 ✅ P2 预检错误进入明细**
  增加 `stage`，预检失败按行或文件记录，支持阶段筛选。

- **#01 ✅ P2 新增逐项结果表**
  保存每项成功或失败状态与失败原因，供分页查看。
