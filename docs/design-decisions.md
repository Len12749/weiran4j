# 关键设计决策

本页整理当前架构仍适用的决策。实现细节见[架构说明](/architecture)，运行时约束见[接口与数据约定](/contracts)。

## 五层业务模块

业务模块分为 `api`、`domain`、`application`、`infrastructure`、`adapter`。领域层只包含业务规则与端口，不依赖 Spring 或持久化类型；应用层编排用例，基础设施层实现端口，适配层提供 HTTP 入口。完整五层目录由 Gradle 自动发现并接入应用。

## 认证由三个环节组成

系统按 JWT 的 `iss` 选择校验器，再解析本地身份并核对令牌版本，最后获取角色和权限。浏览器令牌存放在 HttpOnly Cookie，写请求使用 CSRF Token；脚本可选择 Bearer 令牌模式。外部登录最终也签发同一套本地令牌。外部身份只按 `(provider, external_id)` 关联，避免同名账号被误关联。

## 框架、基座与业务的依赖方向

框架层提供通用契约和 Spring 基础设施，系统基座提供身份、权限与平台能力，业务模块可依赖框架及系统 API。`api` 与 `domain` 只使用框架的纯 Java 契约，不接受 Spring/MyBatis 类型。第三方依赖版本由 Gradle 平台统一管理。

## 下游扩展

下游项目可以通过独立业务模块、业务默认配置、业务页面与组件扩展后台。业务模块的错误码、菜单 ID、权限前缀和 Flyway 目录应先分配独立号段，规则见[接口与数据约定](/contracts)。已合入的 Flyway 迁移保持不变，数据库变更追加新迁移。

## 构建质量

后端使用 JDK 21。格式化由 Spotless 执行，`check` 同时运行编译、测试和静态分析。Gradle 配置缓存保持关闭，因为其与项目使用的 Java 格式化工具存在不稳定的类加载兼容问题。集成测试通过 Testcontainers 启动 MySQL，需要可用的 Docker 环境。
