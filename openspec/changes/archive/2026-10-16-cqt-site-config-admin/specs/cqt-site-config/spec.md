## MODIFIED Requirements

### Requirement: [FR-002] 前台站点配置接口

`GET /api-web/product/getconfig` **MUST** 免登录，返回的 `data` **MUST** 恰好包含以下 22 个键，取值来自 `cqt_setting` 中 `ident` 对应行的 `contents`：
`guanyuwomen`→2、`user_agreement`→71、`yinsixieyi`→3、`about_us`→4、`dizhi`→7、`shouji`→8、`weixin`→9、`youxiang`→10、
`gongsijieshao`→15、`hezuohuoban`→16、`dasaijieshao`→17、`mianzexieyi`→18、`shouhoufuwu`→19、`shangwuhezuo`→20、
`gongzuoshijian`→21、`dasaizhangcheng`→22、`gongzhonghao`→23、`shouyeimage`→66、`certificate_visibility`→67、
`sheng_certificate_visibility`→68、`teacher_org_certificate_competition_id`→69、`guo_certificate_visibility`→70。
只读取 `ident` 在 1–100 之间的行。同一 `ident` 多行时取 `id` 最大的一行。

富文本键 `guanyuwomen`、`user_agreement`、`yinsixieyi`、`about_us`、`gongsijieshao`、`hezuohuoban`、`dasaijieshao`、`mianzexieyi`、`shouhoufuwu`、`shangwuhezuo`、`dasaizhangcheng` **MUST** 返回原始 HTML；其它字符串按现有规则去除 HTML 标签与首尾空白；`gongzhonghao` 仍返回原始值数组。

#### Scenario: 返回全部 22 个键
- **WHEN** 请求 `GET /api-web/product/getconfig`
- **THEN** `data` 的键集合恰为上述 22 个
- **判据**:集成测试断言 `$.data` 的字段名集合与上表相等

#### Scenario: 缺行的配置项为 null
- **WHEN** `cqt_setting` 中不存在 `ident = 9` 的行
- **THEN** `data.weixin` 为 `null`，接口仍返回 `code` 200
- **判据**:集成测试断言 `$.data.weixin` 为 JSON null

## ADDED Requirements

### Requirement: [FR-004] 站点配置后台表单

后台 **MUST** 提供独立的 CQT 站点配置页面和受权限保护的读取、保存接口；管理端读写 `cqt_setting.contents` 原始值，保存按 ident 更新最大 id 行，只有该 ident 无记录时才新建行。`guanyuwomen` 使用 ident 2，`user_agreement` 使用 ident 71。部署迁移和旧库导入 **MUST** 将现有 ident 2 最大 id 行的内容复制为 ident 71 初始值；ident 2 缺行时不得臆造默认内容。保存 **MUST** 记录操作日志。

站点配置页面 **MUST** 按类别使用 Tab 展示；介绍与协议内容 **MUST** 使用可编辑富文本，支持上传并插入图片。后台图片上传 **MUST** 需要 `cqt:site-config:update` 权限，仅允许 jpg/jpeg/png/gif，单张最大 10 MB，并复用 CQT 文件存储。富文本 HTML **MUST** 原样保存，公开配置接口对富文本键返回原始 HTML，其它文本字段保持既有清洗行为。

富文本编辑器 **MUST** 支持粘贴剪贴板中的图片文件；粘贴图片须先经上述后台上传接口上传，再将返回 URL 插入当前编辑位置。普通文本粘贴 **MUST** 继续使用编辑器默认行为。

#### Scenario: 初始化独立协议配置
- **WHEN** 运行新迁移或重复执行旧库导入脚本，ident 2 至少有一行且 ident 71 尚未初始化
- **THEN** ident 71 初始内容与 ident 2 最大 id 行一致，后续修改任一 ident 不影响另一项
- **判据**:数据库断言两个 ident 的初始 `contents` 相同，更新一个 ident 后另一项内容保持不变；重复运行导入不产生多余行

#### Scenario: 后台保存保留原始内容
- **WHEN** 有 `cqt:site-config:update` 权限的管理员提交包含 HTML 的原始内容
- **THEN** 管理端保存该原始 `contents`；若为富文本键，前台 GET 返回原始 HTML，否则继续执行既有清洗
- **判据**:管理 GET 返回原始标签；公开 GET 对富文本保留 HTML、对普通文本去标签，且操作日志记录该次保存

#### Scenario: 富文本中插入已上传图片
- **WHEN** 有更新权限的管理员在富文本编辑器上传 jpg/jpeg/png/gif 图片并保存配置
- **THEN** 图片经后台受权限保护的上传接口写入当前 CQT 文件存储，编辑器以图片 URL 保存 HTML
- **AND** 公开配置接口对富文本字段返回包含该图片的 HTML
- **判据**:后台上传无权限时被拒绝；有权限时返回文件 URL；保存后公开 GET 保留图片标签和 URL

#### Scenario: 按类别切换配置 Tab
- **WHEN** 管理员查看站点配置页面
- **THEN** 配置分为站点介绍、协议与服务、联系方式、首页素材与证书四个 Tab
- **AND** 切换 Tab 后各字段仍保留表单值并可全量保存
- **判据**:前端测试验证类别 Tab 切换与跨 Tab 全量提交

#### Scenario: 粘贴图片上传并插入
- **WHEN** 管理员将剪贴板中的图片粘贴到富文本编辑器
- **THEN** 编辑器使用权限受保护的图片上传接口保存图片，并在当前光标处插入返回的图片 URL
- **AND** 非图片文本粘贴继续由编辑器默认处理
- **判据**:前端测试断言粘贴图片触发上传并渲染返回 URL，普通文本粘贴不触发图片上传

#### Scenario: 未授权用户不能管理站点配置
- **WHEN** 未登录或缺少对应 `cqt:site-config:list` / `cqt:site-config:update` 权限的用户调用管理读写接口
- **THEN** 请求分别返回认证失败或权限拒绝，数据库内容不变
- **判据**:HTTP 集成测试断言未授权状态码及 `cqt_setting.contents` 未改变
