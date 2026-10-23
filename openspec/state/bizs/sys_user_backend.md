# sys_user_backend 后台用户扩展

## 0. 概要

`sys_user_backend` 以 `user_id` 为主键，与 `sys_user.id` 一对一。`sys_user.type = backend` 的账号必须有对应扩展行；`web` 账号不使用此表。现有后台用户管理和认证流程只处理 `backend` 账号。

## 1. 列表

后台用户列表由 `sys_user` 分页，批量读取本表的头像与性别；性别筛选通过本表子查询完成。

## 2. 字段与表单

| 列 | 约束 | 用途 |
| --- | --- | --- |
| `user_id` | bigint 主键 | 关联基础账号 |
| `avatar` | varchar(256) null | 本人头像；资源库引用扫描 |
| `gender` | varchar(16) not null，默认 `unknown` | 后台用户性别 |
| `preferences` | json null | 后台界面偏好 |
| `favorite_menus` | json null | 收藏菜单 ID 数组 |

## 3. 动作

创建后台用户时与基础账号同事务插入扩展行；更新资料时分别定向更新基础账号和扩展行；删除用户时删除扩展行。偏好和收藏仅更新扩展行，不刷新 `sys_user.updated_at`。

## 4. 用到的公共组件

后台用户列表的 `DictTag` 渲染性别；个人中心、布局偏好和收藏菜单沿用原有前端组件与接口。

## 5. 说明与建议

迁移 `V202610231620` 先复制全部现有用户的扩展字段，再删除 `sys_user` 上的原列。旧迁移保持不变。后台认证在查询阶段限定 `sys_user.type = backend`；网站用户认证与资料扩展由后续业务能力实现。

## 6. 已知问题汇总

暂无。

## 7. changelog

**2026-10-23**

- **#01 ✅ P2 拆分后台专属用户资料**
  从 `sys_user` 迁入本表，保留现有后台接口行为。
