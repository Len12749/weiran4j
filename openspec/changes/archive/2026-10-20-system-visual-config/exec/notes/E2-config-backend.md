# E2: 配置分组与 action 路由

## 完成的 tasks.md 条目

- 2.1
- 2.2
- 2.3
- 6.1（Controller 路由声明）

## 改了什么

| 文件 | 动作 | 说明 |
|---|---|---|
| `weiran4j/weiran-system/weiran-system-domain/src/main/java/com/weiran/platform/domain/config/ConfigGroup.java`、`ConfigGroupRepository.java` | 新增 | 以纯 Java 端口表达启用配置分组。 |
| `weiran4j/weiran-system/weiran-system-application/src/main/java/com/weiran/platform/application/config/ConfigApplicationService.java` | 改造 | 从端口读取分组，并在创建/更新时校验组存在且启用。 |
| `weiran4j/weiran-system/weiran-system-infrastructure/src/main/java/com/weiran/platform/infrastructure/persistence/MybatisConfigGroupRepository.java`、`entity/SysGroupDO.java`、`mapper/SysGroupMapper.java` | 新增 | 共享表映射与 `type=config` 查询实现。 |
| `weiran4j/weiran-system/weiran-system-infrastructure/src/main/java/com/weiran/platform/infrastructure/persistence/MybatisDictRepository.java`、`entity/SysDictItemDO.java` | 改造 | 字典查询/写入限定 `type=dict`；Java/API `dictId` 映射新列 `group_id`。 |
| `weiran4j/weiran-system/weiran-system-adapter/src/main/java/com/weiran/platform/adapter/web/ConfigController.java` | 改造 | 所有配置路由改为 `/api/system/config/{action}`，由 Spring MVC 映射生成 OpenAPI。 |
| `CreateConfigGroupCommand.java`、`CreateConfigGroupRequest.java`、`ConfigGroupRepository.java`、`ConfigApplicationService.java`、`MybatisConfigGroupRepository.java`、`ConfigController.java` | 新增/改造 | `POST /api/system/config/createGroup` 校验小写分组编码和重名，写入 `sys_group(type=config)`；需要 `system:config:create` 权限并记录操作日志。 |
| `UpdateConfigGroupCommand.java`、`UpdateConfigGroupRequest.java`、`ConfigGroupView.java`、`ConfigGroupRepository.java`、`MybatisConfigGroupRepository.java`、`ConfigController.java` | 新增/改造 | 分组查询补充 `isBuiltin`、`createdAt`；`PUT /api/system/config/updateGroup/{key}` 只改名称，编码保持不变，权限为 `system:config:update` 并记录操作日志。 |

## 为什么这么做

- 分组校验放在应用层，领域模型不依赖数据库或 Spring。
- DO/Mapper 统一命名为 `SysGroup*`，HTTP 字典 DTO 维持兼容。

## 依赖的契约

- `exec/plan.md` §4 的配置组查询、配置动作、字典 HTTP 兼容契约。

## 越界申报

无。

## 埋的坑 / 遗留

- [ ] 无。

## 自测结果

- 命令：JDK 21 `spotlessApply`；`:weiran-system-api:check`、`:weiran-system-domain:check`、`:weiran-system-application:check`、`:weiran-system-infrastructure:check`、`:weiran-system-adapter:check`；`:weiran-app:test --tests com.weiran.app.PlatformIT`
- 结果：全部通过；集成测试确认列表返回 `createdAt`/`isBuiltin`，名称更新落库且编码不变。
