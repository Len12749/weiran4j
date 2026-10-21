import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { http } from '@/utils/request';
import type { SiteConfigAdminResponse, SiteConfigImageUpload, SiteConfigSaveRequest } from '@/types/api';

export const cqtSiteConfigKeys = {
    all: ['cqt-site-config'] as const,
};

export function useCqtSiteConfig() {
    return useQuery({
        queryKey: cqtSiteConfigKeys.all,
        queryFn: () => http.get<SiteConfigAdminResponse>('/api-backend/cqt/site-config/get'),
    });
}

export function useSaveCqtSiteConfig() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (body: SiteConfigSaveRequest) => http.put<void>('/api-backend/cqt/site-config/update', body),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: cqtSiteConfigKeys.all }),
    });
}

export function useUploadCqtSiteConfigImage() {
    return useMutation({
        mutationFn: (file: File) => {
            const body = new FormData();
            body.append('file', file);
            return http.post<SiteConfigImageUpload>('/api-backend/cqt/site-config/uploadImage', body);
        },
    });
}
