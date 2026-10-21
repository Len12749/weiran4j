import { Button, Input, Pagination, Select, Table, Tag, Toast, Banner } from '@douyinfe/semi-ui';
import type { ColumnProps } from '@douyinfe/semi-ui/lib/es/table';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { SearchToolbar } from '@/components/SearchToolbar';
import { useColumnSettings } from '@/components/ColumnSettings';
import { useActionsColumn, type TableAction } from '@/components/TableActions';
import { usePermission } from '@/hooks/usePermission';
import { useTableDefaults, tableScrollX } from '@/hooks/useTableDefaults';
import { useContentList, useDeleteContent, type ContentResource, type ContentView } from '@/hooks/queries/cqt-content';
import { contentConfig } from './ContentConfig';

export function ContentList({ resource }: { resource: ContentResource }) {
    const config = contentConfig[resource];
    const navigate = useNavigate();
    const { hasAnyPermission } = usePermission();
    const { tableProps, pageSize } = useTableDefaults();
    const [draft, setDraft] = useState({ keyword: '', category: '', status: '' });
    const [filters, setFilters] = useState(draft);
    const [page, setPage] = useState(1);
    const query = useContentList(resource, { page, pageSize, keyword: filters.keyword, category: filters.category, ...(filters.status ? { enabled: filters.status === 'enabled' } : {}) });
    const remove = useDeleteContent(resource);
    const search = () => { setFilters({ ...draft, keyword: draft.keyword.trim() }); setPage(1); };
    const columns: ColumnProps<ContentView>[] = [
        { title: resource === 'experts' ? '姓名' : '标题', dataIndex: config.titleField, width: 240 },
        ...(resource !== 'resources' ? [{ title: '首图', dataIndex: 'image', width: 90, render: (value: string) => <img src={value} alt="首图" style={{ width: 48, height: 48, objectFit: 'cover' }} /> }] : []),
        ...(config.categories.length ? [{ title: '分类', dataIndex: 'category', width: 140, render: (value: string) => config.categories.find((item) => item.value === value)?.label ?? value }] : []),
        ...(resource === 'experts' ? [{ title: '首页展示', dataIndex: 'showOnHome', width: 100, render: (value: boolean) => value ? '展示' : '不展示' }] : []),
        { title: '状态', dataIndex: 'enabled', width: 90, render: (value: boolean) => <Tag color={value ? 'green' : 'grey'}>{value ? '启用' : '禁用'}</Tag> },
        { title: '排序', dataIndex: 'sort', width: 70 },
        { title: '更新时间', dataIndex: 'updatedAt', width: 180 },
    ];
    const permission = `cqt:${config.permission}`;
    if (hasAnyPermission(`${permission}:update`, `${permission}:delete`)) columns.push({ title: '操作', dataIndex: 'actions', fixed: 'right', width: 150 });
    const actions = (record: ContentView): TableAction[] => [
        { key: 'edit', label: '编辑', permission: `${permission}:update`, onClick: () => void navigate(`/cqt/${resource}/edit/${record.id}`) },
        { key: 'delete', label: '删除', danger: true, permission: `${permission}:delete`, disabled: remove.isPending,
            confirm: { title: `确定删除这条${config.name}记录？`, content: '删除后前台将不再展示' }, onClick: () => remove.mutate(record.id, { onSuccess: () => Toast.success('已删除') }) },
    ];
    const { columns: settled, columnSettings } = useColumnSettings(`cqt/${resource}`, columns);
    const { columns: visible } = useActionsColumn(settled, actions);
    return <PageContainer>
        <SearchToolbar tools={columnSettings} onSearch={search}
            onReset={() => { const empty = { keyword: '', category: '', status: '' }; setDraft(empty); setFilters(empty); setPage(1); }}
            onRefresh={() => void query.refetch()} refreshing={query.isFetching}
            leading={<Permission code={`${permission}:create`}><Button theme="solid" onClick={() => void navigate(`/cqt/${resource}/create`)}>新增{config.name}</Button></Permission>}>
            <Input placeholder={resource === 'experts' ? '按姓名搜索' : '按标题搜索'} value={draft.keyword} showClear style={{ width: 220 }} onChange={(keyword) => setDraft({ ...draft, keyword })} onEnterPress={search} />
            {config.categories.length > 0 && <Select placeholder="全部分类" showClear value={draft.category || undefined} optionList={config.categories} style={{ width: 160 }} onChange={(value) => setDraft({ ...draft, category: String(value ?? '') })} />}
            <Select placeholder="全部状态" showClear value={draft.status || undefined} optionList={[{ value: 'enabled', label: '启用' }, { value: 'disabled', label: '禁用' }]} style={{ width: 120 }} onChange={(value) => setDraft({ ...draft, status: String(value ?? '') })} />
        </SearchToolbar>
        {query.isError && <Banner type="danger" description={query.error.message} />}
        <Table<ContentView> {...tableProps} rowKey="id" columns={visible} dataSource={query.data?.list ?? []} loading={query.isFetching} pagination={false} scroll={{ x: tableScrollX(visible) }} />
        {!!query.data?.total && <Pagination style={{ marginTop: 16, justifyContent: 'flex-end' }} currentPage={page} pageSize={pageSize} total={query.data.total} onPageChange={setPage} />}
    </PageContainer>;
}
