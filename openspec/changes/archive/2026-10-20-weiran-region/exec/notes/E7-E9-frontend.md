# E7–E9 前端实施记录

## 实施内容

- `web/src/types/api.ts`：同步 `RegionLevel`、`RegionView`、`RegionNode`、筛选查询和写入请求类型。
- `web/src/hooks/queries/regions.ts`：添加树、平铺、详情、登录态 lookup、增改删 query/mutation；写入成功后失效地区查询缓存。CSV 下载沿用同源 Cookie 会话并按当前筛选下载 Blob。
- `web/src/pages/system/regions/RegionsPage.tsx` 与 `RegionFormModal.tsx`：实现关键字/级别/状态筛选、已生效筛选标签、默认折叠的地区树、ID 首列与同级 ID 倒序、弹性名称列、列设置、展开折叠、按权限显示增改删/启停/导出操作。
- 表单的区划代码限制为 2–12 位数字，地区名称最多 64 字符；编辑时区划代码禁改，与迁移及领域校验一致。
- `web/src/components/RegionSelect.tsx`：封装登录态 lookup Cascader，过滤停用节点及其子树，受控值与回调值均为区划代码路径；支持在省、市、县任一级结束选择。
- `openspec/rules/advisory/components.md`：登记可复用的 `RegionSelect`。
- 前端测试覆盖地区树排序/折叠、筛选、状态更新、权限入口、CSV 筛选下载、启用路径过滤和 code path 回调。

## 验证

- `pnpm --filter @weiran/web exec vitest run src/pages/system/regions/__tests__/RegionsPage.test.tsx src/components/__tests__/RegionSelect.test.tsx`：通过，2 个测试文件、9 个用例。
- `pnpm --filter @weiran/web lint`：通过。
- `pnpm --filter @weiran/web build`：通过（TypeScript 与 Vite build）。

## 范围确认

未修改 `web/.env.development`；前端实现未改动 API 契约或后端文件。没有提交变更。
