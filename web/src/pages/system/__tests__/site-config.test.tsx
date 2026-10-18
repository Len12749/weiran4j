import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { mockFetch, renderWithProviders } from '@/test/helpers';
import type { SystemSiteConfigItem } from '@/types/api';
import SystemSiteConfigPage from '../site-config';

const config: SystemSiteConfigItem[] = [
    { key: 'site_introduction', label: '站点介绍', ident: 1, contents: '<p>旧介绍</p>' },
    { key: 'about', label: '关于我们', ident: 2, contents: null },
    { key: 'user_agreement', label: '用户协议', ident: 3, contents: '<p>旧协议</p>' },
    { key: 'privacy_policy', label: '隐私政策', ident: 4, contents: '' },
    { key: 'disclaimer', label: '免责声明', ident: 5, contents: '' },
    { key: 'contact_address', label: '联系地址', ident: 6, contents: '' },
    { key: 'contact_phone', label: '联系电话', ident: 7, contents: '' },
    { key: 'contact_email', label: '联系邮箱', ident: 8, contents: '' },
    { key: 'working_hours', label: '工作时间', ident: 9, contents: '' },
    { key: 'homepage_image', label: '首页图片', ident: 10, contents: '' },
];

describe('SystemSiteConfigPage', () => {
    afterEach(() => vi.unstubAllGlobals());

    it('跨 Tab 保留表单值，上传多张图片后按顺序插入并全量保存 HTML', async () => {
        const user = userEvent.setup();
        let uploadCount = 0;
        const fetch = mockFetch({
            'GET /api/system/site-config': { items: config },
            'PUT /api/system/site-config': null,
            'POST /api/system/media/images': () => {
                uploadCount += 1;
                const name = `site-${uploadCount}.png`;
                return { url: `/system-uploads/${name}`, name, path: name, size: 3 };
            },
        });
        renderWithProviders(<SystemSiteConfigPage />, {
            permissions: ['system:site-config:update', 'system:media-library:upload'],
        });

        expect(await screen.findByRole('tab', { name: '站点介绍' })).toHaveAttribute('aria-selected', 'true');
        await user.click(screen.getByRole('tab', { name: '协议' }));
        const agreement = await screen.findByRole('textbox', { name: '用户协议' });
        expect(agreement).toHaveTextContent('旧协议');
        const imageFiles = [
            new File(['first'], 'first.png', { type: 'image/png' }),
            new File(['second'], 'second.png', { type: 'image/png' }),
        ];
        fireEvent.change(screen.getByLabelText('用户协议图片文件'), { target: { files: imageFiles } });
        await waitFor(() => expect(within(agreement).getAllByRole('img')).toHaveLength(2));
        expect(within(agreement).getAllByRole('img').map((image) => image.getAttribute('src'))).toEqual([
            '/system-uploads/site-1.png',
            '/system-uploads/site-2.png',
        ]);

        await user.click(screen.getByRole('tab', { name: '联系方式' }));
        await user.click(screen.getByRole('tab', { name: '协议' }));
        expect(within(agreement).getAllByRole('img')).toHaveLength(2);
        fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

        await waitFor(() => expect(fetch.calls.some((call) => call.method === 'PUT')).toBe(true));
        const request = fetch.calls.find((call) => call.method === 'PUT');
        expect(request?.body).toMatchObject({
            items: expect.arrayContaining([
                { key: 'site_introduction', contents: '<p>旧介绍</p>' },
                {
                    key: 'user_agreement',
                    contents: expect.stringContaining('<img src="/system-uploads/site-1.png" alt="site-1.png">'),
                },
                { key: 'about', contents: null },
            ]),
        });
    });

    it('粘贴剪贴板图片先上传再插入 URL，普通文本仍使用编辑器默认行为', async () => {
        const fetch = mockFetch({
            'GET /api/system/site-config': { items: config },
            'POST /api/system/media/images': { url: '/system-uploads/pasted.png', name: 'pasted.png', path: 'pasted.png', size: 3 },
        });
        renderWithProviders(<SystemSiteConfigPage />, { permissions: ['system:media-library:upload'] });

        const editor = await screen.findByRole('textbox', { name: '站点介绍' });
        const imageFile = new File(['img'], 'pasted.png', { type: 'image/png' });
        const clipboardData = {
            items: [{ kind: 'file', type: 'image/png', getAsFile: () => imageFile }],
            getData: () => '',
        } as unknown as DataTransfer;
        fireEvent.paste(editor, { clipboardData });

        expect(await within(editor).findByRole('img', { name: 'pasted.png' })).toHaveAttribute('src', '/system-uploads/pasted.png');
        expect(fetch.calls.some((call) => call.method === 'POST' && call.path === '/api/system/media/images')).toBe(true);

        fireEvent.paste(editor, { clipboardData: { items: [], getData: (type: string) => (type === 'text/plain' ? '普通文本' : '') } });
        await waitFor(() => expect(editor).toHaveTextContent('普通文本'));
        expect(fetch.calls.filter((call) => call.method === 'POST' && call.path === '/api/system/media/images')).toHaveLength(1);
    });
});
