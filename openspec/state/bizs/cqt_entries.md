# cqt_entries / cqt_people / cqt_entry_participants / cqt_competition_participation_keys 作品与报名（常青藤）

> 属于 `openspec/state/`，**只描述事实，不是规范**：过期不会被 `check.mjs` 拦截，**以代码为准**，发现不符回来改这里。
> 下游（weiran-cqt）业务表，索引见 [`README.biz.md`](README.biz.md)。
>
> 事实源：
> [`CompetitionController.java`](../../../weiran4j/weiran-cqt/weiran-cqt-adapter/src/main/java/com/weiran/cqt/adapter/portal/CompetitionController.java)、
> [`SignupApplicationService.java`](../../../weiran4j/weiran-cqt/weiran-cqt-application/src/main/java/com/weiran/cqt/application/entry/SignupApplicationService.java)、
> [`MyEntriesApplicationService.java`](../../../weiran4j/weiran-cqt/weiran-cqt-application/src/main/java/com/weiran/cqt/application/entry/MyEntriesApplicationService.java)、
> [`domain/entry/`](../../../weiran4j/weiran-cqt/weiran-cqt-domain/src/main/java/com/weiran/cqt/domain/entry)、
> [`MybatisEntryRepository.java`](../../../weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/java/com/weiran/cqt/infrastructure/persistence/MybatisEntryRepository.java)、
> [`V202610051001__cqt_entries.sql`](../../../weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/resources/db/migration/cqt/V202610051001__cqt_entries.sql)、
> [`V202610161200__cqt_entry_attachment_file.sql`](../../../weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/resources/db/migration/cqt/V202610161200__cqt_entry_attachment_file.sql)、
> [`scripts/biz/import/cqt_entries.sql`](../../../scripts/biz/import/cqt_entries.sql)。
>
> 盘点基线：change `cqt-signup`（2026-10-05）。

## 0. 概要

| 项 | 值 |
| --- | --- |
| 表 | `cqt_entries`（作品）、`cqt_people`（人员）、`cqt_entry_participants`（参赛人）、`cqt_competition_participation_keys`（参赛唯一键）、`cqt_evaluation_targets`（评审对象）、`cqt_app_sequences`（序列） |
| 结构来源 | FastAPI 2026 库最终结构 + 只加不改的 2027 列（`stage_scope`、`province_entry_id`、`attachment_type`、`declared_group_size`、`credential_type`） |
| 接口 | `POST /api-web/competition/signup`、`GET /api-web/competcategory/productlists`、`GET /api-web/competition/signupdetail`（均需前台登录） |
| 账号关联 | `cqt_people.(source_database, legacy_user_id)` = 账号的 `(source_database, legacy_user_id)` |

## 1. 列表

`productlists`：本账号作为参赛人的全部未删除作品，按作品 ID 倒序，`{data, total}`，不分页。

## 2. 字段与表单

| 表单字段 | 落库 | 规则 |
| --- | --- | --- |
| `title` / `competitionid` | `title` / `competition_id` | 必填 |
| `firstcatid` / `secaodcatid` | `first/second_category_legacy_id` | 一级启用；二级属于一级且启用 |
| `attachment_type` / `purl` / `purlname` | `attachment_*` | ZIP 须本系统地址且 `.zip`；FILE 为本系统存储的单个附件；链接 http(s) 无汉字；赛项要求附件时必填。数据库 enum 为 ZIP/LINK/FILE |
| `zubie` / 成员 `group` | `group_name` / `group_snapshot` | 须在该赛事、该赛项适用的启用组别内 |
| `istuandui` / `team_members` | `entry_type` / 参赛人 | 团体 ≥ 2 人、证件不重复、学校必填、手机号格式 |
| — | `entry_no` | `WEB-<赛事ID>-<10 位序号>`，序列 `fastapi_signup_entry` |
| — | `stage_scope` | 赛事 SEPARATE 为 PROVINCIAL，否则 BOTH |
| — | `source_database` / `scoring_scope` | 当前账号来源；zhongxi 为 ENTRY，qudao / houtai 为 PARTICIPANT |
| — | `review_version` | 作品及评审对象的变更版本，用于初审、对象准备和派生输入校验 |

