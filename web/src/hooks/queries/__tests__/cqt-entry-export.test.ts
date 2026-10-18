import { afterEach, describe, expect, it, vi } from 'vitest';
import { fetchEntryExport, saveEntryExport } from '../cqt-entry-export';
const session = vi.hoisted(() => ({ get: vi.fn(() => 'csrf'), clear: vi.fn() }));
vi.mock('@/utils/session', () => ({ getSession: session.get, clearSession: session.clear }));
afterEach(() => { vi.unstubAllGlobals(); vi.clearAllMocks(); });
const query = { competitionId: 1, phaseId: 2 };
describe('作品Excel下载', () => {
    it('POST携带同源Cookie与CSRF并读取二进制', async () => {
        const fetch = vi.fn().mockResolvedValue(new Response('excel', { headers: { 'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' } })); vi.stubGlobal('fetch', fetch);
        expect(await (await fetchEntryExport(query)).text()).toBe('excel');
        expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/api/cqt/review/entries/export'), expect.objectContaining({ method: 'POST', credentials: 'same-origin', headers: expect.objectContaining({ 'X-CSRF-Token': 'csrf' }), body: JSON.stringify(query) }));
    });
    it.each([[401, 40100, true], [403, 40302, false], [200, 40000, false]])('错误HTTP%s/code%s不返回下载且正确处理会话', async (status, code, clear) => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json({ code, message: '失败' }, { status })));
        await expect(fetchEntryExport(query)).rejects.toThrow('失败'); expect(session.clear).toHaveBeenCalledTimes(clear ? 1 : 0);
    });
    it('拒绝成功状态的非Excel响应', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('html', { headers: { 'Content-Type': 'text/html' } })));
        await expect(fetchEntryExport(query)).rejects.toThrow('格式错误');
    });
    it('安全固定文件名并释放对象URL', async () => {
        const create = vi.fn(() => 'blob:test'); const revoke = vi.fn(); vi.stubGlobal('URL', { createObjectURL: create, revokeObjectURL: revoke });
        const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) { expect(this.download).toBe('cqt-entries-1-2.xlsx'); });
        saveEntryExport(new Blob(['excel']), query); expect(click).toHaveBeenCalledOnce(); await new Promise((done) => setTimeout(done, 1)); expect(revoke).toHaveBeenCalledWith('blob:test'); click.mockRestore();
    });
});
