package com.wulicode.weiran.automation.domain.task;

import org.jspecify.annotations.Nullable;

/** 单个变更文件的评审结论级别。 */
public enum ReviewFileLevel {
    /** 未发现问题。 */
    PASS("pass"),
    /** 有改进建议。 */
    SUGGEST("suggest"),
    /** 发现问题，建议修改。 */
    ISSUE("issue"),
    /** 未参与分析（二进制、超大 diff 或非代码文件）。 */
    SKIP("skip");

    private final String value;

    ReviewFileLevel(final String value) {
        this.value = value;
    }

    /** 持久化取值。 */
    public String value() {
        return this.value;
    }

    /** 解析模型输出；无法识别时按改进建议处理，避免整条评审失败。 */
    public static ReviewFileLevel of(final @Nullable String value) {
        if (value == null) {
            return ReviewFileLevel.SUGGEST;
        }
        for (final ReviewFileLevel level : values()) {
            if (level.value.equalsIgnoreCase(value.trim())) {
                return level;
            }
        }
        return ReviewFileLevel.SUGGEST;
    }
}
