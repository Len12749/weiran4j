# cqt_experts 学术评审团

## 0. 概要

独立内容表 `cqt_experts`，属于 weiran-cqt content 子域。后台「站点资料 → 学术评审团」`/cqt/experts`；管理接口 `/api-backend/cqt/experts`；公开接口 `/api-web/content/experts`。与大赛动态、另外两个内容模块的数据和 ID 互相独立。

## 1. 列表

后台分页、关键字查询、列设置、刷新；佳作和资料支持分类筛选。公开列表仅展示启用记录，排序 `sort ASC,id ASC`，支持分页；公开详情同样检查 enabled。

## 2. 字段与表单

姓名 name、照片 image、介绍 content、首页展示 showOnHome、启用 enabled、排序 sort。通用字段 id/createdAt/updatedAt，表内保留审计信息。

人物照片和介绍必填；首页仅显示 enabled 且 showOnHome 的记录；关于页显示全部启用人物。 sort 必须非负；图片最大10MB，附件复用统一存储上传能力。URL仅支持 http/https 或本地绝对路径。

## 3. 动作

独立列表、新增、编辑、删除和上传；权限 `cqt:expert:list/create/update/delete`，写操作记录操作日志。新增、编辑通过后台Tab打开，保存后关闭原Tab并刷新列表。菜单ID 1020–1026。

## 4. 用到的公共组件

SearchToolbar、ColumnSettings、TableActions、PageContainer、usePageTabs；照片复用系统配置同款 FileUpload，正文复用 SiteConfigRichEditor。

## 5. 说明与建议

不自动迁入旧 news 表数据；需要录入并启用后前台才有内容。三个模块分别使用自己的公开列表/详情接口，不再复用 product/index 和 product/newsdetail。删除为物理删除，移除文件字段不删除存储对象。

## 6. 已知问题汇总

无。

## 7. changelog

**2026-10-23**

- 照片改用系统配置同款 `FileUpload`。

**2026-10-16**

- 新增独立学术评审团管理、公开读取和前台对接。
