# sys_config 系统配置

> 属于 `openspec/state/`，**只描述事实，不是规范**：过期不会被 `check.mjs` 拦截，**以代码为准**，发现不符回来改这里。
> 契约文档 `weiran4j/docs/01-架构与接口契约.md` 只作索引；它与代码不一致的地方以代码为准写在下文，并在 §6 登记。
>
> 事实源：
> [`ConfigController.java`](../../../weiran4j/weiran-system/weiran-system-adapter/src/main/java/com/weiran/platform/adapter/web/ConfigController.java)、
> [`SaveConfigRequest.java`](../../../weiran4j/weiran-system/weiran-system-adapter/src/main/java/com/weiran/platform/adapter/web/request/SaveConfigRequest.java)、
> [`ConfigApplicationService.java`](../../../weiran4j/weiran-system/weiran-system-application/src/main/java/com/weiran/platform/application/config/ConfigApplicationService.java)、
> [`SystemConfig.java`](../../../weiran4j/weiran-system/weiran-system-domain/src/main/java/com/weiran/platform/domain/config/SystemConfig.java) /
> [`ConfigType.java`](../../../weiran4j/weiran-system/weiran-system-domain/src/main/java/com/weiran/platform/domain/config/ConfigType.java)、
> [`JacksonJsonSyntax.java`](../../../weiran4j/weiran-system/weiran-system-infrastructure/src/main/java/com/weiran/platform/infrastructure/json/JacksonJsonSyntax.java)、
> [`MybatisConfigRepository.java`](../../../weiran4j/weiran-system/weiran-system-infrastructure/src/main/java/com/weiran/platform/infrastructure/persistence/MybatisConfigRepository.java)、
> [`V202609260101__platform_init_schema.sql`](../../../weiran4j/weiran-system/weiran-system-infrastructure/src/main/resources/db/migration/platform/V202609260101__platform_init_schema.sql) /
> [`V202609260102__platform_seed_data.sql`](../../../weiran4j/weiran-system/weiran-system-infrastructure/src/main/resources/db/migration/platform/V202609260102__platform_seed_data.sql)、
> [`ConfigsPage.tsx`](../../../web/src/pages/system/configs/ConfigsPage.tsx)（页面、内嵌字段设置表单与分组校验）、
> [`hooks/queries/configs.ts`](../../../web/src/hooks/queries/configs.ts)。
>
> 盘点基线：`e23c511`（2026-09-26，D-008 框架重写后的首次盘点）。

## 0. 概要

| 项 | 值 |
| --- | --- |
| 表 | `sys_config` |
| 菜单 | 系统管理 › 系统配置（`sys_menu.id = 8`） |
| 路由 | `/system/configs` |
| 页面组件 | `system/configs/ConfigsPage`（同文件内的 `ConfigFormModal`） |
| 后端模块 | `weiran-system`；`ConfigController` → `ConfigApplicationService` → `MybatisConfigRepository` |
| 接口前缀 | `/api/configs`；页面另用 `/all`、`/groups` |
| 权限码 | `system:config:list` · `system:config:create` · `system:config:update` · `system:config:delete`；`GET /public/{key}` 为 `@PublicApi`（免登录） |
| 种子 | `system.site.title` = `Weiran Admin`（string，内置）；`system.user.enable_phone_login` = `false`（boolean，内置） |

## 1. 可视化表单

页面不再以分页表格为主要编辑入口，而是按代码定义的分组目录显示 Tab；当前有 `general`（通用设置）和 `user`（用户）。
分组编码暂不允许在管理页面扩充。不同模块可将自己的配置项放入同一目录；以后可以在此元数据模型上继续扩展低代码配置。
配置项可在已有分组中新增、编辑和删除（内置项仍不可删除）。

接口 `GET /api/configs/all` 返回全部配置项，`GET /api/configs/groups` 返回固定分组目录；
`GET /api/configs` 的分页列表接口仍保留。

每条配置项通过 `group_key`、`field_type`、`field_options`、`required` 描述分组、控件和校验规则。
键必须以 `system.` 开头，匿名读取仅允许 `system.site.*`。

## 2. 配置字段

| 字段 | 控件 | 校验 | DB 列 / 类型 |
| --- | --- | --- | --- |
| 配置键 | 输入；内置项不可改 | 必填、最多 128 字符、`system.` 前缀；重复 40900；内置项改键 40901 | `config_key varchar(128)` 唯一 |
| 分组 | 从内置目录选择 | 必须是代码定义的分组编码 | `group_key varchar(64)` |
| 表单字段 | 单行 / 多行 / 数字 / 开关 / 下拉 / JSON | 字段类型与值类型匹配；下拉选项为合法 JSON 数组 | `field_type varchar(16)`、`field_options varchar(2048)` |
| 必填 | 开关 | 必填项不可为空 | `required tinyint(1)` |
| 类型 | 随控件设置 | 只能 `string/number/boolean/json` | `config_type varchar(16)` 默认 `string` |
| 配置值 | 由 `field_type` 渲染 | 最多 4096 字符，并按类型校验（见下） | `config_value varchar(4096)` 默认 `''` |
| 描述 | 多行输入 | 最多 256 字符 | `description varchar(256) null` |

