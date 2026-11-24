import {
    Button,
    Descriptions,
    Empty,
    Popconfirm,
    SideSheet,
    Spin,
    Switch,
    Table,
    Tag,
    Toast,
    Typography,
} from '@douyinfe/semi-ui';
import type { ColumnProps } from '@douyinfe/semi-ui/lib/es/table';
import { RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import {
    useAutomationRunDetail,
    useAutomationRuns,
    useAutomationTasks,
    useDeleteAutomationRun,
    useToggleAutomationTask,
    useTriggerAutomationTask,
} from '@/hooks/queries/automation';
import type { AutomationReviewView, AutomationRunView, AutomationTaskView } from '@/types/api';
import './AutomationTasksPage.css';

type TagColor = 'green' | 'orange' | 'red' | 'grey' | 'blue';

const RUN_STATUS: Record<string, { label: string; color: TagColor }> = {
    running: { label: '执行中', color: 'blue' },
    success: { label: '成功', color: 'green' },
    partial: { label: '部分成功', color: 'orange' },
    failed: { label: '失败', color: 'red' },
};

const REVIEW_STATUS: Record<string, { label: string; color: TagColor }> = {
    processing: { label: '分析中', color: 'blue' },
    reviewed: { label: '已生成评论', color: 'green' },
    failed: { label: '失败', color: 'red' },
    skipped: { label: '已跳过', color: 'grey' },
};

function outputModeLabel(mode: string): string {
    return mode === 'local_and_cloud' ? '本地记录 + 回填云端' : '仅本地产出记录';
}

function durationLabel(durationMs: number | null): string {
    if (durationMs === null || durationMs < 0) return '—';
    if (durationMs < 1000) return `${durationMs} ms`;
    return `${(durationMs / 1000).toFixed(1)} s`;
}

function StatusTagOf({ map, value }: { map: Record<string, { label: string; color: TagColor }>; value: string | null }) {
    if (!value) return <Tag size="small" color="grey">未执行</Tag>;
    const item = map[value];
    return (
        <Tag size="small" color={item?.color ?? 'grey'}>
            {item?.label ?? value}
        </Tag>
    );
}

/** 极简 Markdown：只处理行内代码、加粗与「- 」列表，够展示云效评论。 */
function renderInline(text: string): React.ReactNode[] {
    const parts: React.ReactNode[] = [];
    const pattern = /(`[^`]+`|\*\*[^*]+\*\*)/g;
    let last = 0;
    let match = pattern.exec(text);
    let key = 0;
    while (match !== null) {
        if (match.index > last) parts.push(text.slice(last, match.index));
        const token = match[0];
        parts.push(
            token.startsWith('`') ? (
                <code key={key}>{token.slice(1, -1)}</code>
            ) : (
                <strong key={key}>{token.slice(2, -2)}</strong>
            ),
        );
        key += 1;
        last = match.index + token.length;
        match = pattern.exec(text);
    }
    if (last < text.length) parts.push(text.slice(last));
    return parts;
}

function MarkdownLite({ text }: { text: string }) {
    return (
        <div className="automation-review__comment">
            {text.split('\n').map((line, index) => {
                if (line.trim() === '') return <div key={index} className="automation-review__comment-gap" />;
                if (line.startsWith('- ')) {
                    return (
                        <div key={index} className="automation-review__comment-item">
                            {renderInline(line.slice(2))}
                        </div>
                    );
                }
                return <p key={index}>{renderInline(line)}</p>;
            })}
        </div>
    );
}

function ReviewCard({ review }: { review: AutomationReviewView }) {
    return (
        <div className="automation-review">
            <div className="automation-review__head">
                <span className="automation-review__title">{review.mergeRequestTitle || `合并请求 #${review.mergeRequestId}`}</span>
                <StatusTagOf map={REVIEW_STATUS} value={review.status} />
            </div>
            <div className="automation-review__meta">
                <span>代码库 {review.projectId}</span>
                <span>MR #{review.mergeRequestId}</span>
                {review.authorName && <span>{review.authorName}</span>}
                {review.sourceBranch && review.targetBranch && (
                    <span>
                        {review.sourceBranch} → {review.targetBranch}
                    </span>
                )}
                {review.webUrl && (
                    <a href={review.webUrl} target="_blank" rel="noreferrer">
                        打开云效
                    </a>
                )}
            </div>
            {review.commentText && <MarkdownLite text={review.commentText} />}
            {review.errorMessage && <p className="automation-review__error">{review.errorMessage}</p>}
        </div>
    );
}

