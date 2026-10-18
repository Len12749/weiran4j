import { usePageTabs } from '@/hooks/usePageTabs';
import { NewsForm } from './NewsForm';

export default function NewsCreatePage() {
    const { closeCurrentAndGo } = usePageTabs();
    return <NewsForm target={{ mode: 'create' }} onClose={() => closeCurrentAndGo('/cqt/news')}
        onSaved={() => closeCurrentAndGo('/cqt/news', true)} />;
}
