# sys_security_settings 安全设置

> 属于 `openspec/state/`，只描述当前实现；与代码不一致时以代码为准。

事实源：`SecuritySettingsController`、`SecuritySettingsApplicationService`、`MfaLoginApplicationService`、`JdbcSecuritySettingsRepository`、`SecuritySettingsPage` 与 `V202610272100__system_security_settings.sql`。

## 0. 概要

| 项 | 值 |
| --- | --- |
| 表 | `sys_security_settings`，单例 `id=1` |
| 入口 | 顶栏用户菜单 › 安全设置，位于个人中心与偏好设置之间；隐藏菜单节点 id 23 保留 `/system/security` 路由和权限 |
| 权限 | `system:security:list`、`system:security:update` |
| 用途 | 全局密码规则、手机多因素认证与设备免验期限 |

## 1. 列表

无列表。页面按「密码策略」「多因素认证」两组展示当前单例设置。

## 2. 字段与表单

| 字段 | 当前口径 |
| --- | --- |
| `password_min_length` / `password_max_length` | 8–64，最小值不得大于最大值；默认 8–64 |
| `require_lower` / `require_upper` / `require_digit` / `require_symbol` | 所选类型均为新密码必需；至少选择一种；默认小写字母与数字 |
| `mfa_enabled` | 默认关闭；启用须有短信发送器与验证码模板 |
| `mfa_remember_days` | 0–30 天；0 表示每次登录均验证，默认 7 天 |
| `revision` | 设置每次保存时递增；用于撤销旧设备信任凭据 |
| `updated_at` | 数据库自动更新时间 |

## 3. 动作

- 查看设置须有 `system:security:list`；修改密码规则或多因素认证须有 `system:security:update`，并记操作日志。
- 新建用户、管理员重置密码、本人改密时按当前密码规则校验；已有密码不追溯。
- 开启多因素认证后，已绑定手机号的密码及外部身份登录先发送短信、验证成功后交付会话。未绑定手机号的账号暂时免验。
- 同一浏览器验证后可通过签名 HttpOnly Cookie 在有效期内免验；设置修订号或用户令牌版本变化使旧凭据失效。

## 4. 用到的公共组件

`PageContainer`、`Permission`、Semi UI `SideSheet`、`Checkbox`、`InputNumber`。

## 5. 说明与建议

短信挑战与发送间隔限制保存在单实例内存；进程重启会使未完成的验证失效，多实例部署需要共享存储。验证码有效期 5 分钟，最多尝试 5 次。

## 6. 已知问题汇总

暂无。

## 7. changelog

**2026-10-27**

- 安全设置入口移至顶栏用户菜单；菜单节点隐藏并移为顶级，保留路由与权限。
- 新增全局安全设置、密码策略与短信多因素认证。
