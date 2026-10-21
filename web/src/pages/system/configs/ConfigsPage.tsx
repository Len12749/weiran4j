import { Button, Dropdown, Empty, Form, Modal, Pagination, Space, Tag, Toast, Tooltip } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { Eye, EyeOff, MoreHorizontal, Pencil, Plus, RefreshCw, Settings, Trash2 } from 'lucide-react';
import { useMemo, useRef, useState } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { ConfigImageUpload } from '@/components/ConfigImageUpload';
import { ConfigRichTextEditor } from '@/components/ConfigRichTextEditor';
import { NavListItem, NavListPanel } from '@/components/NavListPanel';
import { Permission } from '@/components/Permission';
import {
    useConfigAll,
    useConfigGroups,
    useCreateConfigGroup,
    useDeleteConfig,
    useDeleteConfigGroup,
    useSaveConfig,
    useUpdateConfigGroup,
} from '@/hooks/queries/configs';
import { usePermission } from '@/hooks/usePermission';
import { useTableDefaults } from '@/hooks/useTableDefaults';
import type { ConfigFieldType, ConfigGroupView, ConfigSaveRequest, ConfigView } from '@/types/api';

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
    { label: '单图上传', value: 'image' },
    { label: '富文本', value: 'richtext' },
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
    if (record.fieldType === 'select' && text && !parseOptions(record.fieldOptions ?? '').some((option) => option.value === text)) {
        return '请选择有效选项';
    }
    return null;
}

