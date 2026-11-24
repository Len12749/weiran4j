package com.wulicode.weiran.automation.infrastructure.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自动化任务外部依赖配置。
 *
 * <p>密钥一律走环境变量或本地 profile 文件，仓库里只保留空默认值（CP-9）。
 *
 * @param enabled 总开关；关闭后不再产生任何执行
 * @param cloud 云效 OpenAPI
 * @param review 知识库与模型
 * @param repository 本地代码仓库
 */
@ConfigurationProperties(prefix = "weiran.automation")
public record AutomationProperties(boolean enabled, Cloud cloud, Review review, Repository repository) {

    /**
     * 云效 OpenAPI。
     *
     * @param endpoint 服务接入点，例如 {@code https://openapi-rdc.aliyuncs.com}
     * @param token 个人访问令牌（x-yunxiao-token），只用于 OpenAPI，不等于 Git 的 HTTPS 密码
     * @param organizationId 组织 ID；中心版必填，Region 版留空
     * @param pageSize 每页大小
     * @param maxPages 最多翻页数，避免接口异常时无限翻页
     */
    public record Cloud(String endpoint, String token, String organizationId, int pageSize, int maxPages) {}

    /**
     * 知识库与模型。
     *
     * @param mcpUrl WeKnora MCP 端点完整地址（含 {@code /mcp/<endpointId>}）
     * @param mcpToken MCP 端点令牌，形如 {@code mcp_...}
     * @param knowledgeBaseIds 知识库 ID，逗号分隔；留空表示端点可见的全部知识库
     * @param llmBaseUrl OpenAI 兼容接口地址
     * @param llmApiKey 模型密钥
     * @param llmModel 模型名
     * @param maxFileChars 单个文件送入模型的 diff 字符上限
     * @param retrieveLimit 知识库检索条数
     * @param allowPaths 允许评审的代码库路径白名单（形如 qyd/lianmeng/kr/web），逗号分隔；留空表示不过滤
     */
    public record Review(
            String mcpUrl,
            String mcpToken,
            String knowledgeBaseIds,
            String llmBaseUrl,
            String llmApiKey,
            String llmModel,
            int maxFileChars,
            int retrieveLimit,
            String allowPaths) {}

    /**
     * 本地代码仓库。
     *
     * @param root 克隆根目录，每个代码库一个子目录
     * @param baseUrl Git 服务地址，用来把代码库路径转成 HTTPS 地址
     * @param timeoutSeconds 单次 git 命令超时
     * @param deepClone 是否完整克隆（保留全部历史）；默认完整克隆，便于比较任意分支
     */
    public record Repository(String root, String baseUrl, int timeoutSeconds, boolean deepClone) {}
}
