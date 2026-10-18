import { Banner, Button, Empty, Form, Space, Spin, Toast, Typography, Upload } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { PageContainer } from '@/components/PageContainer';
import { SingleImageUpload } from '@/components/SingleImageUpload';
import { usePageTabs } from '@/hooks/usePageTabs';
import { useContentDetail, useSaveContent, useUploadContent, type ContentBody, type ContentResource, type ContentView } from '@/hooks/queries/cqt-content';
import type { CqtShowcaseCategory, CqtResourceCategory, SiteConfigImageUpload } from '@/types/api';
import { contentConfig } from './ContentConfig';
import { SiteConfigRichEditor } from './SiteConfigRichEditor';

type Fields = { name: string; title: string; image: string; category: string; author: string; teacher: string; school: string; description: string;
    content: string; showOnHome: boolean; enabled: boolean; sort: number; attachmentUrl: string; attachmentName: string; videoUrl: string; fileUrl: string; fileName: string };

function ContentForm({ resource, record }: { resource: ContentResource; record?: ContentView }) {
    const config = contentConfig[resource];
    const { closeCurrentAndGo } = usePageTabs();
    const api = useRef<FormApi<Fields> | null>(null);
    const submitting = useRef(false);
    const save = useSaveContent(resource);
    const images = useUploadContent(resource, 'images');
    const files = useUploadContent(resource, 'files');
    const [values, setValues] = useState<Fields>({ name: '', title: '', image: '', category: config.categories[0]?.value ?? '', author: '', teacher: '', school: '', description: '',
        content: '', showOnHome: true, enabled: true, sort: 0, attachmentUrl: '', attachmentName: '', videoUrl: '', fileUrl: '', fileName: '', ...record });
    const [pendingUploads, setPendingUploads] = useState(0);
    const uploadCount = useRef(0);
    const trackUpload = async (kind: 'images' | 'files', file: File): Promise<SiteConfigImageUpload> => {
        uploadCount.current += 1;
        setPendingUploads(uploadCount.current);
        try { return await (kind === 'images' ? images : files).mutateAsync(file); }
        finally { uploadCount.current -= 1; setPendingUploads(uploadCount.current); }
    };
    const busy = save.isPending || pendingUploads > 0;
    const change = (field: keyof Fields, value: string) => { setValues((previous) => ({ ...previous, [field]: value })); api.current?.setValue(field, value); };
    const [error, setError] = useState('');
    const cancel = () => closeCurrentAndGo(`/cqt/${resource}`);
    const submit = async () => {
        if (submitting.current || busy || uploadCount.current > 0) return;
        submitting.current = true;
        try {
            const valid = await api.current?.validate();
            if (!valid) return;
            if (resource === 'experts' && !values.content.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim() && !values.content.includes('<img')) {
                setError('请填写人物介绍'); return;
            }
            const common = { content: values.content, enabled: valid.enabled, sort: valid.sort ?? 0 };
            let body: ContentBody;
            if (resource === 'experts') body = { ...common, name: valid.name.trim(), image: values.image, showOnHome: valid.showOnHome };
            else if (resource === 'showcases') body = { ...common, title: valid.title.trim(), image: values.image, category: valid.category as CqtShowcaseCategory,
                author: valid.author ?? '', teacher: valid.teacher ?? '', school: valid.school ?? '', description: valid.description ?? '', attachmentUrl: values.attachmentUrl, attachmentName: values.attachmentName, videoUrl: values.videoUrl };
            else body = { ...common, title: valid.title.trim(), category: valid.category as CqtResourceCategory, fileUrl: values.fileUrl, fileName: values.fileName };
            await save.mutateAsync({ id: record?.id, body });
            Toast.success('已保存');
            closeCurrentAndGo(`/cqt/${resource}`, true);
        } finally { submitting.current = false; }
    };
    const uploadField = (label: string, urlField: 'fileUrl' | 'attachmentUrl' | 'videoUrl', nameField?: 'fileName' | 'attachmentName') => <div style={{ margin: '16px 0' }}>
        <Typography.Text strong>{label}</Typography.Text>
        <div style={{ marginTop: 8 }}><Space>
            {values[urlField] ? <><Typography.Text ellipsis={{ showTooltip: true }} style={{ maxWidth: 480 }}>{nameField ? values[nameField] : values[urlField].split('/').pop()}</Typography.Text>
                <Button disabled={busy} onClick={() => { change(urlField, ''); if (nameField) change(nameField, ''); }}>删除</Button></> :
                <Upload action="" fileList={[]} showUploadList={false} multiple={false} limit={1} disabled={busy} accept={urlField === 'videoUrl' ? 'video/*' : undefined}
                    customRequest={async ({ fileInstance, onSuccess, onError }) => {
                        try { const result = await trackUpload('files', fileInstance); change(urlField, result.url); if (nameField) change(nameField, result.name); onSuccess(result); }
                        catch { onError({ status: 500 }); setError(`${label}上传失败，请重试`); }
                    }}><Button loading={files.isPending} disabled={busy}>上传{label}</Button></Upload>}
        </Space></div>
    </div>;
    return <PageContainer title={`${record ? '编辑' : '新增'}${config.name}`} extra={<Space><Button disabled={busy} onClick={cancel}>取消</Button><Button theme="solid" loading={save.isPending} disabled={busy} onClick={() => void submit().catch(() => undefined)}>保存</Button></Space>}>
        {error && <Banner type="danger" description={error} />}
        <div inert={busy}><Form<Fields> disabled={busy} initValues={values} getFormApi={(formApi) => { api.current = formApi; }} labelPosition="top">
            {resource === 'experts' ? <Form.Input field="name" label="姓名" maxLength={255} rules={[{ required: true, whitespace: true, message: '请输入姓名' }]} /> : <Form.Input field="title" label="标题" maxLength={255} rules={[{ required: true, whitespace: true, message: '请输入标题' }]} />}
            {resource !== 'resources' && <><Form.Input field="image" label="首图" style={{ display: 'none' }} rules={[{ required: true, message: '请上传首图' }]} />
                <SingleImageUpload value={values.image} onChange={(value) => change('image', value)} upload={(file) => trackUpload('images', file)} /></>}
            {config.categories.length > 0 && <Form.Select field="category" label="分类" optionList={config.categories} rules={[{ required: true, message: '请选择分类' }]} style={{ width: '100%' }} />}
            {resource === 'showcases' && <><Form.Input field="author" label="作者" maxLength={255} /><Form.Input field="teacher" label="指导教师" maxLength={255} /><Form.Input field="school" label="学校" maxLength={255} /><Form.TextArea field="description" label="简介" />
                {uploadField('附件', 'attachmentUrl', 'attachmentName')}{uploadField('视频', 'videoUrl')}</>}
            {resource === 'resources' && <><Form.Input field="fileUrl" label="资料文件" style={{ display: 'none' }} rules={[{ required: true, message: '请上传资料文件' }]} />
                <Form.Input field="fileName" label="文件名称" style={{ display: 'none' }} rules={[{ required: true, message: '文件名称不能为空' }]} />{uploadField('资料文件', 'fileUrl', 'fileName')}</>}
            {resource === 'experts' && <Form.Switch field="showOnHome" label="首页展示" />}
            <Form.Switch field="enabled" label="启用" /><Form.InputNumber field="sort" label="排序" min={0} precision={0} style={{ width: '100%' }} />
            <SiteConfigRichEditor label={resource === 'experts' ? '人物介绍（必填）' : '内容'} value={values.content} uploading={pendingUploads > 0} onUploadImage={(file) => trackUpload('images', file)} onChange={(value) => { change('content', value); setError(''); }} />
        </Form></div>
    </PageContainer>;
}

export function ContentCreatePage({ resource }: { resource: ContentResource }) { return <ContentForm resource={resource} />; }
export function ContentEditPage({ resource }: { resource: ContentResource }) {
    const { id: raw } = useParams();
    const id = Number(raw);
    const valid = Number.isSafeInteger(id) && id > 0;
    const query = useContentDetail(resource, valid ? id : undefined);
    const { closeCurrentAndGo } = usePageTabs();
    if (!valid || query.isError) return <PageContainer><Empty title="无法加载记录" description={query.error?.message ?? '记录编号无效'}><Button onClick={() => closeCurrentAndGo(`/cqt/${resource}`)}>返回列表</Button></Empty></PageContainer>;
    if (!query.data) return <Spin />;
    return <ContentForm key={`${resource}-${id}`} resource={resource} record={query.data} />;
}
