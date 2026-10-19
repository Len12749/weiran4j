import { Button, Form, Modal, Space, Tabs, Toast, Typography } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useMemo, useRef, useState } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { useConfigAll, useConfigGroups, useDeleteConfig, useSaveConfig } from '@/hooks/queries/configs';
import { usePermission } from '@/hooks/usePermission';
import type { ConfigFieldType, ConfigSaveRequest, ConfigView } from '@/types/api';

interface ConfigMetadataForm {
    configKey: string;
    configValue: string;
    groupKey: string;
    fieldType: ConfigFieldType;
    fieldOptions: string;
    required: boolean;
    description: string;
}

const FIELD_OPTIONS = [
    { label: '单行输入', value: 'input' },
    { label: '多行文本', value: 'textarea' },
    { label: '数字', value: 'number' },
    { label: '开关', value: 'switch' },
    { label: '下拉选择', value: 'select' },
    { label: 'JSON', value: 'json' },
];

function configType(fieldType: ConfigFieldType): ConfigSaveRequest['configType'] {
    if (fieldType === 'number') return 'number';
    if (fieldType === 'switch') return 'boolean';
    if (fieldType === 'json') return 'json';
    return 'string';
}

function parseOptions(value: string): Array<{ label: string; value: string }> {
    if (!value.trim()) return [];
    try {
        const options: unknown = JSON.parse(value);
        if (!Array.isArray(options)) return [];
        return options.filter(
            (option): option is { label: string; value: string } =>
                typeof option?.label === 'string' && typeof option?.value === 'string',
        );
    } catch {
        return [];
    }
}

function validateField(record: ConfigView, value: unknown): string | null {
    const text = record.fieldType === 'switch' ? String(Boolean(value)) : String(value ?? '');
    if (record.required && !text.trim()) return '此项必填';
    if (record.fieldType === 'number' && text.trim() && (text.trim() !== text || Number.isNaN(Number(text)))) {
        return '请输入合法数字';
    }
    if (record.fieldType === 'switch' && text !== 'true' && text !== 'false') return '请选择开或关';
    if (record.fieldType === 'json') {
        try {
            JSON.parse(text);
        } catch {
            return 'JSON 格式不正确';
        }
    }
    if (record.fieldType === 'select' && text && !parseOptions(record.fieldOptions ?? '').some((option) => option.value === text)) {
        return '请选择有效选项';
    }
    return null;
}

