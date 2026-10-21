# E3: E3-region-errors

## 完成的 tasks.md 条目

- 2.2

## 改了什么

RegionErrors 三个码改为 120101 至 120103。

## 为什么这么做

地区服务独立占用 12，保留原 HTTP 状态。

## 依赖的契约

- 服务 12，资源 01

## 越界申报

- 区划集成断言同步，必要连带。

## 埋的坑 / 遗留

- 无。

## 自测结果

- 后端 `check`、前端测试、lint、build 与 OpenSpec 结构检查通过；L7 证据见 `../evidence/`。
