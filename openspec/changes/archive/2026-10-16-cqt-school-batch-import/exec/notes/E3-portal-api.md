# E3 收尾：Portal API

- 保持 uniapp 使用的 `/api-web/**` 路由、HTTP 200 与 portal 响应字段。
- 学校账号归属来自当前 Portal token；客户端 `userid` 不用于授权。
- 任务查询按当前账号过滤；真实集成用例验证另一所学校读取任务得到 404。
