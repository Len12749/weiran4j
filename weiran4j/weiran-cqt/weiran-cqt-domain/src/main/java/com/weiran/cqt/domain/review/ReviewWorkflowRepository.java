package com.weiran.cqt.domain.review;

import java.util.List;
import java.util.Optional;

/** 两子域共用阶段锁及派生失效边界。 */
public interface ReviewWorkflowRepository {
    WorkflowPhase lockPhase(long id);

    List<WorkflowPhase> lockCompetition(long competitionId);

    Optional<WorkflowPhase> phase(long id);

    List<WorkflowPhase> phases(long competitionId);

    void bumpInput(long phaseId);

    void invalidateTarget(long competitionId, long targetId, String reason);

    /** Invalidate one phase target and its national descendants; never propagate backwards. */
    void invalidatePhaseTarget(long phaseId, long targetId, String reason, boolean includeCurrentFinal);

    void invalidatePhase(long phaseId, String reason);

    void invalidateReviewer(long phaseId, long reviewerId, String reason);

    void invalidateDerived(long phaseId, String reason);

    void invalidate(long phaseId, String reason);

    List<ReviewerSnapshot> reviewerSnapshots(long phaseId);

    void refreshReviewer(long phaseId, ReviewerSnapshot snapshot);

    List<AwardCandidate> awardCandidates(long phaseId);
}
