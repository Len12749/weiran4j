import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { http } from '@/utils/request';
import type { CqtNewsImageUpload, CqtNewsPage, CqtNewsQuery, CqtNewsSaveRequest, CqtNewsView } from '@/types/api';

export const cqtNewsKeys = {
    all: ['cqt-news'] as const,
    page: (query: CqtNewsQuery) => [...cqtNewsKeys.all, query] as const,
};

function queryString(query: CqtNewsQuery): string {
    const params = new URLSearchParams({ page: String(query.page), pageSize: String(query.pageSize) });
    if (query.keyword) params.set('keyword', query.keyword);
    return params.toString();
}

export function useCqtNews(query: CqtNewsQuery) {
    return useQuery({
        queryKey: cqtNewsKeys.page(query),
        queryFn: () => http.get<CqtNewsPage>(`/api/cqt/news?${queryString(query)}`),
    });
}

export function useCreateCqtNews() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (body: CqtNewsSaveRequest) => http.post<number>('/api/cqt/news', body),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: cqtNewsKeys.all }),
    });
}

export function useUpdateCqtNews() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, body }: { id: number; body: CqtNewsSaveRequest }) => http.put<void>(`/api/cqt/news/${id}`, body),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: cqtNewsKeys.all }),
    });
}

export function useDeleteCqtNews() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (id: number) => http.delete<void>(`/api/cqt/news/${id}`),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: cqtNewsKeys.all }),
    });
}

export function useUploadCqtNewsImage() {
    return useMutation({
        mutationFn: (file: File) => {
            const body = new FormData();
            body.append('file', file);
            return http.post<CqtNewsImageUpload>('/api/cqt/news/images', body);
        },
    });
}

export function useCqtNewsDetail(id: number | undefined) {
    return useQuery({
        queryKey: [...cqtNewsKeys.all, 'detail', id] as const,
        queryFn: () => http.get<CqtNewsView>(`/api/cqt/news/${id}`),
        enabled: id !== undefined,
    });
}
