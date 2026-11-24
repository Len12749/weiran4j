package com.wulicode.weiran.automation.domain.task;

import org.jspecify.annotations.Nullable;

/**
 * 待评审合并请求摘要。
 *
 * @param projectId 代码库 ID
 * @param localId 合并请求在代码库内的局部 ID
 * @param repositoryPath 代码库路径（形如 {@code qyd/lianmeng/kr/web}），用于拼克隆地址
 * @param totalCommentCount 云效侧已有的评论总数；为 0 才进入评审
 */
public record ChangeRequestSummary(
        String projectId,
        long localId,
        String title,
        @Nullable String webUrl,
        @Nullable String authorName,
        String sourceBranch,
        String targetBranch,
        int totalCommentCount,
        String repositoryPath,
        @Nullable String updatedAt) {}
