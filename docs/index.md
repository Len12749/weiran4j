---
layout: home

hero:
  name: weiran4j
  text: 管理后台基础框架
  tagline: Java 21 后端 · React 管理端 · 模块化架构
  actions:
    - theme: brand
      text: 开始使用
      link: /usage
    - theme: alt
      text: 查看架构
      link: /architecture

features:
  - title: 快速启动
    details: 准备 JDK、MySQL 和本地配置后，一条命令启动前后端与文档站。
  - title: 清晰分层
    details: 后端按框架、system 基座与业务模块组织，业务模块采用五层结构。
  - title: 统一管理
    details: 用户、角色、菜单、部门、字典、系统配置与地区管理由统一后台承载。
---

# 项目文档

- [使用指南](/usage)：本地环境、启动方式、常用命令和部署入口。
- [架构说明](/architecture)：仓库结构、前后端关系、后端分层和请求流程。
- [系统配置](/configuration)：分组 Tab、动态表单字段、校验与配置键前缀。

数据库、错误码与跨接口业务规则见 `weiran4j/docs/01-架构与接口契约.md`；HTTP 接口定义以 Springdoc 生成的 OpenAPI 为准。生产部署步骤见 `weiran4j/docs/02-部署.md`。
