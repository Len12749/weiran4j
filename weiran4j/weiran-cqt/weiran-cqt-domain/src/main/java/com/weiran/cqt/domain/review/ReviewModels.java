package com.weiran.cqt.domain.review;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 评审领域不可变实体与查询投影，不含持久化类型。 */
public final class ReviewModels {
    private ReviewModels() {}
    /** Entry快照。 */
    public record Entry(
            long id,
            long competitionId,
            String title,
            String source,
            String scoringScope,
            String stageScope,
            String status,
            int legacyStatus,
            long version,
            long firstCategoryId,
            @Nullable Long secondCategoryId,
            @Nullable Long regionId,
            String groupName,
            String attachmentUrl,
            String description) {}
    /** Target快照。 */
    public record Target(
            long id,
            long entryId,
            @Nullable Long participantId,
            long competitionId,
            String source,
            String targetType,
            String name,
            String title,
            long firstCategoryId,
            @Nullable Long secondCategoryId,
            @Nullable Long regionId,
            String groupName,
            String stageScope,
            String status,
            String entryStatus,
            int legacyStatus,
            long version) {}
    /** Member快照。 */
    public record Member(long id, String name, String school, String groupName) {}
    /** Rule快照。 */
    public record Rule(
            long id,
            long phaseId,
            long revision,
            BigDecimal minScore,
            BigDecimal maxScore,
            int minimumReviewers,
            @Nullable BigDecimal passingScore) {}
    /** Dimension快照。 */
    public record Dimension(
            long id,
            long ruleId,
            long firstCategoryId,
            String name,
            String description,
            BigDecimal minScore,
            BigDecimal maxScore,
            int sort) {}
    /** Scope快照。 */
    public record Scope(
            long id,
            long userId,
            long competitionId,
            long phaseId,
            long firstCategoryId,
            @Nullable Long secondCategoryId,
            @Nullable Long regionId,
            boolean enabled,
            long version) {}
    /** Assignment快照。 */
    public record Assignment(
            long id,
            long phaseId,
            long targetId,
            long reviewerId,
            String state,
            long version,
            String userState,
            @Nullable LocalDateTime userUpdatedAt) {}
    /** Score快照。 */
    public record Score(long id, long assignmentId, long currentRevision, String status, long ruleId, long version) {}
    /** ScoreRevision快照。 */
    public record ScoreRevision(
            long id,
            long scoreId,
            long revision,
            @Nullable BigDecimal total,
            String comment,
            String status,
            long ruleId,
            long operatorId,
            LocalDateTime createdAt) {}
    /** DimensionScore快照。 */
    public record DimensionScore(long dimensionId, @Nullable BigDecimal score, String comment) {}
    /** FinalScore快照。 */
    public record FinalScore(
            long id,
            long phaseId,
            long targetId,
            @Nullable BigDecimal finalScore,
            @Nullable BigDecimal averageScore,
            int validReviewerCount,
            String state,
            long version,
            long snapshotId) {}
    /** TargetDecision快照。 */
    public record TargetDecision(
            long phaseId, long targetId, String decision, long version, String reason, long inputFinalVersion) {}
    /** Qualification快照。 */
    public record Qualification(
            long id,
            long sourcePhaseId,
            long sourceTargetId,
            long finalVersion,
            @Nullable Long nationalTargetId,
            String status,
            long version,
            String reason) {}
    /** InitialDecision快照。 */
    public record InitialDecision(
            long id,
            long entryId,
            String decision,
            String reason,
            long operatorId,
            long version,
            LocalDateTime createdAt) {}
    /** Snapshot快照。 */
    public record Snapshot(
            long id,
            long phaseId,
            long targetId,
            long inputVersion,
            String kind,
            String inputJson,
            String outputJson,
            String reason,
            long operatorId,
            LocalDateTime createdAt) {}
    /** Correction快照。 */
    public record Correction(
            long id,
            long finalScoreId,
            @Nullable BigDecimal beforeScore,
            BigDecimal afterScore,
            String reason,
            long operatorId,
            long finalVersion,
            LocalDateTime createdAt) {}
    /** Option快照。 */
    public record Option(long id, String name, @Nullable Long parentId) {}
    /** UserOption快照。 */
    public record UserOption(long id, String username, String nickname) {}
}
