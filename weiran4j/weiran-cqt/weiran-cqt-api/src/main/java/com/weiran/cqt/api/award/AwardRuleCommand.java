package com.weiran.cqt.api.award;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** 规则命令；版本由当前逻辑规则revision决定。 */
public record AwardRuleCommand(
        long phaseId,
        String targetType,
        long firstCategoryId,
        @Nullable Long secondCategoryId,
        @Nullable String groupName,
        String awardName,
        BigDecimal minScore,
        BigDecimal maxScore,
        boolean enabled,
        @Nullable Long expectedVersion) {}
