import { Form, Modal, Toast } from '@douyinfe/semi-ui';
import type { FormApi } from '@douyinfe/semi-ui/lib/es/form';
import { useRef, useState } from 'react';
import { useSetUserAccountField } from '@/hooks/queries/users';
import type { UserAccountField, UserView } from '@/types/api';

interface Props {
    user: UserView;
    onClose: () => void;
}

const FIELD_OPTIONS: { value: UserAccountField; label: string }[] = [
    { value: 'username', label: '用户名' },
    { value: 'phone', label: '手机号' },
    { value: 'email', label: '邮箱' },
];

const CURRENT_VALUE: Record<UserAccountField, (user: UserView) => string> = {
    username: (user) => user.username,
    phone: (user) => user.phone ?? '',
    email: (user) => user.email ?? '',
};

/** 管理员每次只设置一个账号标识；唯一性由后端校验并由数据库约束兜底。 */
export function SetUserAccountFieldModal({ user, onClose }: Props) {
    const api = useRef<FormApi<{ field: UserAccountField; value: string }> | null>(null);
    const [field, setField] = useState<UserAccountField>('username');
    const save = useSetUserAccountField();

    const submit = async () => {
        const values = await api.current?.validate();
        if (!values) return;
        await save.mutateAsync({ id: user.id, body: { field, value: values.value.trim() } });
        Toast.success(`${FIELD_OPTIONS.find((option) => option.value === field)?.label}已更新`);
        onClose();
    };

    const rules =
        field === 'username'
            ? [
                  { required: true, message: '请输入用户名' },
                  { min: 2, max: 32, message: '用户名长度需为 2–32 位' },
                  { pattern: /^[A-Za-z][A-Za-z0-9_.-]*$/, message: '用户名需以字母开头，只能包含字母、数字、下划线、点或横线' },
              ]
              : field === 'email'
              ? [
                    { type: 'email' as const, message: '邮箱格式不正确' },
                    { max: 128, message: '邮箱长度不能超过 128 位' },
                ]
              : [
                    { max: 20, message: '手机号长度不能超过 20 位' },
                ];

    return (
        <Modal
            visible
            title={`设置账号信息：${user.username}`}
            onCancel={onClose}
            onOk={() => submit().catch(() => undefined)}
            okButtonProps={{ loading: save.isPending }}
            maskClosable={false}
        >
            <Form<{ field: UserAccountField; value: string }>
                key={field}
                initValues={{ field, value: CURRENT_VALUE[field](user) }}
                getFormApi={(formApi) => {
                    api.current = formApi;
                }}
            >
                <Form.RadioGroup
                    field="field"
                    label="设置类型"
                    options={FIELD_OPTIONS}
                    onChange={(value: unknown) => {
                        if (value === 'username' || value === 'phone' || value === 'email') setField(value);
                    }}
                />
                <Form.Input
                    field="value"
                    label={FIELD_OPTIONS.find((option) => option.value === field)?.label}
                    placeholder={`请输入${FIELD_OPTIONS.find((option) => option.value === field)?.label}`}
                    maxLength={field === 'username' ? 32 : field === 'email' ? 128 : 20}
                    extraText={field === 'username' ? undefined : '留空可清除当前值'}
                    rules={rules}
                />
            </Form>
        </Modal>
    );
}