**按类型校验的口径**：

| 类型 | 前端校验 | 后端校验 |
| --- | --- | --- |
| string | 不校验 | 不校验 |
| number | 去空白后非空，且 `Number(v)` 不是 `NaN` | 非空白、**首尾不能有空白**、`new BigDecimal(v)` 可解析 |
| boolean | 严格等于 `true` / `false` | 严格等于 `true` / `false` |
| json | `JSON.parse` 成功 | 非空白，Jackson `readTree` 成功且开启 `FAIL_ON_TRAILING_TOKENS` |

`is_builtin` 只能由种子写入；审计列自动填充。

## 3. 动作

| 按钮 | 接口 | 权限码 | `@OperationLog` | 业务规则 / 错误码 |
| --- | --- | --- | --- | --- |
| 新增配置（工具栏最左） | `POST /api/configs` → `{id}` | `system:config:create` | ✅ 系统配置 / 新增配置 | 见 §2 |
| 编辑（行） | `PUT /api/configs/{id}` | `system:config:update` | ✅ 修改配置 | 404 配置不存在；内置项改键 40901；新值按新类型校验 |
| 删除（行「操作」列，确认后执行；窄屏（< 992px）收在「…」菜单里） | `DELETE /api/configs/{id}` | `system:config:delete` | ✅ 删除配置 | 内置 40901「内置配置不可删除」（前端按钮 disabled） |
| —（公开读取） | `GET /api/configs/public/{key}` | 免登录 | — | 键不以 `system.site.` 开头 40300「该配置不允许公开读取」；查不到 40400；按库里的真实键复核前缀 |
| —（详情） | `GET /api/configs/{id}` | `system:config:list` | — | 前端页面未调用 |

## 4. 用到的公共组件

- `PageContainer`、`Permission`、Semi UI `Form` / `Tabs`

## 5. 说明与建议

- 配置写入后没有缓存层，公开接口每次查库。
- 页面按内置分组目录生成 Tab，配置项以 `group_key` 归组；`field_type`、`field_options`、`required` 控制表单渲染和校验。键统一以 `system.` 开头，匿名读取仅允许 `system.site.*`。
- **前端目前不消费任何配置**：`web/src` 里没有调用 `/api/configs/public/*` 的代码，站点标题取自构建期环境变量 `VITE_APP_TITLE`（见 #02）。
- 内置配置只保护「键不可改、不可删」，值与表单配置都可以改。
- [系统配置说明](../../../docs/configuration.md)记录了分组和表单字段的使用方式。

## 6. 已知问题汇总

- **#01 🔴 P3 前端 number 校验比后端宽，部分值要提交后才被拒**
  前端用 `Number(v)` 判断：`' 12 '`、`'0x10'`、`'Infinity'`、`'1e3 '` 这类值能通过；后端要求首尾无空白且
  `new BigDecimal(v)` 可解析，这四个值都会被拒。症状：表单校验通过，提交后 Toast 显示 40000
  「configValue: 不是合法的 number 值」。

- **#02 🔴 P3 种子配置 `system.site.title` 前端仍未读取**
  `GET /api/configs/public/{key}` 为其开放免登录读取；
  但前端 `config.ts` 的 `appTitle` 来自 `import.meta.env.VITE_APP_TITLE`（缺省 `Weiran Admin`），全仓无调用公开配置接口之处。
  症状：在系统配置页修改 `system.site.title` 后，登录页与顶栏标题不变。

## 7. changelog

新条目插在本节最上方（按日期倒序，新在上）。

**2026-10-19**
- 新增 `user` 内置分组与 `system.user.enable_phone_login` 开关（默认关闭）。
- 系统配置改为按固定内置分组目录渲染动态表单，配置键统一使用 `system.`；站点配置独立模块已移除。

**2026-09-27**
- **#04 ✅ P3 系统配置列表按 `rules/advisory/list-view.md` 约定改造**
  「新增配置」移到工具栏最左；操作列（编辑 / 删除）改用 `TableActions`，窄屏（< 992px）收成「…」；加刷新按钮与「已选条件」（关键字）。

**2026-09-26**
- **#03 ✅ P? D-008 框架重写时建立本文件**
  按 `e23c511` 的代码首次盘点 `sys_config` 的列表、表单、公开读取与已知问题。
