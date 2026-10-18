package com.weiran.cqt.api.award;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** 明确候选过滤；空targetIds代表本阶段全部符合过滤条件的对象。 */
public record AwardPreviewCommand(
        long competitionId,
        long phaseId,
        @Nullable List<Long> targetIds,
        @Nullable String targetType,
        @Nullable Long firstCategoryId,
        @Nullable Long secondCategoryId,
        @Nullable String groupName,
        @Nullable Long supersedesBatchId) {}
