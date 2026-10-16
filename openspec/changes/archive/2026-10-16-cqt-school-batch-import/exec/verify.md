---
title: "CQT 学校作品批量导入集成与验收"
status: "passed"
updated_at: "2026-10-16"
---

# 集成记录与规格一致性校验

## 集成记录

按 E1→E2→E3→E4 顺序在同一工作区实现；此前站点配置 change 的文件改动仍保留，不计入本 change 的验收范围。已完成真实学校账号上传到数据库落作品及跨校隔离集成测试。

### 关键决策

- 从 Excel 表头名称映射 20 个报名字段，省赛奖项、国赛奖项不进入导入模型、校验、模板或结果。
- 团体按连续 N 行聚为一件作品；赛项、专业、作品名称和申报人数须组内一致，且人数等于成员行数。
- ZIP 只从本服务存储读取；拒绝越界路径、重复同名附件和超出条目/展开大小上限的压缩包。
- ZIP 命中的附件会抽取为学校账号独立存储对象并关联对应作品；团体任务对每条源数据行持久化成员回显，共享作品组结果。
- 未确认的上传会话两小时失效；过期会话无法确认且不再阻塞学校待处理检查。
- 作品导入沿用 CQT 报名校验、事务写入和参赛唯一键；评价字段仍由作品提交后的评审流程处理。

## L7 命令证据

最终 Java 与 uniapp 源码修改后执行门禁；详细结果保存在 `evidence/`：

| 检查 | 结果 | 证据 |
|---|---|---|
| JDK 21 Gradle `assemble` + web build + uniapp H5 build | 通过；Gradle assemble、Vite production build 和 uni H5 编译成功 | `evidence/build.log` |
| JDK 21 Gradle `test` + web test | 通过；Gradle 测试成功；Web Vitest 33 个文件、277 项通过，含学校导入端到端集成测试 | `evidence/test.log` |
| JDK 21 `spotlessApply check` + web lint | 通过；Gradle `BUILD SUCCESSFUL`，ESLint 退出成功 | `evidence/lint.log` |

uniapp 构建输出了现有 Sass legacy API 与 `@import` 弃用提示；编译成功，不影响本次功能验收。

## tasks.md 逐条核对

| 条目 | 实现位置 | 一致 |
|---|---|---|
| 2.1/2.2 导入领域模型、表头解析与个人/团体校验 | CQT domain/application，`SchoolImportGroupsTest`、解析器测试 | ☑ |
| 3.1/3.2 逐组事务导入、任务持久化与可恢复 worker | CQT application/infrastructure 与 Flyway migration | ☑ |
| 3.3 Excel/ZIP 解析、对象归属和压缩包安全 | `SchoolImportSpreadsheetParser`、`SchoolImportArchiveIndex`、文件存储实现及测试 | ☑ |
| 3.4 学校作品与任务查询 | `SchoolImportApplicationService`、按账号过滤的 repository | ☑ |
| 4.1 portal API 兼容和忽略请求 userid | `SchoolImportController` 与 controller contract tests | ☑ |
| 5.1 uniapp 导入页字段消费 | `uniapp/pages/my/my.vue` | ☑ |
| 6.1–6.4 解析、隔离、幂等、恢复和门禁测试 | CQT domain/application/infrastructure/adapter 单测、`CqtWebIT` 登录边界测试、uniapp build | ☑（覆盖范围见下） |
| 6.5 上传至数据库落作品的真实学校端到端验证 | `CqtWebIT.importsSchoolWorkbookAndEnforcesTaskOwnership` 通过上传、确认、worker、查库与详情接口验证作品、附件、两名成员及逐源行结果，并断言跨校 404 与过期会话拒绝 | ☑ |

## design/specs 一致性

| 验收项 | 证据 | 通过 |
|---|---|---|
| 模板按名称映射示例工作簿；联系方式与学校列正确 | `SchoolImportSpreadsheetParserTest.parsesProvidedWorkbook` | ☑ |
| 省赛/国赛列不被读取、校验、写入，空白模板无奖项列 | parser tests；模板恰含 20 个报名字段 | ☑ |
| 个人/团体分组及组内赛项、专业、作品名、人数校验 | `SchoolImportGroupsTest` | ☑ |
| 每组独立写入，错误记录并继续，重复确认/恢复不重复写作品 | writer 唯一指纹、任务行唯一键；应用层幂等测试 | ☑ |
| 学校身份、真实任务归属和 portal 响应字段 | `CqtWebIT.importsSchoolWorkbookAndEnforcesTaskOwnership` 与 controller contract tests | ☑ |
| worker 可重新领取租约过期任务 | `MybatisSchoolImportTaskRepositoryTest.reclaimsExpiredProcessingTask`（仓储逻辑单测）；端到端用例验证 worker 处理 | ☑ |
| ZIP 附件抽取、账号存储路径和作品关联 | `SchoolImportArchiveIndexTest.extractsRequestedAttachmentToOwnedStorage` 与端到端 `FILE` 类型查库断言 | ☑ |
| 上传两小时失效与待处理检查 | 端到端用例验证过期清理、待处理 false、上传更新拒绝 | ☑ |
| ZIP 路径、重复附件名、条目数和展开大小受限 | `SchoolImportArchiveIndexTest` | ☑ |

## 遗留问题

未发现阻断实现与批准规格一致性的遗留项。导入写入既有报名模型要求的作品目标关联，但不创建省赛/国赛评审或奖项记录。省赛/国赛评价数据及国赛作品入口不属于本 change；如果要建设，另行立项。

## 结论

- [x] tasks.md 全部完成
- [x] L7 build/test/lint 与 uniapp H5 build 全部通过，证据已保存
- [x] design/specs 与实现一致
- [x] 省赛/国赛评价与导入边界符合用户确认
- [x] 已知问题 #03 从 `cqt_entries.md` 欠账移入 changelog

**判定：通过，可进入 L9 人类审阅 diff。**
