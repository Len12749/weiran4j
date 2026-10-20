## ADDED Requirements

### Requirement: [FR-005] 图片资产删除

具备 `system:media-library:delete` 权限的管理员 **MUST** 能通过 `DELETE /api/system/media-library/{id}` 删除登记图片。图片删除不依赖站点配置引用检查；文件对象删除 **MUST** 在数据库事务提交后执行。重复删除对象应安全，确保外部存储错误或数据库提交失败时资产记录仍可重试处理。

#### Scenario: 删除图片
- **WHEN** 管理员有删除权限且目标图片已登记
- **THEN** 系统删除存储对象和媒体资产记录
- **判据**:测试断言对象不可访问、媒体查询不再返回该记录，操作日志包含删除事件

#### Scenario: 未授权删除
- **WHEN** 用户缺少 `system:media-library:delete` 权限或请求的媒体 id 不存在
- **THEN** 请求被拒绝或返回 not-found，文件存储与数据库内容不变
- **判据**:集成测试断言权限/错误响应以及文件和数据库状态

## REMOVED Requirements

### Requirement: [FR-004] 图片删除与引用保护
**Reason**: 系统站点配置能力及 `sys_site_config` 表被移除，媒体库不再有此内部引用源；保留站点配置引用检查会让图片删除继续依赖已删除的子系统。
**Migration**: 已有部署通过迁移删除 `sys_site_config`；图片资产及删除 API 保留，后续删除语义由 FR-005 接管。

#### Scenario: 删除未引用的图片
- **WHEN** 图片 URL 未出现在站点配置内容中，且管理员有删除权限
- **THEN** 系统删除存储对象和媒体资产记录
- **判据**:测试断言对象不可访问、媒体查询不再返回该记录，操作日志包含删除事件

#### Scenario: 拒绝删除仍被站点配置引用的图片
- **WHEN** 站点配置原始 HTML 仍包含目标图片 URL
- **THEN** 系统返回冲突错误且保留存储对象与媒体资产记录
- **判据**:集成测试断言删除被拒绝且文件和记录仍存在

#### Scenario: 未授权删除
- **WHEN** 用户缺少 `system:media-library:delete` 权限或请求的媒体 id 不存在
- **THEN** 请求被拒绝或返回 not-found，文件存储与数据库内容不变
- **判据**:集成测试断言权限/错误响应以及文件和数据库状态
