import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toQueryString } from '@/lib/query';
import { http } from '@/utils/request';
import type {
    SystemMediaImageQuery,
    SystemMediaImageUpload,
    SystemMediaImageView,
    PageResult,
} from '@/types/api';

export const systemMediaKeys = {
    all: ['system-media'] as const,
    page: (query: SystemMediaImageQuery) => [...systemMediaKeys.all, query] as const,
};

export function uploadSystemImage(file: File) {
    const body = new FormData();
    body.append('file', file);
    return http.post<SystemMediaImageUpload>('/api/system/media/images', body);
}

export function useSystemMediaImages(query: SystemMediaImageQuery) {
    return useQuery({
        queryKey: systemMediaKeys.page(query),
        queryFn: () => http.get<PageResult<SystemMediaImageView>>(`/api/system/media-library${toQueryString(query)}`),
        placeholderData: keepPreviousData,
    });
}

export function useUploadSystemImage() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: uploadSystemImage,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: systemMediaKeys.all }),
    });
}

export function useDeleteSystemMediaImage() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (id: number) => http.delete<null>(`/api/system/media-library/${id}`, { silent: true }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: systemMediaKeys.all }),
    });
}
