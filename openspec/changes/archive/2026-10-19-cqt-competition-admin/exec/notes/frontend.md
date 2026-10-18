# 赛事管理前端实施记录

日期：2026-10-18。对应已批准设计 `../../design.md` 与唯一契约 `weiran4j/docs/01-架构与接口契约.md` 的“赛事管理（2026-10-17）”。本记录不替代独立验收结论。

## 实现与文件

- `web/src/pages/cqt/competition/CompetitionsPage.tsx`：名称/年份/状态分页查询，草稿与已生效筛选分离；图标查询和重置，列设置、刷新、权限按钮、状态确认及完整字段只读详情。
- `CompetitionCreatePage.tsx`、`CompetitionEditPage.tsx`、内部 `CompetitionForm.tsx`：新增/编辑独立Tab，全部管理字段、七个时间和三个模板URL；编辑固定省国赛模式，旧编号只读。保存成功关闭发起Tab并刷新列表，取消返回列表；提交防重，卸载忽略迟到UI回调。
- `web/src/hooks/queries/cqt-competitions.ts`：列表、详情、保存和独立状态命令；查询键包含筛选/分页，成功失效赛事缓存。
- `web/src/types/api.ts`：追加管理详情、写入、查询、模式和状态类型。没有修改App或AdminLayout注册业务路由。

接口为 `/api/cqt/competitions` 列表/新增、`/{id}` 详情/编辑、`/{id}/status` 状态命令。编辑和状态携带expectedVersion；表单冻结初始记录版本，不因后台刷新推进CAS版本。409保留输入并提供显式重新加载入口。日期输出ISO本地时间或null；校验成对起止、说明UTF-8字节上限、模板URL协议及凭据查询参数。模板仅URL配置，计划时间不自动调度或发布。

菜单component与1130–1136迁移逐字配对，列表/新增/编辑路径依次为 `/cqt/competitions`、`/create`、`/edit/:id`。写按钮使用各自权限，详情仍要求list。

## 独立复核及修复

reviewer独立静态复核通过。修复了两项：背景详情refetch失败时不得卸载正在编辑的表单，现保留输入并展示警示；说明不按名称字段规则trim，现保留原文，仅空字符串归null。模式、初始version及发起Tab闭包已独立核对。

作者另独立审查executor后端，发现并确认修复正ID校验缺失与说明strip问题；最终五层、完整nullable映射、五默认组别事务、CAS/唯一冲突、模板日志脱敏、菜单与DI静态复核通过。后端门禁由root统一运行，不在此声称本人运行。

## 本地静态检查

在web目录使用本地CLI，最终均exit 0：

- `node node_modules/typescript/bin/tsc -b`：`/private/tmp/cqt-competition-web-tsc.log`。
- 定向ESLint（competition页面目录、query hook、api.ts）：`/private/tmp/cqt-competition-web-eslint.log`。
- `node node_modules/vite/bin/vite.js build`：`/private/tmp/cqt-competition-web-vite.log`，最终输出built in 536ms。

首次误在仓根执行tsc因根目录没有该CLI而失败，之后在web正确执行并覆盖上述最终日志。本轮按授权范围没有新增或运行自动化测试，没有运行Gradle；未声明浏览器交互或真实数据库写入验收完成。此前评审/导出切片289项测试通过不是赛事管理测试证据。

## 单图字段追加收尾（2026-10-18）

站点配置gongzhonghao/shouyeimage及赛事三个证书模板统一复用SingleImageUpload，原字段继续存URL/路径。共享组件仅追加disabled可选属性；图片预览与删除独立于上传触发，删除清空引用。站点两个展示标签改为“公众号图片”“首页图片”，不改配置key或后端标签。

站点内部编辑快照统一持有表单与图片值，不再因背景data刷新仅重挂Form而产生错位。站点单图和富文本批量图片、赛事三个模板均以统一上传计数ref/state追踪；保存入口同步检查ref，上传入口同步检查保存ref。保存期间禁用图片，卸载忽略迟到UI回调。赛事新增资源专属上传hook，不借用其它模块权限；保存兼容严格/uploads/本地路径，不拼开发机origin。

reviewer独立静态复核前端通过；本人独立复核后端上传鉴权、类型/大小、日志及本地路径规则通过。TypeScript、定向ESLint、Vite均exit0，日志`/private/tmp/cqt-single-images-{tsc,eslint,vite}.log`。标签小修后由root统一最终TypeScript/ESLint，不重复构建。未新增或运行测试、未启动服务；浏览器布局验收由root执行。
