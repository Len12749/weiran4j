---
status: "superseded"
superseded_by: "administrative-regions"
---

# cqt-region Specification

## Purpose

本规格记录常青藤曾经使用独立赛区表的历史要求。该能力已被 `administrative-regions` 取代，以下需求仅用于历史归档追溯，不再约束当前实现。新系统统一使用 `region.id`，不迁移旧赛区数据。

## Historical Requirements (superseded)

### Requirement: [FR-001] 赛区存储

历史实现将赛区存放在表 `cqt_regions`，列与原统一库 `cqtxj2026.regions` 一致：`id`、`legacy_id`（唯一）、`parent_legacy_id`、`name`、`code`。对外（接口、账号的 `cities` 字段）使用 `legacy_id` 作为赛区 ID，数据通过独立导入脚本迁入。此要求已废止。

#### Scenario: 导入脚本从原库迁入（历史）
- **WHEN** 在同一 MySQL 实例上同时存在 `cqtxj2026` 与目标库时执行 `scripts/biz/import/cqt_regions.sql` 两次
- **THEN** 历史 `cqt_regions` 行数与 `cqtxj2026.regions` 一致
- **判据**: 执行后两表 `COUNT(*)` 相等，第二次执行后计数不变

### Requirement: [FR-002] 前台赛区列表

历史 `GET` 或 `POST /api-web/competcategory/regions` 免登录，返回 `data` 为数组，每项 `{"id":<legacy_id>,"pid":<parent_legacy_id>,"name":<名称>,"code":<编码或 null>}`，按 `parent_legacy_id`、`id` 升序。此要求已废止。

#### Scenario: 返回赛区列表（历史）
- **WHEN** `cqt_regions` 有 `legacy_id=15,parent_legacy_id=0,name=河北` 一行时请求赛区列表
- **THEN** `data` 中含 `{"id":15,"pid":0,"name":"河北",...}`
- **判据**: 集成测试断言该项存在且字段名为 `id`/`pid`/`name`/`code`

### Requirement: [FR-004] 学生注册使用行政区划树

学生注册、资料维护和作品报名页 **MUST** 使用 `GET /api/regions/lookup` 的启用省市县树作为地区选择数据，并将选中的省级 `region.id` 写入地区字段。旧赛区表及旧地区数据不再使用或迁移。该要求已由 `administrative-regions` 接管，仅在此保留归档追溯。

#### Scenario: 注册选择地区
- **WHEN** 匿名学生选择省、市、县路径并提交注册
- **THEN** 选择器显示行政区划全路径，账号地区保存对应省级 `region.id`
- **判据**: 未登录状态可获取 lookup 树；选择省份后注册请求 `cities` 等于所选节点的 `id`
