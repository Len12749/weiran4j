# E4: E4-auth-consumers

## 完成的 tasks.md 条目

- 3.1

## 改了什么

系统认证、手机号登录和外部身份调用处引用新的系统码。

## 为什么这么做

用户名不存在和密码错误继续共用同一码。

## 依赖的契约

- SystemAuthErrors.code(): int

## 越界申报

- 登录与 SSO 断言同步，必要连带。

## 埋的坑 / 遗留

- 无。

## 自测结果

- 后端 `check`、前端测试、lint、build 与 OpenSpec 结构检查通过；L7 证据见 `../evidence/`。
