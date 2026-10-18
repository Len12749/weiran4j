package com.weiran.cqt.api.award;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;

public interface AwardService {
    PageResult<AwardRuleView> rules(@Nullable Long competitionId, @Nullable Long phaseId, PageQuery page);

    long createRule(long operatorId, AwardRuleCommand command);

    long updateRule(long operatorId, long ruleId, AwardRuleCommand command);

    AwardPreviewView preview(long operatorId, AwardPreviewCommand command);

    long confirm(long operatorId, String token, String requestKey, @Nullable Long supersedesBatchId);

    PageResult<AwardBatchView> batches(
            @Nullable Long competitionId, @Nullable Long phaseId, @Nullable String status, PageQuery page);

    AwardBatchView detail(long id);

    void revoke(long operatorId, long id, String reason, long expectedVersion, String requestKey);
}
