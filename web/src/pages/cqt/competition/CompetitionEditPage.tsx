import { Banner, Button, Empty, Spin } from '@douyinfe/semi-ui';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { PageContainer } from '@/components/PageContainer';
import { useCompetitionDetail } from '@/hooks/queries/cqt-competitions';
import { usePageTabs } from '@/hooks/usePageTabs';
import CompetitionForm from './CompetitionForm';
export default function CompetitionEditPage() {
    const { id: raw } = useParams(); const id = Number(raw); const valid = Number.isSafeInteger(id) && id > 0;
    const query = useCompetitionDetail(valid ? id : undefined); const [revision, setRevision] = useState(0); const { closeCurrentAndGo } = usePageTabs();
    if (!valid || (query.isError && !query.data)) return <PageContainer><Empty title="无法加载赛事" description={query.error?.message ?? '赛事编号无效'}><Button onClick={() => closeCurrentAndGo('/cqt/competitions')}>返回列表</Button>{valid && <Button onClick={() => void query.refetch()}>重试</Button>}</Empty></PageContainer>;
    if (!query.data) return <Spin />;
    return <>{query.isError && <Banner type="warning" description="后台刷新失败，当前输入已保留，可稍后重试加载。" />}<CompetitionForm key={`${id}-${revision}`} record={query.data} onReload={() => { void query.refetch().then((result) => { if (result.isSuccess) setRevision((value) => value + 1); }); }} /></>;
}
