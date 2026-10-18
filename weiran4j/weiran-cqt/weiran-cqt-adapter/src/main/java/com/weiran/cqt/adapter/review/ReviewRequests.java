package com.weiran.cqt.adapter.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weiran.cqt.api.review.ReviewCommands;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** 业务输入；敏感文字仅反序列化，不进入操作日志参数序列化。 */
public final class ReviewRequests {
    private ReviewRequests() {}

    public record Confirm(
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            String previewToken,

            String requestKey,
            boolean acknowledgeSkipped) {
        public ReviewCommands.Confirm command() {
            return new ReviewCommands.Confirm(this.previewToken(), this.requestKey(), this.acknowledgeSkipped());
        }
    }

    public record Decision(
            String decision,

            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable
            String reason,

            long expectedVersion) {
        public ReviewCommands.Decision command() {
            return new ReviewCommands.Decision(this.decision(), this.reason(), this.expectedVersion());
        }
    }

    public record Prepare(long competitionId, List<Long> entryIds) {
        public ReviewCommands.Prepare command() {
            return new ReviewCommands.Prepare(this.competitionId(), this.entryIds());
        }
    }

    public record PhaseCreate(long competitionId) {
        public ReviewCommands.PhaseCreate command() {
            return new ReviewCommands.PhaseCreate(this.competitionId());
        }
    }

    public record Rule(
            BigDecimal minScore,
            BigDecimal maxScore,
            int minimumReviewers,
            @Nullable BigDecimal passingScore,
            long expectedVersion) {
        public ReviewCommands.Rule command() {
            return new ReviewCommands.Rule(
                    this.minScore(),
                    this.maxScore(),
                    this.minimumReviewers(),
                    this.passingScore(),
                    this.expectedVersion());
        }
    }

    public record Dimension(
            @Nullable Long id,
            long firstCategoryId,
            String name,
            @Nullable String description,
            BigDecimal minScore,
            BigDecimal maxScore,
            int sort) {
        public ReviewCommands.Dimension command() {
            return new ReviewCommands.Dimension(
                    this.id(),
                    this.firstCategoryId(),
                    this.name(),
                    this.description(),
                    this.minScore(),
                    this.maxScore(),
                    this.sort());
        }
    }

    public record Dimensions(List<Dimension> dimensions, long expectedVersion) {
        public ReviewCommands.Dimensions command() {
            return new ReviewCommands.Dimensions(
                    this.dimensions() == null
                            ? java.util.List.of()
                            : this.dimensions().stream().map(Dimension::command).toList(),
                    this.expectedVersion());
        }
    }

    public record State(
            String state,

            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable
            String reason,

            long expectedVersion) {
        public ReviewCommands.State command() {
            return new ReviewCommands.State(this.state(), this.reason(), this.expectedVersion());
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

            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable
            String reason,

            @Nullable @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            String previewToken) {
        public ReviewCommands.Scope command() {
            return new ReviewCommands.Scope(
                    this.userId(),
                    this.competitionId(),
                    this.phaseId(),
                    this.firstCategoryId(),
                    this.secondCategoryId(),
                    this.regionId(),
                    this.enabled(),
                    this.expectedVersion(),
                    this.reason(),
                    this.previewToken());
        }
    }

    public record AssignmentPreview(
            long phaseId, String mode, List<Long> targetIds, List<Long> userIds, int reviewersPerTarget) {
        public ReviewCommands.AssignmentPreview command() {
            return new ReviewCommands.AssignmentPreview(
                    this.phaseId(), this.mode(), this.targetIds(), this.userIds(), this.reviewersPerTarget());
        }
    }

    public record AssignmentState(
            String state,

            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable
            String reason,

            long expectedVersion,

            @Nullable @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            String previewToken,

            String requestKey) {
        public ReviewCommands.AssignmentState command() {
            return new ReviewCommands.AssignmentState(
                    this.state(), this.reason(), this.expectedVersion(), this.previewToken(), this.requestKey());
        }
    }

    public record DimensionScore(
            long dimensionId,
            @Nullable BigDecimal score,

            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable
            String comment) {
        public ReviewCommands.DimensionScore command() {
            return new ReviewCommands.DimensionScore(this.dimensionId(), this.score(), this.comment());
        }
    }

    public record Score(
            String status,
            @Nullable BigDecimal total,
            List<DimensionScore> dimensions,

            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable
            String comment,

            long expectedVersion,
            String requestKey) {
        public ReviewCommands.Score command() {
            return new ReviewCommands.Score(
                    this.status(),
                    this.total(),
                    this.dimensions() == null
                            ? java.util.List.of()
                            : this.dimensions().stream()
                                    .map(DimensionScore::command)
                                    .toList(),
                    this.comment(),
                    this.expectedVersion(),
                    this.requestKey());
        }
    }

    public record Correction(
            BigDecimal finalScore,

            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            String reason,

            long expectedVersion) {
        public ReviewCommands.Correction command() {
            return new ReviewCommands.Correction(this.finalScore(), this.reason(), this.expectedVersion());
        }
    }

    public record Advance(
            @Nullable BigDecimal threshold,
            List<Long> targetIds,
            String decision,

            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable
            String reason) {
        public ReviewCommands.Advance command() {
            return new ReviewCommands.Advance(this.threshold(), this.targetIds(), this.decision(), this.reason());
        }
    }

    public record Promote(List<Long> targetIds, long expectedVersion, String requestKey) {
        public ReviewCommands.Promote command() {
            return new ReviewCommands.Promote(this.targetIds(), this.expectedVersion(), this.requestKey());
        }
    }

    public record Attach(
            long nationalTargetId,

            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            String reason,

            long expectedVersion,

            @Nullable @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            String previewToken,

            String requestKey) {
        public ReviewCommands.Attach command() {
            return new ReviewCommands.Attach(
                    this.nationalTargetId(),
                    this.reason(),
                    this.expectedVersion(),
                    this.previewToken(),
                    this.requestKey());
        }
    }

    public record IdVersion(long id, long version) {
        public ReviewCommands.IdVersion command() {
            return new ReviewCommands.IdVersion(this.id(), this.version());
        }
    }

    public record Count(int created, int unchanged, int blocked) {
        public ReviewCommands.Count command() {
            return new ReviewCommands.Count(this.created(), this.unchanged(), this.blocked());
        }
    }
}
