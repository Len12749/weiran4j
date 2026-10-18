import { Download } from 'lucide-react';
import { Permission } from '@/components/Permission';
import EntryExportDialog from './EntryExportDialog';
import { Banner, Button, Form, Select, SideSheet } from '@douyinfe/semi-ui';
import { useState } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { commandKey, useReviewCommand, useReviewQuery } from '@/hooks/queries/cqt-review';
import type { CqtReviewEntry } from '@/types/api';
import { CommandForm, required, useReviewFilters } from './ReviewForms';
import { AttachmentPreview, label, PreviewDialog, ReviewTable, SnapshotTable, usePreviewAction, textValue } from './ReviewShared';

interface EntryDetail { entry: CqtReviewEntry; members: { id: number; name: string; school: string; groupName: string }[]; history: Record<string, unknown>[]; targets: Record<string, unknown>[]; decision?: { version: number; decision: string } }
export default function EntryReviewsPage() {
    const filters = useReviewFilters({ phase: false });
    const command = useReviewCommand();
    const [exporting, setExporting] = useState(false);
    const action = usePreviewAction();
    const [view, setView] = useState<CqtReviewEntry>();
    const [deciding, setDeciding] = useState<CqtReviewEntry>();
    const details = useReviewQuery<EntryDetail>(`/review/entries/${view?.id}`, {}, !!view);
    return <PageContainer>{action.error && <Banner type="danger" description={action.error} />}
        <ReviewTable<CqtReviewEntry> name="cqt/review/entries" path="/review/entries" tools={<Permission code="cqt:entry-review:export"><Button icon={<Download size={16} />} aria-label="导出作品清单" onClick={() => setExporting(true)} /></Permission>} filters={filters.applied} search={<>{filters.search}<Select placeholder="全部来源" showClear style={{ width: 120 }} value={filters.draft.source} optionList={[{ value: 'zhongxi', label: '中戏' }, { value: 'qudao', label: '渠道' }, { value: 'houtai', label: '后台' }]} onChange={(value) => filters.setDraft({ ...filters.draft, source: value as string | undefined })} /><Select placeholder="全部初审状态" showClear style={{ width: 140 }} value={filters.draft.status} optionList={[{ value: '1', label: '待初审' }, { value: '2', label: '已驳回' }, { value: '3', label: '已通过' }]} onChange={(value) => filters.setDraft({ ...filters.draft, status: value as string | undefined })} /></>} onSearch={filters.apply} onReset={filters.reset}
            columns={[{ title: '作品编号', dataIndex: 'id', width: 100 }, { title: '作品', dataIndex: 'title', width: 240 }, { title: '来源', dataIndex: 'source', width: 100, render: label }, { title: '评分对象', dataIndex: 'scoringScope', width: 120, render: label }, { title: '初审状态', dataIndex: 'legacyStatus', width: 110, render: (value: number) => ({ 0: '待提交', 1: '待初审', 2: '已驳回', 3: '已通过' })[value] ?? String(value) }]}
            actions={(record) => [{ key: 'detail', label: '详情', permission: 'cqt:entry-review:list', onClick: () => setView(record) },
                { key: 'decide', label: '初审', permission: 'cqt:entry-review:decide', onClick: () => setDeciding(record) },
                { key: 'prepare', label: '准备对象', permission: 'cqt:entry-review:prepare', onClick: () => void action.prepare('/review/targets/prepare-preview', { competitionId: record.competitionId, entryIds: [record.id] }, '/review/targets/prepare', { requestKey: commandKey() }).catch(() => undefined) }]} />
        {exporting && <EntryExportDialog initial={{ ...filters.applied }} onClose={() => setExporting(false)} />}
        <SideSheet title="作品、成员与初审历史" visible={!!view} width="85vw" onCancel={() => setView(undefined)}>
            {details.isError ? <Banner type="danger" description={details.error.message} /> : <><h3>{details.data?.entry.title ?? view?.title}</h3><p>{details.data?.entry.description}</p><AttachmentPreview url={details.data?.entry.attachmentUrl} /><h4>成员</h4><SnapshotTable rows={(details.data?.members ?? []).map((row) => ({ ...row }))} /><h4>评分对象</h4><SnapshotTable rows={details.data?.targets ?? []} /><h4>初审历史</h4><SnapshotTable rows={details.data?.history ?? []} /></>}
        </SideSheet>
        {deciding && <CommandForm title="作品初审" initial={{ decision: 'APPROVED', reason: '' }} onCancel={() => setDeciding(undefined)} onSubmit={async (values) => {
            if (values.decision === 'REJECTED' && !textValue(values.reason).trim()) throw new Error('驳回必须填写原因');
            await command.mutateAsync({ path: `/review/entries/${deciding.id}/decision`, body: { ...values, expectedVersion: deciding.version } }); setDeciding(undefined);
        }}><Form.Select field="decision" label="初审结论" optionList={[{ value: 'APPROVED', label: '通过' }, { value: 'REJECTED', label: '驳回' }]} rules={required} /><Form.TextArea field="reason" label="意见（驳回必填；重审须填写原因）" maxCount={2000} /><div>初审按作品决定，通过后按来源形成省审评分对象；已有下游评分时不能直接撤回。</div></CommandForm>}
        {action.preview && <PreviewDialog title="评分对象准备预览" preview={action.preview} pending={action.pending} error={action.error} onClose={action.close} onConfirm={(ack) => void action.confirm(ack, () => undefined)} />}
    </PageContainer>;
}
