# Exec Plan

## 1. 任务映射

| 任务 | 执行单元 | 归属 |
| --- | --- | --- |
| 1.1 | L0-contract | root契约、executor类型 |
| 2.1 | L0-migration | executor迁移与菜单 |
| 2.2 | backend-create | executor五层 |
| 2.3 | backend-admin | executor五层 |
| 2.4 | backend-status | executor五层 |
| 3.1 | backend-adapter | executor三个DI/HTTP |
| 4.1 | frontend-pages | auth_audit三页面/hooks/types |
| 4.2 | frontend-forms | auth_audit表单与页签 |
| 5.1 | documentation | root唯一契约/state |
| 6.1 | frontend-single-image | auth_audit两个表单/组件/hook |
| 6.2 | backend-images | executor上传接口/DI/本地路径校验 |
| 6.3 | image-documentation | root契约/state/静态构建，独立reviewer/auth复核 |

未映射条目：无。

## 依赖图

L0-contract → L0-migration → backend-create/backend-admin/backend-status → backend-adapter → 集成审阅。
L0-contract → frontend-pages → frontend-forms → 集成审阅。各单元分别回指上表任务，不合并任务身份。

## Layer 0与契约冻结

root已冻结docs01“赛事管理（2026-10-17）”；design已获user于2026-10-17批准。版本V202610171900、菜单1130–1136、API/完整字段/URL禁用参数按docs01；后端独占迁移和三AutoConfiguration，先串行落类型与迁移，消费方不改冻结签名。

## 文件所有权与worktree

| 单元 | 拥有 | 只读/禁止 |
| --- | --- | --- |
| executor | CQT五层competition管理新增文件、三个DI、V202610171900 | 只读docs/web；不改旧报名、旧迁移、framework/system |
| auth_audit | web业务三页/hooks/types | 只读后端/冻结契约，不改App/AdminLayout硬路由 |
| root | docs01/state、任务勾选、证据/唯一构建 | 保留全部前序dirty，不reset/stash |

WT-0：已有dirty为本会话前序，root确认保留；无其他会话工作被覆盖。WT-1：规划先串行完成。WT-2：不改框架/流水线。WT-3：同一主检出按root明确文件所有权协调，不发明worktree工具。

## 失败与降级

契约冲突立即报告root，暂停相关消费者，不私改design；文件撞写则停止并行。工具权限等待立即报session，不组合源码编辑与升级构建。root唯一Gradle；本轮未单独授权测试，不新增或运行测试。

## L4交接检查清单

- [x] 1 任务逐项映射完整
- [x] 2 未映射无静默遗漏
- [x] 3 DAG无环
- [x] 4 共享迁移归Layer0
- [x] 5 契约已冻结
- [x] 6 文件所有权不重叠
- [x] 7 worktree采用真实项目约束
- [x] 8 失败降级明确
- [x] 9 不向上游设计回写实现步骤
- [x] 10 root接集成审阅及构建，批准后方继续验收


## 用户追加执行单元

| 任务 | 执行单元 | 归属 |
| --- | --- | --- |
| 6.1 | frontend-single-image | auth_audit：两个表单、赛事query、必要时SingleImageUpload扩展 |
| 6.2 | backend-images | executor：赛事Controller/DI及领域模板本地路径校验 |
| 6.3 | image-documentation | root：契约/state/构建证据；reviewer独立审前端、auth独立审后端 |

先root冻结docs01补充，再按独立文件所有权实施。无新迁移、权限号或依赖，已停止的服务不重启；构建静态检查不含测试。
