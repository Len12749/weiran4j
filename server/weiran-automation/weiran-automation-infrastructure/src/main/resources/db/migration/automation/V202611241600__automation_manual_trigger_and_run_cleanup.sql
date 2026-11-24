-- 自动化任务：手动触发与执行记录清理的按钮权限。
insert into sys_menu (id, parent_id, title, type, permission, sort)
values (2102, 2100, '立即检查', 'button', 'automation:task:run', 2),
       (2103, 2100, '删除执行记录', 'button', 'automation:task:run:delete', 3);

-- 超级管理员在逻辑上直接放行，这里仍绑定，便于角色管理页展示一致的勾选状态。
insert ignore into sys_role_menu (role_id, menu_id)
values (1, 2102), (1, 2103);
