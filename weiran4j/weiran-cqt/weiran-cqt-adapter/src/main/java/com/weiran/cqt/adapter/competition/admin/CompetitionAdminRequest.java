package com.weiran.cqt.adapter.competition.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 赛事后台配置协议。 */
public record CompetitionAdminRequest(
        String name,
        @Nullable String edition,
        @Nullable Integer year,
        @Nullable String description,
        String nationalEntryMode,
        @Nullable LocalDateTime registrationStart,
        @Nullable LocalDateTime registrationEnd,
        @Nullable LocalDateTime provinceReviewStart,
        @Nullable LocalDateTime provinceReviewEnd,
        @Nullable LocalDateTime nationalReviewStart,
        @Nullable LocalDateTime nationalReviewEnd,
        @Nullable LocalDateTime resultPublishTime,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable
        String provinceCertificateTemplate,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable
        String nationalCertificateTemplate,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable
        String teacherOrgCertificateTemplate,

        @Nullable Long expectedVersion) {
    public com.weiran.cqt.api.competition.admin.CompetitionAdminCommand command() {
        return new com.weiran.cqt.api.competition.admin.CompetitionAdminCommand(
                this.name(),
                this.edition(),
                this.year(),
                this.description(),
                this.nationalEntryMode(),
                this.registrationStart(),
                this.registrationEnd(),
                this.provinceReviewStart(),
                this.provinceReviewEnd(),
                this.nationalReviewStart(),
                this.nationalReviewEnd(),
                this.resultPublishTime(),
                this.provinceCertificateTemplate(),
                this.nationalCertificateTemplate(),
                this.teacherOrgCertificateTemplate(),
                this.expectedVersion());
    }
}
