package com.wulicode.weiran.automation.infrastructure.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wulicode.weiran.automation.domain.task.AutomationReadiness;
import com.wulicode.weiran.automation.infrastructure.agent.KnowledgeReviewAgent;
import com.wulicode.weiran.automation.infrastructure.agent.McpKnowledgeClient;
import com.wulicode.weiran.automation.infrastructure.agent.OpenAiCompatibleClient;
import com.wulicode.weiran.automation.infrastructure.cloud.GitCodeRepository;
import com.wulicode.weiran.automation.infrastructure.cloud.YunxiaoChangeRequestClient;
import com.wulicode.weiran.automation.infrastructure.persistence.MybatisAutomationRunRepository;
import com.wulicode.weiran.automation.infrastructure.persistence.MybatisAutomationTaskRepository;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** weiran-automation 基础设施自动配置。 */
@AutoConfiguration
@EnableConfigurationProperties(AutomationProperties.class)
@MapperScan("com.wulicode.weiran.automation.infrastructure.persistence.mapper")
@Import({MybatisAutomationTaskRepository.class, MybatisAutomationRunRepository.class})
public class AutomationInfrastructureAutoConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);

    /** 外部 HTTP 调用共用的客户端。 */
    @Bean
    @ConditionalOnMissingBean(name = "automationHttpClient")
    public HttpClient automationHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(AutomationInfrastructureAutoConfiguration.CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** 云效 OpenAPI 客户端。 */
    @Bean
    public YunxiaoChangeRequestClient automationChangeRequestClient(
            final AutomationProperties properties,
            final HttpClient automationHttpClient,
            final ObjectMapper objectMapper) {
        return new YunxiaoChangeRequestClient(properties, automationHttpClient, objectMapper);
    }

    /** 知识库 MCP 客户端。 */
    @Bean
    public McpKnowledgeClient automationMcpKnowledgeClient(
            final AutomationProperties properties,
            final HttpClient automationHttpClient,
            final ObjectMapper objectMapper) {
        return new McpKnowledgeClient(properties, automationHttpClient, objectMapper);
    }

    /** 模型客户端。 */
    @Bean
    public OpenAiCompatibleClient automationOpenAiClient(
            final AutomationProperties properties,
            final HttpClient automationHttpClient,
            final ObjectMapper objectMapper) {
        return new OpenAiCompatibleClient(properties, automationHttpClient, objectMapper);
    }

    /** 评审 Agent：工具并发 8 线。 */
    @Bean
    public KnowledgeReviewAgent automationReviewAgent(
            final McpKnowledgeClient mcpKnowledgeClient,
            final OpenAiCompatibleClient openAiCompatibleClient,
            final AutomationProperties properties,
            final ObjectMapper objectMapper,
            @Qualifier("automationToolExecutor") final ExecutorService toolExecutor) {
        return new KnowledgeReviewAgent(
                mcpKnowledgeClient, openAiCompatibleClient, properties, objectMapper, toolExecutor);
    }

    /** 本地代码仓库同步。 */
    @Bean
    public GitCodeRepository automationCodeRepository(final AutomationProperties properties) {
        return new GitCodeRepository(properties);
    }

    /** 执行前配置检查。 */
    @Bean
    public AutomationReadiness automationReadiness(final AutomationProperties properties) {
        return new DefaultAutomationReadiness(properties);
    }
}
