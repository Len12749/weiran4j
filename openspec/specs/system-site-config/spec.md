---
status: "superseded"
superseded_by: "admin-foundation"
---

# system-site-config Specification

## Purpose
本能力长期负责后台通用站点内容的配置与维护：管理员可按类别编辑站点介绍、协议、联系方式和首页素材，通过富文本维护内容并插入受管理的图片，同时确保配置读取、保存、上传和图片删除受到权限与引用保护。

## Requirements

### Requirement: [FR-001] 通用站点配置管理

后台 **MUST** 提供站点配置页面和受权限保护的读取、保存接口，分别使用 `GET/PUT /api-backend/system/site-config`。配置目录由通用键组成，迁移写入空值初始记录；实现 **MUST NOT** 依赖业务模块表、公开站点读取接口或新闻接口。管理端读写 `sys_site_config.contents` 原始值，保存按 ident 更新最大 id 行，只有该 ident 无记录时才新建行。保存 **MUST** 记录操作日志。

站点配置页面 **MUST** 按类别使用 Tab 展示；介绍、关于、协议与声明内容 **MUST** 使用可编辑富文本，支持上传并插入图片。图片通过 `POST /api-backend/system/media/images` 上传；编辑器上传 **MUST** 需要 `system:media-library:upload` 权限，仅允许 jpg/jpeg/png/gif，单张最大 10 MB，并使用 system 文件存储。成功上传的图片 **MUST** 同时登记为媒体资产。富文本 HTML **MUST** 原样保存。

富文本编辑器 **MUST** 支持粘贴剪贴板中的图片文件和通过文件选择器一次选择多张图片；每张图片须先上传并登记媒体资产，再将返回 URL 按选择顺序插入当前编辑位置。普通文本粘贴 **MUST** 继续使用编辑器默认行为。

#### Scenario: 初始化通用配置项
- **WHEN** 运行 system 迁移
- **THEN** 通用配置目录中的各项以空内容初始化，且迁移不查询业务表
- **判据**:新库迁移成功，配置 API 返回全部目录项，内容为空；迁移 SQL 不引用其他业务表

#### Scenario: 后台保存保留原始内容
- **WHEN** 有 `system:site-config:update` 权限的管理员提交包含 HTML 的原始内容
- **THEN** 管理端保存并返回相同的原始 `contents`
- **判据**:管理 GET 返回原始 HTML，操作日志记录保存动作

#### Scenario: 富文本中插入已上传图片
- **WHEN** 管理员在富文本编辑器上传图片并保存配置
- **THEN** 图片经 `POST /api-backend/system/media/images` 写入 system 文件存储并登记媒体资产，编辑器以图片 URL 保存 HTML
- **判据**:无上传权限时拒绝；有权限时返回文件 URL 且媒体资产可查询；配置 GET 保留图片标签和 URL

#### Scenario: 按类别切换配置 Tab
- **WHEN** 管理员查看站点配置页面
- **THEN** 配置按站点介绍、协议、联系方式和首页素材分组
- **AND** 切换 Tab 后各字段仍保留表单值并可全量保存
- **判据**:前端测试验证类别 Tab 切换与跨 Tab 全量提交

#### Scenario: 粘贴图片上传并插入
- **WHEN** 管理员将剪贴板中的图片粘贴到富文本编辑器
- **THEN** 编辑器使用受权限保护的通用图片上传接口保存并登记图片，在当前光标处插入返回 URL
- **AND** 非图片文本粘贴继续由编辑器默认处理
- **判据**:前端测试验证图片上传、URL 插入与普通文本粘贴

#### Scenario: 未授权用户不能管理站点配置
- **WHEN** 未登录或缺少 `system:site-config:list` / `system:site-config:update` 权限的用户调用管理读写接口
- **THEN** 请求分别返回认证失败或权限拒绝，数据库内容不变
- **判据**:HTTP 集成测试断言未授权状态码及 `sys_site_config.contents` 未改变
