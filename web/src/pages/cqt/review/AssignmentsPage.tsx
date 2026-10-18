import { Banner, Button, Select } from '@douyinfe/semi-ui';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import type { CqtReviewAssignment } from '@/types/api';
import { CommandForm, ReasonField, useReviewFilters } from './ReviewForms';
import { label, PreviewDialog, ReviewTable, usePreviewAction } from './ReviewShared';

export default function AssignmentsPage() {
    const filters = useReviewFilters(); const navigate = useNavigate(); const action = usePreviewAction();
    const [changing, setChanging] = useState<{ record: CqtReviewAssignment; state: string }>();
    return <PageContainer>{action.error && <Banner type="danger" description={action.error} />}
        <ReviewTable<CqtReviewAssignment> name="cqt/review/assignments" path="/review/assignments" filters={filters.applied} search={<>{filters.search}<Select placeholder="全部评委" showClear style={{ width: 150 }} value={filters.draft.userId} optionList={(filters.query.data?.users ?? []).map((user) => ({ value: user.id, label: user.nickname || user.username }))} onChange={(value) => filters.setDraft({ ...filters.draft, userId: value as number | undefined })} /><Select placeholder="全部任务状态" showClear style={{ width: 140 }} value={filters.draft.state} optionList={[{ value: 'ACTIVE', label: '有效任务' }, { value: 'CANCELLED', label: '已取消' }]} onChange={(value) => filters.setDraft({ ...filters.draft, state: value as string | undefined })} /></>} onSearch={filters.apply} onReset={filters.reset}
            leading={<Permission code={['cqt:assignment:create', 'cqt:assignment:auto']}><Button theme="solid" onClick={() => void navigate('/cqt/review/assignments/create')}>分配评委</Button></Permission>}
            columns={[{ title: '任务号', dataIndex: 'id', width: 90 }, { title: '作品', dataIndex: 'target', width: 210, render: (_, record) => record?.target?.title ?? record?.title ?? '—' }, { title: '参赛人/对象', dataIndex: 'name', width: 160, render: (_, record) => record?.target?.targetType === 'ENTRY' ? '作品级' : record?.target?.name ?? record?.name ?? '—' }, { title: '评委', dataIndex: 'reviewerId', width: 140, render: (value: number) => filters.query.data?.users.find((item) => item.id === value)?.nickname ?? value }, { title: '阶段', dataIndex: 'phaseId', width: 110, render: (value: number) => (() => { const phase = filters.query.data?.phases.find((item) => item.id === value); return phase ? `${filters.query.data?.competitions.find((item) => item.id === phase.competitionId)?.name ?? ''} · ${phase.name}` : value; })() }, { title: '状态', dataIndex: 'state', width: 110, render: label }]}
            actions={(record) => [{ key: 'state', label: record.state === 'CANCELLED' ? '恢复任务' : '取消任务', permission: record.state === 'CANCELLED' ? 'cqt:assignment:restore' : 'cqt:assignment:cancel', onClick: () => setChanging({ record, state: record.state === 'CANCELLED' ? 'RESTORE' : 'CANCEL' }) }]} />
        {changing && <CommandForm title="任务状态影响预览" submitLabel="预览影响" onCancel={() => setChanging(undefined)} onSubmit={async (values) => {
            const body = { state: changing.state, reason: values.reason, expectedVersion: changing.record.version };
            await action.prepare(`/review/assignments/${changing.record.id}/state-preview`, body, `/review/assignments/${changing.record.id}/state`, body);
        }}><div>已提交评分的任务取消后，其评分仅留历史，关联成绩和奖项需要复核。</div><ReasonField /></CommandForm>}
        {action.preview && <PreviewDialog title="任务状态变更明细" preview={action.preview} pending={action.pending} error={action.error} onClose={action.close} onConfirm={(ack) => void action.confirm(ack, () => setChanging(undefined))} />}
    </PageContainer>;
}
