package com.weiran.cqt.domain.award;

import com.weiran.cqt.domain.review.AwardCandidate;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 评奖持久化端口，所有写入由应用服务持有共享phase锁。 */
public interface AwardRepository {
    PageResult<AwardRule> rules(@Nullable Long competitionId, @Nullable Long phaseId, PageQuery page);

    Optional<AwardRule> currentRule(long ruleId);

    List<AwardRule> currentRules(long phaseId);

    long insertRule(AwardRule value);

    void retireRule(long ruleId);

    java.math.BigDecimal[] phaseBounds(long currentRuleId);

    boolean validCategory(
            long competitionId, long firstCategoryId, @Nullable Long secondCategoryId, @Nullable String groupName);

    PageResult<AwardBatch> batches(
            @Nullable Long competitionId, @Nullable Long phaseId, @Nullable String status, PageQuery page);

    Optional<AwardBatch> batch(long id);

    Optional<AwardBatch> batchByPreview(String token);

    List<AwardBatch> effectiveBatches(long phaseId);

    long insertBatch(
            long phaseId,
            long inputVersion,
            String token,
            @Nullable Long replaces,
            long operatorId,
            String candidateJson,
            String snapshotJson);

    void insertAward(long batchId, AwardCandidate candidate, AwardRule rule, String snapshot);

    void transitionBatch(long batchId, long expectedVersion, String status, String reason);

    void invalidateBatches(long phaseId, String reason);
}
