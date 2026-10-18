package com.weiran.cqt.domain.content;

import com.weiran.framework.error.BizException;
import java.net.URI;
import java.net.URISyntaxException;

/** 内容边界规则，不依赖Web或持久化框架。 */
public final class ContentValidation {
    private ContentValidation() {}

    public static void required(final String value, final String label, final int max) {
        if (value.isBlank() || value.length() > max) {
            throw BizException.badRequest(label + "不能为空或过长");
        }
    }

    public static void optional(final String value, final String label, final int max) {
        if (value.length() > max) {
            throw BizException.badRequest(label + "过长");
        }
    }

    public static void url(final String value, final boolean required) {
        if (value.isBlank()) {
            if (required) {
                throw BizException.badRequest("文件或图片地址不能为空");
            }
            return;
        }
        if (value.length() > 1024
                || value.startsWith("//")
                || value.indexOf('\\') >= 0
                || !value.equals(value.strip())) {
            throw BizException.badRequest("URL格式无效");
        }
        try {
            final URI uri = new URI(value);
            final String scheme = uri.getScheme();
            if (value.startsWith("/") && !uri.isAbsolute()) {
                return;
            }
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null) {
                return;
            }
        } catch (final URISyntaxException ignored) {
            throw BizException.badRequest("URL格式无效");
        }
        throw BizException.badRequest("URL只允许http、https或本地路径");
    }
}
