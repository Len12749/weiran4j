package com.weiran.cqt.api.award;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** id为逻辑规则ID，revisionId为不可变行ID。 */
public record AwardRuleView(
        long id,
        long revisionId,
        long phaseId,
        String targetType,
        long firstCategoryId,
        @Nullable Long secondCategoryId,
        @Nullable String groupName,
        String awardName,
        BigDecimal minScore,
        BigDecimal maxScore,
        boolean enabled,
        long revision,
        long version,
        LocalDateTime createdAt) {}
