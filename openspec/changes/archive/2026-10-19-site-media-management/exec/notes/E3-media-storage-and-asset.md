# E3：文件存储与媒体资产

## 完成的 tasks.md 条目

- 3.1

## 改了什么

实现 disabled/local/OSS 存储、文件签名与大小校验、资产登记分页、引用检查与安全删除；禁用存储按 503 返回。

## 为什么这么做

- 关键决策：提交后删除对象，并以独立事务清理资产行；事务失败时不会留下数据库记录指向已删除对象。
- 放弃方案：在事务提交前删除外部对象会使数据库回滚后留下失效引用。

## 依赖的契约

- `exec/plan.md` §4 媒体上传和分页响应。

## 越界申报

| 文件 | 为什么不得不改 | 性质 | 是否可能影响其他单元 |
|---|---|---|---|
| `weiran4j/weiran-framework/.../error/{CommonErrors,BizException}.java`、`weiran4j/docs/01-架构与接口契约.md` | disabled storage 按既有部署契约返回 503，需要新增通用服务不可用错误码 | 必要连带 | 通用错误码表 |
| `weiran4j/.env.example` | 登记 system local URL 默认前缀，供部署配置参考 | 必要连带 | system 文件访问地址 |

## 埋的坑 / 遗留

- 存储配置默认 disabled；需配置 system 本地目录或 OSS 参数才能上传。

## 自测结果

- 命令：`./gradlew check --no-daemon`。
- 结果：通过。
