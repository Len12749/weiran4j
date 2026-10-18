package com.weiran.cqt.domain.award;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weiran.cqt.domain.review.AwardCandidate;
import com.weiran.framework.error.BizException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AwardRuleTest {
    @Test
    @DisplayName("闭区间相同端点及通配专业组别重叠必须拒绝")
    void detectsClosedEndpointAndWildcardOverlap() {
        final AwardRule wildcard = AwardRuleTest.rule(null, null, "80", "90", true);
        assertThat(wildcard.overlaps(AwardRuleTest.rule(7L, "高中", "90", "100", true)))
                .isTrue();
        assertThat(wildcard.overlaps(AwardRuleTest.rule(7L, "高中", "90.01", "100", true)))
                .isFalse();
        assertThat(wildcard.overlaps(AwardRuleTest.rule(7L, "高中", "80", "90", false)))
                .isFalse();
        assertThat(AwardRuleTest.rule(7L, "高中", "80", "90", true)
                        .overlaps(AwardRuleTest.rule(8L, "高中", "80", "90", true)))
                .isFalse();
        assertThat(AwardRuleTest.rule(7L, "高中", "80", "90", true)
                        .overlaps(AwardRuleTest.rule(7L, "初中", "80", "90", true)))
                .isFalse();
    }

    @Test
    @DisplayName("相同成绩不限名额且只匹配对应目标类型和适用范围")
    void matchesEqualScoresWithoutQuota() {
        final AwardRule rule = AwardRuleTest.rule(7L, "高中", "80", "90", true);
        assertThat(rule.matches(AwardRuleTest.candidate(1, "ENTRY", "90"))).isTrue();
        assertThat(rule.matches(AwardRuleTest.candidate(2, "ENTRY", "90"))).isTrue();
        assertThat(rule.matches(AwardRuleTest.candidate(3, "PARTICIPANT", "90")))
                .isFalse();
        assertThat(rule.matches(AwardRuleTest.candidate(4, "ENTRY", "90.01"))).isFalse();
    }

    @Test
    @DisplayName("非法奖区间和超过两位小数被领域拒绝")
    void rejectsInvalidIntervals() {
        assertThatThrownBy(() -> AwardRuleTest.rule(null, null, "90", "80", true))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> AwardRuleTest.rule(null, null, "80.001", "90", true))
                .isInstanceOf(BizException.class);
    }

    private static AwardRule rule(
            final @Nullable Long major,
            final @Nullable String group,
            final String min,
            final String max,
            final boolean enabled) {
        return new AwardRule(
                1,
                1,
                4,
                "ENTRY",
                5,
                major,
                group,
                "一等奖",
                new BigDecimal(min),
                new BigDecimal(max),
                enabled,
                1,
                2,
                LocalDateTime.of(2026, 10, 17, 0, 0));
    }

    private static AwardCandidate candidate(final long id, final String type, final String score) {
        return new AwardCandidate(
                id, id, null, type, 5, 7L, "高中", "作品", "标题", new BigDecimal(score), 1, "VALID", 0, true, "");
    }
}
