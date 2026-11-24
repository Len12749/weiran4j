package com.wulicode.weiran.automation.api;

import com.wulicode.weiran.framework.error.ErrorCode;

/** 自动化任务服务（13）的自动化任务资源（01）错误码。 */
public enum AutomationErrors implements ErrorCode {
    TASK_NOT_FOUND(130101, 404, "自动化任务不存在"),
    RUN_NOT_FOUND(130102, 404, "执行记录不存在"),
    RUN_REVIEW_NOT_FOUND(130103, 404, "评审记录不存在");

    private final int code;

    private final int httpStatus;

    private final String message;

    AutomationErrors(final int code, final int httpStatus, final String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public int code() {
        return this.code;
    }

    @Override
    public int httpStatus() {
        return this.httpStatus;
    }

    @Override
    public String message() {
        return this.message;
    }
}
