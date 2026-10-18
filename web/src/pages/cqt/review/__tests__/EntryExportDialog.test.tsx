import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/test/helpers';
import EntryExportDialog from '../EntryExportDialog';
const download = vi.hoisted(() => ({ fetch: vi.fn(), save: vi.fn() }));
vi.mock('@/hooks/queries/cqt-entry-export', () => ({ fetchEntryExport: download.fetch, saveEntryExport: download.save }));
vi.mock('../ReviewShared', async (original) => ({ ...await original<object>(), useReviewOptions: () => ({ data: { competitions: [{ id: 1, name: '赛事' }, { id: 2, name: '赛事二' }], phases: [{ id: 10, competitionId: 1, code: 'PROVINCIAL', name: '省审' }] }, isError: false }) }));
afterEach(() => vi.clearAllMocks());
describe('作品导出弹层', () => {
    it('提交已生效筛选并下载，成功关闭', async () => {
        const blob = new Blob(['xlsx']); download.fetch.mockResolvedValue(blob); const close = vi.fn();
        renderWithProviders(<EntryExportDialog initial={{ competitionId: 1, phaseId: 10, keyword: '作品', source: 'zhongxi', status: '3' }} onClose={close} />);
        fireEvent.click(screen.getByRole('button', { name: '导出' }));
        await waitFor(() => expect(download.save).toHaveBeenCalledWith(blob, { competitionId: 1, phaseId: 10, keyword: '作品', source: 'zhongxi', status: '3' }));
        expect(close).toHaveBeenCalledOnce();
    });
    it('请求期间防重复并保留错误条件，不下载', async () => {
        let reject!: (error: Error) => void; download.fetch.mockReturnValue(new Promise((_, fail) => { reject = fail; }));
        renderWithProviders(<EntryExportDialog initial={{ competitionId: 1, phaseId: 10 }} onClose={vi.fn()} />);
        const button = screen.getByRole('button', { name: '导出' }); fireEvent.click(button); fireEvent.click(button);
        await waitFor(() => expect(download.fetch).toHaveBeenCalledOnce());
        reject(new Error('请缩小范围'));
        expect(await screen.findByText('请缩小范围')).toBeInTheDocument(); expect(download.save).not.toHaveBeenCalled();
    });
    it('赛事和阶段未选择时不发起下载', async () => {
        renderWithProviders(<EntryExportDialog initial={{}} onClose={vi.fn()} />);
        fireEvent.click(screen.getByRole('button', { name: '导出' }));
        await screen.findAllByText('此项必填'); expect(download.fetch).not.toHaveBeenCalled();
    });
    it('卸载后迟到响应不下载', async () => {
        let resolve!: (blob: Blob) => void; download.fetch.mockReturnValue(new Promise((done) => { resolve = done; }));
        const { unmount } = renderWithProviders(<EntryExportDialog initial={{ competitionId: 1, phaseId: 10 }} onClose={vi.fn()} />);
        fireEvent.click(screen.getByRole('button', { name: '导出' })); await waitFor(() => expect(download.fetch).toHaveBeenCalledOnce());
        unmount(); resolve(new Blob(['late'])); await Promise.resolve(); expect(download.save).not.toHaveBeenCalled();
    });
});
