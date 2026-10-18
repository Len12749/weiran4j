# 后端收尾笔记

## 实现

在现有weiran-cqt五层内新增competition/admin子域，不增加Gradle模块或第三方依赖。提供后台赛事分页、详情、新增、全量编辑及状态命令；旧公开赛事查询及报名链路保持原协议。

- 创建固定status=0、version=0、legacyId=null，赛事与五条0/0启用通用组别同事务写入。
- 模式创建后固定；编辑/状态以expectedVersion执行CAS，年届唯一冲突转换409；保留历史nullable字段和createdAt。
- 完整管理名称、年届、说明、七个时间和三模板URL；说明保持原文，校验UTF-8容量。日期计划不自动调度phase或发布，模板仅配置不生成证书。
- 模板URL只允许绝对http/https、有host且无userinfo/控制字符；解码查询键后拒绝契约所列凭据及x-oss-/x-amz-签名参数。adapter三模板输入WRITE_ONLY，错误不回显URL原文。
- 所有写入口权限及OperationLog，空body/正ID/状态/版本校验；真实数据库分页按year/id倒序。

## 变更文件

以下目录均位于`weiran4j/weiran-cqt/`对应五层的`src/main/java/com/weiran/cqt/`：

| 层/目录 | 文件 |
| --- | --- |
| domain/competition/admin | AdminCompetition、CompetitionConfiguration、CompetitionConfigurationRules、CompetitionAdminRepository及package-info |
| api/competition/admin | CompetitionAdminView、CompetitionAdminCommand、CompetitionAdminService及package-info |
| application/competition/admin | CompetitionAdminApplicationService及package-info |
| infrastructure/competition/admin | JdbcCompetitionAdminRepository及package-info |
| adapter/competition/admin | CompetitionAdminController、CompetitionAdminRequest及package-info |
| 三层autoconfigure | 各Cqt AutoConfiguration注册新增服务、仓储、Controller |
| infrastructure resources | 追加V202610171900__cqt_competition_admin.sql |

迁移仅新增cqt_competitions.version和1130–1136菜单/按钮权限；1055目录下Calendar赛事管理排首位，新增/编辑隐藏路由与三个前端component配对。仅绑定super_admin。没有修改旧迁移、旧seed/import、报名/学校导入实现、framework/system或构建规则。

## 契约与授权

唯一契约为docs/01“赛事管理（2026-10-17）”，由root冻结；用户2026-10-17明确“实施”，design已落L3审批信息。本次未新增或运行测试；测试计划仍待单独授权。保留前序所有dirty，不提交、不stash/reset；回滚锚点HEAD67b2de5，不得整体回退前序功能。

## 验证与修复

- auth_audit独立静态复核通过，重点检查事务五组别、模式锁定/CAS、nullable映射、菜单DI和模板日志脱敏。
- 复核发现正ID应400及description不能strip历史原文，两项已修。
- 首轮编译发现ErrorProne ReturnValueIgnored：status锁定结果未使用。修为读取old并比较expectedVersion，保留SQL CAS兜底；query.split显式limit=-1消除surprising行为警告。
- root唯一Gradle runner于2026-10-18报告JDK21执行`spotlessApply assemble compileTestJava checkstyleMain spotbugsMain forbiddenApisMain`全部通过；日志`/private/tmp/cqt-competition-static.log`。compileTestJava仅编译既有测试，不表示执行测试。
- root负责后端重启/迁移与运行态检查，本笔记不把尚未完成的运行检查记作通过。

## 遗留

必要连带：同步既有AuthIT.adminMenusTree断言，将sort0的1130/1131/1132加入1055子菜单最前，原EntryReviews下标移至3；保留1054按钮不泄露到菜单树的原断言。未新增或运行测试，由root最终格式编译。

本单元无已知实现阻断。省国评审计划和证书模板仍仅存储，自动调度、证书生成和赛事删除不在本次范围。运行态迁移/页面验收由root汇总。
