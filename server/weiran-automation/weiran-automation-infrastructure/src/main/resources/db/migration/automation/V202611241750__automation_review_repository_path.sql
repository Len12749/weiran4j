-- 自动化任务：评审结果补记代码库路径，供界面显示仓库名（原先只有数字 project_id）。
alter table automation_review
    add column repository_path varchar(128) not null default '' comment '代码库路径，形如 qyd/lianmeng/kr/web' after project_id;

-- 历史记录从合并请求地址反推路径：https://<host>/<org>/<group>/<repo>/change/<id>
update automation_review
set repository_path = regexp_replace(web_url, '^https?://[^/]+/(.*)/change/.*$', '$1')
where repository_path = ''
  and web_url is not null
  and web_url like '%/change/%';
