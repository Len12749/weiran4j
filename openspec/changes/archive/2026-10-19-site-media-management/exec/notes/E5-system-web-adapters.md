# E5：system HTTP 适配

## 完成的 tasks.md 条目

- 4.1

## 改了什么

新增 `/api/system/**` 管理 API，接入 system 权限、统一响应、操作日志及本地图片访问。本地 system 图片 URL 使用独立的 `/system-uploads/**` 前缀；资源映射保留 `media/` 对象路径。

## 为什么这么做

- 关键决策：管理 API 与菜单统一归属 system。
- 放弃方案：加入业务专属路由会让通用后台依赖下游业务。

## 依赖的契约

- `exec/plan.md` §4 路由、权限及 DTO 契约。

## 越界申报

| 文件 | 为什么不得不改 | 性质 | 是否可能影响其他单元 |
|---|---|---|---|
| `weiran4j/weiran-app/src/main/resources/application.yml` | 配置 system 存储目录和独立的静态访问前缀 | 必要连带 | system 图片 URL |

## 埋的坑 / 遗留

- 既有迁移文件保持不动，新增迁移清除已创建的旧菜单记录。

## 自测结果

- 命令：`./gradlew check --no-daemon`。
- 结果：通过。
