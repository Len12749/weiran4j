# E2: region 表和行政区划快照迁移

## 完成的 tasks.md 条目

- 2.1

## 改了什么

| 文件 | 动作 | 说明 |
|---|---|---|
| `weiran4j/weiran-region/weiran-region-infrastructure/src/main/resources/db/migration/region/V202610190001__region_initial_schema_and_seed.sql` | 新增 | 创建 `region` 表、唯一 code 与父级/级别索引；固化 31 省、342 市、2978 县快照；增加 ID 1000–1004 的菜单/按钮及超管角色绑定。 |

## 为什么这么做

- 关键决策：使用 `china-division@2.7.0` 固定快照；使用 code 字符串关联父节点并保留 source sort；同一追加迁移插入菜单权限。
- 考虑过但放弃的方案 + 放弃理由：运行期下载或依赖 Node 包会让离线部署和数据可重复性受外部影响；使用 `region_area` 与用户指定的表名不符。

## 依赖的契约

- `exec/plan.md` 第 4 节存储、权限契约；`region`、`parent_code`、省市县三层、菜单权限 ID 段。

## 越界申报

| 文件 | 为什么不得不改 | 性质(必要连带/越界扩大) | 是否可能影响其他单元 |
|---|---|---|---|
| — | — | — | — |

## 埋的坑 / 遗留

- [ ] 本地未配置测试数据库；Flyway 运行结果待集成测试环境验证。

## 自测结果

- 命令：Python 快照构建校验（生成迁移时执行）
- 结果：31/342/2978 条；3351 个唯一 code；所有非省级 parent_code 均指向上一级；迁移 SQL 约 212 KB。
