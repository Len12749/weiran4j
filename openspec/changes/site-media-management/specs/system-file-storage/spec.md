## ADDED Requirements

### Requirement: [FR-001] 系统文件存储

存储后端由 system 存储配置选择：`disabled`（默认）时上传 **MUST** 返回统一服务器错误「文件存储未配置」；
`oss` 时 **MUST** 以流式方式写入阿里云 OSS（按文件长度传入输入流，不把整个文件读入内存），`url` 为配置的公网访问前缀加对象名；
`local` 时写入配置的本地目录并经 `/system-uploads/**` 提供访问（仅供开发与测试），使用独立前缀标识 system 托管的文件。
`oss` 模式缺少 AccessKey ID、AccessKey Secret、Bucket、Endpoint、公网访问前缀任一项时应用 **MUST** 启动失败，异常信息列出缺少的环境变量名，**MUST NOT** 含 Secret 的值。
写入失败时 **MUST** 返回统一服务器错误「图片上传失败，请稍后重试」，日志记对象名与异常类名，**MUST NOT** 记凭据。
存储后端 **MUST** 将对象名限制在图片库生成的 `media/` 路径格式内。
存储后端 **MUST** 支持按对象名删除文件；对象名必须匹配图片库生成的对象格式且不得越出本地根目录。删除成功后对应对象不可再通过存储访问；对象不存在时删除可安全重复调用。

#### Scenario: 存储未配置
- **WHEN** `mode=disabled` 时上传
- **THEN** 返回统一服务器错误，提示「文件存储未配置」
- **判据**:应用服务单测断言

#### Scenario: OSS 流式写入
- **WHEN** `mode=oss`，上传 12 字节文件
- **THEN** OSS 客户端收到的调用为「Bucket、对象名、输入流、内容长度 12」，返回地址为 `<公网前缀>/<对象名>`
- **判据**:基础设施单测（假 OSS 客户端）断言参数与返回值

#### Scenario: OSS 配置不全与写入失败
- **WHEN** `mode=oss` 缺 Bucket；或 OSS 客户端写入时抛出异常
- **THEN** 前者构造时抛异常且信息列出缺失配置项、不含 Secret；后者上传返回统一服务器错误「图片上传失败，请稍后重试」
- **判据**:基础设施单测断言配置异常与存储失败异常；应用服务单测断言存储失败映射为统一错误响应

#### Scenario: 判定地址是否由本系统存储产生
- **WHEN** 公网前缀为 `https://cdn.example.com`，分别判定 `https://cdn.example.com/media/user_files/1/202610/a.zip`、`https://cdn.example.com.evil.com/a.zip`、`https://other.com/a.zip`
- **THEN** 依次为是、否、否
- **判据**:基础设施单测（OSS 与本地两种实现各断言）

#### Scenario: 安全删除对象
- **WHEN** 媒体库删除本系统登记的图片对象，或尝试删除前缀外的对象名
- **THEN** 本系统对象可被删除且重复删除安全；前缀外对象被拒绝，不能删除根目录外文件或其他 OSS 对象
- **判据**:本地与 OSS 基础设施测试分别断言对象删除及越界对象被拒绝
