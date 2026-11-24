package com.wulicode.weiran.automation.infrastructure.autoconfigure;

import com.wulicode.weiran.automation.domain.task.AutomationReadiness;
import java.util.ArrayList;
import java.util.List;

/** 汇总外部依赖缺失项，供执行前检查使用。 */
public class DefaultAutomationReadiness implements AutomationReadiness {

    private final AutomationProperties properties;

    /** 构造就绪检查。 */
    public DefaultAutomationReadiness(final AutomationProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<String> missingItems() {
        final List<String> missing = new ArrayList<>();
        if (DefaultAutomationReadiness.blank(this.properties.cloud().endpoint())) {
            missing.add("云效服务接入点（WEIRAN_AUTOMATION_CLOUD_ENDPOINT）");
        }
        if (DefaultAutomationReadiness.blank(this.properties.cloud().token())) {
            missing.add("云效个人访问令牌（WEIRAN_AUTOMATION_CLOUD_TOKEN）");
        }
        if (DefaultAutomationReadiness.blank(this.properties.review().mcpUrl())
                || DefaultAutomationReadiness.blank(this.properties.review().mcpToken())) {
            missing.add("知识库 MCP 地址或令牌（WEIRAN_AUTOMATION_MCP_URL / WEIRAN_AUTOMATION_MCP_TOKEN）");
        }
        if (DefaultAutomationReadiness.blank(this.properties.review().llmBaseUrl())) {
            missing.add("模型接口地址（WEIRAN_AUTOMATION_LLM_BASE_URL）");
        }
        if (DefaultAutomationReadiness.blank(this.properties.review().llmApiKey())) {
            missing.add("模型密钥（WEIRAN_AUTOMATION_LLM_API_KEY）");
        }
        if (DefaultAutomationReadiness.blank(this.properties.review().llmModel())) {
            missing.add("模型名称（WEIRAN_AUTOMATION_LLM_MODEL）");
        }
        if (DefaultAutomationReadiness.blank(this.properties.repository().root())) {
            missing.add("本地代码仓库目录（WEIRAN_AUTOMATION_REPO_ROOT）");
        }
        return missing;
    }

    private static boolean blank(final String value) {
        return value == null || value.isBlank();
    }
}
