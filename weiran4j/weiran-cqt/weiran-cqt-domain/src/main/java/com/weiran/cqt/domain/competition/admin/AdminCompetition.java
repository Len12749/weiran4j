package com.weiran.cqt.domain.competition.admin;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 赛事后台配置协议。 */
public record AdminCompetition(
        long id,
        @Nullable Long legacyId,
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
        @Nullable String provinceCertificateTemplate,
        @Nullable String nationalCertificateTemplate,
        @Nullable String teacherOrgCertificateTemplate,
        int status,
        long version,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt) {}
