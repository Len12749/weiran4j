package com.weiran.cqt.domain.review;

import com.weiran.cqt.domain.review.ReviewModels.Assignment;
import com.weiran.cqt.domain.review.ReviewModels.Correction;
import com.weiran.cqt.domain.review.ReviewModels.Dimension;
import com.weiran.cqt.domain.review.ReviewModels.DimensionScore;
import com.weiran.cqt.domain.review.ReviewModels.Entry;
import com.weiran.cqt.domain.review.ReviewModels.FinalScore;
import com.weiran.cqt.domain.review.ReviewModels.InitialDecision;
import com.weiran.cqt.domain.review.ReviewModels.Member;
import com.weiran.cqt.domain.review.ReviewModels.Option;
import com.weiran.cqt.domain.review.ReviewModels.Qualification;
import com.weiran.cqt.domain.review.ReviewModels.Rule;
import com.weiran.cqt.domain.review.ReviewModels.Scope;
import com.weiran.cqt.domain.review.ReviewModels.Score;
import com.weiran.cqt.domain.review.ReviewModels.ScoreRevision;
import com.weiran.cqt.domain.review.ReviewModels.Snapshot;
import com.weiran.cqt.domain.review.ReviewModels.Target;
import com.weiran.cqt.domain.review.ReviewModels.TargetDecision;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 评审业务持久化端口。 */
public interface ReviewRepository {
    List<Entry> entries(long competitionId);

    Entry entry(long id);

    List<Member> members(long entryId);

    List<Target> targets(long competitionId);

    Target target(long id);

    List<String> credentials(long targetId);

    boolean hasLiveReview(long entryId);

    boolean referenced(long entryId);

    int prepareTargets(long entryId);

    void decideEntry(long entryId, String decision, String reason, long operatorId, long expectedVersion);

    List<InitialDecision> decisions(long entryId);

    long createPhase(long competitionId, String code);

    void phaseState(long phaseId, String state, long expectedVersion);

    Rule rule(long id);

    List<Dimension> dimensions(long ruleId);

    long saveRule(
            long phaseId, BigDecimal min, BigDecimal max, int minimum, @Nullable BigDecimal passing, long operatorId);

    void addDimension(Dimension dimension);

    List<Scope> scopes(long phaseId);

    Scope scope(long id);

    long createScope(Scope scope);

    void updateScope(Scope scope, long expectedVersion);

    List<Assignment> assignments(long phaseId);

    List<Assignment> userAssignments(long userId);

    Assignment assignment(long id);

    long assign(long phaseId, long targetId, long userId, ReviewerSnapshot snapshot);

    void assignmentState(long id, String state, long expectedVersion);

    Optional<Score> score(long assignmentId);

    List<ScoreRevision> revisions(long scoreId);

    List<DimensionScore> dimensionScores(long revisionId);

    long saveScore(
            long assignmentId,
            long ruleId,
            String status,
            @Nullable BigDecimal total,
            String comment,
            List<DimensionScore> dimensions,
            long operatorId,
            long expectedVersion);

    List<FinalScore> finals(long phaseId);

    FinalScore finalScore(long id);

    long saveFinal(long phaseId, long targetId, BigDecimal average, int count, long snapshotId);

    void staleFinal(long phaseId, long targetId);

    long snapshot(
            long phaseId,
            long targetId,
            long inputVersion,
            String kind,
            String inputJson,
            String outputJson,
            String reason,
            long operatorId);

    List<Snapshot> snapshots(long phaseId, long targetId);

    void correct(FinalScore score, BigDecimal value, String reason, long operatorId, long expectedVersion);

    List<Correction> corrections(long scoreId);

    List<TargetDecision> targetDecisions(long phaseId);

    void decideTarget(long phaseId, long targetId, String decision, String reason, long finalVersion);

    long qualify(long phaseId, long targetId, long finalVersion);

    List<Qualification> qualifications(long competitionId);

    Qualification qualification(long id);

    void attach(long id, long nationalTargetId, String reason, long expectedVersion);

    boolean separateNational(long competitionId);

    List<Option> competitions();

    List<Option> categories();

    List<Option> regions();
}
