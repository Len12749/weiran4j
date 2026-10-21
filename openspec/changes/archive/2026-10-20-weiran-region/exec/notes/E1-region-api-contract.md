# E1: 地区公共 API 类型

## 完成的 tasks.md 条目

- 1.2

## 改了什么

| 文件 | 动作 | 说明 |
|---|---|---|
| `weiran-region-api/src/main/java/com/weiran/region/api/region/RegionView.java` | 新增 | 定义包含 ID、code、名称、级别、父级 code、排序、状态和时间的地区视图。 |
| `weiran-region-api/src/main/java/com/weiran/region/api/region/RegionNode.java` | 新增 | 定义树节点 DTO。 |
| `weiran-region-api/src/main/java/com/weiran/region/api/region/RegionLookupService.java` | 新增 | 为其它业务提供只读启用地区树服务契约。 |
| `weiran-region-api/src/main/java/com/weiran/region/api/region/RegionErrors.java` | 新增 | 定义 40020、40920、40921 地区错误码。 |

## 为什么这么做

- 关键决策：API 模块只声明纯 Java 视图/服务/错误契约；层级规则在 domain，领域错误由 application 映射，不让 domain 依赖 API。
- 考虑过但放弃的方案 + 放弃理由：前端重用 department DTO 会把地区的自然键与三级层级语义错误地绑定到基座。

## 依赖的契约

- `exec/plan.md` 第 4 节的 `RegionView`、`RegionNode`、lookup 与地区错误码签名。

## 越界申报

| 文件 | 为什么不得不改 | 性质(必要连带/越界扩大) | 是否可能影响其他单元 |
|---|---|---|---|
| — | — | — | — |

## 埋的坑 / 遗留

- [x] API 模块 Gradle 脚本由地区模块初始化单元维护。

## 自测结果

- 命令：待完整模块构建。
- 结果：DTO 与 service interface 无框架类型；错误码按五位状态码规则定义。
