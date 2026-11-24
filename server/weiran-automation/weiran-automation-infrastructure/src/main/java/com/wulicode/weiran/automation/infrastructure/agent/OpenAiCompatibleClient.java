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
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** OpenAI 兼容的对话补全客户端（默认接 DeepSeek）。 */
public class OpenAiCompatibleClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(180);

    private static final double TEMPERATURE = 0.2;

    private final AutomationProperties.Review properties;

    private final HttpClient client;

    private final ObjectMapper mapper;

    /** 构造客户端。 */
    public OpenAiCompatibleClient(
            final AutomationProperties properties, final HttpClient client, final ObjectMapper mapper) {
        this.properties = properties.review();
        this.client = client;
        this.mapper = mapper;
    }

    /** 一次工具调用请求。 */
    public record ToolCall(String id, String name, String arguments) {}

    /** 一次模型响应：要么是正文，要么是要执行的工具调用。 */
    public record ChatResult(@Nullable String content, List<ToolCall> toolCalls) {

        /** 防御性复制，避免调用方持有的可变列表影响结果。 */
        public ChatResult {
            toolCalls = List.copyOf(toolCalls);
        }
    }

    /**
     * 带工具的对话补全。
     *
     * @param messages OpenAI 消息数组（system / user / assistant / tool）
     * @param tools 工具定义；为空表示不再提供工具
     */
    public ChatResult chatWithTools(final List<Map<String, Object>> messages, final List<Map<String, Object>> tools) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", this.properties.llmModel().strip());
        payload.put("temperature", OpenAiCompatibleClient.TEMPERATURE);
        payload.put("stream", false);
        payload.put("messages", messages);
        if (!tools.isEmpty()) {
            payload.put("tools", tools);
            payload.put("tool_choice", "auto");
        }
        final JsonNode root = this.post(payload);
        final JsonNode message = root.path("choices").path(0).path("message");
        final List<ToolCall> calls = new ArrayList<>();
        for (final JsonNode call : message.path("tool_calls")) {
            calls.add(new ToolCall(
                    call.path("id").asText(""),
                    call.path("function").path("name").asText(""),
                    call.path("function").path("arguments").asText("{}")));
        }
        final JsonNode content = message.path("content");
        return new ChatResult(
                content.isMissingNode() || content.isNull() || content.asText().isBlank() ? null : content.asText(),
                calls);
    }

    /** 发起一次对话补全，返回助手消息正文。 */
    public String chat(final String systemPrompt, final String userPrompt) {
        final String base = this.properties.llmBaseUrl().strip();
        final String url = (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/chat/completions";
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", this.properties.llmModel().strip());
        payload.put("temperature", OpenAiCompatibleClient.TEMPERATURE);
        payload.put("stream", false);
        payload.put(
                "messages",
                List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(OpenAiCompatibleClient.TIMEOUT)
                .header("Authorization", "Bearer " + this.properties.llmApiKey().strip())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(this.write(payload), StandardCharsets.UTF_8))
                .build();
        try {
            final HttpResponse<String> response =
                    this.client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("模型接口返回 HTTP " + response.statusCode());
            }
            final JsonNode root = this.mapper.readTree(response.body());
            final JsonNode content =
                    root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull() || content.asText().isBlank()) {
                throw new IllegalStateException("模型没有返回内容");
            }
            return content.asText();
        } catch (final IOException ex) {
            throw new IllegalStateException("模型响应解析失败：" + ex.getMessage(), ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("模型调用被中断", ex);
        }
    }

    /** 模型配置是否完整（未配置时不发起调用）。 */
    public Optional<String> missingConfiguration() {
        if (this.properties.llmBaseUrl() == null || this.properties.llmBaseUrl().isBlank()) {
            return Optional.of("模型接口地址未配置");
        }
        if (this.properties.llmApiKey() == null || this.properties.llmApiKey().isBlank()) {
            return Optional.of("模型密钥未配置");
        }
        if (this.properties.llmModel() == null || this.properties.llmModel().isBlank()) {
            return Optional.of("模型名称未配置");
        }
        return Optional.empty();
    }

    private JsonNode post(final Map<String, Object> payload) {
        final String base = this.properties.llmBaseUrl().strip();
        final String url = (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/chat/completions";
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(OpenAiCompatibleClient.TIMEOUT)
                .header("Authorization", "Bearer " + this.properties.llmApiKey().strip())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(this.write(payload), StandardCharsets.UTF_8))
                .build();
        try {
            final HttpResponse<String> response =
                    this.client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("模型接口返回 HTTP " + response.statusCode());
            }
            return this.mapper.readTree(response.body());
        } catch (final IOException ex) {
            throw new IllegalStateException("模型响应解析失败：" + ex.getMessage(), ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("模型调用被中断", ex);
        }
    }

    private String write(final Object payload) {
        try {
            return this.mapper.writeValueAsString(payload);
        } catch (final IOException ex) {
            throw new IllegalStateException("模型请求序列化失败：" + ex.getMessage(), ex);
        }
    }
}
