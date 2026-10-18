package com.weiran.cqt.api.content.experts;
/** 学术评审团写入字段。 */
public record ExpertCommand(
        String name,
        String image,
        String content,
        @org.jspecify.annotations.Nullable Boolean showOnHome,
        @org.jspecify.annotations.Nullable Boolean enabled,
        int sort) {
    public ExpertCommand {
        name = java.util.Objects.requireNonNullElse(name, "").strip();
        image = java.util.Objects.requireNonNullElse(image, "");
        content = java.util.Objects.requireNonNullElse(content, "");
    }
}
