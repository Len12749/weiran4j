import { Button, Input, Pagination, Table, Tag, Toast, Typography } from '@douyinfe/semi-ui';
import type { ColumnProps } from '@douyinfe/semi-ui/lib/es/table';
import { useState } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { useColumnSettings } from '@/components/ColumnSettings';
import { SearchToolbar } from '@/components/SearchToolbar';
import { useActionsColumn, type TableAction } from '@/components/TableActions';
import { useCqtNews, useDeleteCqtNews } from '@/hooks/queries/cqt-news';
import { usePermission } from '@/hooks/usePermission';
import { tableScrollX, useTableDefaults } from '@/hooks/useTableDefaults';
import type { CqtNewsView } from '@/types/api';
import { useNavigate } from 'react-router-dom';

export default function CqtNewsPage() {
    const navigate = useNavigate();
    const { tableProps, pageSize } = useTableDefaults();
    const { hasAnyPermission } = usePermission();
    const [keywordInput, setKeywordInput] = useState('');
    const [keyword, setKeyword] = useState('');
    const [page, setPage] = useState(1);
    const query = { page, pageSize, ...(keyword ? { keyword } : {}) };
    const { data, isFetching, refetch } = useCqtNews(query);
    const deleteNews = useDeleteCqtNews();

    const columns: ColumnProps<CqtNewsView>[] = [
        { title: '标题', dataIndex: 'title', width: 240 },
        {
            title: '首图',
            dataIndex: 'image',
            width: 110,
            render: (value: string) => (value ? <img src={value} alt="动态首图" style={{ width: 72, height: 40, objectFit: 'cover' }} /> : '—'),
        },
        {
            title: '首页展示',
            dataIndex: 'showOnHome',
            width: 100,
            render: (value: boolean) => <Tag color={value ? 'green' : 'grey'}>{value ? '展示' : '不展示'}</Tag>,
        },
        { title: '排序', dataIndex: 'sort', width: 70 },
        { title: '更新时间', dataIndex: 'updatedAt', width: 180 },
    ];

    const actions = (record: CqtNewsView): TableAction[] => [
        { key: 'edit', label: '编辑', permission: 'cqt:news:update', onClick: () => void navigate(`/cqt/news/edit/${record.id}`) },
        {
            key: 'delete',
            label: '删除',
            permission: 'cqt:news:delete',
            danger: true,
            confirm: { title: '确定删除这条动态？', content: '删除后前台将不再展示该动态' },
            onClick: () => deleteNews.mutate(record.id, { onSuccess: () => Toast.success('动态已删除') }),
        },
    ];
    if (hasAnyPermission('cqt:news:update', 'cqt:news:delete')) {
        columns.push({ title: '操作', dataIndex: 'actions', fixed: 'right', width: 150 });
    }
    const { columns: settledColumns, columnSettings } = useColumnSettings('cqt/news', columns);
    const { columns: tableColumns } = useActionsColumn(settledColumns, actions);

    return (
        <PageContainer>
            <SearchToolbar
                tools={columnSettings}
                searchIconOnly
                resetIconOnly
                onSearch={() => {
                    setKeyword(keywordInput.trim());
                    setPage(1);
                }}
                onReset={() => {
                    setKeywordInput('');
                    setKeyword('');
                    setPage(1);
                }}
                onRefresh={() => void refetch()}
                refreshing={isFetching}
                leading={
                    <Permission code="cqt:news:create">
                        <Button type="primary" theme="solid" onClick={() => void navigate('/cqt/news/create')}>
                            新增动态
                        </Button>
                    </Permission>
                }
            >
                <Input
                    value={keywordInput}
                    onChange={(value) => setKeywordInput(value)}
                    placeholder="按标题搜索"
                    showClear
                    style={{ width: 240 }}
                    onEnterPress={() => {
                        setKeyword(keywordInput.trim());
                        setPage(1);
                    }}
                />
            </SearchToolbar>
            <Table<CqtNewsView>
                {...tableProps}
                rowKey="id"
                columns={tableColumns}
                dataSource={data?.list ?? []}
                loading={isFetching}
                pagination={false}
                scroll={{ x: tableScrollX(tableColumns) }}
            />
            {data && data.total > 0 && (
                <Pagination
                    style={{ marginTop: 16, justifyContent: 'flex-end' }}
                    currentPage={page}
                    pageSize={pageSize}
                    total={data.total}
                    onPageChange={setPage}
                />
            )}
            {!isFetching && data?.total === 0 && <Typography.Text type="tertiary">暂无动态</Typography.Text>}
        </PageContainer>
    );
}
