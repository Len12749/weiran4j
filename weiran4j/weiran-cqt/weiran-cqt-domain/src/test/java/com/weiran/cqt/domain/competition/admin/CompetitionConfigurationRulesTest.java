package com.weiran.cqt.domain.competition.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompetitionConfigurationRulesTest {

    @Test
    @DisplayName("新赛事允许不填届次和年份，并保留时间范围")
    void allowsMissingEditionAndYear() {
        final LocalDateTime start = LocalDateTime.of(2027, 1, 1, 9, 0);
        final LocalDateTime end = start.plusDays(30);
        final CompetitionConfiguration validated = CompetitionConfigurationRules.validate(new CompetitionConfiguration(
                "2027 青少年戏剧大赛",
                null,
                null,
                null,
                "SHARED",
                start,
                end,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        assertThat(validated.edition()).isNull();
        assertThat(validated.year()).isNull();
        assertThat(validated.registrationStart()).isEqualTo(start);
        assertThat(validated.registrationEnd()).isEqualTo(end);
    }

    @Test
    @DisplayName("保留历史届次和年份时继续校验长度与范围")
    void validatesLegacyEditionAndYear() {
        assertThatThrownBy(() -> CompetitionConfigurationRules.validate(new CompetitionConfiguration(
                        "2027 青少年戏剧大赛",
                        "届次",
                        1899,
                        null,
                        "SHARED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
                .hasMessage("年份须为1900至9999");
    }
}