## 3. 动作

| 动作 | 规则 / 错误 |
| --- | --- |
| 报名 | 赛事不存在 404；未启用 / 未开始 / 已结束 409；赛项 / 附件 / 组别 / 成员 400；同一证件同一赛事同一一级赛项阶段冲突 409（提示已有报名号）；同阶段并发由唯一键兜底 409；全部写入一个事务 |
| 我的报名 | 见 §1 |
| 报名详情 | 非本人 403；`msg` / `step` 按 `legacy_status`；`shengAward` / `guoAward` 为 null |
| 后台导出作品清单 | `POST /api/cqt/review/entries/export`，独立 `cqt:entry-review:export` 权限；选择赛事及省/国审阶段，按已生效筛选导出全部匹配数据，不受分页限制；10000行超限拒绝 |

## 4. 用到的公共组件

无。

## 5. 说明与建议

- 新报名及学校导入的作品 `source_database` 随当前账号；`zhongxi` 创建作品级 `ENTRY` 对象，`qudao` / `houtai` 创建逐参赛人的 `PARTICIPANT` 对象。历史作品以已存来源为准，不根据人员来源反推；已被任务或结果引用的对象不自动转换。`source_table` 保留入口标识。
- 导出Excel按预期评分对象逐行保留作品/对象/成员稳定ID与阶段、成绩状态和版本；未准备、缺成员和来源冲突保留问题行，奖项列为空。ID为文本，不导出证件、电话或评语。填写说明标记模板 `CQT_AWARD_IMPORT_V1`；实际奖项导入执行后续建设，下载不会写入奖项。

## 6. 已知问题汇总

- **#01 🚧 P2 报名不可修改 / 撤回**
  原系统的 `updatesignup`、`withdraw` 未实现（uniapp 未调用）。症状：用户填错只能找后台处理（后台作品管理另开 change）。
- **#02 🚧 P2 报名详情的省奖 / 国奖为空**
  `shengAward` / `guoAward` 恒为 null，`step` 不会到 3；本次后台评审与奖项切片不改变前台报名详情，公开查分与奖项投影留后续切片。
- **#04 ⚠️ P3 2026 历史数据中同一证件可能重复参赛**
  导入回填参赛唯一键用 `INSERT IGNORE`，重复的只保留先写入的一条；不影响新届报名。
- **#06 🚧 P2 独立国赛新作品报名入口尚未建设**
  SEPARATE赛事的省审晋级只产生国赛资格，可关联同赛事、来源、赛项且成员身份匹配的已有NATIONAL/BOTH作品；尚无合法国赛作品时显示待独立报名，当前前台不能创建新国赛报名。`cqt-review-scoring-awards`只建设资格及已有目标关联，独立报名创建留后续报名切片。

## 7. changelog

新条目插在本节最上方（按日期倒序，新在上）。

**2026-10-17**
- 用户追加授权自动化测试及作品清单导出：新增独立权限与Excel下载，保留对象标识、成绩版本和空奖项列；后端342、前端289项测试全部通过，完整后端check/assemble与前端构建、全量ESLint通过。
- change `cqt-review-scoring-awards`：新报名及学校导入按账号来源写作品、人员、参赛人与参赛唯一键，并按来源创建作品或人员级评审对象；后台增加初审、评分、汇总晋级及评奖，前台奖项查询仍后置。编译、静态门禁及独立源码复核通过；未执行真实作品评分或授奖。

**2026-10-16**
- **#03 ✅ P1 change `cqt-school-batch-import` 实现学校作品批量导入**：学校可下载模板、上传并确认 Excel/ZIP、查看任务与逐行结果；省赛/国赛奖项列在导入和模板中忽略，评价流程留待作品提交后单独处理。国赛作品入口与评价流程如需建设，另行立项。

**2026-10-05**
- **#05 ✅ P? change `cqt-signup` 建立本组表与本文件**
