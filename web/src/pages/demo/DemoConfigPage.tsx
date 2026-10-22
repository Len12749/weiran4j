import { Button, Spin, Toast, Typography } from '@douyinfe/semi-ui';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { FileUpload } from '@/components/FileUpload';
import { NavListItem, NavListPanel } from '@/components/NavListPanel';
import { PageContainer } from '@/components/PageContainer';
import { SystemConfigGroupForm } from '@/components/SystemConfigGroupForm';
import { useConfigAll, useSaveConfig } from '@/hooks/queries/configs';
import { usePermission } from '@/hooks/usePermission';
import type { ConfigView } from '@/types/api';
import './demo.css';

const DEMO_PREFIX = 'system.demo.';
const MULTI_FILE_KEY = 'system.demo.images';

function readFiles(value: string): string[] {
    try {
        const parsed: unknown = JSON.parse(value);
        return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : [];
    } catch {
        return [];
    }
}

function DemoMultiFileField({ record, canEdit }: { record: ConfigView; canEdit: boolean }) {
    const [files, setFiles] = useState(() => readFiles(record.configValue));
    const [uploading, setUploading] = useState(false);
    const save = useSaveConfig();

    const saveFiles = async () => {
        if (uploading) return;
        await save.mutateAsync({
            id: record.id,
            body: {
                configKey: record.configKey,
                configValue: JSON.stringify(files),
                configType: record.configType,
                groupKey: record.groupKey,
                fieldType: record.fieldType,
                fieldOptions: record.fieldOptions,
                required: record.required,
                description: record.description,
            },
        });
        Toast.success('多文件配置已保存');
    };

    return (
        <section style={{ marginTop: 24 }}>
            <strong>多文件上传 · FileUpload</strong>
            <p className="demo-page__hint">选择或拖放后立即上传到 system 资源库；拖动文件或用箭头调整顺序。图片可缩放预览，其他文件在浏览器打开或下载。保存后 URL 数组写入 sys_config。</p>
            <FileUpload multiple value={files} onChange={setFiles} disabled={!canEdit} onUploadingChange={setUploading} />
            {canEdit && <Button style={{ marginTop: 12 }} type="primary" theme="solid" loading={save.isPending} disabled={uploading} onClick={() => void saveFiles().catch(() => undefined)}>保存多文件</Button>}
            <p className="demo-page__hint">移除文件后请保存，以清除配置引用；未引用的文件仍在 <Link to="/system/media-library">资源库</Link> 中，可在那里永久删除。</p>
        </section>
    );
}

export default function DemoConfigPage() {
    const query = useConfigAll();
    const { hasPermission } = usePermission();
    const records = (query.data ?? []).filter((record) => record.configKey.startsWith(DEMO_PREFIX));
    const multiFile = records.find((record) => record.configKey === MULTI_FILE_KEY);
    const fields = records.filter((record) => record.configKey !== MULTI_FILE_KEY);
    const canEdit = hasPermission('system:config:update');

    return (
        <div className="config-groups-layout">
            <div className="config-groups-layout__master">
                <NavListPanel
                    title="配置分组"
                    dataSource={[{ key: 'demo', label: '组件演示' }]}
                    renderItem={(group) => <NavListItem key={group.key} active primary={group.label} secondary={group.key} />}
                />
            </div>
            <PageContainer title="组件演示配置" extra={<span className="demo-page__hint">local/test 示例数据，使用真实 system 配置和媒体接口</span>}>
                {query.isPending ? <Spin /> : query.isError ? (
                    <Typography.Text type="danger">配置读取失败；请确认账号具有系统配置查看权限。</Typography.Text>
                ) : fields.length === 0 || !multiFile ? (
                    <Typography.Text type="tertiary">演示配置尚未播种；请以 local/test profile 启动后端并应用 Flyway 迁移。</Typography.Text>
                ) : (
                    <>
                        <SystemConfigGroupForm
                            records={fields}
                            canEdit={canEdit}
                            showSettings={false}
                            onEdit={() => undefined}
                        />
                        <DemoMultiFileField key={`${multiFile.id}:${multiFile.configValue}`} record={multiFile} canEdit={canEdit} />
                        <details style={{ marginTop: 24 }}>
                            <summary>查看已保存数据（从 sys_config 读取）</summary>
                            <pre style={{ overflow: 'auto' }}>{JSON.stringify(records.map((record) => ({ key: record.configKey, value: record.configValue })), null, 2)}</pre>
                        </details>
                    </>
                )}
            </PageContainer>
        </div>
    );
}
