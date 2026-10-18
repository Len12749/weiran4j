package com.weiran.cqt.domain.review;

import com.weiran.cqt.domain.review.ReviewModels.Scope;
import com.weiran.cqt.domain.review.ReviewModels.Target;
import com.weiran.framework.error.BizException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** 无框架的评分、范围及状态规则。 */
public final class ReviewRules {
    private ReviewRules() {}

    public static String text(final @Nullable String value, final String label, final boolean required, final int max) {
        final String v = value == null ? "" : value.strip();
        if ((required && v.isEmpty()) || v.length() > max) {
            throw BizException.badRequest(label + "为空或过长");
        }
        return v;
    }

    public static void number(final @Nullable BigDecimal value, final BigDecimal min, final BigDecimal max) {
        if (value == null
                || value.scale() > 2
                || value.scale() < -6
                || value.abs().compareTo(new BigDecimal("1000000")) >= 0
                || value.compareTo(min) < 0
                || value.compareTo(max) > 0) {
            throw BizException.badRequest("分数必须在规定范围且最多两位小数");
        }
    }

    public static BigDecimal average(final List<BigDecimal> values) {
        if (values.isEmpty()) {
            throw BizException.badRequest("没有有效评分");
        }
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    public static boolean matches(final Scope s, final Target t, final long phase) {
        return s.enabled()
                && s.phaseId() == phase
                && s.competitionId() == t.competitionId()
                && s.firstCategoryId() == t.firstCategoryId()
                && (s.secondCategoryId() == null
                        || s.secondCategoryId() == 0
                        || Objects.equals(s.secondCategoryId(), t.secondCategoryId()))
                && (s.regionId() == null || s.regionId() == 0 || Objects.equals(s.regionId(), t.regionId()));
    }

    public static String eligibility(final Target t, final WorkflowPhase phase, final boolean advanced) {
        if (!"ACTIVE".equals(t.status()) || !"ACTIVE".equals(t.entryStatus())) {
            return "作品或对象已失效";
        }
        if (t.legacyStatus() != 3) {
            return "作品尚未通过初审";
        }
        if (phase.competitionId() != t.competitionId()) {
            return "对象不属于当前赛事";
        }
        if ("PROVINCIAL".equals(phase.code()) && "NATIONAL".equals(t.stageScope())) {
            return "国赛独立作品不可进入省审";
        }
        if ("NATIONAL".equals(phase.code()) && (!advanced || "PROVINCIAL".equals(t.stageScope()))) {
            return "没有有效国赛资格或作品阶段不匹配";
        }
        if ("INITIAL".equals(phase.code())) {
            return "初审不创建数值评分任务";
        }
        return "";
    }
}
