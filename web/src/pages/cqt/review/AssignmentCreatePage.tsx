import { Banner, Button, Form, Input, Select, Space } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useRef, useState } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { usePageTabs } from '@/hooks/usePageTabs';
import type { CqtReviewTarget } from '@/types/api';
import { required } from './ReviewForms';
import { label, options, PreviewDialog, ReviewTable, usePreviewAction, useReviewOptions } from './ReviewShared';
import type { ReviewQuery } from '@/hooks/queries/cqt-review';
import { usePermission } from '@/hooks/usePermission';

export default function AssignmentCreatePage() {
    const query = useReviewOptions(); const action = usePreviewAction(); const tabs = usePageTabs(); const permission = usePermission();
    const api = useRef<FormApi<Record<string, unknown>> | null>(null);
    const [competitionId, setCompetitionId] = useState<number>(); const [phaseId, setPhaseId] = useState<number>();
    const [selected, setSelected] = useState<(string | number)[]>([]);
    const [error, setError] = useState('');
    const [candidateDraft, setCandidateDraft] = useState<ReviewQuery>({});
    const [candidateFilters, setCandidateFilters] = useState<ReviewQuery>({});
    const prepare = async () => {
        setError(''); const values = await api.current?.validate(); if (!values) return;
        if (selected.length === 0 || selected.length > 500) { setError('请选择 1 至 500 个评分对象；当前页全选仅选择本页。'); return; }
        await action.prepare('/review/assignments/preview', { phaseId: values.phaseId, mode: values.mode, targetIds: selected.map(Number), userIds: values.userIds ?? [], reviewersPerTarget: values.reviewersPerTarget ?? 1 }, '/review/assignments/confirm');
    };
    return <PageContainer title="评委分配" extra={<Space><Button disabled={action.pending} onClick={() => tabs.closeCurrentAndGo('/cqt/review/assignments')}>取消</Button><Button theme="solid" loading={action.pending} onClick={() => void prepare().catch(() => undefined)}>预览分配</Button></Space>}>
        {(error || action.error) && <Banner type="danger" description={error || action.error} />}
        {query.isError && <Banner type="danger" description={<>{query.error.message}<Button onClick={() => void query.refetch()}>重试选项</Button></>} />}
        <Form<Record<string, unknown>> initValues={{ mode: permission.hasPermission('cqt:assignment:create') ? 'MANUAL' : 'AUTO', reviewersPerTarget: 1 }} getFormApi={(formApi) => { api.current = formApi; }} labelPosition="top" onValueChange={() => action.close()}>
            <Form.Select field="competitionId" label="赛事" rules={required} optionList={options(query.data?.competitions)} onChange={(value) => { setCompetitionId(Number(value)); setPhaseId(undefined); api.current?.setValue('phaseId', undefined); setSelected([]); }} />
            <Form.Select field="phaseId" label="省审/国审阶段" rules={required} optionList={options(query.data?.phases.filter((item) => item.code !== 'INITIAL' && item.competitionId === competitionId))} onChange={(value) => { setPhaseId(Number(value)); setSelected([]); }} />
            <Form.Select field="mode" label="方式" optionList={[...(permission.hasPermission('cqt:assignment:create') ? [{ value: 'MANUAL', label: '手工：所选对象分配给所选评委' }] : []), ...(permission.hasPermission('cqt:assignment:auto') ? [{ value: 'AUTO', label: '自动：在合法范围内按工作量补齐人数' }] : [])]} rules={required} />
            <Form.Select field="userIds" label="评委池" multiple filter rules={required} optionList={(query.data?.users ?? []).map((item) => ({ value: item.id, label: `${item.nickname || item.username}（${item.username}）` }))} />
            <Form.InputNumber field="reviewersPerTarget" label="自动分配：每对象评委数" min={1} precision={0} rules={required} />
        </Form>
        <div>已选 {selected.length} 个评分对象。仅省审初审通过对象、国审已晋级对象可分配；有阻断请调整选择后重新预览。</div>
        {phaseId && <ReviewTable<CqtReviewTarget> key={`${competitionId}-${phaseId}`} name="cqt/review/candidates" path="/review/assignment-candidates" filters={{ ...candidateFilters, competitionId, phaseId }} search={<>
            <Input value={String(candidateDraft.keyword ?? '')} placeholder="作品或参赛人" showClear onChange={(value) => setCandidateDraft({ ...candidateDraft, keyword: value })} style={{ width: 190 }} />
            <Select placeholder="全部来源" showClear value={candidateDraft.source} optionList={[{ value: 'zhongxi', label: '中戏' }, { value: 'qudao', label: '渠道' }, { value: 'houtai', label: '后台' }]} onChange={(value) => setCandidateDraft({ ...candidateDraft, source: value as string | undefined })} style={{ width: 120 }} />
            <Select placeholder="全部对象类型" showClear value={candidateDraft.targetType} optionList={[{ value: 'ENTRY', label: '作品级' }, { value: 'PARTICIPANT', label: '参赛人级' }]} onChange={(value) => setCandidateDraft({ ...candidateDraft, targetType: value as string | undefined })} style={{ width: 140 }} />
            <Select placeholder="全部赛项" showClear value={candidateDraft.categoryId} optionList={options(query.data?.categories)} onChange={(value) => setCandidateDraft({ ...candidateDraft, categoryId: value as number | undefined })} style={{ width: 150 }} />
        </>} onSearch={() => { setCandidateFilters(candidateDraft); setSelected([]); action.close(); }} onReset={() => { setCandidateDraft({}); setCandidateFilters({}); setSelected([]); action.close(); }} rowSelection={{ selectedRowKeys: selected, onChange: (keys) => { setSelected(keys); action.close(); }, getCheckboxProps: (row) => ({ disabled: row.eligible === false }) }} columns={[
            { title: '评分对象号', dataIndex: 'id', width: 110 }, { title: '作品', dataIndex: 'title', width: 240 }, { title: '参赛人', dataIndex: 'name', width: 130 }, { title: '来源', dataIndex: 'source', width: 90, render: label }, { title: '对象类型', dataIndex: 'targetType', width: 110, render: label }, { title: '已分配数', dataIndex: 'assignedCount', width: 110 }, { title: '不可分配原因', dataIndex: 'blockedReason', width: 230 },
        ]} />}
        {action.preview && <PreviewDialog title="覆盖、重复与工作量预览" preview={action.preview} pending={action.pending} error={action.error} onClose={action.close} onConfirm={(ack) => void action.confirm(ack, () => tabs.closeCurrentAndGo('/cqt/review/assignments', true))} />}
    </PageContainer>;
}
