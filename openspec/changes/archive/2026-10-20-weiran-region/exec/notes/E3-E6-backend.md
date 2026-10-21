# E3–E6: 地区后端实现

## 完成的 tasks.md 条目

- `3.1`、`3.2`
- `4.1`、`4.2`
- `5.1`
- `7.1`（领域规则、筛选/祖先补齐与管理树排序）
- `7.2`（CSV 编码与字段转义；认证、权限和审计 HTTP 集成覆盖留给 E10）

## 改了什么

| 层 | 文件 | 说明 |
|---|---|---|
| domain | `Region`、`RegionLevel`、`RegionHierarchy`、`RegionRepository` | 地区聚合、级别递进、父级存在/环校验、叶节点删除规则与存储端口。 |
| api | `weiran-region-api/build.gradle.kts` | 采用 E1 已定义的视图、错误码和登录态 lookup 契约。 |
| application | `RegionApplicationService`、筛选条件与 create/update 命令 | 实现树、平铺、详情、CRUD、筛选祖先补齐、启用路径 lookup、领域错误映射及事务边界。 |
| infrastructure | `RegionDO`、`RegionMapper`、`MybatisRegionRepository`、自动配置与 imports | 映射 `region` 与审计字段；parent_code 更新策略允许显式清空，代码不访问 `sys_*` 表。 |
| adapter | 请求 DTO、`RegionController`、自动配置与 imports | 管理权限、仅登录 lookup、写操作审计、UTF-8 BOM CSV 下载和服务端输入校验。 |
| 测试 | domain / application / adapter 测试 | 覆盖级别/父级规则、环、叶节点删除、祖先补齐、ID 倒序、停用祖先整支过滤、code 固定、子级级别约束及 CSV 转义。 |

## 为什么这么做

- 关键决策：Domain 不依赖地区 API；Application 将层级错误映射到 `RegionErrors`。管理树同级按 ID 倒序，lookup 保持 sort/code 顺序。已有地区 code 按批准的 proposal 固定，避免改变自然键造成子级孤立。
- `PUT` 请求单独记录 `parentCode` 属性是否出现；显式 `null` 可清空父级，缺省属性则保留现值。MyBatis 对 `parent_code` 使用 `FieldStrategy.ALWAYS`，确保显式清空写入数据库。
- lookup 先验证从命中节点到根的完整路径，路径中存在停用祖先时整支排除，避免把后代提升为根。
- 考虑过但放弃的方案：依赖页面过滤停用祖先；后端 lookup 仍需保证共享选择数据自身完整，因此在 application 层过滤整条路径。

## 依赖的契约

- `exec/plan.md` 第 4 节地区树、平铺/详情、写接口、lookup、CSV、存储与权限契约。
- `weiran-region-api` 的 `RegionView`、`RegionNode`、`RegionLookupService` 和 `RegionErrors`。
- 已批准设计：业务表为 `region`；创建时 code 唯一，更新禁止更改 code；级别仅 province/city/county。

## 越界申报

| 文件 | 为什么不得不改 | 性质 | 是否可能影响其他单元 |
|---|---|---|---|
| 无 | 改动均在地区模块及本执行记录范围内。 | — | 否 |

## 埋的坑 / 遗留

- [ ] HTTP 端点的认证/权限拒绝、操作日志、CSV 下载头及数据库迁移行为仍需 E10 集成测试环境验证；当前 adapter 测试直接验证 CSV 输出内容。
- [x] 未触碰用户已有的 `web/.env.development`；未提交变更。

## 自测结果

- 命令：JDK 21 下 `./gradlew spotlessApply :weiran-region-domain:test :weiran-region-domain:jacocoTestCoverageVerification --no-daemon`
- 结果：通过；domain 单测与既有 JaCoCo 行覆盖率门槛均通过，生成报告显示行覆盖率 98%。
- 首轮模块验证：`spotlessApply :weiran-region-domain:test :weiran-region-application:test :weiran-region-adapter:test :weiran-region-infrastructure:compileJava` 通过。
- 测试覆盖：domain 9 项；application 5 项；adapter CSV 1 项。
