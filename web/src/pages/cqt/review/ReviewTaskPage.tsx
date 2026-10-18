import { Banner, Button, Form, Space, Spin, Typography } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { commandKey, useReviewCommand, useReviewQuery } from '@/hooks/queries/cqt-review';
import { usePermission } from '@/hooks/usePermission';
import { usePageTabs } from '@/hooks/usePageTabs';
import type { CqtReviewTaskDetail } from '@/types/api';
import { AttachmentPreview, label, SnapshotTable, textValue } from './ReviewShared';

function TaskForm({ detail, id }: { detail: CqtReviewTaskDetail; id: number }) {
    const tabs = usePageTabs(); const permission = usePermission(); const command = useReviewCommand();
    const api = useRef<FormApi<Record<string, unknown>> | null>(null);
    const [version, setVersion] = useState(detail.score?.version ?? 0);
    const [error, setError] = useState(''); const [dirty, setDirty] = useState(false);
    const inFlight = useRef(false); const retry = useRef<{ signature: string; key: string } | undefined>(undefined);
    const readOnly = !permission.hasPermission('cqt:review-task:score') || detail.canSubmit === false || detail.assignment.state === 'CANCELLED';
    useEffect(() => {
        const warn = (event: BeforeUnloadEvent) => { if (dirty) event.preventDefault(); };
        window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn);
    }, [dirty]);
    const submit = async (status: 'DRAFT' | 'SUBMITTED') => {
        if (inFlight.current || readOnly) return;
        inFlight.current = true; setError('');
        try {
            const values = api.current?.getValues(); if (!values) return;
            const dimensions = detail.dimensions.map((dimension) => ({ dimensionId: dimension.id, score: values[`score_${dimension.id}`] ?? null, comment: textValue(values[`comment_${dimension.id}`]).trim() }));
            if (status === 'SUBMITTED') {
                const total = values.total;
                if (total === null || total === undefined || total === '' || Number(total) < detail.rule.minScore || Number(total) > detail.rule.maxScore) throw new Error(`综合分必须在 ${detail.rule.minScore} 至 ${detail.rule.maxScore} 之间`);
                for (const dimension of detail.dimensions) { const row = dimensions.find((item) => item.dimensionId === dimension.id); if (row?.score === null || row?.score === undefined || Number(row.score) < dimension.minScore || Number(row.score) > dimension.maxScore || !row.comment) throw new Error(`请完成“${dimension.name}”记录分和评语`); }
            }
            const body = { status, total: values.total ?? null, comment: textValue(values.comment).trim(), dimensions, expectedVersion: version };
            const signature = JSON.stringify(body);
            if (!retry.current || retry.current.signature !== signature) retry.current = { signature, key: commandKey() };
            const result = await command.mutateAsync({ path: `/review/my-tasks/${id}/score`, method: 'PUT', body: { ...body, requestKey: retry.current.key } }) as { version: number };
            setDirty(false); setVersion(result.version);
            if (status === 'SUBMITTED') tabs.closeCurrentAndGo('/cqt/review/my-tasks', true);
        } catch (cause) { setError(cause instanceof Error ? cause.message : '保存失败，请重试'); }
        finally { inFlight.current = false; }
    };
    const initial: Record<string, unknown> = { total: detail.current?.total, comment: detail.current?.comment ?? '' };
    detail.dimensionScores.forEach((row) => { initial[`score_${row.dimensionId}`] = row.score; initial[`comment_${row.dimensionId}`] = row.comment; });
    return <PageContainer title={`评审：${detail.target.title}`} extra={<Space><Button disabled={command.isPending} onClick={() => { if (!dirty) tabs.closeCurrentAndGo('/cqt/review/my-tasks'); else { setError('尚有未保存修改，请先保存草稿，或点击“放弃修改并返回”。'); } }}>返回</Button>{dirty && <Button type="danger" disabled={command.isPending} onClick={() => tabs.closeCurrentAndGo('/cqt/review/my-tasks')}>放弃修改并返回</Button>}<Permission code="cqt:review-task:score"><Button disabled={readOnly || command.isPending} loading={command.isPending} onClick={() => void submit('DRAFT')}>保存草稿</Button><Button theme="solid" disabled={readOnly || command.isPending} loading={command.isPending} onClick={() => void submit('SUBMITTED')}>提交评分</Button></Permission></Space>}>
        {error && <Banner type="danger" description={`${error}。版本冲突时请另存本地修改后刷新重新复核。`} />}{readOnly && <Banner type="warning" description={detail.blockedReason || '任务当前不可提交，请联系管理员'} />}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 24 }}><section style={{ flex: '1 1 420px' }}><Typography.Title heading={5}>作品与当前评分对象</Typography.Title><p>作品编号：{detail.entry.id} {label(detail.target.source)} · {label(detail.target.targetType)}</p><p>参赛人：{detail.target.targetType === 'ENTRY' ? '整件作品' : detail.target.name}</p><p>{detail.entry.description}</p><AttachmentPreview url={detail.entry.attachmentUrl} /><p>维度记录分不加权、不相加，综合得分由评委独立填写。</p></section>
            <section style={{ flex: '1 1 420px' }}><Form<Record<string, unknown>> disabled={readOnly || command.isPending} initValues={initial} getFormApi={(formApi) => { api.current = formApi; }} labelPosition="top" onValueChange={() => setDirty(true)}>
                <Form.InputNumber field="total" label={`综合得分（${detail.rule.minScore}–${detail.rule.maxScore}）`} min={detail.rule.minScore} max={detail.rule.maxScore} precision={2} /><Form.TextArea field="comment" label="总体评语" maxCount={2000} />
                {detail.dimensions.map((dimension) => <div key={dimension.id}><Typography.Title heading={6}>{dimension.name}</Typography.Title><p>{dimension.description}</p><Form.InputNumber field={`score_${dimension.id}`} label={`记录分（${dimension.minScore}–${dimension.maxScore}）`} min={dimension.minScore} max={dimension.maxScore} precision={2} /><Form.TextArea field={`comment_${dimension.id}`} label="维度评语（提交必填）" maxCount={2000} /></div>)}
            </Form></section></div><Typography.Title heading={5}>本人评分修订历史</Typography.Title><SnapshotTable rows={detail.history.map((row) => ({ ...row }))} />
    </PageContainer>;
}
export default function ReviewTaskPage() {
    const { assignmentId } = useParams(); const id = Number(assignmentId); const valid = Number.isSafeInteger(id) && id > 0;
    const query = useReviewQuery<CqtReviewTaskDetail>(`/review/my-tasks/${id}`, {}, valid); const tabs = usePageTabs();
    if (!valid || query.isError) return <PageContainer><Banner type="danger" description={query.error?.message ?? '任务编号无效'} /><Button onClick={() => void query.refetch()}>重试</Button><Button onClick={() => tabs.closeCurrentAndGo('/cqt/review/my-tasks')}>返回</Button></PageContainer>;
    if (!query.data) return <Spin />;
    return <TaskForm key={id} id={id} detail={query.data} />;
}
