import { Banner, Button, Checkbox, Image, ImagePreview, Modal, Pagination, Space, Table, Typography } from '@douyinfe/semi-ui';
import type { ColumnProps } from '@douyinfe/semi-ui/lib/es/table';
import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { useColumnSettings } from '@/components/ColumnSettings';
import { SearchToolbar } from '@/components/SearchToolbar';
import { useActionsColumn, type TableAction } from '@/components/TableActions';
import { tableScrollX, useTableDefaults } from '@/hooks/useTableDefaults';
import { commandKey, useReviewCommand, useReviewQuery, type ReviewQuery } from '@/hooks/queries/cqt-review';
import type { PageResult, CqtReviewOptions, CqtReviewPreview } from '@/types/api';

export const phaseLabels: Record<string, string> = { INITIAL: '初审', PROVINCIAL: '省审', NATIONAL: '国审' };
export const sourceLabels: Record<string, string> = { zhongxi: '中戏', qudao: '渠道', houtai: '后台' };
export const stateLabels: Record<string, string> = { DRAFT: '草稿', OPEN: '开放评分', CLOSED: '已关闭', FROZEN: '已确认汇总', EMPTY: '待评分', SUBMITTED: '已提交', ASSIGNED: '已分配', CANCELLED: '已取消', CONFIRMED: '已确认', REVOKED: '已撤销', ATTACHED: '已关联', STALE: '待复核', VALID: '有效', ACTIVE: '有效', ELIGIBLE: '待决定', ADVANCED: '已晋级', ELIMINATED: '已淘汰', LOCKED: '已锁定', APPROVED: '通过', REJECTED: '驳回', CREATE: '新建分配', UNCHANGED: '无需变更', REPAIR: '修正对象', READY: '可汇总', SKIPPED: '跳过', MISSING: '缺评/待汇总', MATCHED: '已匹配', UNMATCHED: '未命中', BLOCKED: '阻断', PENDING: '待初审', ENTRY: '作品级', PARTICIPANT: '参赛人级' };
export function label(value: string | undefined | null) { return value ? stateLabels[value] ?? phaseLabels[value] ?? sourceLabels[value] ?? value : '—'; }
export interface NamedOption { id: number; name: string; competitionId?: number | undefined; parentId?: number | null; code?: string; state?: string; version?: number }
export interface ReviewOptions { competitions: NamedOption[]; phases: NamedOption[]; categories: NamedOption[]; majors: NamedOption[]; regions: NamedOption[]; users: { id: number; username: string; nickname: string }[] }
export function useReviewOptions() {
    const query = useReviewQuery<CqtReviewOptions>('/review/options');
    const data = useMemo(() => query.data ? { ...query.data,
        phases: (query.data.phases ?? []).map((item) => ({ ...item, competitionId: item.competitionId, name: phaseLabels[item.code] || item.code })),
        categories: (query.data.categories ?? []).filter((item) => !item.parentId),
        majors: (query.data.categories ?? []).filter((item) => !!item.parentId),
    } : undefined, [query.data]);
    return { ...query, data };
}
export function options(rows: NamedOption[] | undefined) { return (rows ?? []).map((row) => ({ value: row.id, label: row.name })); }

export function ReviewTable<T extends object>({ name, path, filters = {}, columns, actions, leading, tools, search, onSearch, onReset, rowSelection, rowKey = 'id', mapRow }: {
    name: string; path: string; rowKey?: string | ((record?: T) => string); mapRow?: (row: T) => T; filters?: ReviewQuery; columns: ColumnProps<T>[]; actions?: (record: T) => TableAction[];
    leading?: ReactNode; tools?: ReactNode; search?: ReactNode; onSearch?: () => void; onReset?: () => void;
    rowSelection?: { selectedRowKeys: (string | number)[]; onChange: (keys: (string | number)[], selectedRows: T[]) => void; getCheckboxProps?: (record: T) => { disabled: boolean } };
}) {
    const { tableProps, pageSize } = useTableDefaults();
    const [page, setPage] = useState(1);
    const query = useReviewQuery<PageResult<T> | T[]>(path, { ...filters, page, pageSize });
    const rawRows = Array.isArray(query.data) ? query.data : query.data?.list ?? [];
    const resultRows = mapRow ? rawRows.map(mapRow) : rawRows;
    const total = Array.isArray(query.data) ? query.data.length : query.data?.total ?? 0;
    const displayedRows = Array.isArray(query.data) ? resultRows.slice((page - 1) * pageSize, page * pageSize) : resultRows;
    const allColumns = actions ? [...columns, { title: '操作', dataIndex: 'actions', fixed: 'right' as const, width: 190 }] : columns;
    const { columns: settled, columnSettings } = useColumnSettings(name, allColumns);
    const { columns: displayed } = useActionsColumn(settled, actions ?? (() => []));
    return <>
        <SearchToolbar tools={<>{columnSettings}{tools}</>} leading={leading} onSearch={() => { onSearch?.(); setPage(1); }} onReset={() => { onReset?.(); setPage(1); }} onRefresh={() => void query.refetch()} refreshing={query.isFetching}><span style={{ display: 'contents' }} onKeyDown={(event) => { if (event.key === 'Enter' && event.target instanceof HTMLInputElement) { onSearch?.(); setPage(1); } }}>{search}</span></SearchToolbar>
        {query.isError && <Banner type="danger" description={query.error.message} />}
        <Table<T> {...tableProps} rowKey={rowKey} columns={displayed} dataSource={displayedRows} loading={query.isFetching} pagination={false} scroll={{ x: tableScrollX(displayed) }} {...(rowSelection ? { rowSelection: { ...rowSelection, onChange: (keys, rows) => rowSelection.onChange(keys ?? [], rows ?? []) } } : {})} />
        {total > 0 && <Pagination style={{ marginTop: 16, justifyContent: 'flex-end' }} currentPage={page} pageSize={pageSize} total={total} onPageChange={setPage} />}
    </>;
}

