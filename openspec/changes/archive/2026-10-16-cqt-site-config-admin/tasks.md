---
title: "CQT 站点配置后台表单实施任务"
status: "complete"
updated_at: "2026-10-15"
---

# Tasks

## 1. 契约层

- [x] 1.1 更新架构与接口契约文档：登记管理读写 API 与 `user_agreement` ident 71 映射(cqt-site-config/FR-002, FR-004)

## 2. 数据层

- [x] 2.1 新增 CQT 迁移：复制 ident 2 最新行到 ident 71，并添加菜单、按钮权限和角色关联种子(cqt-site-config/FR-004)
- [x] 2.2 修改旧库导入脚本：幂等初始化 ident 71，不覆盖已分离编辑的值(cqt-site-config/FR-004)

## 3. 领域层 `weiran-cqt-domain` / `weiran-cqt-api`

- [x] 3.1 定义配置目录、管理端口和 ident 2/71 独立映射(cqt-site-config/FR-002, FR-004)

## 4. 应用与基础设施层 `weiran-cqt-application` / `weiran-cqt-infrastructure`

- [x] 4.1 实现 raw 配置管理读写用例，更新 ident 最大 id 行、缺行插入(cqt-site-config/FR-004)
- [x] 4.2 实现管理仓储读取和更新，保证更新不创建重复 ident 行(cqt-site-config/FR-004)

## 5. 适配层 `weiran-cqt-adapter`

- [x] 5.1 新增管理 GET/PUT API、list/update 权限校验与写操作日志(cqt-site-config/FR-004)
- [x] 5.2 新增后台图片上传 API，校验图片扩展名/大小并复用 CQT 文件存储(cqt-site-config/FR-004)

## 6. 前端 `web`

- [x] 6.1 新增站点配置 API 类型与 query/mutation(cqt-site-config/FR-004)
- [x] 6.2 新增独立站点配置表单页并与动态菜单 component 配对(cqt-site-config/FR-004)
- [x] 6.3 将配置按类别切换为 Tab 展示(cqt-site-config/FR-004)
- [x] 6.4 接入 Tiptap 富文本编辑与站点图片上传(cqt-site-config/FR-004)
- [x] 6.5 支持粘贴剪贴板图片并复用站点图片上传(cqt-site-config/FR-004)

## 7. 测试

- [x] 7.1 覆盖前台 ident 拆分、raw 内容管理、更新幂等及迁移/导入初始化(cqt-site-config/FR-002, FR-004)
- [x] 7.2 覆盖管理接口权限、操作日志、公开 GET 清洗和前端表单保存(cqt-site-config/FR-004)
- [x] 7.3 覆盖图片上传权限/图片类型、富文本公开 HTML 与 Tab 表单切换(cqt-site-config/FR-004)
- [x] 7.4 覆盖剪贴板图片上传插入及普通文本粘贴行为(cqt-site-config/FR-004)
- [x] 7.5 覆盖管理保存接口对缺失配置项列表及 null 列表项的 400 校验（代码审查发现）(cqt-site-config/FR-004)

## 8. 发布

- [x] 8.1 按 Flyway 迁移发布后确认 ident 71 与菜单权限初始化；旧库导入仅用于初始化，不在独立编辑后重跑(cqt-site-config/FR-004)
