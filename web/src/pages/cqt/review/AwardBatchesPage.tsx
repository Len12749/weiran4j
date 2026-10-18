import { Button, Select, SideSheet } from '@douyinfe/semi-ui';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { useReviewCommand, useReviewQuery } from '@/hooks/queries/cqt-review';
import type { CqtAwardBatch } from '@/types/api';
import { CommandForm, ReasonField, useReviewFilters } from './ReviewForms';
import { label, ReviewTable, SnapshotTable } from './ReviewShared';

export default function AwardBatchesPage() {
    const filters = useReviewFilters({ keyword: false });
    const navigate = useNavigate();
    const command = useReviewCommand();
    const [detailId, setDetailId] = useState<number>();
    const [revoke, setRevoke] = useState<CqtAwardBatch>();
    const detail = useReviewQuery<CqtAwardBatch>(`/award/batches/${detailId}`, {}, detailId !== undefined);
    return <PageContainer><ReviewTable<CqtAwardBatch> name="cqt/review/award-batches" path="/award/batches" filters={filters.applied} search={<>{filters.search}<Select placeholder="全部批次状态" showClear value={filters.draft.status} optionList={[{ value: 'CONFIRMED', label: '已确认' }, { value: 'STALE', label: '待复核' }, { value: 'REVOKED', label: '已撤销' }]} style={{ width: 140 }} onChange={(value) => filters.setDraft({ ...filters.draft, status: value as string | undefined })} /></>} onSearch={filters.apply} onReset={filters.reset}
        leading={<Permission code="cqt:award-batch:preview"><Button theme="solid" onClick={() => void navigate('/cqt/review/award-batches/preview')}>评奖预览</Button></Permission>}
        columns={[{ title: '批次号', dataIndex: 'id', width: 100 }, { title: '阶段', dataIndex: 'phaseId', width: 180, render: (value: number) => (() => { const phase = filters.query.data?.phases.find((item) => item.id === value); return phase ? `${filters.query.data?.competitions.find((item) => item.id === phase.competitionId)?.name ?? ''} · ${phase.name}` : value; })() },
            { title: '状态', dataIndex: 'status', width: 120, render: label }, { title: '替代批次', dataIndex: 'supersedesBatchId', width: 100 }, { title: '版本', dataIndex: 'version', width: 80 },
            { title: '确认人编号', dataIndex: 'createdBy', width: 110 }, { title: '创建时间', dataIndex: 'createdAt', width: 180 }]}
        actions={(record) => [{ key: 'detail', label: '明细', permission: 'cqt:award-batch:detail', onClick: () => setDetailId(record.id) },
            { key: 'replace', label: '替代', permission: 'cqt:award-batch:preview', disabled: record.status !== 'CONFIRMED', onClick: () => void navigate(`/cqt/review/award-batches/preview?phaseId=${record.phaseId}&supersedesBatchId=${record.id}`) },
            { key: 'revoke', label: '撤销', danger: true, permission: 'cqt:award-batch:revoke', disabled: record.status === 'REVOKED', onClick: () => setRevoke(record) }]} />
        <SideSheet title="批次快照与奖项明细" visible={detailId !== undefined} width="85vw" onCancel={() => setDetailId(undefined)}>
            {detail.isError ? <div role="alert">{detail.error.message}<Button onClick={() => void detail.refetch()}>重试</Button></div> : <><div>状态：{label(detail.data?.status)} 版本：{detail.data?.version} 原因：{detail.data?.reason || '—'}</div><SnapshotTable rows={(detail.data?.rows ?? []).map((row) => ({ ...row }))} /></>}
        </SideSheet>
        {revoke && <CommandForm title={`撤销批次 ${revoke.id}`} onCancel={() => setRevoke(undefined)} onSubmit={async (values, requestKey) => { await command.mutateAsync({ path: `/award/batches/${revoke.id}/revoke`, body: { reason: values.reason, expectedVersion: revoke.version, requestKey } }); setRevoke(undefined); }}>
            <div>仅撤销本批次所属奖项，保留历史；不会影响替代批次。</div><ReasonField />
        </CommandForm>}
    </PageContainer>;
}
