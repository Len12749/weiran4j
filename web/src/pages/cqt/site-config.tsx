import { Button, Form, Spin, Tabs, Toast, Typography } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useRef } from 'react';
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

function ConfigField({ item }: { item: SiteConfigAdminItem }) {
    return (
        <Form.TextArea
            field={item.key}
            label={item.label}
            rows={2}
            autosize={{ minRows: 2, maxRows: 6 }}
            {...(item.key === 'gongzhonghao' || item.key === 'shouyeimage'
                ? { placeholder: '请输入图片路径或 URL' }
                : {})}
        />
    );
}

function valuesFromItems(items: SiteConfigAdminItem[]): ConfigFormValues {
    return Object.fromEntries(items.map((item) => [item.key, item.contents ?? '']));
}

export default function CqtSiteConfigPage() {
    const formApi = useRef<FormApi<ConfigFormValues> | null>(null);
    const { data, isFetching, isError, refetch } = useCqtSiteConfig();
    const save = useSaveCqtSiteConfig();
    const uploadImage = useUploadCqtSiteConfigImage();

    const submit = async () => {
        const values = await formApi.current?.validate();
        if (!values || !data) return;
        await save.mutateAsync({
            items: data.items.map((item) => ({
                key: item.key,
                contents:
                    item.contents === null && (values[item.key] === '' || values[item.key] === undefined)
                        ? null
                        : (values[item.key] ?? ''),
            })),
        });
        Toast.success('站点配置已保存');
    };

    return (
        <PageContainer title="常青藤站点配置">
            <Typography.Paragraph type="tertiary">
                介绍和协议内容支持富文本及图片上传，HTML 原样保存并供前台富文本组件展示；其它文本配置按原有规则处理。
            </Typography.Paragraph>
            {isFetching && !data ? (
                <Spin />
            ) : isError && !data ? (
                <div>
                    <Typography.Text type="danger">站点配置加载失败</Typography.Text>
                    <Button onClick={() => void refetch()}>重试</Button>
                </div>
            ) : (
                <Form<ConfigFormValues>
                    key={data?.items.map((item) => `${item.key}:${item.contents ?? ''}`).join('|') ?? 'empty'}
                    getFormApi={(api) => {
                        formApi.current = api;
                    }}
                    initValues={data ? valuesFromItems(data.items) : {}}
                    labelPosition="top"
                >
                    <Tabs type="line" keepDOM>
                        {CONFIG_GROUPS.map((group) => (
                            <Tabs.TabPane key={group.key} itemKey={group.key} tab={group.title}>
                                <section className="site-config-tab-panel">
                                    {group.keys.map((key) => {
                                        const item = data?.items.find((config) => config.key === key);
                                        if (!item) return null;
                                        if (RICH_TEXT_KEYS.has(item.key)) {
                                            return (
                                                <SiteConfigRichEditor
                                                    key={item.key}
                                                    label={item.label}
                                                    value={item.contents ?? ''}
                                                    uploading={uploadImage.isPending}
                                                    onUploadImage={(file) => uploadImage.mutateAsync(file)}
                                                    onChange={(value) => formApi.current?.setValue(item.key, value)}
                                                />
                                            );
                                        }
                                        return <ConfigField key={item.key} item={item} />;
                                    })}
                                </section>
                            </Tabs.TabPane>
                        ))}
                    </Tabs>
                    <Permission code="cqt:site-config:update">
                        <Button
                            type="primary"
                            theme="solid"
                            loading={save.isPending}
                            disabled={isFetching}
                            onClick={() => void submit().catch(() => undefined)}
                        >
                            保存配置
                        </Button>
                    </Permission>
                </Form>
            )}
        </PageContainer>
    );
}
