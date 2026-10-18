# E3 / E8 / E10 评奖后端实施

对应 tasks 2.2 / 3.5 / 4.2。作者 reviewer；本记录不是独立验收结论。

## 实现文件

- domain.award：AwardRule（闭区间/专业组别通配交集检测）、AwardBatch、AwardRepository。
- api.award：规则、候选预览与批次完整 DTO、AwardService。
- application.award.AwardApplicationService：规则不可变 revision，阶段锁内范围校验、输入快照、共享预览及命令回执；确认全量重验、过期/跨人/跨用途 token 由共享 Support 拒绝；完整候选替代，单独批次授奖；撤销只影响所属批次。
- infrastructure.award.JdbcAwardRepository：绑定参数 JDBC；规则修订行/当前标记、批次输入与输出快照、专属 awards/grants；CAS 状态更新。
- adapter.award.AwardAdminController：独立权限及操作日志；撤销 reason 仅在 adapter 输入标 WRITE_ONLY，应用完整命令仍参与幂等 payload hash。

## 共享契约

复用 executor 所有的 domain.review.WorkflowPhase、AwardCandidate、WorkflowPreview、WorkflowReceipt、ReviewWorkflowRepository 与 application.review.ReviewWorkflowSupport。phase ID/competition 锁、reviewer 状态惰性失效、inputVersion、30分钟预览和receipt均不自建第二套实现。迁移 V202610170400 与三层自动注册由 executor 单写。

## 行为边界

- 奖项对象只有 ENTRY / PARTICIPANT；规则只允许省/国审，区间不得超当前评分范围。
- 同一 phase 内启用规则的适用域相交且闭区间相交即拒绝；没有名额或排名截断。
- 预览保留 MATCHED / UNMATCHED / BLOCKED；阻断对象拒绝整批确认；零命中奖项不生成空批次。
- 读取/确认消费共享最终分及对象有效性，确认比较完整规则、对象、分数版本和输入 hash。
- 同一对象已存在有效批次时必须明确 supersedesBatchId 且完整复现原候选；同事务使旧批次 STALE 并写新批次专属奖项。替代引用只保存批次 id/version/status，避免快照递归膨胀。
- 规则修改使既有确认批次 STALE；只撤销目标批次，不覆盖独立历史。
- requestKey 最多100字，token为36位UUID，reason最多2000字，避免超列长。

## 检查状态

作者未新增测试、未运行 Gradle 或测试。executor 负责单一编译与注册；独立交叉审由 executor/auth_audit 执行。编译问题由文件作者修复，不把本记录当作验证通过证据。
