package com.weiran.system.domain.batch;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 数据库中的批处理任务快照。 */
public record BatchJob(
        long id,
        String kind,
        String sourceName,
        String status,
        String phase,
        @Nullable Long total,
        long processed,
        long succeeded,
        long failed,
        @Nullable String errorMessage,
        LocalDateTime createdAt,
        @Nullable LocalDateTime finishedAt) {}