function ConfigMetadataModal({
    record,
    defaultGroup,
    groups,
    onClose,
}: {
    record: ConfigView | null;
    defaultGroup?: { key: string; label: string } | undefined;
    groups: Array<{ key: string; label: string }>;
    onClose: () => void;
}) {
    const api = useRef<FormApi<ConfigMetadataForm> | null>(null);
    const save = useSaveConfig();
    const isEdit = record !== null;

    const submit = async () => {
        const value = await api.current?.validate();
        if (!value) return;
        const type = configType(value.fieldType);
        const body: ConfigSaveRequest = {
            configKey: value.configKey.trim(),
            configValue: value.configValue ?? '',
            configType: type,
            groupKey: value.groupKey.trim(),
            fieldType: value.fieldType,
            fieldOptions: value.fieldType === 'select' ? value.fieldOptions.trim() || null : null,
            required: value.required,
            description: value.description.trim() || null,
        };
        await save.mutateAsync({ id: record?.id, body });
        Toast.success(isEdit ? '配置项已更新' : '配置项已创建');
        onClose();
    };

    return (
        <Modal
            visible
            title={isEdit ? '编辑配置项' : '新增配置项'}
            width={640}
            onCancel={onClose}
            onOk={() => submit().catch(() => undefined)}
            okButtonProps={{ loading: save.isPending }}
            maskClosable={false}
        >
            <Form<ConfigMetadataForm>
                getFormApi={(formApi) => {
                    api.current = formApi;
                }}
                initValues={{
                    configKey: record?.configKey ?? 'system.',
                    configValue: record?.configValue ?? '',
                    groupKey: record?.groupKey ?? defaultGroup?.key ?? 'general',
                    fieldType: record?.fieldType ?? 'input',
                    fieldOptions: record?.fieldOptions ?? '',
                    required: record?.required ?? false,
                    description: record?.description ?? '',
                }}
                labelPosition="left"
                labelWidth={110}
            >
                <Form.Input
                    field="configKey"
                    label="配置键"
                    maxLength={128}
                    disabled={record?.isBuiltin ?? false}
                    rules={[{ required: true, message: '请输入配置键' }, { pattern: /^system\.[\w.-]+$/, message: '配置键必须以 system. 开头' }]}
                />
                <Form.Select
                    field="groupKey"
                    label="内置分组"
                    optionList={groups.map((group) => ({ label: group.label, value: group.key }))}
                    style={{ width: '100%' }}
                    rules={[{ required: true, message: '请选择分组' }]}
                />
                <Form.Select field="fieldType" label="表单字段" optionList={FIELD_OPTIONS} style={{ width: '100%' }} />
                <Form.TextArea
                    field="fieldOptions"
                    label="下拉选项 JSON"
                    rows={3}
                    placeholder={'[{"label":"选项一","value":"one"}]'}
                    rules={[
                        {
                            validator: (_rule: unknown, value: unknown) => {
                                if (api.current?.getValue('fieldType') !== 'select' || !value) return true;
                                return parseOptions(String(value)).length > 0;
                            },
                            message: '请输入有效的选项数组 JSON',
                        },
                    ]}
                />
                <Form.Input
                    field="configValue"
                    label="初始值 / 当前值"
                    maxLength={4096}
                />
                <Form.Checkbox field="required" label="必填" />
                <Form.TextArea field="description" label="说明" rows={2} maxLength={256} />
            </Form>
        </Modal>
    );
}

