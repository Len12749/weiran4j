import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { request } from '@/utils/request';

export type ReviewQuery = Record<string, string | number | boolean | undefined>;
export const reviewKeys = ['cqt-review'] as const;
export function useReviewQuery<T>(path: string, query: ReviewQuery = {}, enabled = true) {
    const params = new URLSearchParams();
    Object.entries(query).forEach(([key, value]) => { if (value !== undefined && value !== '') params.set(key, String(value)); });
    return useQuery({ queryKey: [...reviewKeys, path, query], enabled,
        queryFn: ({ signal }) => request<T>(`/api/cqt${path}${params.size ? `?${params}` : ''}`, { signal }),
    });
}
export function useReviewCommand() {
    const client = useQueryClient();
    return useMutation({
        mutationFn: ({ path, body, method = 'POST' }: { path: string; body: object; method?: 'POST' | 'PUT' }) => request<unknown>(`/api/cqt${path}`, { method, body }),
        onSuccess: (_, variables) => variables.path.includes('preview') ? undefined : client.invalidateQueries({ queryKey: reviewKeys }),
    });
}
export function commandKey() { return crypto.randomUUID(); }
