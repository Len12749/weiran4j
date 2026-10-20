# API 路由命名约定

> 新增或修改 Spring Controller 路由前必读。统一规范只适用于 HTTP URL；Java 包名、类名与 DTO 命名不在本文范围。

## 路径结构

新增接口必须使用：

```text
/api/{module}/{resource}/{action}[/{id}[/{subResource}...]]
```

- `module` 是系统或业务模块。后台基座统一使用 `system`；业务模块使用已登记的模块名。
- `resource` 是模块内的业务资源或能力名，例如 `user`、`config`、`dict`、`auth`。
- `action` 是明确的操作名，使用小写动词或 lowerCamelCase。常用动作：`list`、`get`、`create`、`update`、`delete`；领域查询可用 `listGroups`、`getPublic` 等准确动作。
- 资源 ID 与其他定位参数放在 action 之后。HTTP 方法仍按读写语义使用 GET、POST、PUT、DELETE。
- 一条路径至少要有三个固定段 `{module}/{resource}/{action}`；不能把参数占位符当作 action。
- Controller 路由使用类上的 `@RequestMapping` 与方法上的标准 `@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@PatchMapping` 直接声明；不要用自定义组合注解或接口继承声明映射，以便路由守卫准确识别。

示例：

```text
GET    /api/system/config/list
GET    /api/system/config/get/{id}
POST   /api/system/config/create
PUT    /api/system/config/update/{id}
DELETE /api/system/config/delete/{id}
GET    /api/system/config/listGroups
GET    /api/system/config/getPublic/{key}
GET    /api/system/auth/getCurrentUser
GET    /api/region/area/list
```

不要新增 `/api/users`、`/api/configs/{id}` 这类缺少模块、资源或显式动作段的资源式路径。

## 既有接口

仓库现存的资源式路由逐步迁移；本规则不要求一次性破坏所有已发布 URL。守卫通过精确的旧路由基线兼容未迁移接口，并阻止新增不符合约定的路由。迁移旧接口时同步更新 Controller、调用方、测试和对外契约，再从基线移除对应路由。

`/api/health` 是存活探针，允许作为明确例外。新增例外必须在 `openspec/project.json` 登记理由，不能用宽泛前缀豁免。