function RunDetailSheet({ task: openedTask, onClose }: { task: AutomationTaskView; onClose: () => void }) {
    const tasks = useAutomationTasks();
    const task = tasks.data?.find((item) => item.id === openedTask.id) ?? openedTask;
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [runId, setRunId] = useState<number | null>(null);
    const runs = useAutomationRuns(task.id, page, pageSize);
    const detail = useAutomationRunDetail(task.id, runId);
    const trigger = useTriggerAutomationTask();
    const removeRun = useDeleteAutomationRun();

    const columns: ColumnProps<AutomationRunView>[] = [
        {
            title: '开始时间',
            dataIndex: 'startedAt',
            width: 190,
            render: (value: string, record: AutomationRunView) => (
                <span>
                    {value}
                    {record.triggerType === 'manual' && (
                        <Tag size="small" color="blue" style={{ marginLeft: 6 }}>
                            手动
                        </Tag>
                    )}
                </span>
            ),
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (value: string) => <StatusTagOf map={RUN_STATUS} value={value} />,
        },
        {
            title: '耗时',
            dataIndex: 'durationMs',
            width: 90,
            render: (value: number | null) => durationLabel(value),
        },
        { title: '结果简介', dataIndex: 'summary' },
        {
            title: '操作',
            dataIndex: 'actions',
            width: 88,
            fixed: 'right',
            render: (_value: unknown, record: AutomationRunView) => (
                <Permission code="automation:task:run:delete">
                    <span onClick={(event) => event.stopPropagation()}>
                        <Popconfirm
                            title="删除这条执行记录？"
                            content="本次评论内容会一并删除；已评审的合并请求会在下一轮重新分析。"
                            onConfirm={() =>
                                removeRun.mutate(
                                    { taskId: task.id, runId: record.id },
                                    {
                                        onSuccess: () => {
                                            // 删的正是当前查看的那次：关掉评论区，避免刷新时又去查已删除的记录
                                            if (runId === record.id) {
                                                setRunId(null);
                                            }
                                            if ((runs.data?.list.length ?? 0) <= 1 && page > 1) {
                                                setPage(page - 1);
                                            }
                                            Toast.success('已删除');
                                        },
                                    },
                                )
                            }
                        >
                            <Button type="danger" theme="borderless" size="small">
                                删除
                            </Button>
                        </Popconfirm>
                    </span>
                </Permission>
            ),
        },
    ];

    return (
        <SideSheet visible title={`${task.name} · 执行记录`} width={860} onCancel={onClose}>
            <Descriptions
                align="left"
                data={[
                    { key: '触发逻辑', value: task.triggerDescription },
                    { key: '定时配置', value: task.scheduleDescription },
                    { key: '输出方式', value: outputModeLabel(task.outputMode) },
                    { key: '上次执行', value: task.lastRunAt ?? '尚未执行' },
                ]}
            />
            <div className="automation-sheet__actions">
                <Permission code="automation:task:run">
                    <Button
                        type="primary"
                        theme="solid"
                        loading={trigger.isPending}
                        onClick={() =>
                            trigger.mutate(task.id, {
                                onSuccess: () =>
                                    Toast.success('已触发，执行完成后点刷新查看结果'),
                            })
                        }
                    >
                        立即检查
                    </Button>
                </Permission>
                <span className="automation-sheet__hint">
                    手动触发会立即执行一次，并把下次自动检查的时间从这次触发重新计时
                </span>
            </div>
            <Typography.Title heading={6} style={{ margin: '16px 0 8px' }}>
                执行记录
            </Typography.Title>
            <Table<AutomationRunView>
                rowKey="id"
                columns={columns}
                dataSource={runs.data?.list ?? []}
                loading={runs.isFetching}
                scroll={{ x: 760 }}
                onRow={(record) =>
                    record
                        ? {
                              onClick: () => setRunId(record.id),
                              style: { cursor: 'pointer' },
                          }
                        : {}
                }
                pagination={{
                    currentPage: page,
                    pageSize,
                    total: runs.data?.total ?? 0,
                    pageSizeOpts: [10, 20, 50],
                    showSizeChanger: true,
                    onChange: (nextPage: number, nextSize: number) => {
                        setPage(nextPage);
                        setPageSize(nextSize);
                    },
                }}
                empty={
                    <Empty
                        title="还没有执行记录"
                        description="任务启用后，有新的待评审合并请求时才会产生记录"
                    />
                }
            />
            {runId !== null && (
                <>
                    <Typography.Title heading={6} style={{ margin: '20px 0 8px' }}>
                        本次评论内容
                    </Typography.Title>
                    {detail.isPending ? (
                        <Spin />
                    ) : (detail.data?.reviews.length ?? 0) === 0 ? (
                        <Empty title="本次没有合并请求评审结果" />
                    ) : (
                        <div className="automation-reviews">
                            {detail.data?.reviews.map((review) => (
                                <ReviewCard key={review.id} review={review} />
                            ))}
                        </div>
                    )}
                </>
            )}
        </SideSheet>
    );
}

