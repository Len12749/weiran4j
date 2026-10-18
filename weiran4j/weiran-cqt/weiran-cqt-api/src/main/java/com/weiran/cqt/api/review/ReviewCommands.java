package com.weiran.cqt.api.review;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** 业务输入；敏感文字仅反序列化，不进入操作日志参数序列化。 */
public final class ReviewCommands {
    private ReviewCommands() {}

    public record Confirm(String previewToken, String requestKey, boolean acknowledgeSkipped) {
        public Confirm {
            previewToken = previewToken == null ? "" : previewToken;
            requestKey = requestKey == null ? "" : requestKey;
        }
    }

    public record Decision(String decision, @Nullable String reason, long expectedVersion) {
        public Decision {
            decision = decision == null ? "" : decision;
        }
    }

    public record Prepare(long competitionId, List<Long> entryIds) {}

    public record PhaseCreate(long competitionId) {}

    public record Rule(
            BigDecimal minScore,
            BigDecimal maxScore,
            int minimumReviewers,
            @Nullable BigDecimal passingScore,
            long expectedVersion) {}

    public record Dimension(
            @Nullable Long id,
            long firstCategoryId,
            String name,
            @Nullable String description,
            BigDecimal minScore,
            BigDecimal maxScore,
            int sort) {
        public Dimension {
            name = name == null ? "" : name;
        }
    }

    public record Dimensions(List<Dimension> dimensions, long expectedVersion) {}

    public record State(String state, @Nullable String reason, long expectedVersion) {
        public State {
            state = state == null ? "" : state;
        }
    }

    public record Scope(
            long userId,
            long competitionId,
            long phaseId,
            long firstCategoryId,
            @Nullable Long secondCategoryId,
            @Nullable Long regionId,
            boolean enabled,
            long expectedVersion,
            @Nullable String reason,
            @Nullable String previewToken) {}

    public record AssignmentPreview(
            long phaseId, String mode, List<Long> targetIds, List<Long> userIds, int reviewersPerTarget) {
        public AssignmentPreview {
            mode = mode == null ? "" : mode;
        }
    }

    public record AssignmentState(
            String state,
            @Nullable String reason,
            long expectedVersion,
            @Nullable String previewToken,
            String requestKey) {
        public AssignmentState {
            state = state == null ? "" : state;
            requestKey = requestKey == null ? "" : requestKey;
        }
    }

    public record DimensionScore(
            long dimensionId,
            @Nullable BigDecimal score,
            @Nullable String comment) {}

    public record Score(
            String status,
            @Nullable BigDecimal total,
            List<DimensionScore> dimensions,
            @Nullable String comment,
            long expectedVersion,
            String requestKey) {
        public Score {
            status = status == null ? "" : status;
            requestKey = requestKey == null ? "" : requestKey;
        }
    }

    public record Correction(BigDecimal finalScore, String reason, long expectedVersion) {
        public Correction {
            reason = reason == null ? "" : reason;
        }
    }

    public record Advance(
            @Nullable BigDecimal threshold,
            List<Long> targetIds,
            String decision,
            @Nullable String reason) {
        public Advance {
            decision = decision == null ? "" : decision;
        }
    }

    public record Promote(List<Long> targetIds, long expectedVersion, String requestKey) {
        public Promote {
            requestKey = requestKey == null ? "" : requestKey;
        }
    }

    public record Attach(
            long nationalTargetId,
            String reason,
            long expectedVersion,
            @Nullable String previewToken,
            String requestKey) {
        public Attach {
            requestKey = requestKey == null ? "" : requestKey;
            reason = reason == null ? "" : reason;
        }
    }

    public record IdVersion(long id, long version) {}

    public record Count(int created, int unchanged, int blocked) {}
}
