package com.weiran.system.api.media;

import org.jspecify.annotations.NullMarked;

/** 图片上传结果。 */
@NullMarked
public record ImageUploadView(String url, String name, String path, long size) {}