function ConfigMetadataModal({
    record,
    defaultGroup,
    groups,
    canDelete,
    onDelete,
    onClose,
}: {
    record: ConfigView | null;
    defaultGroup?: { key: string; label: string } | undefined;
    groups: Array<{ key: string; label: string }>;
    canDelete: boolean;
    onDelete: (record: ConfigView) => void;
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
            footer={
                <div className="config-metadata-modal__footer">
                    {record ? (
                        <Button
                            type="danger"
                            theme="borderless"
                            disabled={!canDelete || record.isBuiltin}
                            onClick={() => { onClose(); onDelete(record); }}
                        >
                            删除配置项
                        </Button>
                    ) : <span />}
                    <Space>
                        <Button onClick={onClose}>取消</Button>
                        <Button type="primary" theme="solid" loading={save.isPending} onClick={() => submit().catch(() => undefined)}>
                            {isEdit ? '保存' : '新增'}
                        </Button>
                    </Space>
                </div>
            }
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
                    label="配置分组"
                    optionList={groups.map((group) => ({ label: group.label, value: group.key }))}
                    style={{ width: '100%' }}
                    rules={[{ required: true, message: '请选择分组' }]}
                />
                <Form.Select field="fieldType" label="表单字段" optionList={FIELD_OPTIONS} style={{ width: '100%' }} />
                <Form.TextArea
                    field="fieldOptions"
                    label="下拉选项 JSON（仅下拉选择使用）"
                    rows={3}
                    placeholder={'[{"label":"选项一","value":"one"}]'}
                    rules={[
                        {
                            validator: (_rule: unknown, value: unknown) => {
                                if (api.current?.getValue('fieldType') !== 'select' || !value) return true;
                                return parseOptions(typeof value === 'string' ? value : '').length > 0;
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

function ConfigGroupModal({
    record,
    onClose,
    onCreated,
}: {
    record: ConfigGroupView | null;
    onClose: () => void;
    onCreated: (key: string) => void;
}) {
    const api = useRef<FormApi<{ key: string; label: string }> | null>(null);
    const createGroup = useCreateConfigGroup();
    const updateGroup = useUpdateConfigGroup();
    const isEdit = record !== null;

    const submit = async () => {
        const value = await api.current?.validate();
        if (!value) return;
        const key = record?.key ?? value.key.trim();
        if (record) {
            await updateGroup.mutateAsync({ key, body: { label: value.label.trim() } });
            Toast.success('配置分组已更新');
        } else {
            await createGroup.mutateAsync({ key, label: value.label.trim() });
            Toast.success('配置分组已创建');
        }
        onCreated(key);
        onClose();
    };

    return (
        <Modal
            visible
            title={isEdit ? '编辑配置分组' : '新增配置分组'}
            okText={isEdit ? '保存' : '新增'}
            onCancel={onClose}
            onOk={() => void submit().catch(() => undefined)}
            okButtonProps={{ loading: createGroup.isPending || updateGroup.isPending }}
            maskClosable={false}
        >
            <Form<{ key: string; label: string }>
                getFormApi={(formApi) => {
                    api.current = formApi;
                }}
                initValues={{ key: record?.key ?? '', label: record?.label ?? '' }}
                labelPosition="left"
                labelWidth={100}
            >
                <Form.Input
                    field="key"
                    label="分组编码"
                    maxLength={64}
                    disabled={isEdit}
                    rules={[
                        { required: true, message: '请输入分组编码' },
                        { pattern: /^[a-z][a-z0-9_-]*$/, message: '以小写字母开头，只能包含小写字母、数字、下划线和连字符' },
                    ]}
                />
                <Form.Input field="label" label="分组名称" maxLength={64} rules={[{ required: true, message: '请输入分组名称' }]} />
            </Form>
        </Modal>
    );
}

function ConfigGroupPanel({
    records,
    canEdit,
    showSettings,
    onEdit,
}: {
    records: ConfigView[];
    canEdit: boolean;
    showSettings: boolean;
    onEdit: (record: ConfigView) => void;
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
                    configValue: record.fieldType === 'switch' ? String(Boolean(value)) : toTextValue(value),
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
                                <Form.Select {...common} optionList={parseOptions(record.fieldOptions ?? '')} style={{ width: '100%' }} />
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

export default function ConfigsPage() {
    const { hasAnyPermission } = usePermission();
    const { data } = useConfigAll();
    const { data: groupCatalog = [], isFetching: isFetchingGroups, refetch: refetchGroups } = useConfigGroups();
    const deleteConfig = useDeleteConfig();
    const deleteConfigGroup = useDeleteConfigGroup();
    const [editing, setEditing] = useState<ConfigView | null | undefined>(undefined);
    const [creatingGroup, setCreatingGroup] = useState(false);
    const [editingGroup, setEditingGroup] = useState<ConfigGroupView | null | undefined>(undefined);
    const [activeGroup, setActiveGroup] = useState<string | null>(null);
    const [showConfigSettings, setShowConfigSettings] = useState(false);
    const [groupDraft, setGroupDraft] = useState('');
    const [groupKeyword, setGroupKeyword] = useState('');
    const [page, setPage] = useState(1);
    const { pageSize: defaultPageSize, pageSizeOpts } = useTableDefaults();
    const [pageSize, setPageSize] = useState(defaultPageSize);
    const canEdit = hasAnyPermission('system:config:update');
    const canDelete = hasAnyPermission('system:config:delete');
    const canCreate = hasAnyPermission('system:config:create');
    const filteredGroups = groupCatalog.filter((group) => `${group.label} ${group.key}`.toLowerCase().includes(groupKeyword.toLowerCase()));
    const pageCount = Math.max(1, Math.ceil(filteredGroups.length / pageSize));
    const currentPage = Math.min(page, pageCount);
    const pageGroups = filteredGroups.slice((currentPage - 1) * pageSize, currentPage * pageSize);
    const selectedGroup = pageGroups.find((group) => group.key === activeGroup) ?? pageGroups[0] ?? null;
    const selectedRecords = selectedGroup ? (data ?? []).filter((record) => record.groupKey === selectedGroup.key) : [];

    const searchGroups = () => {
        setGroupKeyword(groupDraft.trim());
        setPage(1);
    };

    const removeConfig = (record: ConfigView) => {
        Modal.confirm({
            title: '确定删除该配置项？',
            content: record.configKey,
            onOk: () => deleteConfig.mutateAsync(record.id).then(() => Toast.success('配置项已删除')),
        });
    };

    const removeConfigGroup = (group: ConfigGroupView) => {
        Modal.confirm({
            title: `确定删除配置分组「${group.label}」？`,
            content: '仅空的自定义分组可以删除',
            okText: '删除',
            okButtonProps: { type: 'danger', theme: 'solid' },
            onOk: () =>
                deleteConfigGroup.mutateAsync(group.key).then(() => {
                    setActiveGroup(null);
                    Toast.success('配置分组已删除');
                }),
        });
    };

    return (
        <div className="config-groups-layout">
            <div className="config-groups-layout__master">
                <NavListPanel
                    title="配置分组"
                    headerExtra={
                        <>
                            {canEdit && (
                                <Tooltip content={showConfigSettings ? '关闭设置' : '显示设置'}>
                                    <Button
                                        theme="borderless"
                                        size="small"
                                        aria-label={showConfigSettings ? '关闭设置' : '显示设置'}
                                        icon={showConfigSettings ? <EyeOff size={14} /> : <Eye size={14} />}
                                        onClick={() => setShowConfigSettings((visible) => !visible)}
                                    />
                                </Tooltip>
                            )}
                            <Tooltip content="刷新配置分组">
                                <Button
                                    theme="borderless"
                                    size="small"
                                    aria-label="刷新配置分组"
                                    icon={<RefreshCw size={14} />}
                                    loading={isFetchingGroups}
                                    onClick={() => void refetchGroups()}
                                />
                            </Tooltip>
                            {canCreate && (
                                <Tooltip content="新增分组">
                                    <Button
                                        theme="borderless"
                                        size="small"
                                        aria-label="新增分组"
                                        icon={<Plus size={14} />}
                                        onClick={() => setCreatingGroup(true)}
                                    />
                                </Tooltip>
                            )}
                        </>
                    }
                    search={{
                        value: groupDraft,
                        onChange: setGroupDraft,
                        placeholder: '名称 / 编码',
                        onEnterPress: searchGroups,
                    }}
                    loading={isFetchingGroups && groupCatalog.length === 0}
                    emptyText={groupKeyword ? '没有匹配的配置分组' : '暂无配置分组'}
                    footer={
                        <Pagination
                            size="small"
                            total={filteredGroups.length}
                            currentPage={currentPage}
                            pageSize={pageSize}
                            pageSizeOpts={pageSizeOpts}
                            showSizeChanger
                            onPageChange={setPage}
                            onPageSizeChange={(size) => {
                                setPage(1);
                                setPageSize(size);
                            }}
                        />
                    }
                    dataSource={pageGroups}
                    renderItem={(group) => (
                        <NavListItem
                            key={group.key}
                            active={selectedGroup?.key === group.key}
                            onClick={() => setActiveGroup(group.key)}
                            primary={group.label}
                            secondary={group.key}
                            meta={
                                <>
                                    <span>{group.createdAt ?? ''}</span>
                                    {group.isBuiltin && <Tag size="small" color="blue">内置</Tag>}
                                </>
                            }
                            {...((canEdit || canDelete) && {
                                extra: (
                                    <Dropdown
                                        trigger="click"
                                        position="bottomRight"
                                        clickToHide
                                        render={
                                            <Dropdown.Menu>
                                                {canEdit && (
                                                    <Dropdown.Item icon={<Pencil size={14} />} onClick={() => setEditingGroup(group)}>
                                                        编辑
                                                    </Dropdown.Item>
                                                )}
                                                {canDelete && (
                                                    <Dropdown.Item
                                                        type="danger"
                                                        icon={<Trash2 size={14} />}
                                                        disabled={!group.canDelete}
                                                        onClick={() => removeConfigGroup(group)}
                                                    >
                                                        {group.canDelete
                                                            ? '删除'
                                                            : group.isBuiltin
                                                              ? '内置分组不可删除'
                                                              : '分组下仍有配置项不可删除'}
                                                    </Dropdown.Item>
                                                )}
                                            </Dropdown.Menu>
                                        }
                                    >
                                        <Button theme="borderless" size="small" aria-label={`${group.label}的操作`} icon={<MoreHorizontal size={14} />} />
                                    </Dropdown>
                                ),
                            })}
                        />
                    )}
                />
            </div>
            <PageContainer
                title={
                    selectedGroup?.label ?? '系统配置'
                }
                extra={
                    <Permission code="system:config:create">
                        <Button type="primary" theme="solid" icon={<Plus size={16} />} onClick={() => setEditing(null)}>
                            新增配置项
                        </Button>
                    </Permission>
                }
            >
            <Space vertical align="start" style={{ width: '100%' }} spacing={16}>
                {!selectedGroup ? (
                    <Empty title="暂无配置分组" description="请使用左侧分组菜单新增配置分组" style={{ padding: 48 }} />
                ) : (
                    <ConfigGroupPanel
                        key={`${selectedGroup.key}-${selectedRecords.map((record) => record.id).join('-')}`}
                        records={selectedRecords}
                        canEdit={canEdit}
                        showSettings={showConfigSettings}
                        onEdit={(record) => setEditing(record)}
                    />
                )}
            </Space>
            </PageContainer>
            {editing !== undefined && (
                <ConfigMetadataModal
                    record={editing}
                    defaultGroup={selectedGroup ?? undefined}
                    groups={groupCatalog}
                    canDelete={canDelete}
                    onDelete={removeConfig}
                    onClose={() => setEditing(undefined)}
                />
            )}
            {creatingGroup && (
                <ConfigGroupModal
                    record={null}
                    onClose={() => setCreatingGroup(false)}
                    onCreated={(key) => {
                        setGroupDraft('');
                        setGroupKeyword('');
                        const createdIndex = groupCatalog.findIndex((group) => group.key === key);
                        setPage(Math.ceil(((createdIndex >= 0 ? createdIndex : groupCatalog.length) + 1) / pageSize));
                        setActiveGroup(key);
                    }}
                />
            )}
            {editingGroup && (
                <ConfigGroupModal
                    record={editingGroup}
                    onClose={() => setEditingGroup(undefined)}
                    onCreated={(key) => {
                        setGroupKeyword('');
                        setActiveGroup(key);
                    }}
                />
            )}
        </div>
    );
}
