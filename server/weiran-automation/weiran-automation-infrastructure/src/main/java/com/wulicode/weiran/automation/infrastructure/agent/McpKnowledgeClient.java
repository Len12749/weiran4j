package com.wulicode.weiran.automation.infrastructure.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wulicode.weiran.automation.infrastructure.autoconfigure.AutomationProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * WeKnora MCP 客户端。
 *
 * <p>MCP 端点是 Streamable HTTP 无状态模式：直接 POST {@code tools/call} 即可，不需要 initialize 与会话。
 * 响应可能是一次性 JSON，也可能是 SSE（{@code text/event-stream}），两种都解析。
 */
public class McpKnowledgeClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final AutomationProperties.Review properties;

    private final HttpClient client;

    private final ObjectMapper mapper;

    /** 构造 MCP 客户端。 */
    public McpKnowledgeClient(
            final AutomationProperties properties, final HttpClient client, final ObjectMapper mapper) {
        this.properties = properties.review();
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * 调用 {@code search_knowledge} 工具，返回工具结果的文本内容。
     *
     * @param query 检索词（由调用方拼接的规范类问题）
     */
    public String search(final String query, final int limit) {
        final Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("query", query);
        arguments.put("mode", "hybrid");
        arguments.put("limit", limit);
        final List<String> knowledgeBases = McpKnowledgeClient.splitIds(this.properties.knowledgeBaseIds());
        if (!knowledgeBases.isEmpty()) {
            arguments.put("knowledge_base_ids", knowledgeBases);
        }
        final JsonNode response = this.call("search_knowledge", arguments);
        final JsonNode error = response.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new IllegalStateException("知识库检索失败：" + error.path("message").asText("未知错误"));
        }
        final JsonNode content = response.path("result").path("content");
        if (content.isArray()) {
            for (final JsonNode item : content) {
                if ("text".equals(item.path("type").asText())
                        && !item.path("text").asText().isBlank()) {
                    return item.path("text").asText();
                }
            }
        }
        final JsonNode structured = response.path("result").path("structuredContent");
        return structured.isMissingNode() || structured.isNull() ? "" : structured.toString();
    }

    private JsonNode call(final String tool, final Map<String, Object> arguments) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("id", 1);
        payload.put("method", "tools/call");
        payload.put("params", Map.of("name", tool, "arguments", arguments));
        final HttpRequest request = HttpRequest.newBuilder(
                        URI.create(this.properties.mcpUrl().strip()))
                .timeout(McpKnowledgeClient.TIMEOUT)
                .header("Authorization", "Bearer " + this.properties.mcpToken().strip())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(this.write(payload), StandardCharsets.UTF_8))
                .build();
        try {
            final HttpResponse<String> response =
                    this.client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("知识库 MCP 返回 HTTP " + response.statusCode());
            }
            return this.mapper.readTree(McpKnowledgeClient.unwrapSse(response.body()));
        } catch (final IOException ex) {
            throw new IllegalStateException("知识库 MCP 响应解析失败：" + ex.getMessage(), ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("知识库 MCP 调用被中断", ex);
        }
    }

    /** 从 SSE 响应里取最后一个 data 帧；普通 JSON 原样返回。 */
    private static String unwrapSse(final @Nullable String body) {
        if (body == null || body.isBlank()) {
            return "{}";
        }
        final String trimmed = body.stripLeading();
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        String last = null;
        for (final String line : body.split("\n")) {
            final String candidate = line.strip();
            if (candidate.startsWith("data:")) {
                final String data = candidate.substring("data:".length()).strip();
                if (!data.isEmpty() && !"[DONE]".equals(data)) {
                    last = data;
                }
            }
        }
        return last == null ? "{}" : last;
    }

    private String write(final Object payload) {
        try {
            return this.mapper.writeValueAsString(payload);
        } catch (final IOException ex) {
            throw new IllegalStateException("知识库请求序列化失败：" + ex.getMessage(), ex);
        }
    }

    private static List<String> splitIds(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        final List<String> result = new ArrayList<>();
        for (final String part : value.split(",")) {
            if (!part.isBlank()) {
                result.add(part.strip());
            }
        }
        return result;
    }
}
