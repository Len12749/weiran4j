---
title: "CQT 站点配置后台表单集成与验收"
status: "passed"
updated_at: "2026-10-16"
---

# 集成记录与规格一致性校验

## 集成记录

单执行单元、单工作区顺序实现，无重复实现需要合并。

### 被牺牲的方案

- 不新增配置表或通用动态配置编辑器；沿用 `cqt_setting` 与固定 22 项目录。
- `user_agreement` 独立使用 ident 71，`guanyuwomen` 保持 ident 2。
- 不让后台复用仅面向 Portal JWT 的公开上传接口；新增受后台权限保护的 multipart 上传端点，并复用 CQT 文件存储。

### 越界修改汇总

| 文件/范围 | 改动 | 判定 |
|---|---|---|
| `AuthIT.java`、页面注册测试 | 更新菜单根节点和动态页面注册断言以匹配新增页面/菜单 | 必要连带，保留 |
| `openspec/state/bizs/cqt_setting.md` | 同步配置页、权限、富文本与图片上传现状 | 必要连带，保留 |
| `web/src/utils/request.ts` 与测试 | 支持 multipart FormData，不覆盖浏览器生成的 boundary | 上传接口所需，保留 |

## L7 命令证据

最终源代码修改后按 `openspec/project.json` 重跑，完整日志在 `evidence/`：

| 检查 | 结果 | 证据 |
|---|---|---|
| JDK 21 Gradle `assemble` + 前端 `build` | 通过；`tsc -b && vite build` 成功 | `evidence/build.log`（2026-10-16） |
| JDK 21 Gradle `test` + 前端 `test` | 通过；Vitest 33 文件、277 项通过；另覆盖非法配置保存请求 | `evidence/test.log`（2026-10-16） |
| JDK 21 Gradle `check` + 前端 `lint` | 通过；Gradle `BUILD SUCCESSFUL`，ESLint 成功 | `evidence/lint.log`（2026-10-16） |

Vite 构建显示既有第三方 `lottie-web` eval 与 MDX module directive 警告，未影响构建。

## tasks.md 逐条核对

| 条目 | 实现位置 | 一致 |
|---|---|---|
| 1.1 契约与 ident 映射 | `weiran4j/docs/01-架构与接口契约.md` | ☑ |
| 2.1/2.2 ident 迁移、菜单权限与旧库初始化 | CQT Flyway、`scripts/biz/import/cqt_setting.sql` | ☑ |
| 3.1–4.2 配置目录、管理端口、用例与仓储 | CQT domain/API/application/infrastructure | ☑ |
| 5.1/5.2 管理 GET/PUT 与受限图片上传 | `SiteConfigAdminController` | ☑ |
| 6.1/6.2 类型、请求 hooks 与独立配置页 | `web/src/types/api.ts`、`web/src/hooks/queries/cqt-site-config.ts`、`web/src/pages/cqt/site-config.tsx` | ☑ |
| 6.3 分类 Tab | 配置页四个分类 Tab | ☑ |
| 6.4 Tiptap 与图片上传 | `SiteConfigRichEditor.tsx`、图片上传 hook | ☑ |
| 6.5 粘贴图片上传并插入 | `SiteConfigRichEditor.tsx` 的 `handlePaste` | ☑ |
| 7.1–7.3 领域/接口/上传/富文本/Tab 测试 | CQT domain 测试、`CqtWebIT.java`、前端测试 | ☑ |
| 7.4 粘贴图片与普通文本行为 | `site-config.test.tsx` | ☑ |
| 7.5 非法配置保存请求返回 400 | `SiteConfigAdminApplicationServiceTest`、`CqtWebIT` | ☑ |
| 8.1 发布与初始化顺序 | 契约、迁移与 tasks 发布说明 | ☑ |

未实现条目：无。

## design/specs 一致性

| 验收项 | 证据 | 通过 |
|---|---|---|
| 管理端固定键集 raw GET/PUT、权限与操作日志 | `CqtWebIT`、前后端全量门禁 | ☑ |
| `user_agreement` 使用 ident 71 且与 ident 2 分离 | domain / integration tests | ☑ |
| 四个 Tab 保持跨 Tab 修改并一并提交 | `site-config.test.tsx` | ☑ |
| 11 个富文本键保留 HTML，普通值继续清洗 | `SiteConfigTest`、`CqtWebIT` | ☑ |
| 上传权限、扩展名与大小校验；上传后插入存储 URL | `CqtWebIT`、前端编辑器测试 | ☑ |
| 粘贴剪贴板图片触发同一上传接口，纯文本粘贴不上传 | 前端编辑器测试 | ☑ |
| null 未编辑时仍按 null 保存 | 配置页回归测试 | ☑ |

## 不一致项及遗留问题

独立代码审查发现管理保存接口在 `items` 缺失/null 或包含 null 项时可能触发 NPE 并返回 500。已在 controller/application 边界补充校验，明确返回 400，并以应用层与集成测试覆盖；无未处置不一致项或决定不修的越界问题。

## 结论

- [x] 集成完成，变更范围已记录
- [x] L7 build/test/lint 全部通过（2026-10-16 全量复跑）
- [x] tasks.md 全部完成
- [x] design/specs 与实现一致
- [x] 无未申报越界或遗留问题

**判定：通过，可进入 L9 人类审阅 diff。**
