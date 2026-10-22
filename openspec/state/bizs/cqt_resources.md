# cqt_resources 指导资料

## 0. 概要

独立内容表 `cqt_resources`，属于 weiran-cqt content 子域。后台「站点资料 → 指导资料」`/cqt/resources`；管理接口 `/api-backend/cqt/resources`；公开接口 `/api-web/content/resources`。与大赛动态、另外两个内容模块的数据和 ID 互相独立。

## 1. 列表

后台分页、关键字查询、列设置、刷新；佳作和资料支持分类筛选。公开列表仅展示启用记录，排序 `sort ASC,id ASC`，支持分页；公开详情同样检查 enabled。

## 2. 字段与表单

标题 title、上传资料 materialUrl、类别 category、说明 content、启用 enabled、排序 sort。通用字段 id/createdAt/updatedAt，表内保留审计信息。

类别 SCRIPT_PAPER（剧本写作稿纸）或 DRAMA_MATERIAL（校园戏剧资料）；标题和上传资料必填。`materialUrl` 保存上传后的地址，下载名称使用资料标题。历史 `file_url` 经新迁移更名为 `material_url`，`file_name` 删除。sort 必须非负；图片最大10MB，附件复用统一存储上传能力。URL仅支持 http/https 或本地绝对路径。

## 3. 动作

独立列表、新增、编辑、删除和上传；权限 `cqt:resource:list/create/update/delete`，写操作记录操作日志。新增、编辑通过后台Tab打开，保存后关闭原Tab并刷新列表。菜单ID 1040–1046。

## 4. 用到的公共组件

SearchToolbar、ColumnSettings、TableActions、PageContainer、usePageTabs；资料文件复用系统配置同款 FileUpload，正文复用 SiteConfigRichEditor。

## 5. 说明与建议

不自动迁入旧 news 表数据；需要录入并启用后前台才有内容。三个模块分别使用自己的公开列表/详情接口，不再复用 product/index 和 product/newsdetail。删除为物理删除，移除文件字段不删除存储对象。

## 6. 已知问题汇总

无。

## 7. changelog

**2026-10-23**

- 将 `fileUrl`、`fileName` 合并为单一 `materialUrl` 字段，追加 Flyway 迁移保留历史地址；表单复用系统配置的 `FileUpload`，前台下载改用资料标题命名。

**2026-10-16**

- 新增独立指导资料管理、公开读取和前台对接。
