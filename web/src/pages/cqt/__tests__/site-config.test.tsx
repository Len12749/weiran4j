import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { mockFetch, renderWithProviders } from '@/test/helpers';
import type { SiteConfigAdminItem } from '@/types/api';
import CqtSiteConfigPage from '../site-config';

const config: SiteConfigAdminItem[] = [
    { key: 'guanyuwomen', label: '关于我们', ident: 2, contents: '<p>旧介绍</p>' },
    { key: 'user_agreement', label: '用户协议', ident: 71, contents: '<p>旧协议</p>' },
    { key: 'yinsixieyi', label: '隐私协议', ident: 3, contents: '' },
    { key: 'about_us', label: '关于我们（英文）', ident: 4, contents: null },
    { key: 'dizhi', label: '公司地址', ident: 7, contents: '' },
    { key: 'shouji', label: '联系电话', ident: 8, contents: '' },
    { key: 'weixin', label: '微信号', ident: 9, contents: '' },
    { key: 'youxiang', label: '联系邮箱', ident: 10, contents: '' },
    { key: 'gongsijieshao', label: '公司介绍', ident: 15, contents: '' },
    { key: 'hezuohuoban', label: '合作伙伴', ident: 16, contents: '' },
    { key: 'dasaijieshao', label: '大赛介绍', ident: 17, contents: '' },
    { key: 'mianzexieyi', label: '免责声明', ident: 18, contents: '' },
    { key: 'shouhoufuwu', label: '售后服务', ident: 19, contents: '' },
    { key: 'shangwuhezuo', label: '商务合作', ident: 20, contents: '' },
    { key: 'gongzuoshijian', label: '工作时间', ident: 21, contents: '' },
    { key: 'dasaizhangcheng', label: '大赛章程', ident: 22, contents: '' },
    { key: 'gongzhonghao', label: '公众号图片路径', ident: 23, contents: '' },
    { key: 'certificate_visibility', label: '证书显示设置', ident: 67, contents: '' },
    { key: 'sheng_certificate_visibility', label: '省级证书显示设置', ident: 68, contents: '' },
    { key: 'teacher_org_certificate_competition_id', label: '教师/机构证书赛事编号', ident: 69, contents: '' },
    { key: 'guo_certificate_visibility', label: '国家级证书显示设置', ident: 70, contents: '' },
    { key: 'shouyeimage', label: '首页图片路径', ident: 66, contents: '' },
];

describe('CqtSiteConfigPage', () => {
    afterEach(() => vi.unstubAllGlobals());

    it('按 Tab 编辑配置，上传图片插入富文本并全量保存 HTML', async () => {
        const user = userEvent.setup();
        const fetch = mockFetch({
            'GET /api/cqt/site-config': { items: config },
            'PUT /api/cqt/site-config': null,
            'POST /api/cqt/site-config/images': { url: '/uploads/site.png', name: 'site.png', path: 'site.png', size: 3 },
        });
        renderWithProviders(<CqtSiteConfigPage />, { permissions: ['cqt:site-config:update'] });

        expect(await screen.findByRole('tab', { name: '站点介绍' })).toHaveAttribute('aria-selected', 'true');
        await user.click(screen.getByRole('tab', { name: '协议与服务' }));
        const agreement = await screen.findByRole('textbox', { name: '用户协议' });
        expect(agreement).toHaveTextContent('旧协议');
        const imageFile = new File(['img'], 'site.png', { type: 'image/png' });
        fireEvent.change(screen.getByLabelText('用户协议图片文件'), { target: { files: [imageFile] } });
        await waitFor(() => expect(screen.getByRole('img', { name: 'site.png' })).toHaveAttribute('src', '/uploads/site.png'));
        await user.click(screen.getByRole('tab', { name: '联系方式' }));
        await user.click(screen.getByRole('tab', { name: '协议与服务' }));
        expect(agreement.querySelector('img')).toHaveAttribute('src', '/uploads/site.png');
        fireEvent.click(screen.getByRole('button', { name: '保存配置' }));

        await waitFor(() => expect(fetch.calls.some((call) => call.method === 'PUT')).toBe(true));
        const request = fetch.calls.find((call) => call.method === 'PUT');
        expect(request?.body).toMatchObject({
            items: expect.arrayContaining([
                { key: 'guanyuwomen', contents: '<p>旧介绍</p>' },
                { key: 'user_agreement', contents: expect.stringContaining('<img src="/uploads/site.png" alt="site.png">') },
                { key: 'about_us', contents: null },
            ]),
        });
    });

    it('粘贴剪贴板图片时先上传再插入图片 URL', async () => {
        const fetch = mockFetch({
            'GET /api/cqt/site-config': { items: config },
            'POST /api/cqt/site-config/images': { url: '/uploads/pasted.png', name: 'pasted.png', path: 'pasted.png', size: 3 },
        });
        renderWithProviders(<CqtSiteConfigPage />, { permissions: ['cqt:site-config:update'] });

        const editor = await screen.findByRole('textbox', { name: '关于我们' });
        const imageFile = new File(['img'], 'pasted.png', { type: 'image/png' });
        const clipboardData = {
            items: [{ kind: 'file', type: 'image/png', getAsFile: () => imageFile }],
            getData: () => '',
        } as unknown as DataTransfer;
        fireEvent.paste(editor, { clipboardData });

        expect(await screen.findByRole('img', { name: 'pasted.png' })).toHaveAttribute('src', '/uploads/pasted.png');
        expect(fetch.calls.some((call) => call.method === 'POST' && call.path === '/api/cqt/site-config/images')).toBe(true);

        fireEvent.paste(editor, { clipboardData: { items: [], getData: (type: string) => (type === 'text/plain' ? '普通文本' : '') } });
        await waitFor(() => expect(editor).toHaveTextContent('普通文本'));
        expect(fetch.calls.filter((call) => call.method === 'POST' && call.path === '/api/cqt/site-config/images')).toHaveLength(1);
    });
});
