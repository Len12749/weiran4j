# cqt_review_dimension_scores 各修订维度评分与评语

## 0. 概要

weiran-cqt的review子域。结构来源为追加迁移V202610170400__cqt_review_scoring_awards.sql。实现已完成，JDK21编译及静态检查通过；本地迁移已应用。后台主列表已只读查看；用户追加授权后，项目全量自动化测试、覆盖率和静态门禁通过，具体验证范围见本 change 报告。未对真实业务作品评分、晋级或授奖。

## 1. 列表

后台“赛事评审”下对应业务页面按赛事、阶段及权限查看；明细/历史按所属对象读取，不提供前台公开查询。内部快照与回执不独立暴露通用CRUD。

## 2. 字段与表单

字段：id, score_revision_id, dimension_id, value, comment, created_at, updated_at。

索引/唯一约束：UNIQUE KEY uk_revision_dimension(score_revision_id,dimension_id)。

## 3. 动作

作品初审→按来源准备目标→评委范围与分配→本人评分→关闭后汇总→复核晋级。所有业务写入校验权限、归属和版本，保留必要原因与操作者。确认操作保存requestKey和payloadHash；预览按用途、当前用户及30分钟有效期校验。

## 4. 用到的公共组件

PageContainer、SearchToolbar图标查询/重置、列设置、TableActions、Permission、usePageTabs；表单及预览复用Semi组件。内部表通过所属业务用例展示，不新增数据库编辑器。

## 5. 说明与建议

中戏按ENTRY、渠道/后台按PARTICIPANT。综合分独立填写，维度留档；普通平均HALF_UP两位。初审不数值评分。成绩与奖项确认分开；STALE/REVOKED记录保留但不能冒充有效结果。前台查分和证书生成/公开后置。日志不得包含证件全文或评分评语。

## 6. 已知问题汇总

未发现本轮剩余实现阻断。自动化测试已在隔离测试库执行，未执行真实业务写入；独立国赛新作品报名、前台查分和证书仍后置，见 cqt_entries.md#06。

## 7. changelog

**2026-10-17**

- 新增结构说明，change `cqt-review-scoring-awards`；实现、编译及静态复核完成，本地迁移已应用；具体证据与未执行范围见该 change 的 exec/implementation-report.md。
