package com.wulicode.weiran.automation.domain.task;

import java.util.Optional;

/** 任务执行终态。 */
public enum AutomationRunStatus {
    /** 全部合并请求处理成功，或本轮没有待处理项。 */
    SUCCESS("success"),
    /** 部分合并请求失败，其余成功。 */
    PARTIAL("partial"),
    /** 整体失败：配置缺失、拉取失败或全部合并请求失败。 */
    FAILED("failed");

    private final String value;

    AutomationRunStatus(final String value) {
        this.value = value;
    }

    /** 持久化取值。 */
    public String value() {
        return this.value;
    }

    /** 按持久化取值解析，未知值返回空。 */
    public static Optional<AutomationRunStatus> of(final String value) {
        for (final AutomationRunStatus status : values()) {
            if (status.value.equals(value)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
