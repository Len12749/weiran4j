import { Button, Empty, Spin } from '@douyinfe/semi-ui';
import { useParams } from 'react-router-dom';
import { PageContainer } from '@/components/PageContainer';
import { useCqtNewsDetail } from '@/hooks/queries/cqt-news';
import { usePageTabs } from '@/hooks/usePageTabs';
import { NewsForm } from './NewsForm';

export default function NewsEditPage() {
    const { id: rawId } = useParams();
    const id = Number(rawId);
    const validId = Number.isSafeInteger(id) && id > 0;
    const query = useCqtNewsDetail(validId ? id : undefined);
    const { closeCurrentAndGo } = usePageTabs();
    const cancel = () => closeCurrentAndGo('/cqt/news');
    if (!validId || query.isError) {
        return <PageContainer><Empty title="无法加载动态" description={query.error?.message ?? '动态编号无效'}>
            <Button onClick={cancel}>返回列表</Button>
        </Empty></PageContainer>;
    }
    if (!query.data) return <Spin />;
    return <NewsForm key={id} target={{ mode: 'edit', record: query.data }} onClose={cancel}
        onSaved={() => closeCurrentAndGo('/cqt/news', true)} />;
}
