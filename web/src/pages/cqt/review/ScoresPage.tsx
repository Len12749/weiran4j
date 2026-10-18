import { Banner, Button, Form, Select, SideSheet, Space, Typography } from '@douyinfe/semi-ui';
import { useState } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { useReviewCommand, useReviewQuery } from '@/hooks/queries/cqt-review';
import type { CqtReviewFinal, CqtReviewQualification, CqtReviewTarget } from '@/types/api';
import { CommandForm, ReasonField, required, useReviewFilters } from './ReviewForms';
import { label, PreviewDialog, ReviewTable, SnapshotTable, usePreviewAction } from './ReviewShared';

export default function ScoresPage() {
    const filters = useReviewFilters(); const command = useReviewCommand(); const action = usePreviewAction();
    const [selected, setSelected] = useState<CqtReviewFinal[]>([]); const [historyId, setHistoryId] = useState<number>();
    const history = useReviewQuery<{ snapshots: Record<string, unknown>[]; corrections: Record<string, unknown>[] }>(`/review/scores/${historyId}/history`, {}, !!historyId);
    const [operation, setOperation] = useState<'calculate' | 'advance' | 'promote'>(); const [correct, setCorrect] = useState<CqtReviewFinal>();
    const [qualification, setQualification] = useState<CqtReviewQualification>(); const [nationalTargetId, setNationalTargetId] = useState<number>();
    const [error, setError] = useState('');
    const phaseId = Number(filters.applied.phaseId || 0);
    const openOperation = (value: 'calculate' | 'advance' | 'promote') => { if (!phaseId) { setError('请先查询一个赛事阶段'); return; } setError(''); setOperation(value); };
    return <PageContainer>{(error || action.error) && <Banner type="danger" description={error || action.error} />}
        <ReviewTable<CqtReviewFinal> rowKey={(row) => `${row?.phaseId}-${row?.targetId}`} name="cqt/review/scores" path="/review/scores" filters={filters.applied} search={<>{filters.search}<Select placeholder="全部计算状态" showClear style={{ width: 140 }} value={filters.draft.state} optionList={[{ value: 'MISSING', label: '缺评/待汇总' }, { value: 'VALID', label: '有效成绩' }, { value: 'STALE', label: '待复核' }]} onChange={(value) => filters.setDraft({ ...filters.draft, state: value as string | undefined })} /></>} onSearch={() => { filters.apply(); setSelected([]); action.close(); }} onReset={() => { filters.reset(); setSelected([]); action.close(); }}
            rowSelection={{ selectedRowKeys: selected.map((row) => `${row.phaseId}-${row.targetId}`), onChange: (_, rows) => setSelected(rows) }}
            leading={<Space><Permission code="cqt:score:calculate"><Button theme="solid" onClick={() => openOperation('calculate')}>汇总成绩</Button></Permission><Permission code="cqt:score:advance"><Button onClick={() => openOperation('advance')}>晋级决定</Button></Permission><Permission code="cqt:score:promote"><Button onClick={() => openOperation('promote')}>推进国审</Button></Permission></Space>}
            columns={[{ title: '赛事阶段', dataIndex: 'phaseId', width: 200, render: (value: number) => { const phase = filters.query.data?.phases.find((item) => item.id === value); return phase ? `${filters.query.data?.competitions.find((item) => item.id === phase.competitionId)?.name ?? ''} · ${phase.name}` : value; } }, { title: '对象号', dataIndex: 'targetId', width: 90 }, { title: '作品', dataIndex: 'target', width: 200, render: (_, row) => row?.target?.title ?? '—' }, { title: '参赛人/类型', dataIndex: 'name', width: 130, render: (_, row) => row?.target?.targetType === 'ENTRY' ? '作品级' : row?.target?.name ?? '—' }, { title: '有效评委数', dataIndex: 'validReviewerCount', width: 110 }, { title: '平均分', dataIndex: 'averageScore', width: 90 }, { title: '当前最终分', dataIndex: 'finalScore', width: 110 }, { title: '计算状态', dataIndex: 'state', width: 100, render: label }, { title: '晋级状态', dataIndex: 'decision', width: 110, render: (value: CqtReviewFinal['decision']) => label(value?.decision) }]}
            actions={(row) => [{ key: 'history', label: '快照', permission: 'cqt:score:history', disabled: !(row.id > 0), onClick: () => setHistoryId(row.id) }, { key: 'correct', label: '更正', permission: 'cqt:score:correct', disabled: !(row.id > 0) || row.state !== 'VALID' || filters.query.data?.phases.find((phase) => phase.id === row.phaseId)?.state !== 'FROZEN', onClick: () => setCorrect(row) }]} />
        <Typography.Title heading={5}>独立国赛报名资格</Typography.Title><div>共用作品赛事直接进入国审候选；独立作品赛事只生成资格，需关联合法的现有国赛作品。</div>
        <ReviewTable<CqtReviewQualification> mapRow={(row) => row.qualification ? { ...row.qualification, ...(row.sourceTarget ? { sourceTarget: row.sourceTarget } : {}), ...(row.blockedReason ? { blockedReason: row.blockedReason } : {}) } : row} name="cqt/review/qualifications" path="/review/qualifications" filters={{ competitionId: filters.applied.competitionId }}
            columns={[{ title: '资格号', dataIndex: 'id', width: 90 }, { title: '省审对象', dataIndex: 'sourceTargetId', width: 110 }, { title: '作品与参赛人', dataIndex: 'sourceTarget', width: 230, render: (_, row) => row?.sourceTarget ? `${row.sourceTarget.title} · ${row.sourceTarget.targetType === 'ENTRY' ? '作品级' : row.sourceTarget.name}` : '—' }, { title: '来源成绩版本', dataIndex: 'finalVersion', width: 120 }, { title: '国审对象', dataIndex: 'nationalTargetId', width: 110 }, { title: '状态', dataIndex: 'status', width: 120, render: (value: string, row) => value === 'STALE' || value === 'REVOKED' ? label(value) : value === 'ATTACHED' || (value === 'ELIGIBLE' && row?.nationalTargetId) ? '已关联国赛对象' : value === 'ELIGIBLE' ? '待关联/有效资格' : label(value) }, { title: '关联原因', dataIndex: 'reason', width: 250 }]}
            actions={(row) => [{ key: 'attach', label: '关联国赛作品', permission: 'cqt:score:promote', onClick: () => { setQualification(row); setNationalTargetId(undefined); } }]} />
        {operation && <CommandForm title={operation === 'calculate' ? '预览汇总当前全部候选' : operation === 'advance' ? '晋级建议预览' : '推进国审候选/资格'} submitLabel={operation === 'promote' ? '确认推进' : '生成预览'} initial={{ decision: 'ADVANCED', mode: selected.length ? 'MANUAL' : 'THRESHOLD' }} onCancel={() => setOperation(undefined)} onSubmit={async (values, requestKey) => {
            if (operation === 'calculate') await action.prepare(`/review/phases/${phaseId}/calculate-preview`, {}, `/review/phases/${phaseId}/calculate`);
            else if (operation === 'advance') {
                if (values.mode === 'MANUAL' && !selected.length) throw new Error('请选择目标');
                if (values.mode === 'THRESHOLD' && (values.threshold === undefined || values.threshold === null)) throw new Error('请填写建议分数线');
                await action.prepare(`/review/phases/${phaseId}/advance-preview`, { threshold: values.mode === 'THRESHOLD' ? values.threshold : null, targetIds: values.mode === 'MANUAL' ? selected.map((row) => row.targetId) : [], decision: values.decision, reason: values.reason }, `/review/phases/${phaseId}/advance`);
            } else {
                if (!selected.length) throw new Error('请选择需要推进的晋级对象');
                const phase = filters.query.data?.phases.find((item) => item.id === phaseId);
                await command.mutateAsync({ path: `/review/phases/${phaseId}/promote`, body: { targetIds: selected.map((row) => row.targetId), expectedVersion: phase?.version ?? 0, requestKey } });
            }
            if (operation === 'promote') setOperation(undefined);
        }}>
            {operation === 'calculate' ? <div>按有效提交综合分取算术平均。未达最低人数会列入跳过清单，须在预览中明确接受，才能确认冻结。</div> : operation === 'advance' ? <><Form.Select field="mode" label="候选方式" rules={required} optionList={[{ value: 'MANUAL', label: '仅处理表格中选中的对象' }, { value: 'THRESHOLD', label: '按建议分数线生成候选' }]} /><Form.InputNumber field="threshold" label="建议晋级分数线（不选对象时必填）" precision={2} /><Form.Select field="decision" label="决定" rules={required} optionList={[{ value: 'ADVANCED', label: '晋级' }, { value: 'ELIMINATED', label: '淘汰' }, { value: 'LOCKED', label: '锁定' }]} /><ReasonField /><div>已选 {selected.length} 个对象。分数线只生成建议，必须预览确认。</div></> : <div>已选 {selected.length} 个对象。只有有效晋级资格可推进；独立报名赛事不复制作品。</div>}
        </CommandForm>}
        {correct && <CommandForm title="更正当前最终分" initial={{ finalScore: correct.finalScore }} onCancel={() => setCorrect(undefined)} onSubmit={async (values) => { await command.mutateAsync({ path: `/review/scores/${correct.id}/correct`, body: { finalScore: values.finalScore, reason: values.reason, expectedVersion: correct.version } }); setCorrect(undefined); }}><Form.InputNumber field="finalScore" label="更正后最终分" precision={2} rules={required} /><ReasonField /><div>评委原始评分保留，相关已确认奖项会标记待复核。</div></CommandForm>}
        <SideSheet title="计算、更正和重开历史" width="85vw" visible={!!historyId} onCancel={() => setHistoryId(undefined)}>{history.isError ? <Banner type="danger" description={history.error.message} /> : <><h4>汇总与状态快照</h4><SnapshotTable rows={history.data?.snapshots ?? []} /><h4>最终分更正记录</h4><SnapshotTable rows={history.data?.corrections ?? []} /></>}</SideSheet>
        {qualification && <CommandForm title="关联独立国赛作品资格" submitLabel="预览匹配与影响" onCancel={() => setQualification(undefined)} onSubmit={async (values) => {
            if (!nationalTargetId) throw new Error('请选择现有国赛评分对象');
            const body = { nationalTargetId, reason: values.reason, expectedVersion: qualification.version };
            await action.prepare(`/review/qualifications/${qualification.id}/attach-preview`, body, `/review/qualifications/${qualification.id}/attach`, body);
        }}><ReviewTable<CqtReviewTarget> name="cqt/review/qualification-candidates" path={`/review/qualifications/${qualification.id}/candidates`} rowSelection={{ selectedRowKeys: nationalTargetId ? [nationalTargetId] : [], onChange: (keys) => setNationalTargetId(Number(keys.at(-1))), getCheckboxProps: (row) => ({ disabled: row.eligible === false }) }} columns={[{ title: '对象号', dataIndex: 'id', width: 90 }, { title: '作品', dataIndex: 'title', width: 180 }, { title: '参赛人', dataIndex: 'name', width: 110 }, { title: '匹配说明', dataIndex: 'blockedReason', width: 200 }]} /><ReasonField /></CommandForm>}
        {action.preview && <PreviewDialog title="处理明细与影响确认" preview={action.preview} allowSkipped={action.allowSkipped} pending={action.pending} error={action.error} onClose={action.close} onConfirm={(ack) => void action.confirm(ack, () => { setOperation(undefined); setQualification(undefined); })} />}
    </PageContainer>;
}
