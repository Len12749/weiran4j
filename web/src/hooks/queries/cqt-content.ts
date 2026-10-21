import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { http } from '@/utils/request';
import type { CqtContentQuery, CqtExpertView, CqtShowcaseView, CqtResourceView, PageResult, SiteConfigImageUpload } from '@/types/api';

export type ContentResource = 'experts' | 'showcases' | 'resources';
export type ContentView = CqtExpertView | CqtShowcaseView | CqtResourceView;
export type ContentBody = Omit<CqtExpertView, 'id' | 'createdAt' | 'updatedAt'> | Omit<CqtShowcaseView, 'id' | 'createdAt' | 'updatedAt'> | Omit<CqtResourceView, 'id' | 'createdAt' | 'updatedAt'>;
const key = (resource: ContentResource) => ['cqt-content', resource] as const;
export function useContentList(resource: ContentResource, query: CqtContentQuery) {
    const params = new URLSearchParams();
    Object.entries(query).forEach(([name, value]) => { if (value !== undefined && value !== '') params.set(name, String(value)); });
    return useQuery({ queryKey: [...key(resource), query], queryFn: () => http.get<PageResult<ContentView>>(`/api-backend/cqt/${resource}/list?${params}`) });
}
export function useContentDetail(resource: ContentResource, id?: number) {
    return useQuery({ queryKey: [...key(resource), 'detail', id], queryFn: () => http.get<ContentView>(`/api-backend/cqt/${resource}/get/${id}`), enabled: id !== undefined });
}
export function useSaveContent(resource: ContentResource) {
    const client = useQueryClient();
    return useMutation({ mutationFn: async ({ id, body }: { id: number | undefined; body: ContentBody }) => {
        if (id) await http.put<void>(`/api-backend/cqt/${resource}/update/${id}`, body);
        else await http.post<number>(`/api-backend/cqt/${resource}/create`, body);
    },
        onSuccess: () => client.invalidateQueries({ queryKey: key(resource) }) });
}
export function useDeleteContent(resource: ContentResource) {
    const client = useQueryClient();
    return useMutation({ mutationFn: (id: number) => http.delete<void>(`/api-backend/cqt/${resource}/delete/${id}`), onSuccess: () => client.invalidateQueries({ queryKey: key(resource) }) });
}
export function useUploadContent(resource: ContentResource, kind: 'images' | 'files') {
    return useMutation({ mutationFn: (file: File) => {
        const body = new FormData(); body.append('file', file);
        return http.post<SiteConfigImageUpload>(`/api-backend/cqt/${resource}/${kind === 'images' ? 'uploadImage' : 'uploadFile'}`, body);
    } });
}
