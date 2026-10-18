package com.weiran.cqt.domain.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weiran.cqt.domain.review.ReviewModels.Scope;
import com.weiran.cqt.domain.review.ReviewModels.Target;
import com.weiran.framework.error.BizException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewRulesTest {
    @Test
    @DisplayName("综合分平均独立计算并按HALF_UP保留两位")
    void averagesIndependentTotalsWithHalfUp() {
        assertThat(ReviewRules.average(List.of(new BigDecimal("80.00"), new BigDecimal("80.01"))))
                .isEqualByComparingTo("80.01");
        assertThatThrownBy(() -> ReviewRules.average(List.of())).isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("评分拒绝超精度、超存储范围、空值及极端指数")
    void rejectsInvalidScoreRepresentations() {
        final BigDecimal maximum = new BigDecimal("999999.99");
        for (final BigDecimal score :
                List.of(new BigDecimal("1.001"), new BigDecimal("1000000"), new BigDecimal("1E+100"))) {
            assertThatThrownBy(() -> ReviewRules.number(score, BigDecimal.ZERO, maximum))
                    .isInstanceOf(BizException.class);
        }
        assertThatThrownBy(() -> ReviewRules.number(null, BigDecimal.ZERO, maximum))
                .isInstanceOf(BizException.class);
        ReviewRules.number(new BigDecimal("999999.99"), BigDecimal.ZERO, maximum);
    }

    @Test
    @DisplayName("评委通配范围仍严格限制赛事阶段及一级赛项")
    void wildcardScopeDoesNotCrossCompetitionOrPhase() {
        final Scope wildcard = new Scope(1, 2, 3, 4, 5, null, null, true, 0);
        final Target target = ReviewRulesTest.target("BOTH", "ACTIVE", 3);
        assertThat(ReviewRules.matches(wildcard, target, 4)).isTrue();
        assertThat(ReviewRules.matches(wildcard, target, 9)).isFalse();
        assertThat(ReviewRules.matches(new Scope(1, 2, 99, 4, 5, null, null, true, 0), target, 4))
                .isFalse();
        assertThat(ReviewRules.matches(new Scope(1, 2, 3, 4, 5, 99L, null, true, 0), target, 4))
                .isFalse();
        assertThat(ReviewRules.matches(new Scope(1, 2, 3, 4, 5, null, null, false, 0), target, 4))
                .isFalse();
    }

    @Test
    @DisplayName("初审不评分、国赛须资格、省审排除独立国赛作品和失效对象")
    void enforcesStageAndEligibilityBoundaries() {
        final Target target = ReviewRulesTest.target("BOTH", "ACTIVE", 3);
        assertThat(ReviewRules.eligibility(target, new WorkflowPhase(4, 3, "INITIAL", "OPEN", 1, 0, 0), true))
                .isNotEmpty();
        assertThat(ReviewRules.eligibility(target, new WorkflowPhase(4, 3, "NATIONAL", "OPEN", 1, 0, 0), false))
                .isNotEmpty();
        assertThat(ReviewRules.eligibility(target, new WorkflowPhase(4, 3, "NATIONAL", "OPEN", 1, 0, 0), true))
                .isEmpty();
        assertThat(ReviewRules.eligibility(
                        ReviewRulesTest.target("NATIONAL", "ACTIVE", 3),
                        new WorkflowPhase(4, 3, "PROVINCIAL", "OPEN", 1, 0, 0),
                        false))
                .isNotEmpty();
        assertThat(ReviewRules.eligibility(
                        ReviewRulesTest.target("BOTH", "INACTIVE", 3),
                        new WorkflowPhase(4, 3, "PROVINCIAL", "OPEN", 1, 0, 0),
                        false))
                .isNotEmpty();
    }

    @Test
    @DisplayName("维度评语trim后必须有内容，综合评语允许空")
    void validatesTrimmedComments() {
        assertThat(ReviewRules.text("  合格  ", "评语", true, 2000)).isEqualTo("合格");
        assertThat(ReviewRules.text(null, "综合评语", false, 2000)).isEmpty();
        assertThatThrownBy(() -> ReviewRules.text("  ", "维度评语", true, 2000)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> ReviewRules.text("x".repeat(2001), "维度评语", true, 2000))
                .isInstanceOf(BizException.class);
    }

    private static Target target(final String stage, final String state, final int legacy) {
        return new Target(
                1, 2, 6L, 3, "houtai", "PARTICIPANT", "学生", "作品", 5, 7L, 8L, "高中", stage, state, "ACTIVE", legacy, 0);
    }
}
