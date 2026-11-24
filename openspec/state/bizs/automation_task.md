# automation_task 自动化任务

> 属于 `openspec/state/`，只描述当前实现事实；以代码和已合入 Flyway 迁移为准。
>
> 事实源：[`AutomationTaskController.java`](../../../server/weiran-automation/weiran-automation-adapter/src/main/java/com/wulicode/weiran/automation/adapter/web/AutomationTaskController.java)、
> [`AutomationTaskExecutor.java`](../../../server/weiran-automation/weiran-automation-application/src/main/java/com/wulicode/weiran/automation/application/task/AutomationTaskExecutor.java)、
> [`AutomationTaskScheduler.java`](../../../server/weiran-automation/weiran-automation-application/src/main/java/com/wulicode/weiran/automation/application/task/AutomationTaskScheduler.java)、
> [`KnowledgeReviewAgent.java`](../../../server/weiran-automation/weiran-automation-infrastructure/src/main/java/com/wulicode/weiran/automation/infrastructure/agent/KnowledgeReviewAgent.java)、
> [`V202611241520__automation_initial_schema.sql`](../../../server/weiran-automation/weiran-automation-infrastructure/src/main/resources/db/migration/automation/V202611241520__automation_initial_schema.sql)、
> [`AutomationTasksPage.tsx`](../../../backend/src/pages/automation/AutomationTasksPage.tsx)。

## 0. 概要

| 项 | 值 |
| --- | --- |
| 表 | `automation_task`、`automation_task_run`、`automation_review`、`automation_review_file` |
| 菜单 | 自动化任务（`sys_menu.id = 2100`，无权限码，所有分配了该菜单的角色可见） |
| 路由 | `/automation/tasks` |
| 页面组件 | `automation/AutomationTasksPage` |
| 后端模块 | `weiran-automation` 五层模块 |
| 接口前缀 | `/api-backend/automation/task` |
| 权限码 | `automation:task:toggle`（按钮行 2101） |
| 内置任务 | `yunxiao-pr-review` 云效 PR 智能质检，间隔 60 秒 |

## 1. 列表

页面以卡片网格展示全部任务，卡片内容包括任务说明、触发逻辑、定时配置、输出方式、上次执行时间与结果简介；点开卡片进入右侧抽屉查看执行记录。

执行记录 `GET /listRuns` 按 `started_at` 倒序分页。**没有新增内容时不写执行记录**：本轮无待评审合并请求、或待评审的合并请求都已在本地生成过评论时，只更新任务的 `last_run_*` 字段，避免每分钟堆积空记录。

## 2. 字段与表单

`automation_task`：

| 字段 | 说明 |
| --- | --- |
| `code` | 任务编码，执行器据此分派；目前只有 `yunxiao-pr-review` |
| `output_mode` | `local` 仅本地产出记录；`local_and_cloud` 本地并回填云端（回填未实现） |
| `interval_seconds` | 执行间隔，只能由迁移调整 |
| `enabled` | 启停状态，管理员可改 |
| `next_run_at` / `locked_until` | 调度租约字段，不对外展示 |

`automation_review` 除数字 `project_id` 外还记 `repository_path`（形如 `qyd/lianmeng/kr/web`），界面按它显示仓库名；按 `(task_id, project_id, merge_request_id)` 唯一，状态取值 `processing / reviewed / failed / skipped`；`processing` 超过 5 分钟视为进程中断，允许重新抢占。

## 3. 动作

