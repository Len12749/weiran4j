# E2: E2-system-auth

## 完成的 tasks.md 条目

- 2.1

## 改了什么

SystemAuthErrors 的认证码改为服务 11。

## 为什么这么做

沿用系统 api 中的专属枚举；避免把认证场景塞进框架通用码。

## 依赖的契约

- 服务 11，资源 01

## 越界申报

- 认证应用服务调用处同步，必要连带。

## 埋的坑 / 遗留

- 无。

## 自测结果

- 后端 `check`、前端测试、lint、build 与 OpenSpec 结构检查通过；L7 证据见 `../evidence/`。
