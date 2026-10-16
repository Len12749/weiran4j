package com.weiran.cqt.domain.entry;

/** 学校导入任务状态。 */
public enum SchoolImportTaskStatus {
    UPLOADING,
    QUEUED,
    PROCESSING,
    COMPLETED,
    PARTIAL,
    FAILED
}
