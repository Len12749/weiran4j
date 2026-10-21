# cqt_setting 站点配置（常青藤前台）

> 属于 `openspec/state/`，**只描述事实，不是规范**：过期不会被 `check.mjs` 拦截，**以代码为准**，发现不符回来改这里。
> 下游（weiran-cqt）业务表，索引见 [`README.biz.md`](README.biz.md)。
>
> 事实源：
> [`ProductController.java`](../../../weiran4j/weiran-cqt/weiran-cqt-adapter/src/main/java/com/weiran/cqt/adapter/portal/ProductController.java)、
> [`SiteConfigApplicationService.java`](../../../weiran4j/weiran-cqt/weiran-cqt-application/src/main/java/com/weiran/cqt/application/setting/SiteConfigApplicationService.java)、
> [`SiteConfig.java`](../../../weiran4j/weiran-cqt/weiran-cqt-domain/src/main/java/com/weiran/cqt/domain/setting/SiteConfig.java)、
> [`MybatisSettingRepository.java`](../../../weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/java/com/weiran/cqt/infrastructure/persistence/MybatisSettingRepository.java)、
> [`V202610022200__cqt_setting.sql`](../../../weiran4j/weiran-cqt/weiran-cqt-infrastructure/src/main/resources/db/migration/cqt/V202610022200__cqt_setting.sql)、
> [`scripts/biz/import/cqt_setting.sql`](../../../scripts/biz/import/cqt_setting.sql)。
>
> 盘点基线：change `cqt-web-foundation`（2026-10-02）。

## 0. 概要

| 项 | 值 |
| --- | --- |
| 表 | `cqt_setting`（原统一库 `cqtxj2026.sc_setting`，只改表名，列不变） |
| 菜单 / 页面 | 「站点资料」目录下的「站点配置」（`cqt/site-config`，独立后台表单） |
| 后端模块 | `weiran-cqt`；前台 `ProductController`、后台 `SiteConfigAdminController` 共用站点配置仓储 |
| 接口 | 前台 `GET /api-web/product/getconfig`；后台 `GET /api-backend/cqt/site-config/get、PUT /api-backend/cqt/site-config/update`、`POST /api-backend/cqt/site-config/uploadImage` |
| 权限码 | `cqt:site-config:list`、`cqt:site-config:update` |
| 数据来源 | 原始数据由 `scripts/biz/import/cqt_setting.sql` 导入；Flyway 只追加 ident 71 的一次性拆分行及菜单权限种子 |

## 1. 列表

无后台列表。前台一次取全部配置；后台提供独立站点配置表单，不使用通用参数 CRUD。

## 2. 字段与表单

| 列 | 类型 | 说明 |
| --- | --- | --- |
| `id` | int 自增 PK | 导入时沿用原值 |
| `ident` | int | 配置项编号；前台按编号映射键名（见 §3） |
| `name` | varchar(255) | 配置项名称 |
| `contents` | text | 内容，可能含 HTML |
| `created_at` / `updated_at` / `deleted_at` | timestamp | 原样保留；**读取时不过滤 `deleted_at`**（与原 FastAPI 一致） |
| `key` | varchar(255) | 原样保留，代码未使用 |

## 3. 动作

