import { Button, Form, Spin, Tabs, Toast, Typography } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useEffect, useRef, useState } from 'react';
import { SingleImageUpload } from '@/components/SingleImageUpload';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import {
    useCqtSiteConfig,
    useSaveCqtSiteConfig,
    useUploadCqtSiteConfigImage,
} from '@/hooks/queries/cqt-site-config';
import type { SiteConfigAdminItem } from '@/types/api';
import { SiteConfigRichEditor } from './SiteConfigRichEditor';

type ConfigFormValues = Record<string, string>;

const CONFIG_GROUPS: Array<{ key: string; title: string; keys: string[] }> = [
    { key: 'introduction', title: '站点介绍', keys: ['guanyuwomen', 'about_us', 'gongsijieshao', 'hezuohuoban', 'dasaijieshao'] },
    { key: 'agreements', title: '协议与服务', keys: ['user_agreement', 'yinsixieyi', 'mianzexieyi', 'shouhoufuwu', 'shangwuhezuo'] },
    { key: 'contact', title: '联系方式', keys: ['dizhi', 'shouji', 'weixin', 'youxiang', 'gongzuoshijian'] },
    {
        key: 'assets',
        title: '首页素材与证书',
        keys: [
            'gongzhonghao',
            'shouyeimage',
            'certificate_visibility',
            'sheng_certificate_visibility',
            'teacher_org_certificate_competition_id',
            'guo_certificate_visibility',
            'dasaizhangcheng',
        ],
    },
];

const RICH_TEXT_KEYS = new Set([
    'guanyuwomen',
    'user_agreement',
    'yinsixieyi',
    'about_us',
    'gongsijieshao',
    'hezuohuoban',
    'dasaijieshao',
    'mianzexieyi',
    'shouhoufuwu',
    'shangwuhezuo',
    'dasaizhangcheng',
]);

function valuesFromItems(items: SiteConfigAdminItem[]): ConfigFormValues {
    return Object.fromEntries(items.map((item) => [item.key, item.contents ?? '']));
}

function SiteConfigForm({ items }: { items: SiteConfigAdminItem[] }) {
    const [snapshot] = useState(items);
    const [values, setValues] = useState(() => valuesFromItems(items));
    const formApi = useRef<FormApi<ConfigFormValues> | null>(null);
    const save = useSaveCqtSiteConfig(); const uploadImage = useUploadCqtSiteConfigImage();
    const uploadCount = useRef(0); const submitting = useRef(false); const active = useRef(true);
    const [pendingUploads, setPendingUploads] = useState(0); const [saving, setSaving] = useState(false);
    useEffect(() => { active.current = true; return () => { active.current = false; }; }, []);
    const change = (key: string, value: string) => {
        if (!active.current) return;
        setValues((previous) => ({ ...previous, [key]: value })); formApi.current?.setValue(key, value);
    };
    const trackUpload = async (file: File) => {
        if (submitting.current) throw new Error('正在保存，请稍后上传');
        uploadCount.current += 1; setPendingUploads(uploadCount.current);
        try { return await uploadImage.mutateAsync(file); }
        finally { uploadCount.current -= 1; if (active.current) setPendingUploads(uploadCount.current); }
    };
    const submit = async () => {
        if (submitting.current || uploadCount.current > 0) return;
        submitting.current = true; setSaving(true);
        try {
            const valid = await formApi.current?.validate(); if (!valid || !active.current) return;
            await save.mutateAsync({ items: snapshot.map((item) => ({ key: item.key, contents: item.contents === null && (valid[item.key] === '' || valid[item.key] === undefined) ? null : (valid[item.key] ?? '') })) });
            if (active.current) Toast.success('站点配置已保存');
        } finally { submitting.current = false; if (active.current) setSaving(false); }
    };
    return <Form<ConfigFormValues> getFormApi={(api) => { formApi.current = api; }} initValues={valuesFromItems(snapshot)} labelPosition="top" disabled={saving}>
        <div inert={saving}><Tabs type="line" keepDOM>
            {CONFIG_GROUPS.map((group) => <Tabs.TabPane key={group.key} itemKey={group.key} tab={group.title}><section className="site-config-tab-panel">
                {group.keys.map((key) => {
                    const item = snapshot.find((config) => config.key === key); if (!item) return null;
                    if (RICH_TEXT_KEYS.has(key)) return <SiteConfigRichEditor key={key} label={item.label} value={values[key] ?? ''} uploading={pendingUploads > 0} onUploadImage={trackUpload} onChange={(value) => change(key, value)} />;
                    if (key === 'gongzhonghao' || key === 'shouyeimage') return <div key={key} style={{ margin: '16px 0' }}>
                        <Typography.Paragraph>{key === 'gongzhonghao' ? '公众号图片' : '首页图片'}</Typography.Paragraph>
                        <Form.Input field={key} noLabel style={{ display: 'none' }} />
                        <SingleImageUpload value={values[key] ?? ''} disabled={saving} upload={trackUpload} onChange={(value) => change(key, value)} />
                    </div>;
                    return <Form.TextArea key={key} field={key} label={item.label} rows={2} autosize={{ minRows: 2, maxRows: 6 }} />;
                })}
            </section></Tabs.TabPane>)}
        </Tabs></div>
        <Permission code="cqt:site-config:update"><Button type="primary" theme="solid" loading={saving} disabled={saving || pendingUploads > 0} onClick={() => void submit().catch(() => undefined)}>保存配置</Button></Permission>
    </Form>;
}

export default function CqtSiteConfigPage() {
    const { data, isFetching, isError, refetch } = useCqtSiteConfig();
    return <PageContainer title="常青藤站点配置">
        <Typography.Paragraph type="tertiary">介绍和协议内容支持富文本及图片上传，HTML原样保存；公众号和首页素材使用单图上传，其它文本配置按原有规则处理。</Typography.Paragraph>
        {isFetching && !data ? <Spin /> : isError && !data ? <div><Typography.Text type="danger">站点配置加载失败</Typography.Text><Button onClick={() => void refetch()}>重试</Button></div> : data ? <SiteConfigForm items={data.items} /> : null}
    </PageContainer>;
}
