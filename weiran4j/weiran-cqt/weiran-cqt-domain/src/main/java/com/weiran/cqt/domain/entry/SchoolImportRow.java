package com.weiran.cqt.domain.entry;

import org.jspecify.annotations.Nullable;

/** 学校报名表中一条有内容的数据行；省赛/国赛评价列不进入该模型。 */
public record SchoolImportRow(
        int sourceRow,
        SchoolImportEntryType entryType,
        int declaredGroupSize,
        String name,
        String credentialType,
        String credentialNo,
        String phone,
        String region,
        String school,
        String group,
        String category,
        String major,
        String title,
        @Nullable String description,
        String attachmentMode,
        String attachmentValue,
        @Nullable String teacher1,
        @Nullable String teacher1CredentialNo,
        @Nullable String teacher2,
        @Nullable String teacher2CredentialNo,
        @Nullable String channel,
        @Nullable String validationError) {}