| 动作 | 接口 | 规则 |
| --- | --- | --- |
| 前台读取 | `GET /api-web/product/getconfig` | 读 `ident` 1–100；返回 22 个键：`guanyuwomen`→2、`user_agreement`→71、`yinsixieyi`→3、`about_us`→4、`dizhi`→7、`shouji`→8、`weixin`→9、`youxiang`→10、`gongsijieshao`→15、`hezuohuoban`→16、`dasaijieshao`→17、`mianzexieyi`→18、`shouhoufuwu`→19、`shangwuhezuo`→20、`gongzuoshijian`→21、`dasaizhangcheng`→22、`gongzhonghao`→23、`shouyeimage`→66、`certificate_visibility`→67、`sheng_certificate_visibility`→68、`teacher_org_certificate_competition_id`→69、`guo_certificate_visibility`→70。富文本键 `guanyuwomen`、`user_agreement`、`yinsixieyi`、`about_us`、`gongsijieshao`、`hezuohuoban`、`dasaijieshao`、`mianzexieyi`、`shouhoufuwu`、`shangwuhezuo`、`dasaizhangcheng` 返回原始 HTML；其它字符串去 HTML 标签与首尾空白；`contents` 为 NULL 或缺行 → `null`；`gongzhonghao` 为数组（有值单元素原值、否则空数组）。同一 `ident` 多行时取 `id` 最大的一行 |
| 后台管理 | `GET /api-backend/cqt/site-config/get、PUT /api-backend/cqt/site-config/update`、`POST /api-backend/cqt/site-config/uploadImage` | 页面按四类 Tab 编辑；富文本支持 HTML 编辑和图片上传。GET 返回固定 22 项及原始 `contents`；PUT 全量提交键与内容，图片上传只接受 jpg/jpeg/png/gif；均需 `cqt:site-config:update`（GET 需 `cqt:site-config:list`）；保存操作记日志，图片二进制不记入请求体 |
| 初始拆分 | Flyway / `scripts/biz/import/cqt_setting.sql` | `user_agreement` 使用 ident 71；从 ident 2 最大 id 行复制初值。重复导入时若 ident 71 已有值则不覆盖，避免抹掉独立编辑 |

uniapp 实际读取的键：`dasaijieshao`、`dasaizhangcheng`、`dizhi`、`gongzhonghao`、`guo_certificate_visibility`、
`sheng_certificate_visibility`、`shouhoufuwu`、`shouji`、`shouyeimage`、`weixin`、`youxiang`、`user_agreement`（10 个页面调用）。

## 4. 用到的公共组件

Semi UI `Form`、`Form.TextArea`、`Tabs`、`PageContainer`、`Permission`；站点介绍与协议字段使用 Tiptap 富文本编辑器。公众号与首页图片使用 `SingleImageUpload`，回显旧地址、原图预览、移除清空；统一跟踪单图及富文本上传，上传与保存互斥。

## 5. 说明与建议

- 接口无缓存，每次查库。
- `cqt_setting` 的列整理（`ident` 与 `key` 的关系、软删除列是否保留）留给后续 change。

## 6. 已知问题汇总

- **#01 ❓ P1 统一库导出文件 `cqtxj2026.sql` 里本表（及 `news` 等）中文为双重编码乱码**
  `cqtxj2026.sql` 中 `sc_setting.name` / `contents` 的字节是「UTF-8 被当成 latin1 再编码一次」的结果
  （如「关于我们」存成 `å…³äºŽæˆ‘ä»¬`），原始库导出 `zhongxi.sql` 同一行是正常 UTF-8；`news` 表同样乱码。
  症状：若生产 `cqtxj2026` 本身就是这样，导入后 `getconfig` 返回的地址、手机号说明、协议正文等全部是乱码，uniapp 原样展示。
  已实测 `CONVERT(CAST(CONVERT(x USING latin1) AS BINARY) USING utf8mb4)` 可还原；但不确定是生产库乱码还是只有这份导出文件乱码，
  误转正常数据会把它弄坏，所以导入脚本**不做转换**（2026-10-02 用户决定）。待确认生产库实际编码后，单开 change 统一处理所有表。

## 7. changelog

新条目插在本节最上方（按日期倒序，新在上）。

**2026-10-18**
- **#05 ✅ P2 配置图片统一单图上传（`cqt-competition-admin` 用户追加）**
  gongzhonghao、shouyeimage不再手填URL；沿用图片上传接口与原contents字段，保留历史回显和空值，保存期间禁止变更图片。

**2026-10-15**
- **#04 ✅ P? change `cqt-site-config-admin` 将配置表单改为分类 Tab，增加 Tiptap 富文本和图片上传**
  后台图片上传复用 CQT 文件存储并校验 jpg/jpeg/png/gif；介绍/协议类字段公开返回原始 HTML，供 uniapp 富文本组件呈现。
- **#03 ✅ P? change `cqt-site-config-admin` 增加独立后台表单并拆分用户协议配置**
  `guanyuwomen` 继续读取 ident 2，`user_agreement` 改读 ident 71；后台保存 raw contents，公开接口仍执行既有清洗。

**2026-10-02**
- **#02 ✅ P? change `cqt-web-foundation` 建立本表与本文件**
  Flyway 建 `cqt_setting`、导入脚本、`GET /api-web/product/getconfig`。
