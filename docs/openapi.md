# 查看 OpenAPI 与 Swagger UI

后端从 Controller 和 DTO 自动生成接口定义。开发时先按[使用指南](/usage)启动后端，并确认启用了 `local` profile。默认后端端口是 **5200**。

| 用途 | 本地地址 |
| --- | --- |
| Swagger UI 交互页面 | `http://localhost:5200/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:5200/v3/api-docs` |

也可以从终端获取定义：

```bash
curl -fsS http://localhost:5200/v3/api-docs -o openapi.json
```

`pnpm dev` 启动的管理端在 `http://localhost:5202`，文档站在 `http://localhost:5209`。开发代理只转发 `/api-backend`，因此请直接访问后端的 **5200** 端口查看 Swagger UI 和 `/v3/api-docs`。若通过 `WEIRAN_PORT` 修改了后端端口，应将上述地址中的 `5200` 换成实际端口。

OpenAPI 和 Swagger UI 仅在 `local` profile 启用；默认配置和生产 profile 中都关闭。若返回 404，先确认访问的是后端端口，再确认后端以 `local` profile 启动。部署时不要为了查看文档而将生产服务切换为 `local` profile。

接口调用遵循[接口与数据约定](/contracts)：`/api-backend/n/{module}/...` 无需用户授权，`/api-backend/{module}/...` 需要登录和相应权限。浏览器使用 Cookie 时，写请求还需 CSRF Token；独立脚本可使用登录接口的令牌模式和 Bearer 头。
