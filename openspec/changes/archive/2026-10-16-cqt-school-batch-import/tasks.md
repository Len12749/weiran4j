---
title: "CQT 学校作品批量导入实施任务"
status: "draft"
updated_at: "2026-10-16"
---

# Tasks

## 2. 领域层 `weiran-cqt-api` / `weiran-cqt-domain`(TG-2)

- [x] 2.1 定义学校导入会话、任务、逐行结果、导入行与作品组校验模型及端口契约，不暴露框架/POI/MyBatis 类型(cqt-entry-signup/FR-007)
- [x] 2.2 为按模板表头映射、个人行解析、连续 N 行团体分组、赛项/专业/作品名/人数一致性及省赛/国赛列忽略建立领域规则(cqt-entry-signup/FR-007)

## 3. 应用与基础设施层 `weiran-cqt-application` / `weiran-cqt-infrastructure`(TG-3)

- [x] 3.1 实现学校批量作品导入用例，逐作品组复用报名校验和事务写入，组失败后继续，防止重试重复写入(cqt-entry-signup/FR-007)
- [x] 3.2 新增 CQT 任务与逐行结果迁移、仓储实现、学校/赛事查询和可恢复 worker(cqt-entry-signup/FR-007)
- [x] 3.3 实现 Excel 表头解析、附件 URL/ZIP 文件名匹配、受限文件读取及 ZIP 路径/条目数/展开大小限制(cqt-entry-signup/FR-007)
- [x] 3.4 实现学校作品列表、模板链接和导入任务状态/计数/行结果查询用例，按当前学校隔离(cqt-entry-signup/FR-007)

## 4. 适配层 `weiran-cqt-adapter`(TG-4)

- [x] 4.1 实现 uniapp 实际调用的学校模板、待处理检查、作品列表、上传登记、确认导入、任务列表和任务详情 portal API；忽略请求 userid(cqt-entry-signup/FR-007)

## 5. 前端 `uniapp`

- [x] 5.1 对齐既有导入 API 消费结构，修正成功/失败记录中学校和联系电话列映射，不显示或处理奖项列(cqt-entry-signup/FR-007)

## 6. 测试

- [x] 6.1 覆盖模板字段、个人/团体分组与错误恢复、奖项忽略、附件匹配及 ZIP 安全边界(cqt-entry-signup/FR-007)
- [x] 6.2 以应用/Controller/仓储单测及 portal 边界集成测试覆盖学校身份校验、任务归属逻辑、uniapp 响应结构、重复确认与租约恢复(cqt-entry-signup/FR-007)
- [x] 6.3 使用示例工作簿验证表头映射、行号和样例作品结果，并确认不产生省赛/国赛评价数据(cqt-entry-signup/FR-007)
- [x] 6.4 运行 uniapp build、JDK 21 `spotlessApply` 与后端 `check`，保存 OpenSpec 要求的门禁证据(cqt-entry-signup/FR-007)
- [x] 6.5 补真实学校账号的上传登记→确认→worker→数据库作品/成员/逐源行结果集成验证，并覆盖另一学校无法读取任务及上传会话过期(cqt-entry-signup/FR-007)

