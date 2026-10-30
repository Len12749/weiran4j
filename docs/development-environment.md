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

本机把 JDK、Node、MySQL 放在仓库上级的 `.tools/` 下，MySQL 数据也在那里；具体路径与启动命令见该目录的 README。`server/config/application-local.yml` 只在开发机使用，已被 gitignore，不会进镜像。

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
