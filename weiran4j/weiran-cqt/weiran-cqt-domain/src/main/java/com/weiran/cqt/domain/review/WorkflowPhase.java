package com.weiran.cqt.domain.review;
/** 工作流不可变快照。 */
public record WorkflowPhase(
        long id, long competitionId, String code, String state, long currentRuleId, long inputVersion, long version) {}
