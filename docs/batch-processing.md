# 批处理任务

批处理基座接收文件后立即返回任务 ID，由后台工作线程处理；前端以任务 ID 查询状态。Excel 导入与 ZIP 文件处理通过同一套任务状态、进度和逐项结果能力接入。

## 数据与生命周期

通用任务由 `sys_task` 和 `sys_task_detail` 承载。`sys_task` 保存创建者、状态、阶段、进度计数和任务级失败原因；`sys_task_detail` 保存预检错误及处理阶段的逐项结果，包含阶段、行号或文件序号、名称、成功或失败状态与原因。页面按阶段和状态分页查询明细。无需错误样例 JSON，也不生成私有错误 CSV。业务导入的目标表由具体处理器负责，例如 local/test 演示的 `demo_batch_item`。

文件进入与资源库隔离的 `batch/YYYYMM/` 私有对象目录，不创建 `sys_media` 记录。批处理文件由服务端对象键读取，不经浏览器可访问的资源 URL。阿里云模式为对象设置 private ACL；部署时还应确保 Bucket 策略没有对 `batch/*` 开放匿名读取。本地模式把文件放在资源库根目录旁的 `batch-private/`，不处于 `/system-uploads/**` 的映射范围内。

状态为 `queued`、`running`、`cancel_requested`、`succeeded`、`partial_failed`、`failed`、`cancelled`。阶段按 `waiting`（排队）、`preparing`（准备文件）、`validating`（首次扫描与预检）、`processing`（逐项处理）、`finished`（正常完成）推进，页面用 Step 展示。预检发现错误时逐项写入 `sys_task_detail(stage=validating,status=failed)`，任务停在预检阶段并置为 `failed`，不会写业务结果。失败或取消时保留停止所在阶段，由 `status` 判断任务是否结束。同一账号最多同时保留 3 个未结束任务。预检扫描完成前 `total` 为 `null`，前端显示不确定进度；扫描完成后即使发现错误也保存总数，处理阶段按 `processed / total` 显示。每完成一项立即更新计数，满足 `processed = succeeded + failed`；预检错误不计入处理计数。取消由工作线程在项目之间协作检查，正在处理的一项可能继续完成。

当前工作线程每个应用实例串行消费。数据库原子领取任务；运行期间每 30 秒独立续租，超过两分钟没有心跳的运行中任务可被重新领取，并从源文件重新处理。重新领取时事务内清空旧明细并重置计数。**业务处理器必须按任务 ID 与项目序号或业务唯一键幂等写入**。任务结束 7 天后，后台清理输入文件；任务和逐项结果继续保留。

不同应用实例可以同时领取并处理不同任务；单个实例目前一次只处理一个任务，单个 Excel/ZIP 任务也按行或文件顺序处理。需要单实例并行时，宜先用一个轮询器向有界工作池分发不同任务，并为每项任务独立续租、取消和结束；并发数应按数据库连接池、CPU 与文件存储吞吐设置上限。单任务内并行需要额外处理文件拆分、项目幂等和进度计数争用，不宜只增加调度线程数。

## 接入业务处理器

业务模块依赖 `weiran-system-api`，实现 `BatchProcessor` 并注册为 Spring Bean。`kind()` 是请求使用的处理器类型，`extension()` 声明允许的文件后缀，`maxBytes()` 控制该类型上传上限，`authorize(userId)` 校验该业务处理器的额外权限。`process(BatchSource, BatchProgress)` 可多次打开源文件：先扫描并用 `precheckFailure(itemNo, itemName, message)` 记录逐项预检错误，再调用 `total(value)`；如有预检错误，`total` 会终止执行，业务处理不得开始。预检通过后逐项处理，成功调用 `success(itemNo, itemName)`，可继续的项目错误调用 `failure(itemNo, itemName, message)`，每项前检查 `cancelled()`。同一阶段内的 `itemNo` 必须唯一；文件级预检错误使用 0。业务模块只通过 API 使用基座，不直接操作任务表。

建议每项或每小批独立提交业务事务，并用 `progress.jobId()` 与项目序号建立幂等键。先落业务数据，再更新进度；进程在两步之间退出时，重试的业务写入应无副作用。预检时的文件级致命异常记录在明细第 0 项并结束任务；逐项预检错误可累计展示。处理阶段可继续的项目错误调用 `failure`，使其余项目继续处理。

## HTTP 与页面

所有入口需要登录，任务及明细查询只能访问本人创建的任务。权限为 `system:batch-job:create`、`system:batch-job:list`、`system:batch-job:cancel`。

