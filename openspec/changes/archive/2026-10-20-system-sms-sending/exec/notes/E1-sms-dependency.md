# E1: 短信依赖

## 完成的 tasks.md 条目

- `0.1`
- `1.1`

## 改了什么

| 文件 | 动作 | 说明 |
|---|---|---|
| `weiran4j/weiran-dependencies/build.gradle.kts` | 修改 | 集中约束 `com.aliyun:dysmsapi20170525:4.6.0`。 |

## 为什么这么做

- 关键决策：沿用源项目 SDK 版本并由本仓统一依赖 BOM 管理。
- 考虑过但放弃的方案：在 infrastructure 直接写 SDK 版本；这会产生第二个版本来源。
- 依赖核对发现 okhttp 为 `4.12.0`，与框架解析版本一致，无需新增 CP-6 豁免。

## 依赖的契约

- infrastructure 使用 BOM 约束，无版本依赖声明。

## 越界申报

无。

## 埋的坑 / 遗留

- [ ] 无。

## 自测结果

- 命令：`./gradlew :weiran-app:dependencyInsight --dependency dysmsapi20170525 --configuration runtimeClasspath`、`./gradlew :weiran-app:dependencyInsight --dependency okhttp --configuration runtimeClasspath`、`./gradlew :weiran-app:verifyFrameworkVersions --no-daemon`
- 结果：SDK 解析为 `4.6.0`，okhttp 为 `4.12.0`，框架版本校验通过。
