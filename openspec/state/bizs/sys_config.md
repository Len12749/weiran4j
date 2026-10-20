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
> [`ConfigsPage.tsx`](../../../web/src/pages/system/configs/ConfigsPage.tsx)（页面、内嵌 `ConfigFormModal` 与 `validateConfigValue`）、
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
| 接口前缀 | `/api/system/config` |
| 权限码 | `system:config:list` · `system:config:create` · `system:config:update` · `system:config:delete`；`GET /getPublic/{key}` 为 `@PublicApi`（免登录） |
| 种子 | `system.site.title` = `Weiran Admin`（string，内置）；`general`、`user` 为配置组（`sys_group.type=config`） |

## 1. 列表与分组表单

页面通过 `GET /api/system/config/listAll` 读取全部配置项、`GET /listGroups` 读取分组；左侧分组导航按配置组展示动态表单。每个配置项使用 `field_type` 决定输入、文本、多行、开关、选择或 JSON 控件；保存时提交组内有变化的配置项。
配置项维护列表通过「配置项管理」入口使用分页查询 `GET /list`，按 `id` 升序，支持键 / 描述关键词搜索。分页默认 1 / 20，最大 200。

## 2. 字段与表单

| 字段 | 控件 / 规则 | DB 列 |
| --- | --- | --- |
| 配置键 | `system.` 前缀；内置项不可改键；格式 `^system\.[\w.-]+$` | `config_key varchar(128)` 唯一 |
| 配置值 | 按 `field_type` 渲染；必填由 `required` 控制，按 `config_type` 做后端校验 | `config_value varchar(4096)` |
| 类型 / 控件 | `config_type` 为 `string/number/boolean/json`；`field_type` 为 `input/textarea/number/switch/select/json` | `config_type varchar(16)`、`field_type varchar(16)` |
| 分组 | 分组 key 与标签由 `sys_group`（`type=config`）管理；系统内置组不可删 | `group_key varchar(64)` |
| 下拉选项 | `field_type=select` 时为 `{label,value}[]` JSON | `field_options varchar(2048) null` |
| 描述 / 必填 | 描述最多 256 字；`required` 控制表单校验 | `description varchar(256) null`、`required tinyint(1)` |

**按类型校验的口径**：

| 类型 | 前端 `validateConfigValue` | 后端 `ConfigType.validate` |
| --- | --- | --- |
| string | 不校验 | 不校验 |
| number | 去空白后非空，且 `Number(v)` 不是 `NaN` | 非空白、**首尾不能有空白**、`new BigDecimal(v)` 可解析 |
| boolean | 严格等于 `true` / `false` | 严格等于 `true` / `false` |
| json | `JSON.parse` 成功 | 非空白，Jackson `readTree` 成功且开启 `FAIL_ON_TRAILING_TOKENS` |

`is_builtin` 只能由种子写入；审计列自动填充。

## 3. 动作

| 动作 | 接口 | 权限 / 规则 |
| --- | --- | --- |
| 读配置 / 分组 | `GET /listAll`、`GET /listGroups` | `system:config:list` |
| 新增 / 更新 / 删除配置 | `POST /create`、`PUT /update/{id}`、`DELETE /delete/{id}` | `system:config:create/update/delete`；内置 key 不可改、不可删 |
| 分页与详情 | `GET /list`、`GET /get/{id}` | `system:config:list` |
| 管理分组 | `POST /createGroup`、`PUT /updateGroup/{key}`、`DELETE /deleteGroup/{key}` | `system:config:create/update/delete`；只允许删除空的自定义组 |
| 公开读取 | `GET /getPublic/{key}` | 免登录；仅允许 `system.site.*` |

## 4. 用到的公共组件

- `PageContainer`、`SearchToolbar`（`leading` / `conditions` / `onRefresh`）、`TableActions`（`useActionsColumn`）、`Permission`

## 5. 说明与建议

- 配置写入后没有缓存层，公开接口每次查库。
- **前端目前不消费任何配置**：`web/src` 里没有调用 `/api/configs/public/*` 的代码，站点标题取自构建期环境变量 `VITE_APP_TITLE`（见 #02）。
- 内置配置只保护「键不可改、不可删」，值与类型都可以改（例如把 `sys.site.title` 的类型改成 `number` 并填数字）。
- 建议：前端 number 校验与后端对齐（#01）；登录页 / 顶栏改读 `sys.site.title`，或修改种子描述（#02）。

## 6. 已知问题汇总

- **#01 🔴 P3 前端 number 校验比后端宽，部分值要提交后才被拒**
  前端用 `Number(v)` 判断：`' 12 '`、`'0x10'`、`'Infinity'`、`'1e3 '` 这类值能通过；后端要求首尾无空白且
  `new BigDecimal(v)` 可解析，这四个值都会被拒。症状：表单校验通过，提交后 Toast 显示 40000
  「configValue: 不是合法的 number 值」。

- **#02 🔴 P3 种子配置 `sys.site.title` 描述称用于前端登录页与顶栏，但前端并未读取**
  种子描述写「站点标题（前端登录页与顶栏展示）」，`GET /api/configs/public/{key}` 也专为它开放免登录读取；
  但前端 `config.ts` 的 `appTitle` 来自 `import.meta.env.VITE_APP_TITLE`（缺省 `Weiran Admin`），全仓无调用公开配置接口之处。
  症状：在系统配置页修改 `sys.site.title` 后，登录页与顶栏标题不变。

## 7. changelog

新条目插在本节最上方（按日期倒序，新在上）。

**2026-10-21**
- 系统配置升级为分组表单，配置字段支持自定义控件；新增统一 `SmsService` 配置项，常青藤验证码模板由 `system.user.cqt_sms_template_code` 维护。

**2026-09-27**
- **#04 ✅ P3 系统配置列表按 `rules/advisory/list-view.md` 约定改造**
  「新增配置」移到工具栏最左；操作列（编辑 / 删除）改用 `TableActions`，窄屏（< 992px）收成「…」；加刷新按钮与「已选条件」（关键字）。

**2026-09-26**
- **#03 ✅ P? D-008 框架重写时建立本文件**
  按 `e23c511` 的代码首次盘点 `sys_config` 的列表、表单、公开读取与已知问题。
