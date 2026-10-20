# E7: 配置与迁移集成验证

## 完成的 tasks.md 条目

- 6.2
- 7.1
- 7.2

## 改了什么

| 文件 | 动作 | 说明 |
|---|---|---|
| `weiran4j/weiran-app/src/test/java/com/weiran/app/PlatformIT.java` | 更新 | 断言原字典组类型/字典项关系保留、新配置组目录内容、手机号登录开关值，并将配置 API 用例切到新 action 路径。 |
| `weiran4j/weiran-app/src/test/java/com/weiran/app/PlatformIT.java` | 更新 | 使用具备配置创建权限的管理员新增配置组，并验证写入 `type=config` 后出现在配置组列表。 |
| `weiran4j/weiran-app/src/test/java/com/weiran/app/PlatformIT.java` | 更新 | 校验分组查询返回创建时间与内置标识，并通过编辑接口更新分组名称且保留编码。 |

## 为什么这么做

- 在真实 MySQL Testcontainers 环境验证 Flyway 升级与新旧功能连接。

## 依赖的契约

- `exec/plan.md` §4 的共享组表、配置组查询、配置动作契约。

## 越界申报

无。

## 埋的坑 / 遗留

- [ ] 无。

## 自测结果

- 命令：JDK 21 `./gradlew :weiran-system-api:check :weiran-system-domain:check :weiran-system-application:check :weiran-system-infrastructure:check :weiran-system-adapter:check :weiran-app:test --tests com.weiran.app.PlatformIT --no-daemon`
- 结果：五个 system 模块检查与 `PlatformIT` 全部通过；集成用例验证新增分组的权限、`type=config` 持久化、列表元数据和名称编辑；前端测试验证默认分组、元数据显示和编辑交互。
