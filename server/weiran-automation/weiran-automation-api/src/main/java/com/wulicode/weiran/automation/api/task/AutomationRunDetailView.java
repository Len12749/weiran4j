package com.wulicode.weiran.automation.api.task;

import java.util.List;

/** 一次执行的完整明细：执行摘要 + 该次产生的全部评审结果。 */
public record AutomationRunDetailView(AutomationRunView run, List<AutomationReviewView> reviews) {

    /** 防御性复制，避免调用方持有的可变列表影响结果。 */
    public AutomationRunDetailView {
        reviews = List.copyOf(reviews);
    }
}
