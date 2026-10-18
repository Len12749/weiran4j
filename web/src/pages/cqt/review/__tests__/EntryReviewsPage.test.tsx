import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { mockFetch, page, renderWithProviders } from '@/test/helpers';
import EntryReviewsPage from '../EntryReviewsPage';
afterEach(() => vi.unstubAllGlobals());
describe('作品清单导出权限', () => {
    it.each([[['cqt:entry-review:list'], false], [['cqt:entry-review:list', 'cqt:entry-review:export'], true]])('按按钮独立权限控制', async (permissions, visible) => {
        mockFetch({ 'GET /api/cqt/review/options': { competitions: [], phases: [], categories: [], regions: [], users: [] }, 'GET /api/cqt/review/entries': page([{ id: 1, title: '作品一', source: 'zhongxi', scoringScope: 'ENTRY', legacyStatus: 1 }]) });
        renderWithProviders(<EntryReviewsPage />, { permissions });
        await screen.findByText('作品一'); expect(!!screen.queryByRole('button', { name: '导出作品清单' })).toBe(visible);
    });
});
