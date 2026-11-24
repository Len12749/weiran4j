package com.wulicode.weiran.automation.infrastructure.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 极简 JSON over HTTP 工具：只为自动化任务的外部调用服务。
 *
 * <p>失败时抛出带状态码和响应片段的异常；响应片段截断，避免把整页 HTML 塞进日志。
 */
final class HttpJson {

    private static final int BODY_PREVIEW_LIMIT = 300;

    private final HttpClient client;

    private final ObjectMapper mapper;

    HttpJson(final HttpClient client, final ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    JsonNode get(final String url, final Map<String, String> headers, final Duration timeout) {
        final HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET();
        concat(headers, Map.of("Accept", "application/json")).forEach(builder::header);
        return this.send(builder.build(), url);
    }

    JsonNode post(final String url, final Map<String, String> headers, final Object body, final Duration timeout) {
        try {
            final String payload = this.mapper.writeValueAsString(body);
            final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
            concat(headers, Map.of("Accept", "application/json", "Content-Type", "application/json"))
                    .forEach(builder::header);
            return this.send(builder.build(), url);
        } catch (final IOException ex) {
            throw new IllegalStateException("请求体序列化失败：" + ex.getMessage(), ex);
        }
    }

    private JsonNode send(final HttpRequest request, final String url) {
        try {
            final HttpResponse<String> response =
                    this.client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            final String body = response.body() == null ? "" : response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("外部接口返回 HTTP " + response.statusCode() + "：" + preview(body));
            }
            return this.mapper.readTree(body);
        } catch (final IOException ex) {
            throw new IllegalStateException("外部接口响应解析失败：" + ex.getMessage(), ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("外部接口调用被中断", ex);
        }
    }

    private static Map<String, String> concat(final Map<String, String> first, final Map<String, String> second) {
        final java.util.Map<String, String> merged = new java.util.LinkedHashMap<>(first);
        merged.putAll(second);
        return merged;
    }

    private static String preview(final String body) {
        final String trimmed = body == null ? "" : body.strip();
        return trimmed.length() <= HttpJson.BODY_PREVIEW_LIMIT
                ? trimmed
                : trimmed.substring(0, HttpJson.BODY_PREVIEW_LIMIT) + "…";
    }
}
