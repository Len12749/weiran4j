package com.weiran.cqt.application.review;

import com.weiran.cqt.domain.review.ReviewWorkflowRepository;
import com.weiran.cqt.domain.review.ReviewerSnapshot;
import com.weiran.cqt.domain.review.WorkflowPhase;
import com.weiran.framework.error.BizException;
import com.weiran.system.api.user.UserService;
import com.weiran.system.api.user.UserView;
import java.util.Objects;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 独立事务持久化基座状态变化引起的惰性失效，不被后续陈旧预览拒绝回滚。 */
public class ReviewUserValidationService {
    private final ReviewWorkflowRepository workflow;
    private final UserService users;

    public ReviewUserValidationService(final ReviewWorkflowRepository workflow, final UserService users) {
        this.workflow = workflow;
        this.users = users;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void validate(final long competitionId) {
        this.workflow.lockCompetition(competitionId);
        boolean changed = false;
        for (final WorkflowPhase related : this.workflow.phases(competitionId)) {
            for (final ReviewerSnapshot old : this.workflow.reviewerSnapshots(related.id())) {
                ReviewerSnapshot current;
                try {
                    final UserView user = this.users.get(old.userId());
                    current = new ReviewerSnapshot(user.id(), user.status(), user.updatedAt());
                } catch (final BizException ex) {
                    if (ex.getErrorCode().httpStatus() != 404) {
                        throw ex;
                    }
                    current = new ReviewerSnapshot(old.userId(), "disabled", null);
                }
                if (!Objects.equals(old, current)) {
                    this.workflow.refreshReviewer(related.id(), current);
                    this.workflow.invalidateReviewer(related.id(), current.userId(), "评委账号状态或资料已变化，需重新复核");
                    changed = true;
                }
            }
        }
        if (changed) {
            for (final WorkflowPhase related : this.workflow.phases(competitionId)) {
                this.workflow.bumpInput(related.id());
            }
        }
    }
}
