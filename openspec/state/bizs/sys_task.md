# sys_task 批处理任务

## 0. 概要

| 项 | 值 |
| --- | --- |
| 表 | `sys_task` |
| 模块 | `weiran-system` |
| 演示页面 | `/demo/batch`（仅 local/test） |
| 权限 | `system:batch-job:create/list/cancel` |

## 1. 列表

按创建人隔离，可按 `kind` 列表筛选后返回最近 30 个任务，按 ID 倒序。通用工作台只展示配置的处理类型，列表显示文件名、状态和进度。

## 2. 字段与表单

`kind` 标识处理器；`source_key` 是私有输入文件键，保留期后清空。`status`、`phase`、`total`、`processed`、`succeeded`、`failed` 保存进度；阶段依次为 `waiting`、`preparing`、`validating`、`processing`、`finished`，失败或取消时保留停止所在阶段。`error_message` 保存任务级失败原因。`worker` 与 `heartbeat_at` 用于领取及过期恢复；`created_by` 用于所有查询的用户隔离。

## 3. 动作

上传并创建、查询列表和详情、协作式取消。worker 原子领取任务，逐项提交计数和 `sys_task_detail` 明细；致命异常结束任务。中断后任务可重新领取，业务处理器应保证幂等。

## 4. 用到的公共组件

`BatchProcessor`、`BatchProgress`、`BatchFileStorage`；前端复用 `BatchJobWorkbench`，用 Semi UI Step、表格与进度组件。

## 5. 说明与建议

任务源文件不进入 `sys_media`，完成后保留 7 天；任务和明细继续保留。Excel/ZIP 演示处理器在可选的 `weiran-demo` 模块中。接入方式见 `docs/batch-processing.md`。

## 6. 已知问题汇总

无。

## 7. changelog

**2026-10-24**

- **#04 ✅ P2 类型筛选与可复用工作台**
  列表支持按处理类型筛选，前端业务页可配置类型与结果组件。

- **#01 ✅ P2 私有任务输入保留期清理**
  任务完成 7 天后清除源文件对象，任务和明细继续保留。

- **#02 ✅ P2 长耗时项目续租**
  独立线程每 30 秒更新租约。

- **#03 ✅ P2 批处理任务落地**
  新增任务上传、领取、进度、取消及逐项结果入口。
