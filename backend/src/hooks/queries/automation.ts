import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { http } from '@/utils/request';
import type {
    AutomationRunDetailView,
    AutomationRunView,
    AutomationTaskView,
    PageResult,
    ToggleAutomationTaskRequest,
} from '@/types/api';

const automationRootKeys = ['automation'] as const;

export const automationKeys = {
    all: automationRootKeys,
    tasks: () => [...automationRootKeys, 'tasks'] as const,
    runs: (taskId: number, page: number, pageSize: number) =>
        [...automationRootKeys, 'runs', taskId, page, pageSize] as const,
    runDetail: (taskId: number, runId: number) => [...automationRootKeys, 'run', taskId, runId] as const,
};

/** 全部自动化任务；有任务在跑时缩短轮询间隔，便于看到最新结果。 */
export function useAutomationTasks() {
    return useQuery({
        queryKey: automationKeys.tasks(),
        queryFn: () => http.get<AutomationTaskView[]>('/api-backend/automation/task/list'),
        refetchInterval: (query) =>
            (query.state.data ?? []).some((task) => task.running) ? 5000 : 60000,
    });
}

/** 某个任务的执行记录分页。 */
export function useAutomationRuns(taskId: number | null, page: number, pageSize: number) {
    return useQuery({
        queryKey: automationKeys.runs(taskId ?? 0, page, pageSize),
        queryFn: () =>
            http.get<PageResult<AutomationRunView>>(
                `/api-backend/automation/task/listRuns?taskId=${taskId}&page=${page}&pageSize=${pageSize}`,
            ),
        enabled: taskId !== null,
        placeholderData: (previous) => previous,
    });
}

/** 一次执行的评论明细，含逐文件意见。 */
export function useAutomationRunDetail(taskId: number | null, runId: number | null) {
    return useQuery({
        queryKey: automationKeys.runDetail(taskId ?? 0, runId ?? 0),
        queryFn: () =>
            http.get<AutomationRunDetailView>(
                `/api-backend/automation/task/getRun/${runId}?taskId=${taskId}`,
            ),
        enabled: taskId !== null && runId !== null,
    });
}

/** 管理员立即执行一次（同时成为下一次自动检查的基线）。 */
export function useTriggerAutomationTask() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (id: number) => http.post<AutomationTaskView>(`/api-backend/automation/task/run/${id}`),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: automationKeys.all }),
    });
}

/** 删除一次执行记录及其本地评论。 */
export function useDeleteAutomationRun() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ taskId, runId }: { taskId: number; runId: number }) =>
            http.delete<null>(`/api-backend/automation/task/run/${runId}?taskId=${taskId}`),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: automationKeys.all }),
    });
}

/** 管理员启停任务。 */
export function useToggleAutomationTask() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, body }: { id: number; body: ToggleAutomationTaskRequest }) =>
            http.put<AutomationTaskView>(`/api-backend/automation/task/toggle/${id}`, body),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: automationKeys.all }),
    });
}
