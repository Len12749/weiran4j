import { Banner, Button, Form, Space } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { usePermission } from '@/hooks/usePermission';
import { useReviewQuery } from '@/hooks/queries/cqt-review';
import type { CqtAwardBatch } from '@/types/api';
import { usePageTabs } from '@/hooks/usePageTabs';
import { required } from './ReviewForms';
import { options, PreviewDialog, usePreviewAction, useReviewOptions } from './ReviewShared';

export default function AwardPreviewPage() {
    const [params] = useSearchParams();
    const [initial] = useState(() => ({ phaseId: params.get('phaseId') ? Number(params.get('phaseId')) : undefined, supersedesBatchId: params.get('supersedesBatchId') ? Number(params.get('supersedesBatchId')) : undefined }));
    const query = useReviewOptions(); const permission = usePermission();
    const preview = usePreviewAction();
    const tabs = usePageTabs();
    const api = useRef<FormApi<Record<string, unknown>> | null>(null);
    const [competitionId, setCompetitionId] = useState<number>();
    const [categoryId, setCategoryId] = useState<number>();
    const original = useReviewQuery<CqtAwardBatch>(`/award/batches/${initial.supersedesBatchId}`, {}, !!initial.supersedesBatchId);
    useEffect(() => {
        if (initial.phaseId && query.data) { const phase = query.data.phases.find((item) => item.id === initial.phaseId); api.current?.setValue('competitionId', phase?.competitionId); api.current?.setValue('phaseId', initial.phaseId); }
    }, [initial.phaseId, query.data]);
    const prepare = async () => {
        const values = await api.current?.validate(); if (!values) return;
        if (initial.supersedesBatchId && !original.data) throw new Error('原批次尚未加载，请重试');
        await preview.prepare('/award/preview', { ...values, targetIds: original.data ? original.data.rows.map((row) => row.targetId) : [], supersedesBatchId: initial.supersedesBatchId ?? null }, '/award/batches', { supersedesBatchId: initial.supersedesBatchId ?? null });
    };
    return <PageContainer title={initial.supersedesBatchId ? `替代批次 ${initial.supersedesBatchId}：评奖预览` : '评奖预览'} extra={<Space><Button disabled={preview.pending} onClick={() => tabs.closeCurrentAndGo('/cqt/review/award-batches')}>取消</Button><Permission code="cqt:award-batch:preview"><Button theme="solid" loading={preview.pending || original.isFetching} disabled={!!initial.supersedesBatchId && !original.data} onClick={() => void prepare().catch(() => undefined)}>生成预览</Button></Permission></Space>}>
        {original.isFetching && <Banner type="info" description="正在加载原批次完整候选，完成后可生成替代预览" />}
        {preview.error && <Banner type="danger" description={preview.error} />}
        {original.isError && <Banner type="danger" description={<>{original.error.message}<Button onClick={() => void original.refetch()}>重试原批次</Button></>} />}
        {query.isError && <Banner type="danger" description={<>{query.error.message}<Button onClick={() => void query.refetch()}>重试选项</Button></>} />}
        <Form<Record<string, unknown>> initValues={initial} getFormApi={(formApi) => { api.current = formApi; }} labelPosition="top" onValueChange={() => preview.close()}>
            <Form.Select field="competitionId" label="赛事" disabled={!!initial.supersedesBatchId} rules={required} optionList={options(query.data?.competitions)} onChange={(value) => { setCompetitionId(Number(value)); api.current?.setValue('phaseId', undefined); }} />
            <Form.Select field="phaseId" label="省审/国审阶段" disabled={!!initial.supersedesBatchId} rules={required} optionList={options(query.data?.phases?.filter((item) => item.code !== 'INITIAL' && (!competitionId || item.competitionId === competitionId)))} />
            <Form.Select field="targetType" label="对象类型（不限可留空）" showClear optionList={[{ value: 'ENTRY', label: '作品级' }, { value: 'PARTICIPANT', label: '参赛人级' }]} />
            <Form.Select field="firstCategoryId" label="赛项（不限可留空）" showClear onChange={(value) => { setCategoryId(Number(value)); api.current?.setValue('secondCategoryId', undefined); }} optionList={options(query.data?.categories)} /><Form.Select field="secondCategoryId" label="专业（不限可留空）" showClear optionList={options(query.data?.majors.filter((item) => !categoryId || item.parentId === categoryId))} /><Form.Input field="groupName" label="组别（不限可留空）" />
            <div>按有效确认最终成绩评奖；同分同奖、区间无名额。确认前请检查所有命中、未命中及阻断明细。替代时必须保持原批次完整候选范围。</div>
        </Form>
        {preview.preview && <PreviewDialog canConfirm={permission.hasPermission('cqt:award-batch:confirm')} preview={preview.preview} title="评奖命中预览" pending={preview.pending} error={preview.error} onClose={preview.close} onConfirm={(ack) => void preview.confirm(ack, () => tabs.closeCurrentAndGo('/cqt/review/award-batches', true))} />}
    </PageContainer>;
}
