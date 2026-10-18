import type { ContentResource } from '@/hooks/queries/cqt-content';
export const contentConfig = {
    experts: { name: '学术评审团', permission: 'expert', titleField: 'name', categories: [] },
    showcases: { name: '佳作欣赏', permission: 'showcase', titleField: 'title', categories: [{ value: 'SCRIPT', label: '剧本写作' }, { value: 'PERFORMANCE', label: '剧目演出' }] },
    resources: { name: '指导资料', permission: 'resource', titleField: 'title', categories: [{ value: 'SCRIPT_PAPER', label: '剧本写作稿纸' }, { value: 'DRAMA_MATERIAL', label: '校园戏剧资料' }] },
} satisfies Record<ContentResource, { name: string; permission: string; titleField: string; categories: { value: string; label: string }[] }>;
