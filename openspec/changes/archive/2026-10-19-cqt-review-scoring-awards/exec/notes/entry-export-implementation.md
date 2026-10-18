# 作品清单导出实现

- 契约：`docs/01-架构与接口契约.md`「作品清单导出（2026-10-17追加）」。
- 独立 `entryexport` 五层：API command/service；domain 行投影与仓储端口；JDBC 单查询；application 事务用例及 POI writer；后台下载控制器。
- 查询按来源期望类型左关联 ACTIVE 对象，保留缺人、未准备及来源冲突问题行，LIMIT 10001 检测超限后拒绝，禁止静默截断；不创建对象。
- 用例 READ_COMMITTED，先调用共享阶段锁/用户惰性校验；工作簿 23 列、填写说明，ID 文本、无公式及证件手机号评语，非 VALID 分数空白。
- 成功使用 `SkipApiResponse` 直出 XLSX 与 no-store，错误保留标准包络；独立 export 权限及1054追加迁移；三层自动配置已登记。
- 新增 writer/usecase 单测和 `CqtEntryExportIT`，覆盖长ID、前导零、公式文本、STALE、空表、参数/阶段、超限、真实SQL问题行、二进制下载和错误包络。
- 作者未执行 Gradle/测试；由统一 runner 运行。独立审查由 auth_audit 完成，编译/运行结论以 runner 记录为准，不以本说明代替验收。
