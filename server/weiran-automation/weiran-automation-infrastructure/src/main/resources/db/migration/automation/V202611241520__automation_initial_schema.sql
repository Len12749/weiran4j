-- 自动化任务：任务定义固定由迁移播种，界面只能启停，不能新增或改调度。
create table automation_task
(
    id                   bigint       not null auto_increment comment 'ID',
    code                 varchar(64)  not null comment '任务编码（固定）',
    name                 varchar(64)  not null comment '任务名称',
    description          varchar(512) not null comment '任务说明',
    trigger_description  varchar(512) not null comment '触发逻辑说明',
    schedule_description varchar(128) not null comment '定时配置说明',
    schedule_kind        varchar(32)  not null comment '调度类型：interval',
    interval_seconds     int          not null comment '执行间隔（秒）',
    output_mode          varchar(24)  not null default 'local' comment '输出方式：local 仅本地，local_and_cloud 本地并回填云端',
    enabled              tinyint      not null default 1 comment '是否启用：1 启用 0 停用',
    last_run_at          datetime     null comment '最近一次执行时间',
    last_run_status      varchar(16)  null comment '最近一次执行状态',
    last_run_summary     varchar(512) null comment '最近一次执行结果简介',
    last_run_id          bigint       null comment '最近一次执行记录 ID',
    next_run_at          datetime     null comment '下次允许执行的时间；为空表示立即可执行',
    locked_until         datetime     null comment '执行租约到期时间，多实例防重',
    created_at           datetime     not null default current_timestamp comment '创建时间',
    updated_at           datetime     not null default current_timestamp on update current_timestamp comment '更新时间',
    created_by           bigint       null comment '创建人',
    updated_by           bigint       null comment '更新人',
    primary key (id),
    unique key uk_automation_task_code (code)
) engine = InnoDB
  default charset = utf8mb4 comment ='自动化任务定义';

create table automation_task_run
(
    id                  bigint       not null auto_increment comment 'ID',
    task_id             bigint       not null comment '任务 ID',
    trigger_type        varchar(16)  not null comment '触发方式：schedule',
    output_mode         varchar(24)  not null default 'local' comment '本次执行的输出方式',
    status              varchar(16)  not null comment '执行状态：success/partial/failed',
    started_at          datetime     not null comment '开始时间',
    finished_at         datetime     null comment '结束时间',
    duration_ms         bigint       null comment '耗时（毫秒）',
    summary             varchar(512) not null comment '结果简介',
    error_message       varchar(1024) null comment '失败原因',
    merge_request_count int          not null default 0 comment '本次拉取的待评审合并请求数',
    reviewed_count      int          not null default 0 comment '成功生成评论的合并请求数',
    comment_count       int          not null default 0 comment '生成的评论条数',
    created_at          datetime     not null default current_timestamp comment '创建时间',
    updated_at          datetime     not null default current_timestamp on update current_timestamp comment '更新时间',
    created_by          bigint       null comment '创建人',
    updated_by          bigint       null comment '更新人',
    primary key (id),
    key idx_automation_run_task_started (task_id, started_at)
) engine = InnoDB
  default charset = utf8mb4 comment ='自动化任务执行记录';

create table automation_review
(
    id                  bigint       not null auto_increment comment 'ID',
    run_id              bigint       not null comment '执行记录 ID',
    task_id             bigint       not null comment '任务 ID',
    project_id          varchar(64)  not null comment '代码库 ID',
    merge_request_id    bigint       not null comment '合并请求局部 ID',
    merge_request_title varchar(256) not null comment '合并请求标题',
    web_url             varchar(512) null comment '合并请求页面地址',
    author_name         varchar(64)  null comment '创建人',
    source_branch       varchar(128) null comment '源分支',
    target_branch       varchar(128) null comment '目标分支',
    status              varchar(16)  not null comment '评审状态：processing/reviewed/failed/skipped',
    comment_text        text         null comment '生成的评论正文（暂存本地，不回写云效）',
    model               varchar(64)  null comment '生成评论的模型',
    file_count          int          not null default 0 comment '参与评审的变更文件数',
    add_lines           int          not null default 0 comment '新增行数',
    del_lines           int          not null default 0 comment '删除行数',
    error_message       varchar(1024) null comment '失败原因',
    created_at          datetime     not null default current_timestamp comment '创建时间',
    updated_at          datetime     not null default current_timestamp on update current_timestamp comment '更新时间',
    created_by          bigint       null comment '创建人',
    updated_by          bigint       null comment '更新人',
    primary key (id),
    unique key uk_automation_review_mr (task_id, project_id, merge_request_id),
    key idx_automation_review_run (run_id)
) engine = InnoDB
  default charset = utf8mb4 comment ='自动化任务评审结果（按合并请求）';

create table automation_review_file
(
    id          bigint        not null auto_increment comment 'ID',
    review_id   bigint        not null comment '评审结果 ID',
    path        varchar(512)  not null comment '文件路径',
    add_lines   int           not null default 0 comment '新增行数',
    del_lines   int           not null default 0 comment '删除行数',
    level       varchar(16)   not null comment '结论：pass/suggest/issue/skip',
    comment_text varchar(1024) null comment '文件级意见',
    skip_reason varchar(256)  null comment '跳过原因',
    created_at  datetime      not null default current_timestamp comment '创建时间',
    updated_at  datetime      not null default current_timestamp on update current_timestamp comment '更新时间',
    created_by  bigint        null comment '创建人',
    updated_by  bigint        null comment '更新人',
    primary key (id),
    key idx_automation_file_review (review_id)
) engine = InnoDB
  default charset = utf8mb4 comment ='自动化任务评审的文件级意见';

-- 固定任务：云效待评审合并请求的智能质检。任务类型与调度不可在界面新增或修改。
insert into automation_task (code, name, description, trigger_description, schedule_description, schedule_kind,
                             interval_seconds, output_mode, enabled)
values ('yunxiao-pr-review', '云效 PR 智能质检',
        '拉取云效代码库中尚未评论的待评审合并请求，按知识库规范分析本次变更文件，生成评论并暂存平台内等待审查。',
        '每分钟轮询云效 OpenAPI：拉取状态为待评审、总评论数为 0 的合并请求，先同步本地代码仓库，再比对源分支与目标分支的变更文件，逐条调用知识库评审 Agent 生成评论。',
        '每 1 分钟', 'interval', 60, 'local', 1);

-- 自动化任务菜单：不绑定权限码，所有已分配该菜单的角色都可见；启停按钮单独授权。
insert into sys_menu (id, parent_id, title, type, path, component, icon, permission, sort, visible)
values (2100, 0, '自动化任务', 'menu', '/automation/tasks', 'automation/AutomationTasksPage', 'Workflow', null, 4, 1),
       (2101, 2100, '启停自动化任务', 'button', null, null, null, 'automation:task:toggle', 1, 1);

insert into sys_role_menu (role_id, menu_id)
select id, 2100
from sys_role;
