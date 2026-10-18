package com.weiran.cqt.domain.review;
/** 工作流不可变快照。 */
public record WorkflowReceipt(
        long operatorId, String operation, String requestKey, String payloadHash, String result) {}
