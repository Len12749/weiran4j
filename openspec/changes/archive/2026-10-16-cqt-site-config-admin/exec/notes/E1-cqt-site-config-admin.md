# E1 执行记录

## 完成内容

- 完成契约、Flyway 数据初始化、旧库导入幂等逻辑、CQT domain/API/application/infrastructure/adapter、后台页面及对应测试；`tasks.md` 1.1–8.1 全部完成。
- 将 `user_agreement` 从 ident 2 拆到 ident 71，迁移时从 ident 2 最大 id 行复制初始内容；旧库导入只在 ident 71 缺失时初始化，避免重跑覆盖单独编辑值。
- 管理接口读写原始配置内容；更新已有 ident 最大 id 行，缺行才新增。
- 必要连带：更新 `AuthIT` 的菜单根节点断言及页面注册测试，以匹配新增后台页面/菜单；更新 `cqt_setting` 现状文档。
- 按用户追加需求，将配置表单分为四个 Tab；为 11 个富文本配置项接入 Tiptap，并新增带权限校验的图片上传 API（仅 JPG/JPEG/PNG/GIF，最大 10 MB），上传后将 CQT 存储 URL 插入 HTML。
- 按用户追加需求支持剪贴板图片粘贴：编辑器识别图片文件后调用同一上传 mutation，并将上传 URL 插入当前光标处；普通文本继续使用编辑器默认粘贴行为。
- 公开 `getconfig` 对上述富文本键保留 HTML，其它文本值仍沿用旧清洗规则；管理页保持未编辑的 null 原值。

## 方案取舍

- 用户确认两个字段拆成独立值：沿用 `guanyuwomen→ident 2`，为 `user_agreement` 分配 ident 71。
- 后台采用独立页面及完整表单提交，页面通过菜单组件 `cqt/site-config` 挂载。
- 操作日志遵循现有 `@OperationLog` 行为：请求体按框架规则脱敏并截断到 4096 字符。用户已批准保留此行为；字段为公开站点内容。
- 单工作区串行执行，无并行集成；没有重复实现需要合并，也没有牺牲方案待记录。

## 验证

- JDK 21 Gradle `assemble`、`test`、`check` 与前端 `build`、`test`（33 文件、277 项通过）、`lint` 均按 `openspec/project.json` 重跑；最终前端日志追加于 `exec/evidence/`。
- Vite 构建有第三方 `lottie-web` eval 与 MDX module-directive 警告，不影响构建。
