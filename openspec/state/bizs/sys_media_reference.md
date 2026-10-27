# sys_media_reference 资源引用

## 0. 概要

| 项 | 值 |
|---|---|
| 表 | `sys_media_reference` |
| 模块 | `weiran-system` |
| 用途 | 多来源资源引用索引和删除保护 |

## 1. 列表

没有独立管理页；资源库列表按当前页文件 URL 批量查询引用位置，并与来源检查器结果合并。

## 2. 字段与表单

`id` 自增主键；`media_id` 指向 `sys_media.id`；`source_type` 为来源类型；`source_id` 为来源记录标识；`field_key` 为来源字段；`label` 为显示位置。`media_id + source_type + source_id + field_key` 唯一。无需直接编辑表单。

## 3. 动作

来源字段保存时全量替换该字段的资源引用，来源删除时清空；资源删除先检查全部已登记和实时检查到的引用，再锁住资源行并重新检查索引。

## 4. 用到的公共组件

`MediaReferenceService`、`MediaReferenceChecker` 与资源库列表。

## 5. 说明与建议

业务模块只通过 `weiran-system-api` 接入，写入需与来源字段共用事务；旧数据可由检查器过渡。完整时序见 [`docs/资源库引用与删除设计.md`](../../../docs/资源库引用与删除设计.md)。

## 6. 已知问题汇总

业务模块接入进度见 [`sys_media.md#03`](sys_media.md#03)。

## 7. changelog

**2026-10-27**
- 用户头像改为 `sys_user.avatar` 文字/背景色 JSON 后，清理历史 `user_avatar` 引用；资源引用不再包含用户头像。

**2026-10-24**
- 新增引用索引、配置和头像回填，以及多来源登记入口。
