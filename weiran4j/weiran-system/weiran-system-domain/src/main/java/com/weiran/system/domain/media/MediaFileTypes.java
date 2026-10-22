package com.weiran.system.domain.media;

import java.util.Locale;

/** 资源库允许存储的文件类型。 */
public final class MediaFileTypes {

    private MediaFileTypes() {}

    public static String extension(final String name) {
        final int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean allowed(final String extension) {
        return extension.matches("jpg|jpeg|png|gif|webp|pdf|txt|csv|doc|docx|xls|xlsx|ppt|pptx|zip");
    }

    public static boolean image(final String extension) {
        return extension.matches("jpg|jpeg|png|gif|webp");
    }

    public static String category(final String name) {
        final String extension = MediaFileTypes.extension(name);
        if (MediaFileTypes.image(extension)) {
            return "image";
        }
        if (extension.equals("zip")) {
            return "archive";
        }
        return "document";
    }

    public static String contentType(final String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain; charset=utf-8";
            case "csv" -> "text/csv; charset=utf-8";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }
}
