# demo_batch_item 批处理演示结果

## 0. 概要

| 项 | 值 |
| --- | --- |
| 表 | `demo_batch_item` |
| 模块 | 可选 `weiran-demo`，仅 local/test |
| 页面 | `/demo/batch` |

## 1. 列表

按任务 ID 分页查询全部结果，按 `item_no` 升序展示；任务结束后点击页面「结束与数据」步骤核对导入或摘要计算结果。

## 2. 字段与表单

`job_id` 与 `item_no` 组成唯一键；`item_name` 保存 Excel 的 name 或 ZIP 文件路径，`item_value` 保存 Excel 的 value 或文件 SHA-256。

## 3. 动作

Excel 逐行写入，ZIP 逐文件写入；重复执行时按唯一键覆盖，实现演示任务的幂等。

## 4. 用到的公共组件

通用 `BatchProcessor` 接口与 Semi UI 表格。

## 5. 说明与建议

仅用于说明任务处理和结果核对，不进入生产 `bootJar`；业务模块应写自己的业务表。

## 6. 已知问题汇总

无。

## 7. changelog

**2026-10-24**

- **#02 ✅ P2 演示结果分页展示**
  任务结束后可分页核对全部写入的数据；错误样例用于验证预检失败时业务表为空。

- **#01 ✅ P2 新增演示结果表**
  保存 Excel 导入行和 ZIP 文件摘要，供批处理页面核对。
