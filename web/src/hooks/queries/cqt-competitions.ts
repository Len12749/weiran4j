import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { http } from '@/utils/request';
import type { CqtCompetitionAdminView, CqtCompetitionBody, CqtCompetitionQuery, CqtCompetitionStatus, PageResult, SiteConfigImageUpload } from '@/types/api';
export const competitionKeys = ['cqt-competitions'] as const;
const base = '/api/cqt/competitions';
export function useCompetitionList(query: CqtCompetitionQuery) {
    const params = new URLSearchParams();
    Object.entries(query).forEach(([key, value]) => { if (value !== undefined && value !== '') params.set(key, String(value)); });
    return useQuery({ queryKey: [...competitionKeys, query], queryFn: ({ signal }) => http.get<PageResult<CqtCompetitionAdminView>>(`${base}?${params}`, { signal }) });
}
export function useCompetitionDetail(id?: number) {
    return useQuery({ queryKey: [...competitionKeys, 'detail', id], enabled: id !== undefined, queryFn: ({ signal }) => http.get<CqtCompetitionAdminView>(`${base}/${id}`, { signal }) });
}
export function useSaveCompetition() {
    const client = useQueryClient();
    return useMutation({ mutationFn: ({ id, body, expectedVersion }: { id?: number; body: CqtCompetitionBody; expectedVersion?: number }) => id
        ? http.put<{ id: number; version: number }>(`${base}/${id}`, { ...body, expectedVersion })
        : http.post<{ id: number; version: number }>(base, body),
    onSuccess: () => client.invalidateQueries({ queryKey: competitionKeys }) });
}
export function useCompetitionStatus() {
    const client = useQueryClient();
    return useMutation({ mutationFn: ({ id, status, expectedVersion }: { id: number; status: CqtCompetitionStatus; expectedVersion: number }) => http.post<{ id: number; version: number }>(`${base}/${id}/status`, { status, expectedVersion }),
        onSuccess: () => client.invalidateQueries({ queryKey: competitionKeys }) });
}

export function useUploadCompetitionImage() {
    return useMutation({ mutationFn: (file: File) => {
        const body = new FormData(); body.append('file', file);
        return http.post<SiteConfigImageUpload>(`${base}/images`, body);
    } });
}
