package com.wulicode.weiran.automation.domain.task;

import java.util.List;

/** 评审 Agent 产出的评论草稿。 */
public record ReviewDraft(String commentText, String model, List<ReviewFileDraft> files) {

    /** 防御性复制，避免调用方持有的可变列表影响结果。 */
    public ReviewDraft {
        files = List.copyOf(files);
    }
}
