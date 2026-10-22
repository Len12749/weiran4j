# cqt_competitions / cqt_competition_categories / cqt_competition_groups 赛事、赛项、组别（常青藤）

> 属于 `openspec/state/`，**只描述事实，不是规范**：过期不会被 `check.mjs` 拦截，**以代码为准**，发现不符回来改这里。
> 下游（weiran-cqt）业务表，索引见 [`README.biz.md`](README.biz.md)。
>
> 事实源：
> [`CompetCategoryController.java`](../../../weiran4j/weiran-cqt/weiran-cqt-adapter/src/main/java/com/weiran/cqt/adapter/portal/CompetCategoryController.java)、
> [`CompetitionQueryApplicationService.java`](../../../weiran4j/weiran-cqt/weiran-cqt-application/src/main/java/com/weiran/cqt/application/competition/CompetitionQueryApplicationService.java)、
> [`Competition.java`](../../../weiran4j/weiran-cqt/weiran-cqt-domain/src/main/java/com/weiran/cqt/domain/competition/Competition.java)、
> [`MybatisCompetitionRepository.java`](../../../weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/java/com/weiran/cqt/infrastructure/persistence/MybatisCompetitionRepository.java)、
> [`V202610051000__cqt_competitions.sql`](../../../weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/resources/db/migration/cqt/V202610051000__cqt_competitions.sql)、
> [`scripts/biz/import/cqt_competitions.sql`](../../../scripts/biz/import/cqt_competitions.sql)、[`scripts/biz/seed/competition_next.sql`](../../../scripts/biz/seed/competition_next.sql)。
>
> 盘点基线：change `cqt-signup`（2026-10-05）。

## 0. 概要

| 项 | 值 |
| --- | --- |
| 表 | `cqt_competitions`（届）、`cqt_competition_categories`（赛项，全局）、`cqt_competition_groups`（组别，按赛事） |
| 结构来源 | FastAPI 2026 库最终结构；`cqt_competitions` 另加 `national_entry_mode`（SHARED / SEPARATE） |
| 菜单 / 页面 | 赛事评审 → 赛事管理（cqt/competition/CompetitionsPage），新增/编辑独立Tab；uniapp「作品提交」表单 |
| 接口 | 后台 `/api-backend/cqt/competitions` 分页、详情、新增、编辑、状态；公开 `competitionlists`、`getsecondcat`、`secondcategory`、`groups` |
| 数据来源 | 历史：`cqt_competitions.sql` 导入；新届：后台创建或 `competition_next.sql` 种子 |

## 1. 列表

后台按名称、年份、状态过滤并分页，year/id倒序；列表首列固定显示赛事 ID，另有查询与重置图标、列设置与刷新，无高级筛选。前台 `competitionlists` 按年份、ID 倒序，可按 `status`、`name` 过滤。

## 2. 字段与表单

| 列 | 说明 |
| --- | --- |
| `status` | 1 启用（可报名）、0 停用草稿、2 已结束（2026 届） |
| `edition` / `year` | 历史赛事保留；新赛事不再填写，可为空，编辑历史赛事时保留原值 |
| `registration_start` / `registration_end` | 报名时间窗，空为不限 |
| `national_entry_mode` | 创建后固定；SHARED 省国赛共用作品（前台报名记 BOTH）；SEPARATE 分别报名（前台记 PROVINCIAL，国赛作品走导入） |
| `version` | 编辑及状态CAS版本；成功递增，旧版本409 |
| 计划时间与三模板URL | 可空；计划不自动开关评审或公布成绩，模板采用SingleImageUpload，支持上传/原图预览/清空；仅配置不生成证书 |
| 赛项 `legacy_id` / `parent_legacy_id` | 对外 ID；一级赛项 parent 为 0；`attachment_required` 要求附件 |
| 组别 `first/second_category_legacy_id` | 0 表示该层级通用 |

## 3. 动作

| 动作 | 接口 | 规则 |
| --- | --- | --- |
| 后台查询/详情 | GET `/api-backend/cqt/competitions/list`、`/get/{id}` | list权限，完整管理字段 |
| 新增 | POST `/api-backend/cqt/competitions/create` | 草稿、legacyId=null、五通用组别同事务 |
| 编辑 | PUT `/api-backend/cqt/competitions/update/{id}` | 全量受管字段、expectedVersion；模式不可改变 |
| 状态 | POST `/api-backend/cqt/competitions/updateStatus/{id}` | 0/1/2、expectedVersion；不改变历史评审/成绩 |
| 赛事列表 | `competitionlists` | 见 §1 |
| 一级赛项 | `getsecondcat?id=<赛事>` | 赛事不存在或未启用为空；否则全局启用的一级赛项 |
| 二级赛项 | `secondcategory?id=<一级>` | 启用的二级赛项；`competition_id` 参数忽略 |
| 组别 | `groups?competition_id&firstcatid&secondcatid` | 赛事或一级为 0 时为空 |

## 4. 用到的公共组件

PageContainer、SearchToolbar、ColumnSettings、TableActions、Permission、Semi Form/DatePicker/RadioGroup/SideSheet、SingleImageUpload、usePageTabs。新增/编辑赛事的报名、省审、国审时间使用日期时间范围控件；成绩公布使用单个日期时间控件。

## 5. 说明与建议

- 单库多届：届 = 赛事；服务端忽略 `X-CQTXJ-Database`。

## 6. 已知问题汇总

- **#01 ⚠️ P2 赛项全局共用，不分届**
  所有赛事共用同一套赛项（同 2026）。症状：新一届若要调整赛项（停用 / 新增），会同时影响历届赛事的赛项名称显示与报名可选项。按届区分赛项（FastAPI 2027 的 `competition_categories.competition_id`）待定。
- **#02 🚧 P2 赛项与组别仍无后台管理（部分解决）**
  `cqt-competition-admin`已提供赛事管理及默认五组别创建；赛项和自定义组别仍需种子或直接改库，管理员无法在后台调整报名可选项。

## 7. changelog

新条目插在本节最上方（按日期倒序，新在上）。

**2026-10-22**
- 赛事列表首列显示固定的赛事 ID，方便定位新建记录。
- 新增赛事表单移除届次、年份输入，创建时写入空值；历史值编辑时保留。三组开始/结束时间改为范围选择，省国赛模式改为单选。

**2026-10-18**
- **#05 ✅ P2 三类证书模板统一单图上传（`cqt-competition-admin` 用户追加）**
  复用存储；POST `/api-backend/cqt/competitions/uploadImage`需create或update权限，图片≤10MB；兼容安全/uploads/本地路径，不存localhost origin。
- **#04 ✅ P2 change `cqt-competition-admin` 增加赛事管理**
  查询、新增、编辑、状态、全时间/模板配置与独立Tab；追加version和1130–1136菜单；默认五组别原子创建。赛项/自定义组别仍见#02。

**2026-10-05**
- **#03 ✅ P? change `cqt-signup` 建立本组表与本文件**
