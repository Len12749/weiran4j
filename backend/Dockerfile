# weiran4j 前端镜像（docs/02-部署.md）。构建上下文是仓库根（pnpm 工作区要用根目录的 lockfile）：
#   docker build -f backend/Dockerfile -t weiran4j-web .
# Nginx 提供静态文件，并把 /api-backend/ 同源反代到后端（backend/nginx.conf）。

# ---- 构建 ----
FROM node:22-alpine AS build
WORKDIR /src
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY backend/package.json backend/package.json
COPY server/package.json server/package.json
RUN pnpm install --frozen-lockfile --filter @weiran/backend...
COPY backend backend
RUN pnpm --filter @weiran/backend build

# ---- 运行 ----
FROM nginx:1.27-alpine
LABEL org.opencontainers.image.source="https://github.com/weiran-tech/weiran4j"
COPY backend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/backend/dist /usr/share/nginx/html
EXPOSE 80
