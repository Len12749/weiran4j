package com.weiran.cqt.api.content.resources;
/** 指导资料写入字段。 */
public record ResourceCommand(
        String title,
        String category,
        String fileUrl,
        String fileName,
        String content,
        @org.jspecify.annotations.Nullable Boolean enabled,
        int sort) {
    public ResourceCommand {
        title = java.util.Objects.requireNonNullElse(title, "").strip();
        category = java.util.Objects.requireNonNullElse(category, "");
        fileUrl = java.util.Objects.requireNonNullElse(fileUrl, "");
        fileName = java.util.Objects.requireNonNullElse(fileName, "");
        content = java.util.Objects.requireNonNullElse(content, "");
    }
}