function TaskCard({ task, onOpen }: { task: AutomationTaskView; onOpen: (task: AutomationTaskView) => void }) {
    const toggle = useToggleAutomationTask();
    const [switching, setSwitching] = useState(false);

    const changeEnabled = (enabled: boolean) => {
        setSwitching(true);
        toggle.mutate(
            { id: task.id, body: { enabled } },
            {
                onSuccess: () => Toast.success(enabled ? '任务已启用' : '任务已停用'),
                onSettled: () => setSwitching(false),
            },
        );
    };

    return (
        <article
            className="automation-card"
            role="button"
            tabIndex={0}
            onClick={() => onOpen(task)}
            onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    onOpen(task);
                }
            }}
        >
            <div className="automation-card__head">
                <span className="automation-card__title">{task.name}</span>
                <Tag size="small" color={task.running ? 'blue' : task.enabled ? 'green' : 'grey'}>
                    {task.running ? '执行中' : task.enabled ? '已启用' : '已停用'}
                </Tag>
            </div>
            <p className="automation-card__desc">{task.description}</p>
            <dl className="automation-card__meta">
                <dt>触发逻辑</dt>
                <dd>{task.triggerDescription}</dd>
                <dt>定时配置</dt>
                <dd>{task.scheduleDescription}</dd>
                <dt>输出方式</dt>
                <dd>{outputModeLabel(task.outputMode)}</dd>
                <dt>上次执行</dt>
                <dd>
                    {task.lastRunAt ?? '尚未执行'}
                    {task.lastRunStatus && (
                        <span className="automation-card__run-status">
                            <StatusTagOf map={RUN_STATUS} value={task.lastRunStatus} />
                        </span>
                    )}
                </dd>
            </dl>
            <p className="automation-card__summary">{task.lastRunSummary ?? '暂无执行结果'}</p>
            <div className="automation-card__foot">
                <span className="automation-card__hint">查看执行记录 →</span>
                <Permission
                    code="automation:task:toggle"
                    fallback={
                        <span className="automation-card__readonly">{task.enabled ? '已启用' : '已停用'}</span>
                    }
                >
                    <span className="automation-card__switch" onClick={(event) => event.stopPropagation()}>
                        <Switch checked={task.enabled} loading={switching} disabled={task.running} onChange={changeEnabled} />
                    </span>
                </Permission>
            </div>
        </article>
    );
}

/** 自动化任务：全员可见任务说明与执行结果，管理员可启停。 */
export default function AutomationTasksPage() {
    const tasks = useAutomationTasks();
    const [selected, setSelected] = useState<AutomationTaskView | null>(null);

    return (
        <PageContainer
            title="自动化任务"
            extra={
                <Button
                    icon={<RefreshCw size={14} />}
                    loading={tasks.isFetching}
                    onClick={() => void tasks.refetch()}
                >
                    刷新
                </Button>
            }
        >
            <p className="automation-page__intro">
                平台内置的定时任务。任务类型与定时配置由平台维护，这里只能启停；点开卡片可以查看每次执行的评论内容。
            </p>
            {tasks.isPending ? (
                <Spin />
            ) : (tasks.data?.length ?? 0) === 0 ? (
                <Empty title="暂时没有自动化任务" />
            ) : (
                <div className="automation-grid">
                    {tasks.data?.map((task) => (
                        <TaskCard key={task.id} task={task} onOpen={setSelected} />
                    ))}
                </div>
            )}
            {selected && <RunDetailSheet task={selected} onClose={() => setSelected(null)} />}
        </PageContainer>
    );
}
