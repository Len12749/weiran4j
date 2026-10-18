# components.biz.md —— 常青藤（下游）前端公共组件清单(先查再造)

> 与上游 [`components.md`](components.md) 并列的**下游**登记来源（D-012）：`components-registry` 守卫把两份清单一并当作登记来源，
> 查「有没有现成的」时两份都要看。上游文件不改，常青藤自己新增到 `web/src/components/` 或 `web/src/layouts/` 的组件登记在这里。
>
> 业务专属、只被一个页面用的组件放在页面目录（`web/src/pages/cqt/**`）旁边，不进公共目录，也不用登记。

## 业务公共组件

| 组件 | 位置 | 什么时候用 | 坑 |
| --- | --- | --- | --- |
| **SingleImageUpload** | `components/SingleImageUpload.tsx` + `SingleImageUpload.css` | 单图上传、预览与移除；受控 `value` / `onChange`，注入 `upload(File): Promise<{url}>`；可选 `disabled` 禁止上传、预览及移除，父表单须同步跟踪上传互斥保存；96px 方形，支持点击和拖放 | 基于 Semi Upload / ImagePreview；预览和删除独立于上传触发区域，每次打开预览默认以原图尺寸 100% 显示；组件管理上传互斥、失败重试和卸载后的回调保护；移除仅清空字段，不删除存储对象 |
| **RegionSelect** | `components/RegionSelect.tsx` | 后台表单选择省、市、县；受控值为区划代码路径 | 从 `/api/regions/lookup` 获取启用树，父子关系使用 `code` / `parentCode`，不要使用数据库自增 ID 作为业务区划码 |

## 页签操作

`hooks/usePageTabs.ts` 提供 `usePageTabs().closeCurrentAndGo(path, refresh?)`：关闭当前后台页签并返回目标页；`refresh=true` 会重建目标页面，适用于新增/编辑保存成功后的列表刷新。由 `AdminLayout` 提供上下文，支持菜单路径参数的独立页签。

## 评审业务内部复用

`pages/cqt/review/ReviewShared.tsx` 为评审/评奖页提供既有列表能力的薄封装、预览确认、历史快照及附件展示；`ReviewForms.tsx` 提供业务筛选和Semi弹层表单。它们只服务本业务，不替代公共SearchToolbar/TableActions或改变公共组件协议。预览绑定当前请求代次，关闭/切上下文忽略迟到结果；确认重试复用同一requestKey，字段联动清除失效子项。不得以原始JSON输入替代业务表单。
