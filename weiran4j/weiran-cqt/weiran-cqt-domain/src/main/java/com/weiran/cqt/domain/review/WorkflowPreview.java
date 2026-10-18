package com.weiran.cqt.domain.review;
/** 工作流不可变快照。 */
public record WorkflowPreview(
        String token,
        String purpose,
        long operatorId,
        long phaseId,
        long inputVersion,
        String payloadHash,
        String snapshot,
        java.time.LocalDateTime expiresAt) {}
