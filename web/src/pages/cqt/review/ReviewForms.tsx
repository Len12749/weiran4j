import { Banner, Button, Form, Input, Modal, Select, Space } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useRef, useState, type ReactNode } from 'react';
import { options, phaseLabels, useReviewOptions } from './ReviewShared';
import { commandKey } from '@/hooks/queries/cqt-review';
import type { ReviewQuery } from '@/hooks/queries/cqt-review';

export function useReviewFilters({ phase = true, keyword = true }: { phase?: boolean; keyword?: boolean } = {}) {
    const query = useReviewOptions();
    const [draft, setDraft] = useState<ReviewQuery>({});
    const [applied, setApplied] = useState<ReviewQuery>({});
    const search = <>
        {query.isError && <Banner type="danger" description={<>{query.error.message}<Button onClick={() => void query.refetch()}>重试选项</Button></>} />}
        <Select placeholder="全部赛事" showClear style={{ width: 200 }} value={draft.competitionId} optionList={options(query.data?.competitions)} onChange={(value) => setDraft({ ...draft, competitionId: value as number | undefined, phaseId: undefined })} />
        {phase && <Select placeholder="全部阶段" showClear style={{ width: 130 }} value={draft.phaseId} optionList={options(query.data?.phases?.filter((item) => !draft.competitionId || item.competitionId === draft.competitionId)).map((item) => ({ ...item, label: draft.competitionId ? phaseLabels[item.label] ?? item.label : `${query.data?.competitions.find((competition) => competition.id === query.data?.phases.find((phase) => phase.id === item.value)?.competitionId)?.name ?? ''} · ${phaseLabels[item.label] ?? item.label}` }))} onChange={(value) => setDraft({ ...draft, phaseId: value as number | undefined })} />}
        {keyword && <Input showClear placeholder="关键字" style={{ width: 190 }} value={String(draft.keyword ?? '')} onChange={(value) => setDraft({ ...draft, keyword: value })} />}
    </>;
    return { query, draft, setDraft, applied, search, apply: () => setApplied({ ...draft, keyword: String(draft.keyword ?? '').trim() }), reset: () => { setDraft({}); setApplied({}); } };
}
export function CommandForm({ title, initial = {}, children, onCancel, onSubmit, submitLabel = '保存', resetDependencies = {} }: {
    title: string; initial?: Record<string, unknown>; children: ReactNode; onCancel: () => void;
    onSubmit: (values: Record<string, unknown>, requestKey: string) => Promise<unknown>; submitLabel?: string; resetDependencies?: Record<string, string[]>;
}) {
    const api = useRef<FormApi<Record<string, unknown>> | null>(null);
    const previous = useRef(initial);
    const inFlight = useRef(false);
    const receipt = useRef({ signature: '', key: '' });
    const [pending, setPending] = useState(false);
    const [error, setError] = useState('');
    const submit = async () => {
        if (inFlight.current) return;
        inFlight.current = true; setPending(true); setError('');
        try { const values = await api.current?.validate(); if (values) { const signature = JSON.stringify(values); if (receipt.current.signature !== signature) receipt.current = { signature, key: commandKey() }; await onSubmit(values, receipt.current.key); } }
        catch (cause) { setError(cause instanceof Error ? cause.message : '请检查表单后重试'); }
        finally { inFlight.current = false; setPending(false); }
    };
    return <Modal title={title} visible width={720} onCancel={() => { if (!pending) onCancel(); }} footer={<Space><Button disabled={pending} onClick={onCancel}>取消</Button><Button theme="solid" loading={pending} onClick={() => void submit()}>{submitLabel}</Button></Space>}>
        {error && <Banner type="danger" description={error} />}<Form initValues={initial} onValueChange={(values) => { const old = previous.current; previous.current = values; Object.entries(resetDependencies).forEach(([field, children]) => { if (old[field] !== values[field]) children.forEach((child) => api.current?.setValue(child, undefined)); }); }} disabled={pending} labelPosition="top" getFormApi={(formApi) => { api.current = formApi; }}>{children}</Form>
    </Modal>;
}
export const required = [{ required: true, message: '此项必填' }];
export function ReasonField() { return <Form.TextArea field="reason" label="操作原因" maxCount={2000} rules={required} />; }
