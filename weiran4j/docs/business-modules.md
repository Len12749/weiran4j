# 业务模块号段登记

> 本表是通用后台仓库的号段登记入口。上游只维护表头与框架/基座行；下游模块由其维护者登记。

| 模块 | 错误码序号段 | 菜单 ID 段 | 权限码前缀 | Flyway 目录 · 表名 |
|---|---|---|---|---|
| 框架与基座（`weiran-framework`、`weiran-system`） | `00`–`19` | `1`–`999` | `system:` | `system/`、`platform/` · `sys_` |
| `weiran-region` | `20`–`29` | `1000`–`1099` | `region:` | `region/` · `region` |
