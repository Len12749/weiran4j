import { Banner, Button, Form } from '@douyinfe/semi-ui';
import { useState } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { useReviewCommand } from '@/hooks/queries/cqt-review';
import type { CqtReviewerScope } from '@/types/api';
import { CommandForm, ReasonField, required, useReviewFilters } from './ReviewForms';
import { options, PreviewDialog, ReviewTable, usePreviewAction } from './ReviewShared';

export default function ReviewersPage() {
    const filters = useReviewFilters(); const command = useReviewCommand(); const action = usePreviewAction();
    const [editing, setEditing] = useState<CqtReviewerScope | 'new'>(); const [competitionId, setCompetitionId] = useState<number>(); const [categoryId, setCategoryId] = useState<number>();
    const edit = (record: CqtReviewerScope | 'new') => { setCompetitionId(record === 'new' ? undefined : record.competitionId); setCategoryId(record === 'new' ? undefined : record.firstCategoryId); setEditing(record); };
    return <PageContainer>{action.error && <Banner type="danger" description={action.error} />}
        <ReviewTable<CqtReviewerScope> name="cqt/review/reviewers" path="/review/reviewers" filters={filters.applied} search={filters.search} onSearch={filters.apply} onReset={filters.reset}
            leading={<Permission code="cqt:reviewer:create"><Button theme="solid" onClick={() => edit('new')}>新增评委范围</Button></Permission>}
            columns={[{ title: '评委', dataIndex: 'userId', width: 160, render: (value: number) => filters.query.data?.users.find((item) => item.id === value)?.nickname ?? value }, { title: '赛事', dataIndex: 'competitionId', width: 200, render: (value: number) => filters.query.data?.competitions.find((item) => item.id === value)?.name ?? value }, { title: '阶段', dataIndex: 'phaseId', width: 110, render: (value: number) => filters.query.data?.phases.find((item) => item.id === value)?.name ?? value }, { title: '赛项', dataIndex: 'firstCategoryId', width: 130, render: (value: number) => filters.query.data?.categories.find((item) => item.id === value)?.name ?? value }, { title: '专业', dataIndex: 'secondCategoryId', width: 120, render: (value: number | null) => value ? filters.query.data?.majors.find((item) => item.id === value)?.name ?? value : '不限' }, { title: '赛区', dataIndex: 'regionId', width: 110, render: (value: number | null) => value ? filters.query.data?.regions.find((item) => item.id === value)?.name ?? value : '不限' }, { title: '状态', dataIndex: 'enabled', width: 80, render: (value: boolean) => value ? '启用' : '停用' }]}
            actions={(record) => [{ key: 'edit', label: '编辑范围', permission: 'cqt:reviewer:update', onClick: () => edit(record) }]} />
        {editing && <CommandForm title={editing === 'new' ? '新增评委范围' : '编辑范围及启停影响'} initial={editing === 'new' ? { enabled: true } : { ...editing }} resetDependencies={{ competitionId: ['phaseId'], firstCategoryId: ['secondCategoryId'] }} onCancel={() => setEditing(undefined)} submitLabel={editing === 'new' ? '保存' : '预览影响'} onSubmit={async (values) => {
            const body = { userId: values.userId, competitionId: values.competitionId, phaseId: values.phaseId, firstCategoryId: values.firstCategoryId, secondCategoryId: values.secondCategoryId ?? null, regionId: values.regionId ?? null, enabled: values.enabled, expectedVersion: editing === 'new' ? 0 : editing.version, reason: values.reason ?? null };
            if (editing === 'new') { await command.mutateAsync({ path: '/review/reviewers', body }); setEditing(undefined); }
            else { await action.prepare(`/review/reviewers/${editing.id}/state-preview`, body, `/review/reviewers/${editing.id}`, body, 'PUT'); }
        }}>
            <Form.Select field="userId" label="后台用户" rules={required} filter optionList={(filters.query.data?.users ?? []).map((item) => ({ value: item.id, label: `${item.nickname || item.username}（${item.username}）` }))} />
            <Form.Select field="competitionId" label="赛事" disabled={editing !== 'new'} rules={required} optionList={options(filters.query.data?.competitions)} onChange={(value) => setCompetitionId(Number(value))} />
            <Form.Select field="phaseId" label="省审/国审阶段" disabled={editing !== 'new'} rules={required} optionList={options(filters.query.data?.phases.filter((item) => item.code !== 'INITIAL' && (!competitionId || item.competitionId === competitionId)))} />
            <Form.Select field="firstCategoryId" label="赛项" rules={required} optionList={options(filters.query.data?.categories)} onChange={(value) => setCategoryId(Number(value))} />
            <Form.Select field="secondCategoryId" label="专业（留空不限）" showClear optionList={options(filters.query.data?.majors.filter((item) => !categoryId || item.parentId === categoryId))} />
            <Form.Select field="regionId" label="赛区（留空不限）" showClear optionList={options(filters.query.data?.regions)} /><Form.Switch field="enabled" label="启用范围" />
            {editing !== 'new' && <ReasonField />}<div>只配置现有后台用户的评审范围；账号创建与密码维护在用户管理完成。</div>
        </CommandForm>}
        {action.preview && <PreviewDialog title="范围变更影响明细" preview={action.preview} pending={action.pending} error={action.error} onClose={action.close} onConfirm={(ack) => void action.confirm(ack, () => setEditing(undefined))} />}
    </PageContainer>;
}
