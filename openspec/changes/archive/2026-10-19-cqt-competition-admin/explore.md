---
title: "赛事管理现实校验"
status: "draft"
updated_at: "2026-10-17"
---
# Explore

## 调研范围与顺序

先以rg --files确认CQT五层competition包，再rg定位nationalEntryMode/legacyId/报名引用，定点精读下列8份业务文件，最后反查报名/学校writer引用。规则文件和组件目录作为约束来源，不复制过时模板的PHP/Drizzle/App硬路由事实。

## 现有实现与真实约束

| 能力 | 位置 | 现状/影响 |
| --- | --- | --- |
| 领域 | weiran4j/weiran-cqt/weiran-cqt-domain/src/main/java/com/weiran/cqt/domain/competition/Competition.java:17 | status0/1/2；requireOpenForSignup在52；模式决定BOTH/PROVINCIAL |
| 端口 | weiran4j/weiran-cqt/weiran-cqt-domain/src/main/java/com/weiran/cqt/domain/competition/CompetitionRepository.java:12 | 只有查询，管理另扩端口，不破坏消费者 |
| 持久化 | weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/java/com/weiran/cqt/infrastructure/persistence/MybatisCompetitionRepository.java:46 | 公开列表按year/id倒序；组别0为通配 |
| 旧视图 | weiran4j/weiran-cqt/weiran-cqt-api/src/main/java/com/weiran/cqt/api/competition/CompetitionView.java:18 | 旧前台字段不扩张，管理单独DTO |
| 表 | weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/resources/db/migration/cqt/V202610051000__cqt_competitions.sql:3 | 已有全部管理配置列；25行年届唯一、51行组别唯一；无version |
| 种子 | scripts/biz/seed/competition_next.sql:15 | 草稿，25行起创建固定五组别；可复用名称与0/0语义 |
| 报名 | weiran4j/weiran-cqt/weiran-cqt-application/src/main/java/com/weiran/cqt/application/entry/SignupApplicationService.java:94 | 读取状态/报名窗，97行取mode阶段，146行复制可空legacyId |
| 学校导入 | weiran4j/weiran-cqt/weiran-cqt-application/src/main/java/com/weiran/cqt/application/entry/SchoolImportEntryWriter.java:100 | 同报名校验，137行取模式；固定mode可避免并发转换 |

## 可复用点

复用现有五层、CQT三个AutoConfiguration、数据库分页/统一错误、菜单动态路由、PageContainer/SearchToolbar/TableActions/Permission、Semi Form与usePageTabs.closeCurrentAndGo。Calendar图标已注册。无新依赖、模块、上传协议或公共组件。

## 影响面

新增competition admin类型/端口/服务/仓储/Controller与三页面/hooks/types；CQT三个自动配置注册、追加迁移；root同步唯一docs01与现状文档。不会碰framework、system、build-logic、BOM、App.tsx、AdminLayout、uniapp、旧报名/导入链路、旧迁移或旧seed/import脚本。

### 共享层命中

| ID | 文件 | 判定 |
| --- | --- | --- |
| SL-1 | settings.gradle.kts | 未命中，已有业务五层自动发现 |
| SL-2 | weiran-dependencies/build.gradle.kts | 未命中，无依赖版本变更 |
| SL-3 | weiran-app/build.gradle.kts | 未命中，无装配依赖变更 |
| SL-4 | CQT Flyway菜单迁移 | 命中，V202610171900及1130–1136先冻结 |
| SL-5 | web/src/pages与page-registry | 命中，三component文件与迁移配对；不硬注册App |
| SL-6 | CommonErrors | 未命中，复用400/404/409 |
| SL-7 | framework page/response | 未命中，原协议复用；管理业务TS手动同步 |

| 序号资源 | 判定 |
| --- | --- |
| Flyway | V202610171900__cqt_competition_admin.sql全局唯一，仅追加 |
| 菜单 | 1130–1136当前未占，1055已存在 |
| 构建约定/BOM | 未命中，不改规则及依赖 |
| 自动配置/业务DTO | 命中，串行Layer0冻结、后端单写者 |

## 对interview的反向修正

用户只提出增加赛事管理，详细方案来自代理调查，L3回复“实施”后接受；已区分来源。省国审时间只是现有存储字段，没有自动调度；模板URL没有证书生成能力。

## 风险与Gate

NULL年届可绕过MySQL唯一键，后台写入强制非空；legacyId不是主键不可伪造；模板URL拒绝凭据且输入日志WRITE_ONLY；mode固定避免报名并发竞态。root确认既有dirty均为本会话前序，保留不stash/reset。共享层逐项已判定，可复用点已确认，无未决边界。
