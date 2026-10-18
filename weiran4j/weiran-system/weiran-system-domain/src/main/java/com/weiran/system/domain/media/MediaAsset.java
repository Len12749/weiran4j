package com.weiran.system.domain.media;

import java.time.Instant;
import org.jspecify.annotations.NullMarked;

/** 已登记的图片资产。 */
@NullMarked
public record MediaAsset(
        long id, String originalName, String url, String objectName, long size, long uploadedBy, Instant createdAt) {}
