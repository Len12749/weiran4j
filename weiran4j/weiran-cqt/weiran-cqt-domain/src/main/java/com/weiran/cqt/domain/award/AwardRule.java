package com.weiran.cqt.domain.award;

import com.weiran.cqt.domain.review.AwardCandidate;
import com.weiran.framework.error.BizException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** 每个revision为独立行，ruleId为稳定逻辑标识。 */
public record AwardRule(
        long id,
        long ruleId,
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
        long createdBy,
        LocalDateTime createdAt) {
    public AwardRule {
        if (!java.util.Set.of("ENTRY", "PARTICIPANT").contains(targetType)
                || firstCategoryId <= 0
                || (secondCategoryId != null && secondCategoryId <= 0)) {
            throw BizException.badRequest("奖项适用对象或赛项无效");
        }
        awardName = awardName.strip();
        groupName = groupName == null || groupName.isBlank() ? null : groupName.strip();
        if (awardName.isEmpty() || awardName.length() > 255 || (groupName != null && groupName.length() > 255)) {
            throw BizException.badRequest("奖项名称或组别不能为空或过长");
        }
        if (minScore.scale() > 2 || maxScore.scale() > 2 || minScore.compareTo(maxScore) > 0) {
            throw BizException.badRequest("奖项分数区间无效，最多两位小数");
        }
    }

    public boolean overlaps(final AwardRule other) {
        return this.enabled
                && other.enabled
                && this.phaseId == other.phaseId
                && this.targetType.equals(other.targetType)
                && this.firstCategoryId == other.firstCategoryId
                && (this.secondCategoryId == null
                        || other.secondCategoryId == null
                        || Objects.equals(this.secondCategoryId, other.secondCategoryId))
                && (this.groupName == null || other.groupName == null || this.groupName.equals(other.groupName))
                && this.minScore.compareTo(other.maxScore) <= 0
                && this.maxScore.compareTo(other.minScore) >= 0;
    }

    public boolean matches(final AwardCandidate candidate) {
        return this.enabled
                && this.targetType.equals(candidate.targetType())
                && this.firstCategoryId == candidate.firstCategoryId()
                && (this.secondCategoryId == null
                        || Objects.equals(this.secondCategoryId, candidate.secondCategoryId()))
                && (this.groupName == null || this.groupName.equals(candidate.groupName()))
                && candidate.score() != null
                && this.minScore.compareTo(candidate.score()) <= 0
                && this.maxScore.compareTo(candidate.score()) >= 0;
    }
}
