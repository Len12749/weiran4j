import { Banner, Button, Input, InputNumber, Pagination, Select, SideSheet, Spin, Table, Tag, Descriptions } from '@douyinfe/semi-ui';
import type { ColumnProps } from '@douyinfe/semi-ui/lib/es/table';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { SearchToolbar } from '@/components/SearchToolbar';
import { useColumnSettings } from '@/components/ColumnSettings';
import { useActionsColumn, type TableAction } from '@/components/TableActions';
import { tableScrollX, useTableDefaults } from '@/hooks/useTableDefaults';
import { useCompetitionDetail, useCompetitionList, useCompetitionStatus } from '@/hooks/queries/cqt-competitions';
import type { CqtCompetitionAdminView, CqtCompetitionStatus } from '@/types/api';
export const competitionStatuses = [{ value: 0, label: '草稿/停用' }, { value: 1, label: '启用' }, { value: 2, label: '已结束' }];
export default function CompetitionsPage() {
    const navigate = useNavigate(); const { tableProps, pageSize } = useTableDefaults();
    const [draft, setDraft] = useState<{ keyword: string; year?: number | undefined; status?: CqtCompetitionStatus | undefined }>({ keyword: '' });
    const [viewId, setViewId] = useState<number>(); const details = useCompetitionDetail(viewId);
    const [applied, setApplied] = useState(draft); const [page, setPage] = useState(1); const [error, setError] = useState('');
    const query = useCompetitionList({ ...applied, page, pageSize }); const status = useCompetitionStatus();
    const search = () => { setApplied({ ...draft, keyword: draft.keyword.trim() }); setPage(1); };
    const columns: ColumnProps<CqtCompetitionAdminView>[] = [
        { title: '赛事名称', dataIndex: 'name', width: 240 }, { title: '年份', dataIndex: 'year', width: 80 }, { title: '届次', dataIndex: 'edition', width: 100 },
        { title: '省国赛模式', dataIndex: 'nationalEntryMode', width: 140, render: (value: string) => value === 'SHARED' ? '共用作品' : '分别报名' },
        { title: '报名开始', dataIndex: 'registrationStart', width: 158, render: (value: string | null) => value?.replace('T', ' ') ?? '不限' },
        { title: '报名截止', dataIndex: 'registrationEnd', width: 158, render: (value: string | null) => value?.replace('T', ' ') ?? '不限' },
        { title: '状态', dataIndex: 'status', width: 100, render: (value: number) => <Tag color={value === 1 ? 'green' : 'grey'}>{competitionStatuses.find((item) => item.value === value)?.label ?? value}</Tag> },
        { title: '操作', dataIndex: 'actions', fixed: 'right', width: 260 },
    ];
    const actions = (record: CqtCompetitionAdminView): TableAction[] => [
        { key: 'detail', label: '详情', permission: 'cqt:competition:list', onClick: () => setViewId(record.id) },
        { key: 'edit', label: '编辑', permission: 'cqt:competition:update', onClick: () => void navigate(`/cqt/competitions/edit/${record.id}`) },
        ...competitionStatuses.filter((item) => item.value !== record.status).map((item): TableAction => ({ key: `status-${item.value}`, label: item.label, permission: 'cqt:competition:status', disabled: status.isPending,
            confirm: { title: `将赛事设为${item.label}？`, content: '仅影响后续报名校验，不修改既有作品、评审或奖项。' }, onClick: () => { setError(''); status.mutate({ id: record.id, status: item.value as CqtCompetitionStatus, expectedVersion: record.version }, { onError: (cause) => setError(`${cause.message}；请刷新列表后重试。`) }); } })),
    ];
    const { columns: settled, columnSettings } = useColumnSettings('cqt/competitions', columns); const { columns: visible } = useActionsColumn(settled, actions);
    return <PageContainer><SearchToolbar tools={columnSettings} leading={<Permission code="cqt:competition:create"><Button theme="solid" onClick={() => void navigate('/cqt/competitions/create')}>新增赛事</Button></Permission>} onSearch={search} onReset={() => { setDraft({ keyword: '' }); setApplied({ keyword: '' }); setPage(1); }} onRefresh={() => void query.refetch()} refreshing={query.isFetching}>
        <Input placeholder="赛事名称" maxLength={300} showClear value={draft.keyword} onChange={(keyword) => setDraft({ ...draft, keyword })} onKeyDown={(event) => { if (event.key === 'Enter') search(); }} style={{ width: 220 }} />
        <InputNumber placeholder="年份" min={1900} max={9999} precision={0} value={draft.year ?? ''} onChange={(value) => setDraft({ ...draft, year: typeof value === 'number' ? value : undefined })} style={{ width: 120 }} />
        <Select placeholder="全部状态" showClear value={draft.status} optionList={competitionStatuses} onChange={(value) => setDraft({ ...draft, status: value as CqtCompetitionStatus | undefined })} style={{ width: 140 }} />
    </SearchToolbar>{(error || query.isError) && <Banner type="danger" description={error || query.error?.message} />}
        <Table<CqtCompetitionAdminView> {...tableProps} rowKey="id" columns={visible} dataSource={query.data?.list ?? []} loading={query.isFetching} pagination={false} scroll={{ x: tableScrollX(visible) }} />
        {!!query.data?.total && <Pagination style={{ marginTop: 16, justifyContent: 'flex-end' }} currentPage={page} pageSize={pageSize} total={query.data.total} onPageChange={setPage} />}
        <SideSheet title="赛事详情" visible={viewId !== undefined} onCancel={() => setViewId(undefined)} width="70vw">
            {details.isError ? <Banner type="danger" description={<>{details.error.message}<Button onClick={() => void details.refetch()}>重试</Button></>} /> : !details.data ? <Spin /> : <Descriptions align="left" data={[
                ['编号', details.data.id], ['旧编号', details.data.legacyId], ['赛事名称', details.data.name], ['年份', details.data.year], ['届次', details.data.edition], ['说明', details.data.description],
                ['省国赛模式', details.data.nationalEntryMode === 'SHARED' ? '共用作品' : '分别报名'], ['状态', competitionStatuses.find((item) => item.value === details.data?.status)?.label],
                ['报名开始', details.data.registrationStart], ['报名截止', details.data.registrationEnd], ['省审开始（计划）', details.data.provinceReviewStart], ['省审结束（计划）', details.data.provinceReviewEnd],
                ['国审开始（计划）', details.data.nationalReviewStart], ['国审结束（计划）', details.data.nationalReviewEnd], ['成绩公布（计划）', details.data.resultPublishTime],
                ['省赛证书模板URL', details.data.provinceCertificateTemplate], ['国赛证书模板URL', details.data.nationalCertificateTemplate], ['教师/组织证书模板URL', details.data.teacherOrgCertificateTemplate],
                ['版本', details.data.version], ['创建时间', details.data.createdAt], ['更新时间', details.data.updatedAt],
            ].map(([key, value]) => ({ key: String(key), value: value === null || value === undefined || value === '' ? '—' : String(value) }))} />}
        </SideSheet>
    </PageContainer>;
}
