package com.weiran.cqt.api.content.resources;
/** 指导资料写入字段。 */
public record ResourceCommand(
        String title,
        String category,
        String materialUrl,
        String content,
        @org.jspecify.annotations.Nullable Boolean enabled,
        int sort) {
    public ResourceCommand {
        title = java.util.Objects.requireNonNullElse(title, "").strip();
        category = java.util.Objects.requireNonNullElse(category, "");
        materialUrl = java.util.Objects.requireNonNullElse(materialUrl, "");
        content = java.util.Objects.requireNonNullElse(content, "");
    }
}
