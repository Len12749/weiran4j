# 开发与部署环境约定

同一份代码有两套跑法：开发机原生进程、服务器容器。这样分工是因为两者的优化目标不同——开发机要快，服务器要一致。

## 开发机：原生进程

业务代码直接跑在宿主，改完即生效。

- 工具链：JDK 21、Node.js 22.13+、pnpm 12、MySQL 8.4（与镜像同版本）。
- 后端 `:5200`，前端 Vite `:5202`（`/api-backend` 代理到后端），热更新可用。
- **不用 Compose 起整套开发环境**：首次构建慢，且没有热更新。
- Docker 只在需要容器的场景出现，例如 `./gradlew check` 里的 Testcontainers 集成测试。

工具链可以放在仓库之外，通过环境变量指过去，不动系统环境：

```bash
# 后端：仓库根目录执行
cd server
JAVA_HOME=<JDK 21 根目录> GRADLE_USER_HOME=<Gradle 缓存目录> ./gradlew :weiran-app:bootRun

# 前端：仓库根目录执行
PATH=<Node bin>:<pnpm bin>:$PATH pnpm dev:web
```

本机把 JDK、Node、MySQL 放在仓库内的 `.tools/` 下（已 gitignore），MySQL 数据也在那里；具体路径、本地库口令见该目录的 README。`server/config/application-local.yml` 只在开发机使用，已被 gitignore，不会进镜像。

### 本机启动顺序（便携工具链）

按顺序起这几样，命令都在仓库根目录执行。

| 顺序 | 服务 | 端口 | 启动方式 |
| --- | --- | --- | --- |
| 1 | MySQL 8.4 | 3306 | 便携版 `mysqld`，数据在 `.tools/mysql-data` |
| 2 | 知识库 MCP（WeKnora-lite） | 8080 | 桌面端拉起的独立二进制，自动化任务的 `search_knowledge` 走它 |
| 3 | 后端 + 前端 + 文档站 | 5200 / 5202 / 5209 | `pnpm dev`（Turbo 一次起三个） |

```bash
# 1) MySQL：先确保 socket 目录存在，再起 mysqld（完整参数见 .tools/README.md）
mkdir -p .tools/mysql-run
.tools/mysql-8.4.0-macos14-arm64/bin/mysqld \
  --basedir=$PWD/.tools/mysql-8.4.0-macos14-arm64 --datadir=$PWD/.tools/mysql-data \
  --port=3306 --bind-address=127.0.0.1 --socket=$PWD/.tools/mysql-run/mysql.sock \
  --log-error=$PWD/.tools/mysql-run/error.log --mysqlx=0

# 2) 知识库 MCP：确认 8080 在听；没起来就先开桌面端
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/

# 3) 后端 + 前端 + 文档站
export PATH=$PWD/.tools/node-v22.23.4-darwin-arm64/bin:$PWD/.tools/npm-global/bin:$PATH
export JAVA_HOME_21=$PWD/.tools/jdk-21.0.13+9/Contents/Home
pnpm dev
```

验证：`curl -o /dev/null -w '%{http_code}\n' http://localhost:5200/api-backend/n/system/auth/listProviders` 返回 200 即后端就绪；后台是 <http://localhost:5202>，文档站是 <http://localhost:5209>。Flyway 在每次后端启动时补跑未应用的迁移，改过表结构直接重启即可。

只起后端（不想要前端和文档站）：

```bash
export JAVA_HOME=$PWD/.tools/jdk-21.0.13+9/Contents/Home
export JAVA_HOME_21=$JAVA_HOME
export GRADLE_USER_HOME=$PWD/.tools/gradle-home
node server/scripts/dev.mjs          # 等价于 ./gradlew :weiran-app:bootRun
```

四个容易卡住的点：

- **`pnpm dev` 会丢掉 `GRADLE_USER_HOME`。** Turbo 是严格模式，`turbo.json` 的 `dev.passThroughEnv` 只放行 `JAVA_HOME`、`JAVA_HOME_21`、`WEIRAN_*`、`SERVER_PORT`、`SPRING_*`，其余变量静默丢弃。所以走 `pnpm dev` 时 Gradle 用默认的 `~/.gradle`；要指定缓存目录就别走 Turbo，直接 `node server/scripts/dev.mjs`。
- **必须显式给 `JAVA_HOME_21`。** 本机没装系统 JDK，`/usr/libexec/java_home -v 21` 会失败，`dev.mjs` 只能退到 `JAVA_HOME`，都没有就直接报「找不到 JDK 21」。
- **前端只能用 `localhost:5202` 访问。** Vite 监听在 `[::1]`，用 `127.0.0.1:5202` 会连不上。
- **改 Java 代码靠 devtools 自动重启**；改了 `weiran.automation.*` 一类的配置同样要重启才生效。

自动化任务还依赖两件本机状态：云效仓库的 HTTPS 凭据（系统 git credential helper，`automation-repos/` 下的克隆要用）和 `server/config/application-local.yml` 里的云效令牌、模型 Key、知识库 MCP 地址。缺任一项时任务会在执行记录里写明原因，配置缺失还会自动停用任务。

## 服务器：Docker Compose

```bash
cp server/.env.example server/.env          # 填 WEIRAN_DB_PASSWORD、WEIRAN_JWT_SECRET
export COMPOSE_ENV_FILES=server/.env
docker compose up -d --build
```

- 三个服务：`mysql`(8.4) + `backend` + `web`(Nginx)，入口是 web 的 `5202`。
- **必须同源**：Nginx 把 `/api-backend` 反代给后端；认证 Cookie 是 `SameSite=Strict`，前后端跨域部署会表现为登录成功但立刻掉线。
- 数据与日志挂卷（`mysql-data`、`backend-logs`），`restart: unless-stopped`，后端停机留 40s。
- 只用 http 试跑时设 `WEIRAN_COOKIE_SECURE=false`；线上 HTTPS 保持默认 `true`。
- 完整步骤、反向代理与排障见 [部署与运维](/deployment)。

## 版本对齐

JDK 21、Node 22、MySQL 8.4 在开发机、CI（`.github/workflows/ci.yml`）和镜像（`server/Dockerfile`、`backend/Dockerfile`、`docker-compose.yml`）三处保持一致。改版本时一起改，别只改一边——版本漂移是"本地能跑、线上报错"最常见的来源。
