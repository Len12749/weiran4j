package com.wulicode.weiran.automation.domain.task;

import org.jspecify.annotations.Nullable;

/** 评审结果的输出方式。 */
public enum OutputMode {
    /** 仅在本平台产出记录，不回写云效。 */
    LOCAL("local"),
    /** 本地产出记录，并回填到云效合并请求。 */
    LOCAL_AND_CLOUD("local_and_cloud");

    private final String value;

    OutputMode(final String value) {
        this.value = value;
    }

    /** 持久化取值。 */
    public String value() {
        return this.value;
    }

    /** 按持久化取值解析，未知值按仅本地处理，避免误回写云端。 */
    public static OutputMode of(final @Nullable String value) {
        for (final OutputMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        return OutputMode.LOCAL;
    }
}
