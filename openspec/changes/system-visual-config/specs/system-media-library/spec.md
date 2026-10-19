## MODIFIED Requirements

### Requirement: [FR-004] 图片删除与引用保护

具备 `system:media-library:delete` 权限的管理员 **MUST** 能通过 `DELETE /api/system/media-library/{id}` 删除登记图片。文件对象删除 **MUST** 在数据库事务提交后执行；重复删除对象应安全，确保外部存储错误或数据库提交失败时资产记录仍可重试处理。

#### Scenario: 删除图片
- **WHEN** 管理员有删除权限且目标图片已登记
- **THEN** 系统删除存储对象和媒体资产记录
- **判据**:测试断言对象不可访问、媒体查询不再返回该记录，操作日志包含删除事件

#### Scenario: 未授权删除
- **WHEN** 用户缺少 `system:media-library:delete` 权限或请求的媒体 id 不存在
- **THEN** 请求被拒绝或返回 not-found，文件存储与数据库内容不变
- **判据**:集成测试断言权限/错误响应以及文件和数据库状态
