import { Banner, Button, Form, Space, Toast, Typography } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import dayjs from 'dayjs';
import { useEffect, useRef, useState } from 'react';
import { SingleImageUpload } from '@/components/SingleImageUpload';
import { PageContainer } from '@/components/PageContainer';
import { usePageTabs } from '@/hooks/usePageTabs';
import { useSaveCompetition, useUploadCompetitionImage } from '@/hooks/queries/cqt-competitions';
import { ApiError } from '@/utils/request';
import { DATE_TIME_FORMAT } from '@/utils/date';
import type { CqtCompetitionAdminView, CqtCompetitionBody, CqtCompetitionMode } from '@/types/api';
const required = [{ required: true, message: '此项必填' }];
const windows = [
    { field: 'registrationRange', start: 'registrationStart', end: 'registrationEnd', label: '报名时间' },
    { field: 'provinceReviewRange', start: 'provinceReviewStart', end: 'provinceReviewEnd', label: '省审时间（计划）' },
    { field: 'nationalReviewRange', start: 'nationalReviewStart', end: 'nationalReviewEnd', label: '国审时间（计划）' },
] as const;
const templates = ['provinceCertificateTemplate', 'nationalCertificateTemplate', 'teacherOrgCertificateTemplate'] as const;
const text = (value: unknown) => typeof value === 'string' ? value.trim() : '';
function templateUrl(value: unknown) {
    const raw = text(value); if (!raw) return null;
    if (raw.length > 1000 || [...raw].some((character) => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127)) throw new Error('模板地址格式无效');
    if (raw.startsWith('/uploads/')) {
        let decoded: string; try { decoded = decodeURIComponent(raw); } catch { throw new Error('模板路径无效'); }
        if (/[\\?#]/.test(raw) || /[\\?#]/.test(decoded) || decoded.split('/').some((segment) => segment === '.' || segment === '..') || [...decoded].some((character) => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127)) throw new Error('模板路径无效');
        return raw;
    }
    let url: URL; try { url = new URL(raw); } catch { throw new Error('模板地址须为完整http/https地址'); }
    const forbidden = ['accesskeyid', 'access_key_id', 'access_key_secret', 'secret', 'token', 'password', 'signature', 'security-token', 'ossaccesskeyid'];
    if (!['http:', 'https:'].includes(url.protocol) || !url.hostname || url.username || url.password || [...url.searchParams.keys()].some((key) => forbidden.includes(key.toLowerCase()) || /^x-(oss|amz)-/i.test(key))) throw new Error('模板地址不可包含凭据或签名参数');
    return raw;
}
export default function CompetitionForm({ record, onReload }: { record?: CqtCompetitionAdminView; onReload?: () => void }) {
    const [original] = useState(record);
    const [initial] = useState<Record<string, unknown>>(() => ({ name: '', nationalEntryMode: 'SHARED', ...record,
        ...Object.fromEntries(windows.map(({ field, start, end }) => [field,
            record?.[start] && record?.[end] ? [new Date(record[start]), new Date(record[end])] : undefined])),
        resultPublishTime: record?.resultPublishTime ? new Date(record.resultPublishTime) : undefined }));
    const api = useRef<FormApi<Record<string, unknown>> | null>(null); const submitting = useRef(false); const active = useRef(true);
    const save = useSaveCompetition(); const upload = useUploadCompetitionImage();
    const [images, setImages] = useState(() => Object.fromEntries(templates.map((field) => [field, record?.[field] ?? ''])));
    const uploadCount = useRef(0); const [pendingUploads, setPendingUploads] = useState(0);
    const trackUpload = async (file: File) => {
        if (submitting.current) throw new Error('正在保存，请稍后上传');
        uploadCount.current += 1; setPendingUploads(uploadCount.current);
        try { return await upload.mutateAsync(file); }
        finally { uploadCount.current -= 1; if (active.current) setPendingUploads(uploadCount.current); }
    }; const { closeCurrentAndGo } = usePageTabs();
    const [error, setError] = useState(''); const [conflict, setConflict] = useState(false); const [pending, setPending] = useState(false);
    useEffect(() => { active.current = true; return () => { active.current = false; }; }, []);
    const submit = async () => {
        if (submitting.current || uploadCount.current > 0) return;
        submitting.current = true; setPending(true); setError(''); setConflict(false);
        try {
            const values = await api.current?.validate(); if (!values || !active.current) return;
            const name = text(values.name);
            if (!name) throw new Error('请输入赛事名称');
            const description = typeof values.description === 'string' && values.description !== '' ? values.description : null;
            if (description && new TextEncoder().encode(description).length > 65535) throw new Error('说明内容过长');
            const parseTime = (value: unknown) => {
                if (!value) return null;
                const parsed = dayjs(value as string | Date); if (!parsed.isValid()) throw new Error('计划时间无效');
                return parsed.format(DATE_TIME_FORMAT);
            };
            const times = {} as Pick<CqtCompetitionBody, 'registrationStart' | 'registrationEnd' | 'provinceReviewStart' | 'provinceReviewEnd' | 'nationalReviewStart' | 'nationalReviewEnd' | 'resultPublishTime'>;
            for (const { field, start, end } of windows) {
                const range = values[field];
                if (range && (!Array.isArray(range) || range.length !== 2 || !range[0] || !range[1])) throw new Error('请选择完整的时间范围');
                if (!range && original && Boolean(original[start]) !== Boolean(original[end])) {
                    times[start] = parseTime(original[start]); times[end] = parseTime(original[end]);
                    continue;
                }
                times[start] = Array.isArray(range) ? parseTime(range[0]) : null;
                times[end] = Array.isArray(range) ? parseTime(range[1]) : null;
                if (times[start] && times[end] && times[start] > times[end]) throw new Error('开始时间不能晚于对应结束时间');
            }
            times.resultPublishTime = parseTime(values.resultPublishTime);
            const body: CqtCompetitionBody = { name, edition: original?.edition ?? null, year: original?.year ?? null, description,
                nationalEntryMode: original?.nationalEntryMode ?? values.nationalEntryMode as CqtCompetitionMode, ...times,
                provinceCertificateTemplate: templateUrl(values.provinceCertificateTemplate), nationalCertificateTemplate: templateUrl(values.nationalCertificateTemplate), teacherOrgCertificateTemplate: templateUrl(values.teacherOrgCertificateTemplate) };
            await save.mutateAsync(original ? { id: original.id, body, expectedVersion: original.version } : { body });
            if (active.current) { Toast.success('赛事已保存'); closeCurrentAndGo('/cqt/competitions', true); }
        } catch (cause) {
            if (active.current) { setConflict(cause instanceof ApiError && cause.status === 409); setError(cause instanceof ApiError && cause.code === -1 ? '保存结果未知，请先核查赛事列表再决定是否重试，避免重复创建。' : cause instanceof Error ? cause.message : '请检查表单后重试'); }
        } finally { submitting.current = false; if (active.current) setPending(false); }
    };
    return <PageContainer title={original ? '编辑赛事' : '新增赛事'} extra={<Space><Button disabled={pending} onClick={() => closeCurrentAndGo('/cqt/competitions')}>取消</Button><Button theme="solid" loading={pending} disabled={pendingUploads > 0} onClick={() => void submit()}>保存</Button></Space>}>
        {error && <Banner type="danger" description={error} />}
        {conflict && <Banner type="warning" description={<>已保留当前输入。请核对最新记录后重新编辑；重新加载会丢弃当前修改。{onReload && <Button disabled={pending} onClick={onReload}>重新加载最新记录</Button>}</>} />}
        <Banner type="info" description="新赛事保存为草稿并创建五个默认组别。评审和公布时间仅为计划，不自动开关评审阶段或发布成绩；证书模板仅配置，不生成证书。" />
        {original && <Typography.Paragraph>赛事编号：{original.id} / 旧编号：{original.legacyId ?? '无'} / 版本：{original.version}</Typography.Paragraph>}
        <Form<Record<string, unknown>> initValues={initial} disabled={pending} labelPosition="top" getFormApi={(value) => { api.current = value; }}>
            <Typography.Title heading={5}>基本信息</Typography.Title>
            <Form.Input field="name" label="赛事名称" maxLength={300} rules={[{ required: true, whitespace: true, message: '请输入赛事名称' }]} />
            <Form.RadioGroup field="nationalEntryMode" label="省国赛模式（创建后固定）" disabled={!!original || pending} options={[{ value: 'SHARED', label: '共用作品' }, { value: 'SEPARATE', label: '分别报名' }]} rules={required} />
            <Form.TextArea field="description" label="说明" autosize={{ minRows: 4, maxRows: 10 }} />
            <Typography.Title heading={5}>时间安排</Typography.Title>
            {windows.map(({ field, label }) => <Form.DatePicker key={field} field={field} label={label} type="dateTimeRange" showClear style={{ width: '100%' }} />)}
            <Form.DatePicker field="resultPublishTime" label="成绩公布（计划）" type="dateTime" showClear style={{ width: '100%' }} />
            <Typography.Title heading={5}>证书模板</Typography.Title>
            {templates.map((field, index) => <div key={field} style={{ margin: '16px 0' }}>
                <Typography.Paragraph>{['省赛证书模板', '国赛证书模板', '教师/组织证书模板'][index]}</Typography.Paragraph>
                <Form.Input field={field} noLabel style={{ display: 'none' }} />
                <SingleImageUpload value={images[field] ?? ''} disabled={pending} upload={trackUpload} onChange={(value) => { if (!active.current) return; setImages((previous) => ({ ...previous, [field]: value })); api.current?.setValue(field, value); }} />
            </div>)}
        </Form>
    </PageContainer>;
}
