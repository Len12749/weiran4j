package com.wulicode.weiran.automation.domain.task;

/** 合并请求级评审状态。 */
public enum AutomationReviewStatus {
    /** 已被某一轮执行占用，正在分析。 */
    PROCESSING("processing"),
    /** 已生成评论并落库。 */
    REVIEWED("reviewed"),
    /** 调用评审 Agent 失败。 */
    FAILED("failed"),
    /** 本次跳过（已在本地评审过，或没有可分析的代码变更）。 */
    SKIPPED("skipped");

    private final String value;

    AutomationReviewStatus(final String value) {
        this.value = value;
    }

    /** 持久化取值。 */
    public String value() {
        return this.value;
    }

    /** 按持久化取值解析，未知值按失败处理，避免脏数据影响展示。 */
    public static AutomationReviewStatus of(final String value) {
        for (final AutomationReviewStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return AutomationReviewStatus.FAILED;
    }
}
