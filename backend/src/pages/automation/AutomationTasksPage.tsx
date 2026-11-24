import { Button, Empty, Popconfirm, SideSheet, Spin, Switch, Table, Toast } from '@douyinfe/semi-ui';
import type { ColumnProps } from '@douyinfe/semi-ui/lib/es/table';
import { ArrowUpRight, RefreshCw, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { useIsMobile } from '@/hooks/useMediaQuery';
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

/** 状态圆点的语义色，统一在 CSS 里按 tone 取色。 */
type Tone = 'active' | 'ok' | 'warn' | 'bad' | 'idle';

const RUN_STATUS: Record<string, { label: string; tone: Tone }> = {
    running: { label: '执行中', tone: 'active' },
    success: { label: '成功', tone: 'ok' },
    partial: { label: '部分成功', tone: 'warn' },
    failed: { label: '失败', tone: 'bad' },
};

const REVIEW_STATUS: Record<string, { label: string; tone: Tone }> = {
    processing: { label: '分析中', tone: 'active' },
    reviewed: { label: '已生成评论', tone: 'ok' },
    failed: { label: '失败', tone: 'bad' },
    skipped: { label: '已跳过', tone: 'idle' },
};

function outputModeLabel(mode: string): string {
    return mode === 'local_and_cloud' ? '本地 + 云端' : '仅本地产出';
}

function durationLabel(durationMs: number | null): string {
    if (durationMs === null || durationMs < 0) return '—';
    if (durationMs < 1000) return `${durationMs} ms`;
    return `${(durationMs / 1000).toFixed(1)} s`;
}

/** 后端时间是 `YYYY-MM-DD HH:mm:ss`，Safari 不认空格分隔，这里手动拆开。 */
function parseMoment(value: string | null): Date | null {
    if (!value) return null;
    const matched = /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?/.exec(value);
    if (!matched) return null;
    return new Date(
        Number(matched[1]),
        Number(matched[2]) - 1,
        Number(matched[3]),
        Number(matched[4]),
        Number(matched[5]),
        Number(matched[6] ?? '0'),
    );
}

/** 近期用相对时间，久了退回日期，避免卡片上出现一串精确到秒的数字。 */
function momentLabel(value: string | null): string {
    if (!value) return '尚未执行';
    const moment = parseMoment(value);
    if (!moment) return value;
    const diff = Date.now() - moment.getTime();
    if (diff < 0) return value.slice(0, 16);
    const minute = 60_000;
    if (diff < minute) return '刚刚';
    if (diff < 60 * minute) return `${Math.floor(diff / minute)} 分钟前`;
    if (diff < 24 * 60 * minute) return `${Math.floor(diff / (60 * minute))} 小时前`;
    return value.slice(5, 16);
}

function StatusText({ tone, children }: { tone: Tone; children: React.ReactNode }) {
    return (
        <span className={`automation-status automation-status--${tone}`}>
            <i className="automation-status__dot" />
            {children}
        </span>
    );
}

function StatusDot({ map, value, fallback = '未执行' }: { map: Record<string, { label: string; tone: Tone }>; value: string | null; fallback?: string }) {
    const item = value ? map[value] : undefined;
    return <StatusText tone={item?.tone ?? 'idle'}>{item?.label ?? fallback}</StatusText>;
}

/** 任务自身的状态：执行中 > 启用 / 停用。 */
function taskState(task: AutomationTaskView): { label: string; tone: Tone } {
    if (task.running) return { label: '执行中', tone: 'active' };
    return task.enabled ? { label: '已启用', tone: 'ok' } : { label: '已停用', tone: 'idle' };
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

function CommentBody({ text }: { text: string }) {
    return (
        <div className="automation-comment">
            {text.split('\n').map((line, index) => {
                if (line.trim() === '') return <div key={index} className="automation-comment__gap" />;
                if (line.startsWith('- ')) {
                    return (
                        <div key={index} className="automation-comment__item">
                            {renderInline(line.slice(2))}
                        </div>
                    );
                }
                return <p key={index}>{renderInline(line)}</p>;
            })}
        </div>
    );
}

/** 仓库取路径末两节（kr/web），既比数字 ID 好认又保留了分组；完整路径放 tooltip。 */
function repositoryLabel(review: AutomationReviewView): { name: string; full: string } {
    const path = review.repositoryPath?.trim();
    if (!path) return { name: review.projectId, full: review.projectId };
    const segments = path.split('/').filter(Boolean);
    return { name: segments.slice(-2).join('/'), full: path };
}

function ReviewItem({ review }: { review: AutomationReviewView }) {
    const repo = repositoryLabel(review);
    return (
        <article className="automation-review">
            <header className="automation-review__head">
                <h4 className="automation-review__title">
                    {review.mergeRequestTitle || `合并请求 #${review.mergeRequestId}`}
                </h4>
                <StatusDot map={REVIEW_STATUS} value={review.status} />
            </header>
            <p className="automation-review__meta">
                <span className="automation-review__repo" title={repo.full}>
                    {repo.name}
                </span>
                <span>MR #{review.mergeRequestId}</span>
                {review.authorName && <span>{review.authorName}</span>}
                {review.sourceBranch && review.targetBranch && (
                    <span className="automation-review__mono">
                        {review.sourceBranch} → {review.targetBranch}
                    </span>
                )}
                {review.webUrl && (
                    <a href={review.webUrl} target="_blank" rel="noreferrer">
                        打开云效 <ArrowUpRight size={11} />
                    </a>
                )}
            </p>
            {review.commentText && <CommentBody text={review.commentText} />}
            {review.errorMessage && <p className="automation-review__error">{review.errorMessage}</p>}
        </article>
    );
}

function TaskSheet({ task: openedTask, onClose }: { task: AutomationTaskView; onClose: () => void }) {
    const tasks = useAutomationTasks();
    const task = tasks.data?.find((item) => item.id === openedTask.id) ?? openedTask;
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [runId, setRunId] = useState<number | null>(null);
    const runs = useAutomationRuns(task.id, page, pageSize);
    const detail = useAutomationRunDetail(task.id, runId);
    const isMobile = useIsMobile();
    const trigger = useTriggerAutomationTask();
    const removeRun = useDeleteAutomationRun();

    const columns: ColumnProps<AutomationRunView>[] = [
        {
            title: '开始时间',
            dataIndex: 'startedAt',
            width: 198,
            render: (value: string, record: AutomationRunView) => (
                <span className="automation-run__time">
                    {value}
                    {record.triggerType === 'manual' && <em className="automation-run__manual">手动</em>}
                </span>
            ),
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 104,
            render: (value: string) => <StatusDot map={RUN_STATUS} value={value} />,
        },
        {
            title: '耗时',
            dataIndex: 'durationMs',
            width: 84,
            align: 'right',
            render: (value: number | null) => <span className="automation-run__dim">{durationLabel(value)}</span>,
        },
        {
            title: '结果简介',
            dataIndex: 'summary',
            render: (value: string) => <span className="automation-run__summary">{value}</span>,
        },
        {
            title: '',
            dataIndex: 'actions',
            width: 56,
            render: (_value: unknown, record: AutomationRunView) => (
                <Permission code="automation:task:run:delete">
                    <span onClick={(event) => event.stopPropagation()}>
                        <Popconfirm
                            title="删除这条执行记录？"
                            content="本次评论内容会一并删除；已评审的合并请求会在下一轮重新分析。"
                            okType="danger"
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
                            <Button
                                className="automation-run__delete"
                                icon={<Trash2 size={13} />}
                                theme="borderless"
                                type="tertiary"
                                size="small"
                                title="删除记录"
                                aria-label="删除执行记录"
                            />
                        </Popconfirm>
                    </span>
                </Permission>
            ),
        },
    ];

    return (
        <SideSheet
            visible
            className="automation-sheet"
            width="min(880px, 94vw)"
            onCancel={onClose}
            title={
                <span className="automation-sheet__title">
                    {task.name}
                    <StatusText tone={taskState(task).tone}>{taskState(task).label}</StatusText>
                </span>
            }
        >
            <section className="automation-facts">
                <div className="automation-facts__cell">
                    <span className="automation-facts__label">定时配置</span>
                    <span className="automation-facts__value">{task.scheduleDescription}</span>
                </div>
                <div className="automation-facts__cell">
                    <span className="automation-facts__label">输出方式</span>
                    <span className="automation-facts__value">{outputModeLabel(task.outputMode)}</span>
                </div>
                <div className="automation-facts__cell">
                    <span className="automation-facts__label">上次执行</span>
                    <span className="automation-facts__value">{momentLabel(task.lastRunAt)}</span>
                </div>
            </section>

            <p className="automation-sheet__trigger">
                <span className="automation-sheet__trigger-label">触发逻辑</span>
                {task.triggerDescription}
            </p>

            <div className="automation-sheet__toolbar">
                <Permission code="automation:task:run">
                    <Button
                        type="primary"
                        theme="solid"
                        loading={trigger.isPending}
                        onClick={() =>
                            trigger.mutate(task.id, {
                                onSuccess: () => Toast.success('已触发，执行完成后点刷新查看结果'),
                            })
                        }
                    >
                        立即检查
                    </Button>
                </Permission>
                <span className="automation-sheet__hint">手动触发会立即执行一次，并重新计算下次自动检查的时间</span>
            </div>

            <h3 className="automation-section-title">执行记录</h3>
            <Table<AutomationRunView>
                rowKey="id"
                size="small"
                bordered={false}
                columns={columns}
                dataSource={runs.data?.list ?? []}
                loading={runs.isFetching}
                // 桌面撑满抽屉宽度；窄屏列放不下，改成横向滚动而不是裁掉右侧
                scroll={isMobile ? { x: 620 } : undefined}
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
                    <Empty title="还没有执行记录" description="任务启用后，有新的待评审合并请求时才会产生记录" />
                }
            />

            {runId !== null && (
                <>
                    <h3 className="automation-section-title">本次评论内容</h3>
                    {detail.isPending ? (
                        <Spin />
                    ) : (detail.data?.reviews.length ?? 0) === 0 ? (
                        <Empty title="本次没有合并请求评审结果" />
                    ) : (
                        <div className="automation-reviews">
                            {detail.data?.reviews.map((review) => <ReviewItem key={review.id} review={review} />)}
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

    const state = taskState(task);

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
                <h3 className="automation-card__title">{task.name}</h3>
                <Permission
                    code="automation:task:toggle"
                    fallback={<StatusText tone={state.tone}>{state.label}</StatusText>}
                >
                    <span className="automation-card__switch" onClick={(event) => event.stopPropagation()}>
                        <StatusText tone={state.tone}>{state.label}</StatusText>
                        <Switch
                            size="small"
                            checked={task.enabled}
                            loading={switching}
                            disabled={task.running}
                            onChange={changeEnabled}
                        />
                    </span>
                </Permission>
            </div>

            <p className="automation-card__desc">{task.description}</p>

            <dl className="automation-card__facts">
                <div>
                    <dt>定时</dt>
                    <dd>{task.scheduleDescription}</dd>
                </div>
                <div>
                    <dt>上次执行</dt>
                    <dd>{momentLabel(task.lastRunAt)}</dd>
                </div>
                <div>
                    <dt>上次结果</dt>
                    <dd>
                        <StatusDot map={RUN_STATUS} value={task.lastRunStatus} />
                    </dd>
                </div>
            </dl>

            <p className="automation-card__line" title={task.triggerDescription}>
                <span className="automation-card__line-label">触发</span>
                <span className="automation-card__line-text">{task.triggerDescription}</span>
            </p>
            {task.lastRunSummary && (
                <p className="automation-card__line" title={task.lastRunSummary}>
                    <span className="automation-card__line-label">结果</span>
                    <span className="automation-card__line-text">{task.lastRunSummary}</span>
                </p>
            )}

            <div className="automation-card__foot">
                <span className="automation-card__hint">
                    查看执行记录 <ArrowUpRight size={12} />
                </span>
                <span className="automation-card__output">{outputModeLabel(task.outputMode)}</span>
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
                    theme="borderless"
                    type="tertiary"
                    loading={tasks.isFetching}
                    onClick={() => void tasks.refetch()}
                >
                    刷新
                </Button>
            }
        >
            <p className="automation-page__intro">
                平台维护的定时任务，只能启停。点开卡片查看每次执行的评论内容。
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
            {selected && <TaskSheet task={selected} onClose={() => setSelected(null)} />}
        </PageContainer>
    );
}