function ConfigGroupPanel({
    records,
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: {
    records: ConfigView[];
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (record: ConfigView) => void;
    onDelete: (record: ConfigView) => void;
}) {
    const api = useRef<FormApi<Record<string, unknown>> | null>(null);
    const save = useSaveConfig();
    const initValues = useMemo(
        () => Object.fromEntries(records.map((record) => [String(record.id), record.fieldType === 'switch' ? record.configValue === 'true' : record.configValue])),
        [records],
    );

    const submit = async () => {
        const values = await api.current?.validate();
        if (!values) return;
        for (const record of records) {
            const raw = values[String(record.id)];
            const error = validateField(record, raw);
            if (error) {
                Toast.error(`${record.description || record.configKey}：${error}`);
                return;
            }
        }
        await Promise.all(
            records.map((record) => {
                const value = values[String(record.id)];
                const body: ConfigSaveRequest = {
                    configKey: record.configKey,
                    configValue: record.fieldType === 'switch' ? String(Boolean(value)) : String(value ?? ''),
                    configType: record.configType,
                    groupKey: record.groupKey,
                    fieldType: record.fieldType,
                    fieldOptions: record.fieldOptions,
                    required: record.required,
                    description: record.description,
                };
                return save.mutateAsync({ id: record.id, body });
            }),
        );
        Toast.success('分组配置已保存');
    };

    return (
        <Form<Record<string, unknown>>
            getFormApi={(formApi) => {
                api.current = formApi;
            }}
            initValues={initValues}
            labelPosition="top"
        >
            <Space vertical align="start" style={{ width: '100%' }} spacing={20}>
                {records.map((record) => {
                    const field = String(record.id);
                    const label = (
                        <Space>
                            <span>{record.description || record.configKey}</span>
                            <Typography.Text type="tertiary" size="small">{record.configKey}</Typography.Text>
                            {record.isBuiltin && <Typography.Text type="tertiary" size="small">内置</Typography.Text>}
                        </Space>
                    );
                    const common = {
                        field,
                        label,
                        disabled: !canEdit,
                        rules: record.required ? [{ required: true, message: '此项必填' }] : [],
                    };
                    return (
                        <div key={record.id} style={{ width: '100%' }}>
                            {record.fieldType === 'textarea' ? (
                                <Form.TextArea {...common} rows={3} maxLength={4096} />
                            ) : record.fieldType === 'number' ? (
                                <Form.Input {...common} type="number" />
                            ) : record.fieldType === 'switch' ? (
                                <Form.Switch {...common} />
                            ) : record.fieldType === 'select' ? (
                                <Form.Select {...common} optionList={parseOptions(record.fieldOptions ?? '')} style={{ width: '100%' }} />
                            ) : record.fieldType === 'json' ? (
                                <Form.TextArea {...common} rows={5} maxLength={4096} />
                            ) : (
                                <Form.Input {...common} maxLength={4096} />
                            )}
                            <Space style={{ marginTop: 4 }}>
                                {canEdit && <Button theme="borderless" size="small" onClick={() => onEdit(record)}>字段设置</Button>}
                                {canDelete && (
                                    <Button
                                        theme="borderless"
                                        type="danger"
                                        size="small"
                                        disabled={record.isBuiltin}
                                        onClick={() => onDelete(record)}
                                    >
                                        删除
                                    </Button>
                                )}
                            </Space>
                        </div>
                    );
                })}
                {canEdit && (
                    <Button type="primary" theme="solid" loading={save.isPending} onClick={() => void submit().catch(() => undefined)}>
                        保存本组
                    </Button>
                )}
            </Space>
        </Form>
    );
}

export default function ConfigsPage() {
    const { hasAnyPermission } = usePermission();
    const { data, isFetching, refetch } = useConfigAll();
    const { data: groupCatalog = [] } = useConfigGroups();
    const deleteConfig = useDeleteConfig();
    const [editing, setEditing] = useState<ConfigView | null | undefined>(undefined);
    const [activeGroup, setActiveGroup] = useState('general');
    const groups = useMemo(
        () => groupCatalog.map((group) => ({ ...group, records: (data ?? []).filter((config) => config.groupKey === group.key) })),
        [data, groupCatalog],
    );
    const canEdit = hasAnyPermission('system:config:update');
    const canDelete = hasAnyPermission('system:config:delete');

    const removeConfig = (record: ConfigView) => {
        Modal.confirm({
            title: '确定删除该配置项？',
            content: record.configKey,
            onOk: () => deleteConfig.mutateAsync(record.id).then(() => Toast.success('配置项已删除')),
        });
    };

    return (
        <PageContainer>
            <Space vertical align="start" style={{ width: '100%' }} spacing={16}>
                <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                    <Typography.Text type="tertiary">按分组维护系统参数；表单控件与校验由每个配置项的字段设置生成。</Typography.Text>
                    <Space>
                        <Button icon="🔄" aria-label="刷新配置" loading={isFetching} onClick={() => void refetch()} />
                        <Permission code="system:config:create">
                            <Button type="primary" theme="solid" onClick={() => setEditing(null)}>新增配置项</Button>
                        </Permission>
                    </Space>
                </Space>
                {groups.length === 0 ? (
                    <Typography.Text type="tertiary">尚未注册内置分组。</Typography.Text>
                ) : (
                    <Tabs type="line" keepDOM activeKey={activeGroup} onChange={(key) => setActiveGroup(String(key))}>
                        {groups.map((group) => (
                            <Tabs.TabPane key={group.key} itemKey={group.key} tab={group.label}>
                                <ConfigGroupPanel
                                    key={`${group.key}-${group.records.map((record) => record.id).join('-')}`}
                                    records={group.records}
                                    canEdit={canEdit}
                                    canDelete={canDelete}
                                    onEdit={(record) => setEditing(record)}
                                    onDelete={removeConfig}
                                />
                            </Tabs.TabPane>
                        ))}
                    </Tabs>
                )}
            </Space>
            {editing !== undefined && (
                <ConfigMetadataModal
                    record={editing}
                    defaultGroup={groupCatalog.find((group) => group.key === activeGroup) ?? groupCatalog[0]}
                    groups={groups}
                    onClose={() => setEditing(undefined)}
                />
            )}
        </PageContainer>
    );
}
