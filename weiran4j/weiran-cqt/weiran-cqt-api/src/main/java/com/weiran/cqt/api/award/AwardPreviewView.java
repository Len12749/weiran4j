package com.weiran.cqt.api.award;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AwardPreviewView(
        String previewToken,
        LocalDateTime expiresAt,
        long inputVersion,
        Map<String, Integer> summary,
        List<AwardPreviewRow> rows,
        List<AwardPreviewRow> blockedRows) {}
