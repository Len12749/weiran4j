## ADDED Requirements

### Requirement: [FR-004] 学生注册使用行政区划树

学生注册、资料维护和作品报名页 **MUST** 使用 `GET /api/regions/lookup` 的启用省市县树作为地区选择数据，并将选中的省级 `region.id` 写入地区字段。旧赛区表及旧地区数据不再使用或迁移。

#### Scenario: 注册选择地区
- **WHEN** 匿名学生选择省、市、县路径并提交注册
- **THEN** 选择器显示行政区划全路径，账号地区保存对应省级 `region.id`
- **判据**: 未登录状态可获取 lookup 树；选择省份后注册请求 `cities` 等于所选节点的 `id`
