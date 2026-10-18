---
title: "评审评分评奖与作品导出验证"
status: "passed"
updated_at: "2026-10-17"
---

# 一、集成记录（L6）

单工作区，无并行集成。E0–E18 按文件所有权实施，共用协议由指定作者维护，最终由 root 唯一执行 Gradle。全部19项任务完成，无未映射实现。

## 输入检查

- 各领域、前端、导出及复核说明已读取，见 notes/、notes-web.md。
- 唯一契约先于实现更新，追加导出不改变已批准的评分和评奖规则。
- 后端完整门禁、前端自动化测试及构建已通过。

## 重复实现消除

评奖复用评审阶段锁、预览与回执，不另建确认协议。Excel二进制下载只在业务hook处理，未复制成第二套通用JSON请求框架。

## 被牺牲的方案

- 不照搬旧Python按作品平均人员目标的导出；改为明确阶段、逐评分对象，防止后续奖项错配。
- 不用作品名称定位导入对象；保留稳定ID、来源、类型和版本。
- 不为通过检查而降低覆盖率、禁用测试或修改旧迁移。

## 越界修改汇总

| 文件 | 性质 | 处置 |
|---|---|---|
| AuthIT | 新菜单落地后旧清单断言失效的必要连带 | 更新完整菜单及隐藏按钮断言，保留测试 |
| CqtSchoolImportProgressIT | 已有SQL字面量超过140字符，阻断全量Checkstyle | 仅拆分等价字符串，行为不变 |
| 前序内容、上传与学校导入脏文件 | 本会话此前成果 | 全部保留，未stash/reset/提交 |

# 二、规格一致性（L8）

## L7 硬闸门证据

`openspec/project.json` 配置的命令如下。实际执行时使用 JDK21、已有本地 Maven mirror init script，以及包内 Node CLI；Gradle `spotlessApply check assemble` 同时覆盖 assemble/test/check，前端分别执行 tsc、Vite、Vitest和全量ESLint。未放宽任何规则。

| 检查 | 配置命令 | 实际结果 | 证据 |
|---|---|---|---|
| build | `(cd weiran4j && JAVA_HOME=${JAVA_HOME_21:-$(/usr/libexec/java_home -v 21)} ./gradlew assemble --no-daemon) && pnpm --filter @weiran/web build` | Gradle BUILD SUCCESSFUL；TypeScript及Vite退出0 | evidence/build.log |
| test | `(cd weiran4j && JAVA_HOME=${JAVA_HOME_21:-$(/usr/libexec/java_home -v 21)} ./gradlew test --no-daemon) && pnpm --filter @weiran/web test` | 后端342测试、前端289测试全部通过，0失败/0跳过 | evidence/test.log |
| lint | `(cd weiran4j && JAVA_HOME=${JAVA_HOME_21:-$(/usr/libexec/java_home -v 21)} ./gradlew check --no-daemon) && pnpm --filter @weiran/web lint` | 完整check含覆盖率/静态门禁成功，前端eslint src退出0 | evidence/lint.log |

首轮发现旧菜单断言与4处新测试嵌套Mockito stubbing错误，修正后重新运行。完整check还发现学校导入测试SQL行宽，等价拆分后重跑通过。前端首次默认并发下旧App文案查找超时，未改测试，限制两个worker重跑全部通过。不以失败日志或跳过用例作为最终证据。

## tasks.md 逐条核对

| 条目 | 实现位置 | 一致 |
|---|---|---|
| 0.1 / 1.1 | docs/01、V202610170400及菜单配对 | 是 |
| 2.1 / 3.1 | domain.review、ReviewApplicationService、两报名入口及持久化来源映射 | 是 |
| 3.2 / 4.1 | 阶段维度、范围、分配、ReviewAdminController、权限与日志脱敏 | 是 |
| 3.3 / 3.4 | 本人评分/CAS、缺评汇总、纠错、重开、单向精准失效、晋级资格 | 是 |
| 2.2 / 3.5 / 4.2 | award五层、区间规则、批次替代与撤销隔离 | 是 |
| 5.1 / 5.2 | 8主页面及3隐藏Tab、types与query hooks | 是 |
| 6.1 / 7.1 | 静态及独立复核、20表现状文档与索引 | 是 |
| 8.1 / 8.2 | entryexport五层、V202610171100、导出弹层/下载及独立按钮权限 | 是 |
| 8.3 / 8.4 | 新规则/共享协议/HTTP闭环/边界/导出测试、最终门禁、契约与运行记录 | 是 |

未实现条目：无。前台查分、证书和实际奖项导入执行属于明确后续范围。

## design/specs 核对

| 能力/场景 | 验证方式 | 结果 |
|---|---|---|
| FR001–006 初审、对象、范围、分配、本人评分 | CqtReviewWorkflowIT：ENTRY及双人员对象、初审准备、规则维度、范围分配、本人访问隔离、重复提交及CAS；规则单测 | 通过 |
| FR007 缺评汇总 | HTTP预览含完整4对象、缺评1对象；未接受跳过拒绝，显式接受后只生成3有效final | 通过 |
| FR008 重开与受影响结果 | CqtReviewBoundaryIT：关闭拒写、国审重开不逆向、省国相关对象惰性失效且无关对象不受影响 | 通过 |
| FR009 晋级及独立资格 | 独立源码复核资格匹配、有效唯一关联、不可自动复制报名及保留人工决定 | 静态通过，未声明独立报名全流程自动化覆盖 |
| FR010 幂等、权限与隐私 | Support 9单测及主闭环HTTP重放/CAS；两名非作者交叉复核 | 通过；mock调用协议测试不等于数据库并发压测 |
| award规则与独立批次 | 闭区间/通配冲突规则单测；HTTP完整授奖、完整候选替代、撤旧不影响新批次 | 通过 |
| FR011 作品Excel导出 | writer/usecase单测及CqtEntryExportIT：长ID/公式文本/空表/STALE、限额、问题行、精确关键字、XLSX MIME/文件名/no-store与400错误包络；前端12回归 | 通过 |

## 越界与不一致项

无未申报越界。未修改 framework、system、build-logic 或已应用迁移。上述菜单断言、SQL字面量及新测试配置属于实现/测试错误，已回实现修复；没有修改规格迁就错误行为。

## 运行与遗留范围

后端3300已加载V202610171100，启动成功；作品初审页面可见导出图标，位于列设置与刷新之间。Chrome原生密码管理提示阻挡进一步人工点击，未绕过提示；弹层与下载由自动化测试验证，不宣称已人工下载真实作品文件。当前列表为空，未为演示创建真实赛事、作品或奖项。

本轮数据库评分、晋级和授奖写入仅发生在Testcontainers测试库。前台奖项查询仍见cqt_entries.md#02，独立国赛新作品报名仍见cqt_entries.md#06。奖项导入执行另行建设，本次仅导出供后续填写和匹配。

## 流程反馈与结论

源码修改与权限升级构建调用必须拆开；子代理审批等待时及时汇报，由root集中构建，避免团队等待不透明。作者与独立复核分离，报告不代替测试证据。

**判定：通过，进入 L9 人类审阅 diff。** 未自动提交、合并或归档。
