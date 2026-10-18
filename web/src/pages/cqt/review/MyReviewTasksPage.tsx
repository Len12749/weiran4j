import { Select } from '@douyinfe/semi-ui';
import { useNavigate } from 'react-router-dom';
import { PageContainer } from '@/components/PageContainer';
import type { CqtReviewAssignment } from '@/types/api';
import { useReviewFilters } from './ReviewForms';
import { label, ReviewTable } from './ReviewShared';

export default function MyReviewTasksPage() {
    const filters = useReviewFilters(); const navigate = useNavigate();
    return <PageContainer><ReviewTable<CqtReviewAssignment> name="cqt/review/my-tasks" path="/review/my-tasks" filters={filters.applied} onSearch={filters.apply} onReset={filters.reset}
        search={<>{filters.search}<Select placeholder="全部评分状态" showClear style={{ width: 140 }} value={filters.draft.status} optionList={[{ value: 'EMPTY', label: '待评分' }, { value: 'DRAFT', label: '草稿' }, { value: 'SUBMITTED', label: '已提交' }]} onChange={(value) => filters.setDraft({ ...filters.draft, status: String(value ?? '') })} /></>}
        columns={[{ title: '任务号', dataIndex: 'id', width: 90 }, { title: '作品', dataIndex: 'target', width: 230, render: (_, record) => record?.target?.title ?? record?.title ?? '—' }, { title: '参赛人/对象', dataIndex: 'name', width: 150, render: (_, record) => record?.target?.targetType === 'ENTRY' ? '作品级' : record?.target?.name ?? record?.name ?? '—' }, { title: '来源', dataIndex: 'source', width: 90, render: (_, record) => label(record?.target?.source ?? record?.source) }, { title: '阶段', dataIndex: 'phaseId', width: 110, render: (value: number) => (() => { const phase = filters.query.data?.phases.find((item) => item.id === value); return phase ? `${filters.query.data?.competitions.find((item) => item.id === phase.competitionId)?.name ?? ''} · ${phase.name}` : value; })() }, { title: '任务状态', dataIndex: 'state', width: 110, render: label }, { title: '评分状态', dataIndex: 'scoreStatus', width: 110, render: label }]}
        actions={(record) => [{ key: 'score', label: '评审/查看', permission: 'cqt:review-task:list', onClick: () => void navigate(`/cqt/review/my-tasks/${record.id}`) }]} />
    </PageContainer>;
}
