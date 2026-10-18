# Web 实施记录（作者记录，待独立复核）

## 实现范围

- 新增 `web/src/pages/cqt/review/`：8 个主页面 EntryReviewsPage、ReviewPhasesPage、ReviewersPage、AssignmentsPage、MyReviewTasksPage、ScoresPage、AwardRulesPage、AwardBatchesPage；3 个独立页签 AssignmentCreatePage、ReviewTaskPage、AwardPreviewPage。
- 内部复用 ReviewShared/ReviewForms，未修改上游公共组件协议。复用 SearchToolbar 图标查询/重置、列设置、刷新、TableActions、Permission、usePageTabs、Semi Form/ArrayField/Modal/SideSheet/ImagePreview。
- 新增 `hooks/queries/cqt-review.ts`，统一数字0响应、查询键、命令失效；预览不无谓刷新所有列表。`types/api.ts` 同步评审、评奖输入输出所需类型。
- 完成作品初审、来源对象准备、阶段规则/维度、范围配置和影响预览、手工/自动分配候选选择及确认、评分草稿/提交/CAS、汇总与缺评接受、最终分更正/历史、晋级/推进、独立国审资格关联、规则配置、评奖预览/替代/确认/撤销/明细。

## 交互与并发

- 分配始终以 targetId 定位；成绩行键为 phaseId-targetId，避免 ENTRY/PARTICIPANT 与跨阶段混淆。无最终成绩行不可更正或读取不存在的历史。
- 评分草稿保存留页；提交成功关闭 Tab 并刷新本人列表；CAS 失败保留输入。请求期间禁用重复保存。未保存修改提供浏览器离开提示及显式放弃入口。
- 预览请求与确认均有世代检查，取消/换上下文后迟到响应不覆盖新状态；确认 ref 防重入，确认在途不关闭 Modal。网络重试保留确认 requestKey；表单命令按相同内容复用 key。
- 缺评/失效对象清单由服务端返回，汇总确认必须勾选接受跳过；分配、评奖阻断不能直接确认。操作表单在预览之后保留，确认冲突后可返回修改。
- 规则/范围联动切父字段清专业或阶段。已实际对照 ReviewAdminController/AwardAdminController：scope state-preview、assignment userId、mytasks status、资格查询与候选路径一致。
- Options 无 majors/name 字段的实际结构在 UI 归一化：从 categories.parentId 派生专业，从 phase.code 派生中文名。
- 实际 PhaseView 返回数组、其他列表标准 PageResult，内部列表均可处理；ScoreHistory 按 snapshots/corrections 双表呈现；QualificationView 嵌套数据扁平后显示及传递原 version。
- 附件 src/href 只接受 http/https 或本系统单斜杠绝对路径；图片预览使用初始原图缩放。无 console PII、alert/prompt 或 JSON 输入占位表单。

## 静态检查

已执行并成功：

- 本地 TypeScript CLI `tsc -b`：`/private/tmp/review-web-tsc.log`。
- 定向 ESLint（新页面目录、query hook、api.ts）：`/private/tmp/review-web-eslint.log`。
- 本地 Vite CLI `vite build`：`/private/tmp/review-web-vite.log`，exit 0；仅现有依赖的打包警告。

按本轮授权没有新增或运行测试。本文不声明真浏览器、真实业务数据评分/授奖或全量 check 已完成。独立审查由 reviewer/root 负责。

## 独立复核回合

reviewer 最终只读复核未发现新增阻断；提出“替代预览原批次未加载时按钮吞错无反馈”的小边界，已通过原批次加载提示、按钮禁用及失败重试修复。修后重新执行 TypeScript、定向 ESLint 与 Vite，均 exit 0。此记录为作者对已收到审查消息和修复证据的记载，不替代 root 最终验收。

## 作品清单导出与测试授权追加（2026-10-17）

用户随后授权自动化测试并追加作品Excel导出。作品列表工具区在列设置后、刷新前加入独立权限图标；Semi弹层选择赛事与省审/国审阶段，沿用已生效关键字/来源/状态，切赛事清阶段。业务下载工具独立处理Cookie/CSRF与Excel二进制，JSON/HTTP错误不下载；401清会话，40302保留。请求防重复、请求中禁止关闭，卸载中止并忽略迟到响应；使用契约固定文件名，释放对象URL。

新增3个测试文件共12例，覆盖权限、筛选提交、必填、防重、错误、卸载竞态、二进制、会话与URL回收。完整Vitest重跑36文件/289测试通过（`--maxWorkers=2`），日志 `/private/tmp/cqt-review-web-vitest-final.log`。首轮默认并发出现旧App路由测试1秒查找欢迎文案超时，降低并发后全部通过，未改旧测试。

最终TypeScript、定向ESLint与Vite构建exit0：`/private/tmp/cqt-export-tsc.log`、`/private/tmp/cqt-export-eslint.log`、`/private/tmp/cqt-export-vite.log`。reviewer独立静态复核导出前端通过；后端导出实现由reviewer负责，我独立核对接口、权限、匹配与工作簿写入未发现阻断。本文不声明真实浏览器下载或数据库集成验证完成。
