import { Button, Form, Tag } from '@douyinfe/semi-ui';
import { useState } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { useReviewCommand } from '@/hooks/queries/cqt-review';
import type { CqtAwardRule } from '@/types/api';
import { CommandForm, required, useReviewFilters } from './ReviewForms';
import { label, options, ReviewTable } from './ReviewShared';

export default function AwardRulesPage() {
    const filters = useReviewFilters({ keyword: false });
    const command = useReviewCommand();
    const [editing, setEditing] = useState<CqtAwardRule | 'new'>();
    const [phaseId, setPhaseId] = useState<number>();
    const [categoryId, setCategoryId] = useState<number>();
    const edit = (record: CqtAwardRule | 'new') => { setEditing(record); setPhaseId(record === 'new' ? undefined : record.phaseId); setCategoryId(record === 'new' ? undefined : record.firstCategoryId); };
    return <PageContainer>
        <ReviewTable<CqtAwardRule> name="cqt/review/award-rules" path="/award/rules" filters={filters.applied} search={filters.search} onSearch={filters.apply} onReset={filters.reset}
            leading={<Permission code="cqt:award-rule:create"><Button theme="solid" onClick={() => edit('new')}>新增奖项规则</Button></Permission>}
            columns={[
                { title: '阶段', dataIndex: 'phaseId', width: 180, render: (value: number) => (() => { const phase = filters.query.data?.phases.find((item) => item.id === value); return phase ? `${filters.query.data?.competitions.find((item) => item.id === phase.competitionId)?.name ?? ''} · ${phase.name}` : value; })() },
                { title: '奖项', dataIndex: 'awardName', width: 180 }, { title: '对象', dataIndex: 'targetType', width: 110, render: label },
                { title: '赛项', dataIndex: 'firstCategoryId', width: 140, render: (value: number) => filters.query.data?.categories.find((item) => item.id === value)?.name ?? value },
                { title: '最低分（含）', dataIndex: 'minScore', width: 110 }, { title: '最高分（含）', dataIndex: 'maxScore', width: 110 },
                { title: '状态', dataIndex: 'enabled', width: 90, render: (value: boolean) => <Tag>{value ? '启用' : '停用'}</Tag> },
                { title: '修订', dataIndex: 'revision', width: 80 },
            ]} actions={(record) => [{ key: 'edit', label: '编辑', permission: 'cqt:award-rule:update', onClick: () => edit(record) }]} />
        {editing && <CommandForm resetDependencies={{ firstCategoryId: ['secondCategoryId'] }} title={editing === 'new' ? '新增奖项规则' : '编辑奖项规则'} initial={editing === 'new' ? { enabled: true, targetType: 'ENTRY', minScore: 0, maxScore: 100 } : { ...editing }} onCancel={() => setEditing(undefined)} onSubmit={async (values) => {
                        await command.mutateAsync({ path: editing === 'new' ? '/award/rules' : `/award/rules/${editing.id}`, method: editing === 'new' ? 'POST' : 'PUT', body: { phaseId: values.phaseId, targetType: values.targetType, firstCategoryId: values.firstCategoryId, awardName: values.awardName, minScore: values.minScore, maxScore: values.maxScore, enabled: values.enabled, secondCategoryId: values.secondCategoryId ?? null, groupName: values.groupName || null, ...(editing !== 'new' ? { expectedVersion: editing.version } : {}) } }); setEditing(undefined);
        }}>
            <Form.Select field="phaseId" label="赛事阶段" rules={required} optionList={(filters.query.data?.phases ?? []).filter((item) => item.code !== 'INITIAL').map((item) => ({ value: item.id, label: `${filters.query.data?.competitions.find((competition) => competition.id === item.competitionId)?.name ?? ''} · ${item.name}` }))} onChange={(value) => setPhaseId(Number(value))} disabled={editing !== 'new'} />
            <Form.Select field="targetType" label="评分对象" rules={required} optionList={[{ value: 'ENTRY', label: '作品级' }, { value: 'PARTICIPANT', label: '参赛人级' }]} />
            <Form.Select field="firstCategoryId" label="赛项" rules={required} optionList={options(filters.query.data?.categories)} onChange={(value) => setCategoryId(Number(value))} />
            <Form.Select field="secondCategoryId" label="专业（留空不限）" showClear optionList={options(filters.query.data?.majors.filter((item) => !categoryId || item.parentId === categoryId))} />
            <Form.Input field="groupName" label="组别（留空不限）" maxLength={255} /><Form.Input field="awardName" label="奖项名称" rules={required} maxLength={255} />
            <Form.InputNumber field="minScore" label="最低分（含）" precision={2} rules={required} /><Form.InputNumber field="maxScore" label="最高分（含）" precision={2} rules={required} />
            <Form.Switch field="enabled" label="启用" /><div>同一适用范围的启用规则不可重叠，不设获奖名额。{phaseId ? '' : '请选择赛事阶段。'}</div>
        </CommandForm>}
    </PageContainer>;
}
