package com.weiran.cqt.api.content.showcases;
/** 佳作欣赏写入字段。 */
public record ShowcaseCommand(
        String title,
        String image,
        String category,
        String author,
        String teacher,
        String school,
        String description,
        String content,
        String attachmentUrl,
        String attachmentName,
        String videoUrl,
        @org.jspecify.annotations.Nullable Boolean enabled,
        int sort) {
    public ShowcaseCommand {
        title = java.util.Objects.requireNonNullElse(title, "").strip();
        image = java.util.Objects.requireNonNullElse(image, "");
        category = java.util.Objects.requireNonNullElse(category, "");
        author = java.util.Objects.requireNonNullElse(author, "");
        teacher = java.util.Objects.requireNonNullElse(teacher, "");
        school = java.util.Objects.requireNonNullElse(school, "");
        description = java.util.Objects.requireNonNullElse(description, "");
        content = java.util.Objects.requireNonNullElse(content, "");
        attachmentUrl = java.util.Objects.requireNonNullElse(attachmentUrl, "");
        attachmentName = java.util.Objects.requireNonNullElse(attachmentName, "");
        videoUrl = java.util.Objects.requireNonNullElse(videoUrl, "");
    }
}
