import { useState } from 'react';
import { NavListItem, NavListPanel } from '@/components/NavListPanel';
import { PageContainer } from '@/components/PageContainer';
import { SystemConfigGroupForm } from '@/components/SystemConfigGroupForm';
import type { ConfigView } from '@/types/api';
import './demo.css';

const CREATED_AT = '2026-10-21 00:00:00';

const EXAMPLES: ConfigView[] = [
    { id: 1, configKey: 'system.general.demo_input', configValue: 'Weiran Admin', configType: 'string', groupKey: 'general', fieldType: 'input', fieldOptions: null, required: true, description: '单行输入 · input', isBuiltin: false, createdAt: CREATED_AT, updatedAt: CREATED_AT },
    { id: 2, configKey: 'system.general.demo_textarea', configValue: '多行文本示例\n可以输入较长的说明。', configType: 'string', groupKey: 'general', fieldType: 'textarea', fieldOptions: null, required: false, description: '多行文本 · textarea', isBuiltin: false, createdAt: CREATED_AT, updatedAt: CREATED_AT },
    { id: 3, configKey: 'system.general.demo_number', configValue: '42', configType: 'number', groupKey: 'general', fieldType: 'number', fieldOptions: null, required: false, description: '数字 · number', isBuiltin: false, createdAt: CREATED_AT, updatedAt: CREATED_AT },
    { id: 4, configKey: 'system.general.demo_switch', configValue: 'true', configType: 'boolean', groupKey: 'general', fieldType: 'switch', fieldOptions: null, required: false, description: '开关 · switch', isBuiltin: false, createdAt: CREATED_AT, updatedAt: CREATED_AT },
    { id: 5, configKey: 'system.general.demo_select', configValue: 'local', configType: 'string', groupKey: 'general', fieldType: 'select', fieldOptions: '[{"label":"本地","value":"local"},{"label":"阿里云","value":"aliyun"}]', required: false, description: '下拉选择 · select', isBuiltin: false, createdAt: CREATED_AT, updatedAt: CREATED_AT },
    { id: 6, configKey: 'system.general.demo_json', configValue: '{\n  "enabled": true,\n  "limit": 10\n}', configType: 'json', groupKey: 'general', fieldType: 'json', fieldOptions: null, required: false, description: 'JSON · json', isBuiltin: false, createdAt: CREATED_AT, updatedAt: CREATED_AT },
    { id: 7, configKey: 'system.general.demo_image', configValue: '', configType: 'string', groupKey: 'general', fieldType: 'image', fieldOptions: null, required: false, description: '单图上传 · image', isBuiltin: false, createdAt: CREATED_AT, updatedAt: CREATED_AT },
    { id: 8, configKey: 'system.general.demo_richtext', configValue: '<p><strong>富文本示例</strong>：支持标题、加粗和列表。</p>', configType: 'string', groupKey: 'general', fieldType: 'richtext', fieldOptions: null, required: false, description: '富文本 · richtext', isBuiltin: false, createdAt: CREATED_AT, updatedAt: CREATED_AT },
];

function localImagePreview(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
            if (typeof reader.result === 'string') resolve(reader.result);
            else reject(new Error('图片读取失败'));
        };
        reader.onerror = () => reject(new Error('图片读取失败'));
        reader.readAsDataURL(file);
    });
}

export default function DemoConfigPage() {
    const [records, setRecords] = useState(EXAMPLES);

    return (
        <div className="config-groups-layout">
            <div className="config-groups-layout__master">
                <NavListPanel
                    title="配置分组"
                    dataSource={[{ key: 'general', label: '通用设置' }]}
                    renderItem={(group) => <NavListItem key={group.key} active primary={group.label} secondary={group.key} />}
                />
            </div>
            <PageContainer title="通用设置" extra={<span className="demo-page__hint">八种字段各一条；修改与保存仅影响当前页面</span>}>
                <SystemConfigGroupForm
                    records={records}
                    canEdit
                    showSettings={false}
                    onEdit={() => undefined}
                    onUploadImage={localImagePreview}
                    onSave={(values) => setRecords((current) => current.map((record, index) => ({ ...record, configValue: values[index]?.configValue ?? record.configValue })))}
                />
            </PageContainer>
        </div>
    );
}
