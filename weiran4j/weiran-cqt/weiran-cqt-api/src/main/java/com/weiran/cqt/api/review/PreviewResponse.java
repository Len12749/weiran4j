package com.weiran.cqt.api.review;

import java.time.LocalDateTime;
import java.util.List;

/** 服务端保存的预览及阻断明细。 */
public record PreviewResponse<T>(
        String previewToken,
        LocalDateTime expiresAt,
        long inputVersion,
        String summary,
        List<T> rows,
        List<T> blockedRows) {}
