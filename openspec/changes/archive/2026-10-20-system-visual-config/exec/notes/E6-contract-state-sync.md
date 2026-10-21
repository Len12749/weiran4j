# E6: 契约与状态同步

## 完成的 tasks.md 条目

- 6.1
- 6.3
- 6.4

## 改了什么

| 文件 | 动作 | 说明 |
|---|---|---|
| `web/src/types/api.ts` | 更新 | 标明 `dictId` 是兼容 HTTP 字段，对应持久化列 `group_id`。 |
| `openspec/state/bizs/sys_config.md` | 更新 | 记录共享配置组、双栏页面及新配置动作路径。 |
| `openspec/state/bizs/sys_dict.md` → `sys_group.md`、`bizs/README.md`、`cross-biz.md` | 更新 | 状态索引与关联关系改为共享表和 `group_id`。 |
| `weiran4j/docs/01-架构与接口契约.md` | 已核对 | 保留跨接口约定和业务规则，不重复 OpenAPI 的接口列表。 |
| `web/src/types/api.ts`、`openspec/changes/system-visual-config/design.md`、`exec/plan.md` | 更新 | 同步新增配置分组请求类型及 `createGroup` 路由契约。 |
| `web/src/types/api.ts`、`openspec/state/bizs/{sys_config,sys_group}.md`、`design.md`、`exec/plan.md` | 更新 | 同步分组查询的 `createdAt`/`isBuiltin` 元数据及按编码修改名称的接口契约。 |

## 为什么这么做

- 持久化列改名不改变现存字典 HTTP DTO；状态文档按实际表名调整。

## 依赖的契约

- `exec/plan.md` §4 的 OpenAPI 路由与字典 HTTP 兼容契约。

## 越界申报

| 文件 | 为什么不得不改 | 性质 | 是否可能影响其他单元 |
|---|---|---|---|
| `docs/configuration.md` | 文档中的公开读取路径需随 API 迁移更新 | 必要连带 | 否 |

## 埋的坑 / 遗留

- [ ] 无。

## 自测结果

- 命令：`node openspec/check.mjs --change system-visual-config --json`、`git diff --check`
- 结果：通过，`ok: true`，无 findings；无空白差异错误。
