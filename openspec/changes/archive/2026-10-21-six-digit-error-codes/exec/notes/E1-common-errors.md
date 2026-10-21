# E1: E1-common-errors

## 完成的 tasks.md 条目

- 1.1

## 改了什么

CommonErrors 的服务号改为 10，ErrorCode 与响应体继续使用 int。

## 为什么这么做

保留数字 JSON 契约；放弃前导零字符串方案，因为最终编号无前导零。

## 依赖的契约

- ErrorCode.code(): int；httpStatus(): int

## 越界申报

- 相关 Java 调用处和测试断言同步，必要连带。

## 埋的坑 / 遗留

- 无。

## 自测结果

- 后端 `check`、前端测试、lint、build 与 OpenSpec 结构检查通过；L7 证据见 `../evidence/`。
