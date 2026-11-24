package com.wulicode.weiran.automation.domain.task;

import org.jspecify.annotations.Nullable;

/**
 * 一个变更文件的差异。
 *
 * @param diff 统一 diff 文本；二进制文件为空
 */
public record ChangedFile(
        String path,
        @Nullable String oldPath,
        int addLines,
        int delLines,
        boolean binary,
        boolean deleted,
        boolean renamed,
        String diff) {}
