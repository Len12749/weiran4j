import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { mockFetch, renderWithProviders } from '@/test/helpers';
import type { ConfigGroupView, ConfigView } from '@/types/api';
import ConfigsPage from '../ConfigsPage';

const initialGroups: ConfigGroupView[] = [
    { key: 'general', label: '通用设置', isBuiltin: true, canDelete: false, createdAt: '2026-10-20 12:00:00' },
    { key: 'user', label: '用户', isBuiltin: true, canDelete: false, createdAt: '2026-10-20 12:00:00' },
];

const builtinConfig: ConfigView = {
    id: 1,
    configKey: 'system.site.title',
    configValue: 'Weiran Admin',
    configType: 'string',
    groupKey: 'general',
    fieldType: 'input',
    fieldOptions: null,
    required: true,
    description: '系统标题',
    isBuiltin: true,
    createdAt: '2026-10-20 12:00:00',
    updatedAt: '2026-10-20 12:00:00',
};

const editableConfig: ConfigView = {
    ...builtinConfig,
    id: 2,
    configKey: 'system.site.footer',
    configValue: '© Weiran',
    required: false,
    description: '页脚信息',
    isBuiltin: false,
};

function setup() {
    const groups = [...initialGroups];
    const fetch = mockFetch({
        'GET /api/system/config/listAll': [builtinConfig, editableConfig],
        'GET /api/system/config/listGroups': () => groups,
        'POST /api/system/config/createGroup': (init?: RequestInit) => {
            const body = JSON.parse(typeof init?.body === 'string' ? init.body : '{}') as { key: string; label: string };
            const group: ConfigGroupView = { ...body, isBuiltin: false, canDelete: true, createdAt: '2026-10-20 12:00:00' };
            groups.push(group);
            return { id: groups.length };
        },
        'DELETE /api/system/config/deleteGroup/custom_group': () => {
            const index = groups.findIndex((item) => item.key === 'custom_group');
            if (index >= 0) groups.splice(index, 1);
            return null;
        },
        'PUT /api/system/config/updateGroup/general': (init?: RequestInit) => {
            const body = JSON.parse(typeof init?.body === 'string' ? init.body : '{}') as { label: string };
            const group = groups.find((item) => item.key === 'general');
            if (group) group.label = body.label;
            return null;
        },
    });
    renderWithProviders(<ConfigsPage />);
    return fetch;
}

describe('ConfigsPage 配置分组侧栏', () => {
    it('新增配置项默认使用当前选中的分组', async () => {
        setup();
        fireEvent.click(await screen.findByText('用户'));
        fireEvent.click(screen.getByRole('button', { name: '新增配置项' }));

        expect(screen.getByText('用户', { selector: '.config-group-heading > span:first-child' })).toBeInTheDocument();
        const userLabels = await screen.findAllByText('用户');
        expect(userLabels).toHaveLength(3);
        expect(screen.getAllByText('配置分组')).toHaveLength(2);
    });

    it('左栏菜单可以新增配置分组并自动选中', async () => {
        const user = userEvent.setup();
        const fetch = setup();
        expect(await screen.findByText('通用设置', { selector: '.nav-list-item__primary' })).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: '配置分组操作' }));
        await user.click(await screen.findByRole('menuitem', { name: '新增分组' }));
        await user.type(screen.getByLabelText('分组编码'), 'custom_group');
        await user.type(screen.getByLabelText('分组名称'), '自定义组');
        await user.click(screen.getByText('新增').closest('button')!);

        expect(await screen.findAllByText('自定义组')).toHaveLength(2);
        await waitFor(() =>
            expect(screen.getByText('自定义组', { selector: '.nav-list-item__primary' }).closest('.nav-list-item')).toHaveClass('nav-list-item--active'),
        );
        expect(fetch.calls).toContainEqual(
            expect.objectContaining({
                method: 'POST',
                path: '/api/system/config/createGroup',
                body: { key: 'custom_group', label: '自定义组' },
            }),
        );
    });

    it('左栏展示分组时间和内置标识，详情标题不重复元数据，并允许修改分组名称', async () => {
        const user = userEvent.setup();
        const fetch = setup();
        expect(await screen.findAllByText('2026-10-20 12:00:00')).toHaveLength(2);
        expect(screen.getAllByText('内置')).toHaveLength(3);
        expect(screen.getByText('通用设置', { selector: '.page-container__title' })).toBeInTheDocument();
        expect(screen.queryByText('可删除', { exact: true })).not.toBeInTheDocument();
        expect(screen.queryByText('不可删除', { exact: true })).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: '通用设置的操作' }));
        await user.click(await screen.findByRole('menuitem', { name: '编辑' }));
        const labelInput = screen.getByLabelText('分组名称');
        await user.clear(labelInput);
        await user.type(labelInput, '基础设置');
        await user.click(screen.getByText('保存').closest('button')!);

        expect(await screen.findAllByText('基础设置')).toHaveLength(2);
        expect(fetch.calls).toContainEqual(
            expect.objectContaining({
                method: 'PUT',
                path: '/api/system/config/updateGroup/general',
                body: { label: '基础设置' },
            }),
        );
    });

    it('分组不展示可删除标签，内置组操作受保护，空自定义组提供删除操作', async () => {
        const user = userEvent.setup();
        setup();
        expect(await screen.findByText('通用设置', { selector: '.nav-list-item__primary' })).toBeInTheDocument();
        expect(screen.queryByText('可删除', { exact: true })).not.toBeInTheDocument();
        expect(screen.queryByText('不可删除', { exact: true })).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: '通用设置的操作' }));
        const builtinDelete = await screen.findByRole('menuitem', { name: '内置分组不可删除' });
        expect(builtinDelete.closest('li')).toHaveClass('semi-dropdown-item-disabled');
        await user.keyboard('{Escape}');
        await user.click(screen.getByRole('button', { name: '配置分组操作' }));
        await user.click(await screen.findByRole('menuitem', { name: '新增分组' }));
        await user.type(screen.getByLabelText('分组编码'), 'custom_group');
        await user.type(screen.getByLabelText('分组名称'), '自定义组');
        await user.click(screen.getByText('新增').closest('button')!);

        expect(screen.queryByText('可删除', { exact: true })).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: '自定义组的操作' }));
        expect(await screen.findByRole('menuitem', { name: '删除' })).toBeEnabled();
    });

    it('配置项标题不展示配置键，悬停编辑入口打开设置弹窗并在弹窗内提供删除', async () => {
        const user = userEvent.setup();
        setup();
        expect(await screen.findByText('系统标题')).toBeInTheDocument();
        expect(screen.queryByText('system.site.title')).not.toBeInTheDocument();
        expect(screen.getAllByText('内置')).toHaveLength(4);

        const editButton = screen.getByRole('button', { name: '字段设置：页脚信息' });
        expect(editButton).toHaveClass('config-field-title__edit');
        expect(editButton.querySelector('svg')).toHaveClass('lucide-settings');
        expect(screen.queryByRole('button', { name: '删除配置项' })).not.toBeInTheDocument();
        await user.click(editButton);
        expect(await screen.findByRole('button', { name: '删除配置项' })).toBeEnabled();
        await user.click(screen.getByRole('button', { name: '取消' }));
        await user.click(screen.getByRole('button', { name: '字段设置：系统标题' }));
        expect(await screen.findByRole('button', { name: '删除配置项' })).toBeDisabled();
    });
});
