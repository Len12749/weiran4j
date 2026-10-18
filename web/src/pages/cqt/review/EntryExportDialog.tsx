import { Banner, Button, Form, Modal, Space } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useEffect, useRef, useState } from 'react';
import type { ReviewQuery } from '@/hooks/queries/cqt-review';
import { fetchEntryExport, saveEntryExport, type EntryExportQuery } from '@/hooks/queries/cqt-entry-export';
import { required } from './ReviewForms';
import { options, useReviewOptions } from './ReviewShared';

export default function EntryExportDialog({ initial, onClose }: { initial: ReviewQuery; onClose: () => void }) {
    const choices = useReviewOptions();
    const [competitionId, setCompetitionId] = useState(initial.competitionId);
    const [pending, setPending] = useState(false);
    const [error, setError] = useState('');
    const api = useRef<FormApi<ReviewQuery> | null>(null);
    const active = useRef(true); const inFlight = useRef(false); const controller = useRef<AbortController | null>(null);
    useEffect(() => { active.current = true; return () => { active.current = false; controller.current?.abort(); }; }, []);
    const submit = async () => {
        if (inFlight.current) return;
        inFlight.current = true; setPending(true); setError('');
        try {
            const values = await api.current?.validate();
            if (!values || !active.current) return;
            const query: EntryExportQuery = { competitionId: Number(values.competitionId), phaseId: Number(values.phaseId), ...(initial.keyword ? { keyword: String(initial.keyword) } : {}), ...(initial.source ? { source: String(initial.source) } : {}), ...(initial.status !== undefined ? { status: String(initial.status) } : {}) };
            controller.current = new AbortController();
            const blob = await fetchEntryExport(query, controller.current.signal);
            if (!active.current) return;
            saveEntryExport(blob, query); onClose();
        } catch (cause) { if (active.current) setError(cause instanceof Error ? cause.message : '导出失败，请重试'); }
        finally { inFlight.current = false; if (active.current) setPending(false); }
    };
    return <Modal visible title="导出作品清单" onCancel={() => { if (!inFlight.current) onClose(); }} footer={<Space><Button disabled={pending} onClick={onClose}>取消</Button><Button loading={pending} onClick={() => void submit()} theme="solid">导出</Button></Space>}>
        {error && <Banner type="danger" description={error} />}
        {choices.isError && <Banner type="danger" description={<>{choices.error.message}<Button onClick={() => void choices.refetch()}>重试选项</Button></>} />}
        <Form initValues={initial} disabled={pending} getFormApi={(value) => { api.current = value; }}>
            <Form.Select field="competitionId" label="赛事" rules={required} optionList={options(choices.data?.competitions)} onChange={(value) => { setCompetitionId(value as number); api.current?.setValue('phaseId', undefined); }} />
            <Form.Select field="phaseId" label="阶段" rules={required} optionList={options(choices.data?.phases.filter((item) => item.competitionId === competitionId && (item.code === 'PROVINCIAL' || item.code === 'NATIONAL')))} />
        </Form>
        <p>按已生效的关键字、来源、初审状态导出全部匹配作品；仅填写下载表格的奖项列。</p>
    </Modal>;
}
