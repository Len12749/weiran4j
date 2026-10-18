package com.weiran.cqt.domain.review;

import java.util.Optional;

/** 跨评审与评奖的预览及命令回执。 */
public interface WorkflowPreviewStore {
    void save(WorkflowPreview preview);

    Optional<WorkflowPreview> find(String token);

    Optional<WorkflowReceipt> receipt(long operatorId, String operation, String requestKey);

    void consume(String token);

    void saveReceipt(WorkflowReceipt receipt);
}
