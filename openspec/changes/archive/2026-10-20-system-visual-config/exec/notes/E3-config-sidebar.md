# E3: 配置分组侧栏

## 完成的 tasks.md 条目

- 3.1
- 3.2
- 3.3
- 3.4

## 改了什么

| 文件 | 动作 | 说明 |
|---|---|---|
| `web/src/pages/system/configs/ConfigsPage.tsx` | 改造 | 左侧 `NavListPanel` 搜索/选择分组，右侧显示所选组动态表单；保留权限、字段设置、保存与删除。 |
| `web/src/hooks/queries/configs.ts` | 改造 | 查询与变更使用冻结的 action 路由。 |
| `web/src/styles/global.css` | 新增样式 | 双栏桌面布局与窄屏堆叠。 |
| `web/src/layouts/__tests__/NavPreferences.test.tsx` | 更新 | 页面缓存用例改为验证分组搜索输入状态。 |
| `web/src/pages/system/configs/__tests__/ConfigsPage.test.tsx` | 新增 | 验证新增配置项继承当前组，以及侧栏新增分组后自动选中。 |
| `web/src/pages/system/configs/ConfigsPage.tsx`、`web/src/hooks/queries/configs.ts`、`web/src/types/api.ts` | 改造 | 左栏增加创建时间、内置标识与编辑菜单；右栏标题取当前组名并移除说明；分组编辑只改名称。 |
| `web/src/pages/system/configs/ConfigsPage.tsx`、`web/src/styles/global.css` | 改造 | 配置键不再作为独立文本显示；内置项显示标签；字段设置按钮悬停/聚焦标题时显示，删除操作进入设置弹窗；新增按钮带图标并位于标题栏右侧。 |
| `web/src/pages/system/configs/__tests__/ConfigsPage.test.tsx` | 更新 | 验证配置键隐藏、内置标签、字段设置入口及弹窗内删除操作。 |

## 为什么这么做

- 左侧导航复用字典页的 `NavListPanel`，保留既有动态表单作为右侧内容。
- 搜索分组名称和编码，分组增多时仍可快速定位。
- 刷新与新增分组收纳在左栏操作菜单，刷新同时重取分组和配置；移除右侧重复刷新入口。新增配置表单默认当前选中的分组。

## 依赖的契约

- `exec/plan.md` §4 的组列表响应及配置动作路由。

## 越界申报

| 文件 | 为什么不得不改 | 性质 | 是否可能影响其他单元 |
|---|---|---|---|
| `web/src/styles/global.css` | 双栏和窄屏布局样式需要全局样式表承载 | 必要连带 | 否 |
| `web/src/layouts/__tests__/NavPreferences.test.tsx` | 旧缓存测试依赖已移除的配置项搜索输入 | 必要连带 | 否 |

## 埋的坑 / 遗留

- [ ] 无。

## 自测结果

- 命令：`pnpm --filter @weiran/web lint`、`pnpm --filter @weiran/web test`、`pnpm --filter @weiran/web build`
- 结果：上一轮 lint、36 个测试文件/295 项测试及生产构建通过；本轮针对页面测试 1 个文件/4 项测试通过，完整 lint/test/build 正在复验。
