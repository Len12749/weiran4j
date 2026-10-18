import { config } from '@/config';
import { ApiError, CODE_BAD_CREDENTIALS, CODE_UNAUTHORIZED, CSRF_HEADER } from '@/utils/request';
import { clearSession, getSession } from '@/utils/session';

export interface EntryExportQuery { competitionId: number; phaseId: number; keyword?: string; source?: string; status?: string | number }
export async function fetchEntryExport(query: EntryExportQuery, signal?: AbortSignal): Promise<Blob> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json', Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' };
    const session = getSession();
    if (session) headers[CSRF_HEADER] = session;
    const response = await fetch(`${config.apiBaseUrl}/api/cqt/review/entries/export`, { method: 'POST', credentials: 'same-origin', headers, body: JSON.stringify(query), ...(signal ? { signal } : {}) });
    if (!response.ok || response.headers.get('Content-Type')?.includes('json')) {
        const payload = await response.json().catch(() => null) as { code?: number; message?: string; requestId?: string } | null;
        const code = payload?.code ?? response.status * 100;
        if (code === CODE_UNAUTHORIZED || (response.status === 401 && code !== CODE_BAD_CREDENTIALS)) { if (session) clearSession(); }
        throw new ApiError(code, payload?.message || `导出失败（HTTP ${response.status}）`, response.status, payload?.requestId || response.headers.get('X-Request-Id') || undefined);
    }
    if (!response.headers.get('Content-Type')?.includes('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')) throw new ApiError(-1, '导出响应格式错误');
    return response.blob();
}
export function saveEntryExport(blob: Blob, query: EntryExportQuery) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url; link.download = `cqt-entries-${query.competitionId}-${query.phaseId}.xlsx`;
    document.body.appendChild(link);
    try { link.click(); } finally { link.remove(); setTimeout(() => URL.revokeObjectURL(url), 0); }
}
