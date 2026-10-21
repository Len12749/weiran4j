# API 路由命名约定

> 新增或修改 Spring Controller 路由前必读。统一规范只适用于 HTTP URL；Java 包名、类名与 DTO 命名不在本文范围。

## 路径结构

新增接口必须使用：

```text
/api-{type}/{module}/{resource}/{action}[/{id}[/{subResource}...]]
/api-{type}/n/{module}/{resource}/{action}[/{id}[/{subResource}...]]
```

- `/api-{type}/{module}/...` 是用户登录后的入口；`/api-{type}/n/{module}/...` 是未鉴权入口。`n` 是固定的免用户鉴权标记，不属于模块名。当前已登记的 `type` 是 `backend`。
- `type` 使用小写英文字母和数字，以字母开头，名称须保持稳定。新增 `type` 前必须同步注册后端认证拦截范围、前端开发代理、部署反向代理与路由守卫；只有路由和代理均生效后才能对外使用。
- `/api-{type}/{module}/...` 下每个请求都必须校验有效用户身份；接口声明了权限码时还必须校验权限。不能靠 `@PublicApi` 绕过用户鉴权。浏览器 Cookie 认证的写请求继续校验 CSRF。
- `/api-{type}/n/{module}/...` 不要求也不解析用户身份，不依赖 `CurrentUser`；公开只表示免用户鉴权，接口本身仍须执行参数校验、业务约束、限流等适用的保护。需要当前用户的流程必须放在登录后入口，例如外部账号绑定发起接口。
- 禁止使用旧 `/api/**`、`/api-n{type}/**` 前缀，或把需登录的接口放进 `/api-{type}/n/**`。同一能力有登录前和登录后操作时，按方法分别声明在对应路径下。
- `module` 是系统或业务模块。后台基座统一使用 `system`；业务模块使用已登记的模块名。
- `resource` 是模块内的业务资源或能力名，例如 `user`、`config`、`dict`、`auth`。
- `action` 是明确的操作名，使用小写动词或 lowerCamelCase。常用动作：`list`、`get`、`create`、`update`、`delete`；领域查询可用 `listGroups`、`getPublic` 等准确动作。
- 资源 ID 与其他定位参数放在 action 之后。HTTP 方法仍按读写语义使用 GET、POST、PUT、DELETE。
- 一条路径至少要有三个固定段 `{module}/{resource}/{action}`；不能把参数占位符当作 action。
- Controller 路由使用类上的 `@RequestMapping` 与方法上的标准 `@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@PatchMapping` 直接声明；不要用自定义组合注解或接口继承声明映射，以便路由守卫准确识别。

示例：

```text
GET    /api-backend/system/config/list
GET    /api-backend/system/config/get/{id}
POST   /api-backend/system/config/create
PUT    /api-backend/system/config/update/{id}
DELETE /api-backend/system/config/delete/{id}
GET    /api-backend/system/config/listGroups
GET    /api-backend/n/system/config/getPublic/{key}
GET    /api-backend/system/auth/getCurrentUser
GET    /api-backend/region/area/list
```

不要新增 `/api-backend/users`、`/api-backend/n/configs/{id}` 这类缺少模块、资源或显式动作段的资源式路径。

## 既有接口

仓库基座接口已统一迁移为上述结构。路径变更会影响所有 HTTP 调用方；迁移时必须同步更新 Controller、前后端调用、测试、回调地址与部署文档。健康探针是唯一明确保留的短路由例外。

`/api-backend/n/health` 是存活探针，允许作为明确例外。新增例外必须在 `openspec/project.json` 登记理由，不能用宽泛前缀豁免。
