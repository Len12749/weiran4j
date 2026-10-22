import { Button, Form, Space, Toast } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useRef, useState } from 'react';
import { FileUpload } from '@/components/FileUpload';
import { PageContainer } from '@/components/PageContainer';
import { useCreateCqtNews, useUpdateCqtNews, useUploadCqtNewsImage } from '@/hooks/queries/cqt-news';
import type { CqtNewsSaveRequest, CqtNewsView } from '@/types/api';
import { SiteConfigRichEditor } from './SiteConfigRichEditor';

export type NewsTarget = { mode: 'create' } | { mode: 'edit'; record: CqtNewsView };

export function NewsForm({ target, onClose, onSaved }: { target: NewsTarget; onClose: () => void; onSaved: () => void }) {
    const api = useRef<FormApi<CqtNewsSaveRequest> | null>(null);
    const [content, setContent] = useState(target.mode === 'edit' ? target.record.content : '');
    const [image, setImage] = useState(target.mode === 'edit' ? target.record.image : '');
    const create = useCreateCqtNews();
    const update = useUpdateCqtNews();
    const uploadImage = useUploadCqtNewsImage();
    const [uploading, setUploading] = useState(false);
    const isEdit = target.mode === 'edit';

    const submit = async () => {
        if (uploading || uploadImage.isPending) return;
        const values = await api.current?.validate();
        if (!values) return;
        const body = { ...values, image, content };
        if (isEdit) {
            await update.mutateAsync({ id: target.record.id, body });
        } else {
            await create.mutateAsync(body);
        }
        Toast.success(isEdit ? '动态已保存' : '动态已创建');
        onSaved();
    };

    const form = (
        <Form<CqtNewsSaveRequest>
            getFormApi={(formApi) => {
                api.current = formApi;
            }}
            initValues={
                isEdit
                    ? target.record
                    : { title: '', image: '', showOnHome: false, sort: 0, content: '' }
            }
            labelPosition="top"
        >
            <Form.Input field="title" label="标题" maxLength={255} rules={[{ required: true, message: '请输入标题' }]} />
            <Form.Input field="image" label="首图" rules={[{ required: true, message: '请上传首图' }]} style={{ display: 'none' }} />
            <FileUpload
                value={image}
                allowedExtensions={['.jpg', '.jpeg', '.png', '.gif']}
                onUpload={(file) => uploadImage.mutateAsync(file)}
                onUploadingChange={setUploading}
                onChange={(value) => {
                    setImage(value);
                    api.current?.setValue('image', value);
                }}
            />
            <Form.Switch field="showOnHome" label="首页展示" />
            <Form.InputNumber field="sort" label="排序" min={0} style={{ width: '100%' }} />
            <SiteConfigRichEditor
                label="动态内容"
                value={content}
                uploading={uploadImage.isPending}
                onUploadImage={(file) => uploadImage.mutateAsync(file)}
                onChange={(value) => {
                    setContent(value);
                    api.current?.setValue('content', value);
                }}
            />
        </Form>
    );
    const saving = create.isPending || update.isPending || uploading || uploadImage.isPending;
    const save = () => void submit().catch(() => undefined);
    return (
        <PageContainer
            title={isEdit ? '编辑大赛动态' : '新增大赛动态'}
            extra={
                <Space>
                    <Button onClick={onClose} disabled={saving}>取消</Button>
                    <Button theme="solid" onClick={save} loading={saving}>保存</Button>
                </Space>
            }
        >
            {form}
        </PageContainer>
    );
}
