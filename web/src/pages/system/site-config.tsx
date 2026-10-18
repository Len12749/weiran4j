import { Button, Form, Spin, Tabs, Toast, Typography } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useRef } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import {
    useSaveSystemSiteConfig,
    useSystemSiteConfig,
    useUploadSystemImage,
} from '@/hooks/queries/system-media';
import type { SystemSiteConfigItem } from '@/types/api';
import { SiteConfigRichEditor } from './SiteConfigRichEditor';

type ConfigValues = Record<string, string>;

const GROUPS = [
    { key: 'introduction', title: '站点介绍', keys: ['site_introduction', 'about'] },
    { key: 'agreements', title: '协议', keys: ['user_agreement', 'privacy_policy', 'disclaimer'] },
    { key: 'contact', title: '联系方式', keys: ['contact_address', 'contact_phone', 'contact_email', 'working_hours'] },
    { key: 'assets', title: '首页素材', keys: ['homepage_image'] },
];

const RICH_TEXT_KEYS = new Set([
    'site_introduction',
    'user_agreement',
    'about',
    'privacy_policy',
    'disclaimer',
]);

function valuesFromItems(items: SystemSiteConfigItem[]): ConfigValues {
    return Object.fromEntries(items.map((item) => [item.key, item.contents ?? '']));
}

function ConfigField({ item }: { item: SystemSiteConfigItem }) {
    return (
        <Form.TextArea
            field={item.key}
            label={item.label}
            rows={2}
            autosize={{ minRows: 2, maxRows: 6 }}
            {...(['homepage_image'].includes(item.key) ? { placeholder: '请输入图片路径或 URL' } : {})}
        />
    );
}

export default function SystemSiteConfigPage() {
    const formApi = useRef<FormApi<ConfigValues> | null>(null);
    const { data, isFetching, isError, refetch } = useSystemSiteConfig();
    const save = useSaveSystemSiteConfig();
    const upload = useUploadSystemImage();

    const submit = async () => {
        const values = await formApi.current?.validate();
        if (!values || !data) return;
        await save.mutateAsync({
            items: data.items.map((item) => ({
                key: item.key,
                contents: item.contents === null &&
                        (values[item.key] === '' || values[item.key] === undefined || values[item.key] === '<p></p>')
                    ? null
                    : (values[item.key] ?? ''),
            })),
        });
        Toast.success('站点配置已保存');
    };

    return (
        <PageContainer title="站点配置">
            <Typography.Paragraph type="tertiary">
                介绍和协议内容支持富文本及图片上传，HTML 原样保存；其它文本配置按原有规则处理。
            </Typography.Paragraph>
            {isFetching && !data ? (
                <Spin />
            ) : isError && !data ? (
                <div>
                    <Typography.Text type="danger">站点配置加载失败</Typography.Text>
                    <Button onClick={() => void refetch()}>重试</Button>
                </div>
            ) : (
                <Form<ConfigValues>
                    key={data?.items.map((item) => `${item.key}:${item.contents ?? ''}`).join('|') ?? 'empty'}
                    getFormApi={(api) => {
                        formApi.current = api;
                    }}
                    initValues={data ? valuesFromItems(data.items) : {}}
                    labelPosition="top"
                >
                    <Tabs type="line" keepDOM>
                        {GROUPS.map((group) => (
                            <Tabs.TabPane key={group.key} itemKey={group.key} tab={group.title}>
                                <section className="site-config-tab-panel">
                                    {group.keys.map((key) => {
                                        const item = data?.items.find((config) => config.key === key);
                                        if (!item) return null;
                                        return RICH_TEXT_KEYS.has(item.key) ? (
                                            <SiteConfigRichEditor
                                                key={item.key}
                                                label={item.label}
                                                value={item.contents ?? ''}
                                                uploading={upload.isPending}
                                                onUploadImage={(file) => upload.mutateAsync(file)}
                                                onChange={(value) => formApi.current?.setValue(item.key, value)}
                                            />
                                        ) : (
                                            <ConfigField key={item.key} item={item} />
                                        );
                                    })}
                                </section>
                            </Tabs.TabPane>
                        ))}
                    </Tabs>
                    <Permission code="system:site-config:update">
                        <Button type="primary" theme="solid" loading={save.isPending} disabled={isFetching} onClick={() => void submit().catch(() => undefined)}>
                            保存配置
                        </Button>
                    </Permission>
                </Form>
            )}
        </PageContainer>
    );
}