export type ReviewPreview = CqtReviewPreview;
const columnTitles: Record<string, string> = { targetId: '评分对象', entryId: '作品编号', title: '作品', entryTitle: '作品', entryNo: '报名号', participantName: '参赛人', userId: '评委编号', reviewerName: '评委', name: '名称', reason: '原因', blockedReason: '阻断原因', comment: '评语', revision: '修订', scoreVersion: '评分版本', ruleId: '规则', ruleRevision: '规则修订', finalVersion: '成绩版本', phaseId: '阶段编号', assignmentId: '任务号', createdBy: '操作人编号', operatorId: '操作人编号', sourceTargetId: '省审对象', nationalTargetId: '国审对象', inputVersion: '输入版本', inputJson: '评分依据', outputJson: '计算结果', finalScoreId: '最终成绩编号', scoreId: '评分编号', participantId: '参赛人编号', firstCategoryId: '赛项编号', secondCategoryId: '专业编号', regionId: '赛区编号', competitionId: '赛事编号', scoringScope: '评分对象类型', rule: '规则', dimensions: '维度记录', totalScore: '综合分', minimumReviewers: '最低评委数', submitted: '已提交', duplicates: '重复', reviewers: '评委工作量', load: '当前工作量', userVersion: '用户版本', reviewerId: '评委编号', updatedAt: '更新时间', assignedCount: '已分配数', targets: '评分对象', assignments: '分配记录', qualifying: '满足条件', validReviewerCount: '有效评委数', averageScore: '平均分', beforeScore: '原分', afterScore: '新分', kind: '变更类型', description: '说明', groupName: '组别', school: '学校', enabled: '启用', score: '分数', finalScore: '最终分', average: '平均分', reviewerCount: '有效评委数', count: '数量', awardName: '奖项', source: '来源', targetType: '评分对象类型', state: '状态', status: '状态', decision: '决定', action: '操作', message: '说明', createdAt: '时间', operatorName: '操作人', before: '原值', after: '新值', type: '类型', matched: '匹配数', unmatched: '未命中', skipped: '跳过', created: '新增', unchanged: '未变', blocked: '阻断', total: '总数', eligible: '可处理', missing: '缺少', pending: '待评', conflicts: '冲突', version: '版本' };
function display(value: unknown): string {
    if (value === null || value === undefined) return '—';
    if (typeof value === 'boolean') return value ? '是' : '否';
    if (Array.isArray(value)) return value.map(display).join('、');
    if (typeof value === 'object') return Object.entries(value).map(([key, item]) => `${columnTitles[key] ?? key}：${display(item)}`).join('；');
    return typeof value === 'string' ? label(value) : typeof value === 'number' ? String(value) : '—';
}
export function SnapshotTable({ rows }: { rows: Record<string, unknown>[] }) {
    const normalized = rows.map((row) => Object.fromEntries(Object.entries(row).map(([key, value]) => {
        if (['inputJson', 'outputJson', 'detailJson'].includes(key) && typeof value === 'string') {
            try { return [key, JSON.parse(value) as unknown]; } catch { return [key, '历史快照不可读取']; }
        }
        return [key, value];
    })));
    const keys = [...new Set(normalized.flatMap((row) => Object.keys(row)))].filter((key) => !['previewToken', 'payloadHash', 'targetHash', 'idCard', 'idcard'].includes(key));
    return <Table pagination={false} dataSource={normalized.map((row, index) => ({ ...row, _rowKey: String(index) }))} rowKey="_rowKey" scroll={{ x: Math.max(600, keys.length * 150) }} columns={keys.map((key) => ({ title: columnTitles[key] ?? key, dataIndex: key, width: 150, render: display }))} empty="暂无记录" />;
}
export function PreviewDialog({ preview, title, onClose, onConfirm, pending, allowSkipped = false, error, canConfirm = true }: {
    preview: ReviewPreview; title: string; onClose: () => void; onConfirm: (acknowledgeSkipped: boolean) => void; pending: boolean; allowSkipped?: boolean; error?: string; canConfirm?: boolean;
}) {
    const [acknowledged, setAcknowledged] = useState(false);
    const [now, setNow] = useState(() => Date.now());
    useEffect(() => { const timer = setInterval(() => setNow(Date.now()), 1000); return () => clearInterval(timer); }, []);
    const expires = Date.parse(preview.expiresAt);
    const expired = Number.isFinite(expires) && expires <= now;
    return <Modal title={title} visible width="90vw" onCancel={() => { if (!pending) onClose(); }} footer={<Space><Button disabled={pending} onClick={onClose}>返回修改</Button><Button theme="solid" loading={pending} disabled={!canConfirm || expired || (preview.blockedRows.length > 0 && !allowSkipped) || (allowSkipped && preview.blockedRows.length > 0 && !acknowledged)} onClick={() => onConfirm(acknowledged)}>确认</Button></Space>}>
        {error && <Banner type="danger" description={error} />}{expired && <Banner type="warning" description="预览已过期，请返回重新预览" />}
        <Typography.Paragraph>{typeof preview.summary === 'string' ? preview.summary : Object.entries(preview.summary).map(([key, value]) => `${columnTitles[key] ?? key}：${display(value)}`).join(' ')} 有效期至 {preview.expiresAt}</Typography.Paragraph>
        <Typography.Title heading={6}>处理明细</Typography.Title><SnapshotTable rows={preview.rows} />
        {preview.blockedRows.length > 0 && <><Typography.Title heading={6}>{allowSkipped ? '缺评或失效对象' : '阻断原因'}</Typography.Title><SnapshotTable rows={preview.blockedRows} />{allowSkipped && <Checkbox checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked ?? false)}>我确认已检查并接受以上跳过对象</Checkbox>}</>}
    </Modal>;
}
export function usePreviewAction() {
    const command = useReviewCommand();
    const [preview, setPreview] = useState<ReviewPreview>();
    const [error, setError] = useState('');
    const generation = useRef(0);
    const confirming = useRef(false);
    const [allowSkipped, setAllowSkipped] = useState(false);
    useEffect(() => () => { generation.current++; }, []);
    const context = useRef<{ path: string; body: object; key: string; method: 'POST' | 'PUT' }>({ path: '', body: {}, key: '', method: 'POST' });
    const prepare = async (path: string, body: object, confirmPath: string, confirmBody: object = {}, method: 'POST' | 'PUT' = 'POST') => {
        const active = ++generation.current; setAllowSkipped(confirmPath.endsWith('/calculate')); setPreview(undefined); setError('');
        try { const result = await command.mutateAsync({ path, body }); if (active !== generation.current) return; context.current = { path: confirmPath, body: confirmBody, key: commandKey(), method }; setPreview(result as ReviewPreview); }
        catch (cause) { if (active === generation.current) setError(cause instanceof Error ? cause.message : '预览失败'); throw cause; }
    };
    const confirm = async (acknowledgeSkipped: boolean, done: () => void) => {
        if (confirming.current || !preview) return;
        confirming.current = true;
        const active = generation.current;
        const current = { ...context.current };
        const token = preview.previewToken;
        setError('');
        try {
            await command.mutateAsync({ path: current.path, method: current.method, body: { ...current.body, previewToken: token, requestKey: current.key, ...(current.path.endsWith('/calculate') ? { acknowledgeSkipped } : {}) } });
            if (active === generation.current) { setPreview(undefined); done(); }
        } catch (cause) { if (active === generation.current) setError(cause instanceof Error ? `${cause.message}；请返回重新预览后重试` : '确认失败'); }
        finally { confirming.current = false; }
    };
    return { preview, error, prepare, confirm, pending: command.isPending, allowSkipped, close: () => { generation.current++; setPreview(undefined); setError(''); } };
}
export function AttachmentPreview({ image, url, name }: { image?: string | undefined; url?: string | undefined; name?: string | undefined }) {
    const [visible, setVisible] = useState(false);
    const safe = (value: string | undefined) => value && /^(https?:\/\/|\/(?!\/))/i.test(value.trim()) ? value.trim() : '';
    const address = safe(url);
    const imageUrl = safe(image) || (address && /\.(jpe?g|png|gif|webp)(?:[?#]|$)/i.test(address) ? address : '');
    return <Space vertical align="start">{imageUrl && <><Image src={imageUrl} width={200} height={160} preview={false} imgStyle={{ objectFit: 'contain' }} /><Button onClick={() => setVisible(true)}>查看原图</Button>{visible && <ImagePreview src={[imageUrl]} visible initialZoom={1} onVisibleChange={setVisible} />}</>}{address && <Typography.Text link={{ href: address, target: '_blank', rel: 'noopener' }}>{name || '打开作品附件'}</Typography.Text>}</Space>;
}

export function textValue(value: unknown): string { return typeof value === 'string' ? value : ''; }
