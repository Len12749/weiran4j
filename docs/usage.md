# 使用指南

## 环境要求

- JDK 21
- Node.js 22 或更高版本、pnpm
- MySQL 8；也可以用仓库提供的 Docker Compose 部署方式

## 本地启动

在仓库根目录执行：

```bash
mysql -uroot -e "CREATE DATABASE weiran4j DEFAULT CHARACTER SET utf8mb4"
cp weiran4j/config/application-local.yml.example weiran4j/config/application-local.yml
```

编辑 `weiran4j/config/application-local.yml`，填写数据库密码，并用 `openssl rand -base64 48` 生成 JWT 密钥。
然后启动：

```bash
pnpm install
pnpm dev
```

打开后台 `http://localhost:5373`。文档站运行在 `http://localhost:5173`。首次登录账号为 `admin`，密码为 `admin123`；部署到共享环境后应立即修改。

首次启动时 Flyway 会在空库中建表并写入种子数据。请勿将本地配置指向已有业务库。

## 常用命令

| 命令 | 用途 |
| --- | --- |
| `pnpm dev` | 同时启动后端、后台前端和文档站 |
| `pnpm dev:server` | 只启动后端 |
| `pnpm dev:web` | 只启动后台前端 |
| `pnpm dev:docs` | 只启动 VitePress 文档站 |
| `pnpm build` | 通过 Turbo 构建前端和文档站 |
| `pnpm test` | 运行前端测试 |
| `pnpm lint` | 检查前端代码 |
| `cd weiran4j && ./gradlew check --no-daemon` | 运行后端检查；需使用 JDK 21，集成测试需 Docker |
| `pnpm openspec:check` | 检查 OpenSpec 需求文档结构 |

后端也可在 `weiran4j/` 下单独运行 Gradle 命令。所有 Gradle 构建使用 JDK 21。

## 短信联调

短信发送默认关闭。受控的本地开发环境可设置 `WEIRAN_SMS_MODE=log`：系统会将模板代码和参数写入应用日志，不会发送短信。模板参数可能含验证码或个人信息，不要在生产环境启用此模式。阿里云短信凭据、签名与超时配置使用 `WEIRAN_SMS_ALIYUN_*`。

短信模板在阿里云短信控制台创建并审核，不在本项目配置模板正文。业务模块调用 `SmsService.send` 时，将阿里云模板 CODE 填入 `SmsSendCommand.templateCode`，模板变量填入 `templateParameters`。验证码的生成、校验、过期和发送频率由业务模块负责；system 只负责按模板发送。

## 部署与接口

- Docker Compose 部署步骤、环境变量和反向代理配置：`weiran4j/docs/02-部署.md`
- 数据库、权限码、错误码与跨接口业务规则：`weiran4j/docs/01-架构与接口契约.md`；HTTP 接口定义以 OpenAPI 为准
- 关键架构决策：`weiran4j/docs/00-决策记录.md`
