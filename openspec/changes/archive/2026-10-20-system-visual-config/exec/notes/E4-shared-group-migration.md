# E4: 共享分组迁移

## 完成的 tasks.md 条目

- 4.1

## 改了什么

| 文件 | 动作 | 说明 |
|---|---|---|
| `weiran4j/weiran-system/weiran-system-infrastructure/src/main/resources/db/migration/platform/V202610201001__platform_unify_dict_config_groups.sql` | 新增 | 重命名 `sys_dict` 为 `sys_group`，新增类型/排序，调整唯一索引，`dict_id` 改为 `group_id`，追加 `general` 与 `user` 配置组。 |

## 为什么这么做

- Flyway 只追加迁移；原字典 ID 与字典项关联值原地保留。
- `type` 默认 `dict`，兼容迁移时现存字典数据；配置组显式写入 `config`。

## 依赖的契约

- `exec/plan.md` §4 的共享组表结构。

## 越界申报

无。

## 埋的坑 / 遗留

- [ ] 无；MySQL 迁移已由 `PlatformIT` 覆盖。

## 自测结果

- 命令：`./gradlew :weiran-app:test --tests com.weiran.app.PlatformIT --no-daemon`（JDK 21）
- 结果：通过。
