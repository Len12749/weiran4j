import { Button, Form, Space, Tag, Toast } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { Settings } from 'lucide-react';
import { useMemo, useRef } from 'react';
import { ConfigImageUpload } from '@/components/ConfigImageUpload';
import { ConfigRichTextEditor } from '@/components/ConfigRichTextEditor';
import { useSaveConfig } from '@/hooks/queries/configs';
import type { ConfigSaveRequest, ConfigView } from '@/types/api';

export function parseConfigFieldOptions(value: string): Array<{ label: string; value: string }> {
    if (!value.trim()) return [];
    try {
        const options: unknown = JSON.parse(value);
        if (!Array.isArray(options)) return [];
        return options.filter((option: unknown): option is { label: string; value: string } => {
            if (typeof option !== 'object' || option === null) return false;
            const candidate = option as Record<string, unknown>;
            return typeof candidate.label === 'string' && typeof candidate.value === 'string';
        });
    } catch {
        return [];
    }
}

function toTextValue(value: unknown): string {
    return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' ? String(value) : '';
}

function toStringValue(value: unknown): string {
    return typeof value === 'string' ? value : '';
}

function validateField(record: ConfigView, value: unknown): string | null {
    const text = record.fieldType === 'switch' ? String(Boolean(value)) : toTextValue(value);
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
    if (record.fieldType === 'select' && text && !parseConfigFieldOptions(record.fieldOptions ?? '').some((option) => option.value === text)) {
        return '请选择有效选项';
    }
    return null;
}

export function SystemConfigGroupForm({
    records,
    canEdit,
    showSettings,
    onEdit,
    onSave,
    onUploadImage,
}: {
    records: ConfigView[];
    canEdit: boolean;
    showSettings: boolean;
    onEdit: (record: ConfigView) => void;
    onSave?: (values: ConfigSaveRequest[]) => Promise<void> | void;
    onUploadImage?: (file: File) => Promise<string>;
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
        const requests: ConfigSaveRequest[] = records.map((record) => {
            const value = values[String(record.id)];
            return {
                configKey: record.configKey,
                configValue: record.fieldType === 'switch' ? String(Boolean(value)) : toTextValue(value),
                configType: record.configType,
                groupKey: record.groupKey,
                fieldType: record.fieldType,
                fieldOptions: record.fieldOptions,
                required: record.required,
                description: record.description,
            };
        });
        if (onSave) {
            await onSave(requests);
        } else {
            await Promise.all(records.map((record, index) => save.mutateAsync({ id: record.id, body: requests[index]! })));
        }
        Toast.success(onSave ? '示例值已保存到当前页面' : '分组配置已保存');
    };

    return (
        <Form<Record<string, unknown>>
            style={{ width: '100%' }}
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
                        <div className="config-field-title">
                            {canEdit && showSettings && (
                                <Button
                                    className="config-field-title__edit"
                                    theme="borderless"
                                    size="small"
                                    icon={<Settings size={14} />}
                                    aria-label={`字段设置：${record.description || record.configKey}`}
                                    onClick={(event) => {
                                        event.preventDefault();
                                        event.stopPropagation();
                                        onEdit(record);
                                    }}
                                />
                            )}
                            <span>{record.description || '未命名配置项'}</span>
                            {record.isBuiltin && <Tag size="small" color="blue">内置</Tag>}
                        </div>
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
                                <Form.Select {...common} optionList={parseConfigFieldOptions(record.fieldOptions ?? '')} style={{ width: '100%' }} />
                            ) : record.fieldType === 'json' ? (
                                <Form.TextArea {...common} rows={5} maxLength={4096} />
                            ) : record.fieldType === 'image' ? (
                                <div>
                                    <Form.Input field={field} style={{ display: 'none' }} />
                                    <div style={{ marginBottom: 8 }}>{label}</div>
                                    <ConfigImageUpload
                                        key={`${record.id}:${record.configValue}`}
                                        value={toStringValue(initValues[field])}
                                        disabled={!canEdit}
                                        {...(onUploadImage ? { onUpload: onUploadImage } : {})}
                                        onChange={(url) => {
                                            api.current?.setValue(field, url);
                                        }}
                                    />
                                </div>
                            ) : record.fieldType === 'richtext' ? (
                                <div>
                                    <Form.Input field={field} style={{ display: 'none' }} />
                                    <div style={{ marginBottom: 8 }}>{label}</div>
                                    <ConfigRichTextEditor
                                        value={toStringValue(initValues[field])}
                                        disabled={!canEdit}
                                        onChange={(html) => {
                                            api.current?.setValue(field, html);
                                        }}
                                    />
                                </div>
                            ) : (
                                <Form.Input {...common} maxLength={4096} />
                            )}
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
