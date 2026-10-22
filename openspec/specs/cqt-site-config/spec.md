---
status: "active"
---

# cqt-site-config Specification

## Purpose

常青藤站点文案、联系方式、协议正文和证书展示配置由基座系统配置管理；uniapp 仍通过 `GET /api-web/product/getconfig` 读取稳定的 22 键响应。

## Requirements

### Requirement: [FR-001] 站点配置存储

站点配置 **MUST** 存放在 `sys_config` 的 `cqt_site` 分组，配置键为 `system.cqt_site.<前台键名>`。Flyway **MUST** 取旧 `cqt_setting` 中每个 ident 最大 id 行的原始内容，生成 22 个配置项后删除旧表。旧库导入脚本 **MUST** 直接写入系统配置，不再重建旧表。已发布的旧 Flyway 脚本 **MUST NOT** 修改。

#### Scenario: 空库完成迁移
- **WHEN** 应用在空库上完成 Flyway 迁移
- **THEN** `sys_config` 存在 22 个 `cqt_site` 配置项，且不存在 `cqt_setting` 表
- **判据**:集成测试检查配置项数量和 `information_schema.tables`

#### Scenario: 已有数据完成迁移
- **WHEN** 同一 ident 有多行或值含 HTML
- **THEN** 系统配置保存 id 最大行的完整原始值，富文本不被 4096 字符限制截断
- **判据**:迁移测试核对最新行及长富文本内容

### Requirement: [FR-002] 前台站点配置接口

`GET /api-web/product/getconfig` **MUST** 免登录并返回既有 22 个键。富文本字段返回原始 HTML；其它文本去除 HTML 标签并去掉首尾空白；`gongzhonghao` 有值时返回单元素原值数组，否则返回空数组。系统配置值为空时，普通键返回 `null`。

#### Scenario: 后台修改后前台立即读取
- **WHEN** 管理员通过系统配置页面更新 `system.cqt_site.guanyuwomen`
- **THEN** 前台 `guanyuwomen` 返回新 HTML，其他 21 个键的名称与形状不变
- **判据**:HTTP 集成测试通过系统配置 API 更新，再断言前台响应

### Requirement: [FR-003] 站点配置清洗规则

富文本字段返回原始 HTML；普通文本去除 HTML 标签并去掉首尾空白；`gongzhonghao` 有值时返回单元素原值数组，否则返回空数组。系统配置值为空时，普通键返回 `null`。

#### Scenario: 保留前台取值形状
- **WHEN** 配置项含 HTML、空值或公众号图片地址
- **THEN** 各字段按上述规则转换
- **判据**:单元测试和 HTTP 集成测试断言响应值

### Requirement: [FR-004] 站点配置后台表单

后台 **MUST** 在“系统配置”页面的“常青藤站点”分组展示并编辑 22 项。富文本和单图字段使用系统配置组件及系统媒体上传；旧 CQT 站点配置页面、管理接口、菜单与权限 **MUST** 移除。原有授权角色 **MUST** 获得相应的系统配置查看、修改和图片上传权限。

#### Scenario: 站点配置进入系统配置
- **WHEN** 有原站点配置权限的管理员登录
- **THEN** 可进入系统配置的常青藤站点分组编辑配置并上传图片
- **判据**:迁移脚本核对菜单和角色权限，前端使用动态系统配置表单