| 用途 | 接口 |
| --- | --- |
| 上传并创建 | `POST /api-backend/system/batchJob/create`，multipart 字段 `kind`、`file` |
| 最近 30 个任务 | `GET /api-backend/system/batchJob/list?kinds=demo_excel,demo_zip`；`kinds` 可省略，传入后先按类型筛选再取最近 30 个 |
| 任务详情 | `GET /api-backend/system/batchJob/get/{id}` |
| 取消 | `POST /api-backend/system/batchJob/cancel/{id}` |
| 逐项结果 | `GET /api-backend/system/batchJob/listDetails/{id}?stage=validating&status=failed&page=1&pageSize=20`；`stage` 可省略或为 `validating` / `processing`，`status` 可省略或为 `succeeded` / `failed` |

前端上传时显示上传状态，任务创建后按任务 ID 轮询详情。刷新页面后从最近任务列表恢复查看。`local/test` 的「组件演示 → 批处理任务」页面用一组可点击的 Step 同时展示任务当前位置与切换阶段内容：「上传与排队」包含文件上传和 Excel、ZIP 正常或错误样例下载；任务结束后在「结束与数据」分页查看全部已写入的演示数据。

## 演示文件

- **Excel**：仅 `.xlsx`，最大 5 MB，首张工作表的首行必须为 `name`、`value`，最多 10000 行。首次扫描检查空字段和过长字段，逐行输出预检错误；通过后写入 `demo_batch_item`。
- **ZIP**：最大 100 MB、1000 个文件，单文件解压后最大 10 MB、总计最大 200 MB、压缩比不超过 100。首次扫描检查路径、类型和大小，逐文件输出可定位的预检错误；通过后逐项计算 SHA-256 并写入 `demo_batch_item`。仅处理文档、文本、表格和常见图片类型；不解压到文件系统。

验证预检错误时，下载页面上的「错误 Excel」或「错误 ZIP」，选择对应处理类型并上传：

| 示例 | 预期预检明细 | 预期任务结果 |
| --- | --- | --- |
| 错误 Excel | 第 3 行缺少 `name`、第 4 行缺少 `value`，各记录一条 `validating/failed` | `status=failed`、`phase=validating`，处理计数为 0，演示数据为 0 条 |
| 错误 ZIP | 第 2、3 个文件分别为 `unsupported.exe`、`unsupported.bat`，各记录一条 `validating/failed` | `status=failed`、`phase=validating`，处理计数为 0，演示数据为 0 条 |

点击「预检」步骤核对行号、文件名和原因；切换到「逐项处理」应无处理明细，「结束与数据」应显示失败原因和空业务结果。正常样例预检通过后进入逐项处理，任务结束时可在「结束与数据」核对落库结果。

## 下游作为插件接入

通用能力有两处扩展点：后端 `weiran-system-api` 的 `BatchProcessor` 是任务处理插件接口，前端 `web/src/components/BatchJobWorkbench.tsx` 是可配置的 Step 工作台。下游保留基座和该组件，可以省略整个 `weiran-demo` 及 `web/src/pages/demo/`，无需复制演示 Excel/ZIP 处理器或演示表。

1. 在下游业务模块实现 `BatchProcessor`，用模块自己的 `@AutoConfiguration` 注册为 Spring Bean；`kind()` 与前端的类型 `value` 保持一致。业务表由模块自己的 Flyway 迁移创建，预检错误通过 `precheckFailure` 写入通用明细。
2. 新建业务页面，传入类型列表、允许的文件后缀、可选下载样例及业务结果组件。工作台按配置的类型筛选最近任务，并复用上传、任务查询、取消、阶段切换和明细分页；`ResultPanel` 在任务结束后才挂载，接收当前 `job`，可按 `job.id` 查询业务表。
3. 业务结果查询接口应先调用 `BatchJobService.get(jobId, CurrentUser.require().id())` 校验任务归属，再读取自己的结果表。按项目页面规则添加菜单迁移，`component` 指向新业务页面。

```tsx
import { BatchJobWorkbench, type BatchJob } from '@/components/BatchJobWorkbench';

function ImportResults({ job }: { job: BatchJob }) {
  return <YourResultTable taskId={job.id} />;
}

export default function OrderImportPage() {
  return <BatchJobWorkbench
    title="订单导入"
    description="上传订单文件并查看每个阶段的结果"
    kinds={[{ value: 'order_import', label: '订单 Excel', accept: '.xlsx' }]}
    ResultPanel={ImportResults}
  />;
}
```

`YourResultTable` 是业务自己的分页结果组件，需要由下游实现。`samples` 可传 `{ label, href }[]` 提供业务样例下载，`sampleHint` 与 `uploadHint` 可补充说明。通用接口仍使用 `system:batch-job:create/list/cancel` 权限；业务处理器可在 `authorize(userId)` 增加自身的授权检查。

演示模块 `weiran-demo` 仅在 local/test profile 装配，不进入生产 `bootJar`。下游复制框架时可不复制该模块，再按业务需要提供自己的 `BatchProcessor`。
