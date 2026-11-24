package com.wulicode.weiran.automation.adapter.web.request;

import jakarta.validation.constraints.NotNull;

/**
 * 启停自动化任务请求。
 *
 * <p>用包装类型而不是 {@code boolean}：漏传字段时必须报错，而不是静默按「停用」处理。
 */
public record ToggleAutomationTaskRequest(
        @NotNull(message = "缺少启用状态") Boolean enabled) {

    /** 目标状态。 */
    public boolean requiredEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }
}