- **启停**：`PUT /toggle/{id}`，需要 `automation:task:toggle`；普通用户只读。
- **立即检查**：`POST /run/{id}`，需要 `automation:task:run`；异步执行、不受自动基线限制，触发时把 `next_run_at` 重置为「当前时间 + 间隔」，因此这次手动执行的时间就是下一次自动检查的基线；任务正在执行时返回冲突。
- **删除执行记录**：`DELETE /run/{runId}?taskId=`，需要 `automation:task:run:delete`；连同本次评审结果与逐文件意见一起删除，若删的是最新一条，任务的 `last_run_*` 回退到剩余最新记录。删除已评审的记录会让对应合并请求在下一轮重新进入评审。
- **调度**：应用内每 15 秒检查一次到期任务，`next_run_at` 未到或租约未释放时跳过；执行期间持有 30 分钟租约，多实例不会重复进入。进程启动时把 10 分钟前仍是 `running` 的执行记录标记为「执行中断（进程重启）」，避免界面一直转圈。
- **执行**：配置就绪检查 → 拉取云效待评审合并请求（`totalCommentCount = 0`）→ 过滤本地已评审 → 单轮最多 3 个，并行分析 → 每个合并请求先抢占评审权 → 同步本地代码仓库 → 取变更文件 → 交给 ReAct 评审 Agent → 落库。
- **评审 Agent**：把「MR 信息 + 变更 diff」作为初始上下文，提示词只交代工作顺序（了解代码库技术栈 → 检索对应开发规范 → 找事实性错误），工具为 `read_file`、`grep`（git grep）、`list_files`、`git_log`、`git_show`（都限制在本次同步下来的仓库分支工作区内）与 `search_knowledge`（WeKnora MCP）。轮数不设上限（200 轮仅作死循环保护），同一轮的多个工具调用并发执行（8 线程）；收尾时强制 `response_format: json_object`，模型只输出 `{"findings":[{"path","comment"}]}`；评论标题由后端固定生成。
- **失败处理**：配置缺失或输出方式为 `local_and_cloud` 时记录失败并**自动停用任务**，避免每分钟重复失败。

## 4. 用到的公共组件

`PageContainer`、`Permission`；页面内的卡片与抽屉为页面私有组件，未登记到 `components/`。

## 5. 说明与建议

- 云效 OpenAPI 令牌（`x-yunxiao-token`）与 Git 的 HTTPS 密码是两套凭据：前者调 OpenAPI，后者由系统 git credential helper 在 clone/fetch 时使用。
- `ListChangeRequests` 不返回克隆地址，只返回展示名 `nameWithNamespace`（形如 `qyd / lianmeng / kr / web`）与 `detailUrl`；仓库路径由展示名还原（去掉空格），再按 `weiran.automation.repository.base-url` 拼 HTTPS 地址。
- 本地代码仓库目录独立于知识库的仓库目录，由 `weiran.automation.repository.root` 指定。
- 评论形态与云效 PR 评论一致：一句话结论 + 问题列表，条目为「`路径`：意见」的轻量 Markdown；**只报事实型问题**（参数不匹配、变量/方法不存在、空集合取键、数据截断、鉴权缺失等能指出明确错误后果的问题），观点型问题由提示词约束在模型侧，不进评论。标题由后端固定（`发现 N 处问题，建议修复后再合并。` / `未发现需要修改的问题。`），正文不截断（保护上限 20000 字符），最多 10 条。
- 回写云效未实现；评论只落在 `automation_review.comment_text`，供平台内审查。

## 6. 已知问题汇总

- **#01 🚧 云端回填未实现**：`output_mode=local_and_cloud` 时任务直接失败并停用，避免出现「显示已回填但实际没有」的假记录。
- **#02 ⚠️ 知识库检索结果按宽松结构解析**：MCP `search_knowledge` 的返回体没有以真实 200 响应校准，解析失败时退化为把原始文本截断后交给模型。
- **#03 ⚠️ 并发分析用进程内线程池**：单轮 3 个合并请求并行；重复执行的防护靠数据库唯一键，跨实例安全，但没有做外部接口的全局限流。

## 7. changelog

**2026-11-24**

- 新建模块：任务卡片页、执行记录与评论明细、管理员启停、每分钟轮询云效待评审合并请求并生成评论。
- 首次真实执行：3 个待评审合并请求（`qyd/lianmeng/kr/web`）118 秒生成 3 条评论；下一轮检查识别为「均已生成过评论，未重复执行」。
- 评审结果补记 `repository_path`，界面不再显示数字 `project_id`；历史记录按 `web_url` 反解回填。
- 界面改版：卡片改为状态圆点 + 三列指标，抽屉换成信息条与细线分隔的评论条目。
