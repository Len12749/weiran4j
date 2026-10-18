package com.weiran.cqt.application.award;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiran.cqt.api.award.AwardBatchView;
import com.weiran.cqt.api.award.AwardPreviewCommand;
import com.weiran.cqt.api.award.AwardPreviewRow;
import com.weiran.cqt.api.award.AwardPreviewView;
import com.weiran.cqt.api.award.AwardRuleCommand;
import com.weiran.cqt.api.award.AwardRuleView;
import com.weiran.cqt.api.award.AwardService;
import com.weiran.cqt.application.review.ReviewWorkflowSupport;
import com.weiran.cqt.domain.award.AwardBatch;
import com.weiran.cqt.domain.award.AwardRepository;
import com.weiran.cqt.domain.award.AwardRule;
import com.weiran.cqt.domain.review.AwardCandidate;
import com.weiran.cqt.domain.review.ReviewWorkflowRepository;
import com.weiran.cqt.domain.review.WorkflowPhase;
import com.weiran.cqt.domain.review.WorkflowPreview;
import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 所有评奖写入与评审共用阶段锁、输入版本及幂等回执。 */
public class AwardApplicationService implements AwardService {
    private static final String PURPOSE = "AWARD_CONFIRM";
    private final AwardRepository repository;
    private final ReviewWorkflowRepository workflow;
    private final ReviewWorkflowSupport support;
    private final ObjectMapper mapper;
    private final Clock clock;

    public AwardApplicationService(
            final AwardRepository repository,
            final ReviewWorkflowRepository workflow,
            final ReviewWorkflowSupport support,
            final ObjectMapper mapper,
            final Clock clock) {
        this.repository = repository;
        this.workflow = workflow;
        this.support = support;
        this.mapper = mapper;
        this.clock = clock;
    }
    /** 保存于预览及批次的完整不可变输入。 */
    public record Snapshot(
            AwardPreviewCommand command,
            long inputVersion,
            List<AwardRule> rules,
            List<AwardCandidate> candidates,
            @Nullable BatchReference replacing,
            List<AwardPreviewRow> rows) {}

    public record BatchReference(long id, long version, String status) {}

    public record Confirmation(
            String previewToken, @Nullable Long supersedesBatchId) {}

    public record Revocation(long id, String reason, long expectedVersion) {}

    @Override
    public PageResult<AwardRuleView> rules(
            final @Nullable Long competitionId, final @Nullable Long phaseId, final PageQuery page) {
        return this.repository.rules(competitionId, phaseId, page).map(AwardApplicationService::ruleView);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long createRule(final long operatorId, final AwardRuleCommand command) {
        final WorkflowPhase phase = this.support.lockAndValidate(command.phaseId());
        final AwardRule value = this.rule(operatorId, 0, 1, command, phase);
        this.checkOverlap(value);
        final long id = this.repository.insertRule(value);
        this.repository.invalidateBatches(phase.id(), "奖项规则已变更");
        this.workflow.bumpInput(phase.id());
        return id;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long updateRule(final long operatorId, final long ruleId, final AwardRuleCommand command) {
        final AwardRule before =
                this.repository.currentRule(ruleId).orElseThrow(() -> BizException.notFound("奖项规则不存在"));
        final WorkflowPhase phase = this.support.lockAndValidate(before.phaseId());
        final AwardRule current =
                this.repository.currentRule(ruleId).orElseThrow(() -> BizException.notFound("奖项规则不存在"));
        if (command.phaseId() != current.phaseId()
                || command.expectedVersion() == null
                || command.expectedVersion() != current.revision()) {
            throw BizException.conflict("规则版本或阶段已变化，请刷新");
        }
        final AwardRule value = this.rule(operatorId, ruleId, current.revision() + 1, command, phase);
        this.checkOverlap(value);
        this.repository.retireRule(ruleId);
        this.repository.insertRule(value);
        this.repository.invalidateBatches(phase.id(), "奖项规则已变更");
        this.workflow.bumpInput(phase.id());
        return value.revision();
    }

    private AwardRule rule(
            final long operatorId,
            final long ruleId,
            final long revision,
            final AwardRuleCommand command,
            final WorkflowPhase phase) {
        AwardApplicationService.numericPhase(phase);
        if (phase.currentRuleId() <= 0) {
            throw BizException.conflict("请先配置本阶段评分范围");
        }
        if (command.minScore() == null || command.maxScore() == null) {
            throw BizException.badRequest("请填写奖项分数上下界");
        }
        final AwardRule value = new AwardRule(
                0,
                ruleId,
                phase.id(),
                Objects.requireNonNullElse(command.targetType(), ""),
                command.firstCategoryId(),
                command.secondCategoryId(),
                command.groupName(),
                Objects.requireNonNullElse(command.awardName(), ""),
                command.minScore(),
                command.maxScore(),
                command.enabled(),
                revision,
                operatorId,
                LocalDateTime.now(this.clock));
        final BigDecimal[] bounds = this.repository.phaseBounds(phase.currentRuleId());
        if (value.minScore().compareTo(bounds[0]) < 0 || value.maxScore().compareTo(bounds[1]) > 0) {
            throw BizException.badRequest("奖项区间必须在本阶段评分范围内");
        }
        if (!this.repository.validCategory(
                phase.competitionId(), value.firstCategoryId(), value.secondCategoryId(), value.groupName())) {
            throw BizException.badRequest("赛项、专业或组别配置无效");
        }
        return value;
    }

    private void checkOverlap(final AwardRule value) {
        for (final AwardRule other : this.repository.currentRules(value.phaseId())) {
            if (other.ruleId() != value.ruleId() && value.overlaps(other)) {
                throw BizException.conflict("适用范围和分数区间与规则 " + other.ruleId() + " 重叠");
            }
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AwardPreviewView preview(final long operatorId, final AwardPreviewCommand command) {
        final WorkflowPhase phase = this.support.lockAndValidate(command.phaseId());
        final Snapshot snapshot = this.snapshot(phase, command);
        final WorkflowPreview saved =
                this.support.preview(AwardApplicationService.PURPOSE, operatorId, phase.id(), snapshot);
        return new AwardPreviewView(
                saved.token(),
                saved.expiresAt(),
                phase.inputVersion(),
                AwardApplicationService.summary(snapshot.rows()),
                snapshot.rows(),
                snapshot.rows().stream()
                        .filter(row -> "BLOCKED".equals(row.status()))
                        .toList());
    }

    private Snapshot snapshot(final WorkflowPhase phase, final AwardPreviewCommand command) {
        AwardApplicationService.numericPhase(phase);
        if (phase.competitionId() != command.competitionId() || !"FROZEN".equals(phase.state())) {
            throw BizException.conflict("仅可对所选赛事已冻结阶段评奖");
        }
        if (command.targetType() != null && !Set.of("ENTRY", "PARTICIPANT").contains(command.targetType())) {
            throw BizException.badRequest("对象类型无效");
        }
        final List<AwardRule> rules = this.repository.currentRules(phase.id()).stream()
                .filter(AwardRule::enabled)
                .toList();
        if (rules.isEmpty()) {
            throw BizException.conflict("没有启用的奖项规则");
        }
        final List<Long> ids = command.targetIds();
        if (ids != null && (ids.size() > 500 || ids.stream().anyMatch(id -> id == null || id <= 0))) {
            throw BizException.badRequest("候选对象ID无效或超过500个");
        }
        final Set<Long> requested = ids == null ? Set.of() : Set.copyOf(ids);
        final List<AwardCandidate> candidates = this.workflow.awardCandidates(phase.id()).stream()
                .filter(c -> requested.isEmpty() || requested.contains(c.targetId()))
                .filter(c ->
                        command.targetType() == null || command.targetType().equals(c.targetType()))
                .filter(c -> command.firstCategoryId() == null || command.firstCategoryId() == c.firstCategoryId())
                .filter(c -> command.secondCategoryId() == null
                        || Objects.equals(command.secondCategoryId(), c.secondCategoryId()))
                .filter(c -> command.groupName() == null || command.groupName().equals(c.groupName()))
                .sorted(Comparator.comparingLong(AwardCandidate::targetId))
                .toList();
        if (candidates.isEmpty() || candidates.size() > 500) {
            throw BizException.badRequest("候选为空或超过500个，请缩小范围");
        }
        if (!requested.isEmpty() && candidates.size() != requested.size()) {
            throw BizException.conflict("部分候选不存在、归属不符或不符合筛选条件");
        }
        final List<Long> candidateIds =
                candidates.stream().map(AwardCandidate::targetId).toList();
        final AwardBatch replacing;
        if (command.supersedesBatchId() != null) {
            replacing = this.repository
                    .batch(command.supersedesBatchId())
                    .orElseThrow(() -> BizException.notFound("待替代批次不存在"));
            if (replacing.phaseId() != phase.id()
                    || "REVOKED".equals(replacing.status())
                    || !this.ids(replacing).equals(candidateIds)) {
                throw BizException.conflict("替代必须包含原批次完整候选且属于同一有效阶段");
            }
        } else {
            replacing = null;
        }
        final Set<Long> occupied = new HashSet<>();
        for (final AwardBatch batch : this.repository.effectiveBatches(phase.id())) {
            if (replacing == null || batch.id() != replacing.id()) {
                occupied.addAll(this.ids(batch));
            }
        }
        final List<AwardPreviewRow> rows = new ArrayList<>();
        for (final AwardCandidate candidate : candidates) {
            final List<AwardRule> matches =
                    rules.stream().filter(r -> r.matches(candidate)).toList();
            String reason = null;
            if (!candidate.eligible() || !"VALID".equals(candidate.finalState()) || candidate.score() == null) {
                reason = candidate.blockedReason().isBlank() ? "成绩或对象已失效、尚未确认" : candidate.blockedReason();
            } else if (occupied.contains(candidate.targetId())) {
                reason = "对象已有有效批次，请显式选择完整替代";
            } else if (matches.size() > 1) {
                reason = "对象重复命中奖项规则";
            }
            final AwardRule match = matches.size() == 1 ? matches.getFirst() : null;
            rows.add(new AwardPreviewRow(
                    candidate.targetId(),
                    candidate.entryId(),
                    candidate.participantId(),
                    candidate.targetType(),
                    candidate.displayName(),
                    candidate.title(),
                    candidate.score(),
                    candidate.finalVersion(),
                    match == null ? null : match.ruleId(),
                    match == null ? null : match.revision(),
                    match == null ? null : match.awardName(),
                    reason != null ? "BLOCKED" : match == null ? "UNMATCHED" : "MATCHED",
                    reason));
        }
        return new Snapshot(
                command,
                phase.inputVersion(),
                rules,
                candidates,
                replacing == null ? null : new BatchReference(replacing.id(), replacing.version(), replacing.status()),
                List.copyOf(rows));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long confirm(
            final long operatorId,
            final String token,
            final String requestKey,
            final @Nullable Long supersedesBatchId) {
        AwardApplicationService.token(token);
        AwardApplicationService.key(requestKey);
        final Confirmation body = new Confirmation(token, supersedesBatchId);
        final Optional<Long> old =
                this.support.receipt(operatorId, AwardApplicationService.PURPOSE, requestKey, body, Long.class);
        if (old.isPresent()) {
            return old.get();
        }
        this.support.lockPreview(token, operatorId);
        final Optional<Long> replay =
                this.support.receipt(operatorId, AwardApplicationService.PURPOSE, requestKey, body, Long.class);
        if (replay.isPresent()) {
            return replay.get();
        }
        final WorkflowPreview preview = this.support.requirePreview(token, AwardApplicationService.PURPOSE, operatorId);
        final WorkflowPhase phase = this.workflow.lockPhase(preview.phaseId());
        this.support.consumePreview(token);
        if (this.repository.batchByPreview(token).isPresent()) {
            throw BizException.conflict("预览已被确认，请重新预览");
        }
        final Snapshot stored = this.support.read(preview.snapshot(), Snapshot.class);
        if (!Objects.equals(stored.command().supersedesBatchId(), supersedesBatchId)) {
            throw BizException.conflict("替代批次与预览不一致");
        }
        final Snapshot current = this.snapshot(phase, stored.command());
        if (preview.inputVersion() != phase.inputVersion()
                || !preview.payloadHash().equals(this.support.hash(current))) {
            throw BizException.conflict("预览输入已变化，请重新预览");
        }
        if (current.rows().stream().anyMatch(row -> "BLOCKED".equals(row.status()))) {
            throw BizException.conflict("预览存在阻断对象，不能确认");
        }
        if (current.rows().stream().noneMatch(row -> "MATCHED".equals(row.status()))) {
            throw BizException.conflict("没有匹配奖项，不能创建空批次");
        }
        if (current.replacing() != null) {
            this.repository.transitionBatch(
                    current.replacing().id(), current.replacing().version(), "STALE", "已被新批次替代");
        }
        final long id = this.repository.insertBatch(
                phase.id(),
                phase.inputVersion(),
                token,
                supersedesBatchId,
                operatorId,
                this.json(current.candidates().stream()
                        .map(AwardCandidate::targetId)
                        .toList()),
                this.json(current));
        for (final AwardPreviewRow row : current.rows()) {
            if (!"MATCHED".equals(row.status())) {
                continue;
            }
            final AwardCandidate candidate = current.candidates().stream()
                    .filter(c -> c.targetId() == row.targetId())
                    .findFirst()
                    .orElseThrow();
            final AwardRule rule = current.rules().stream()
                    .filter(r -> Objects.equals(r.ruleId(), row.ruleId())
                            && Objects.equals(r.revision(), row.ruleRevision()))
                    .findFirst()
                    .orElseThrow();
            this.repository.insertAward(id, candidate, rule, this.json(row));
        }
        this.support.complete(operatorId, AwardApplicationService.PURPOSE, requestKey, body, id);
        return id;
    }

    @Override
    public PageResult<AwardBatchView> batches(
            final @Nullable Long competitionId,
            final @Nullable Long phaseId,
            final @Nullable String status,
            final PageQuery page) {
        if (status != null && !Set.of("CONFIRMED", "STALE", "REVOKED").contains(status)) {
            throw BizException.badRequest("批次状态无效");
        }
        final Set<Long> validated = new HashSet<>();
        PageResult<AwardBatch> result = this.repository.batches(competitionId, phaseId, status, page);
        while (true) {
            final List<Long> phases = result.list().stream()
                    .map(AwardBatch::phaseId)
                    .distinct()
                    .filter(id -> !validated.contains(id))
                    .sorted()
                    .toList();
            if (phases.isEmpty()) {
                break;
            }
            for (final long id : phases) {
                this.support.lockAndValidate(id);
                validated.add(id);
            }
            result = this.repository.batches(competitionId, phaseId, status, page);
        }
        return result.map(this::batchView);
    }

    @Override
    public AwardBatchView detail(final long id) {
        final AwardBatch before = this.repository.batch(id).orElseThrow(() -> BizException.notFound("批次不存在"));
        this.support.lockAndValidate(before.phaseId());
        return this.batchView(this.repository.batch(id).orElseThrow(() -> BizException.notFound("批次不存在")));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void revoke(
            final long operatorId,
            final long id,
            final String reason,
            final long expectedVersion,
            final String requestKey) {
        AwardApplicationService.required(reason, "撤销原因");
        AwardApplicationService.key(requestKey);
        final Revocation body = new Revocation(id, reason, expectedVersion);
        if (this.support
                .receipt(operatorId, "AWARD_REVOKE", requestKey, body, Long.class)
                .isPresent()) {
            return;
        }
        final AwardBatch before = this.repository.batch(id).orElseThrow(() -> BizException.notFound("批次不存在"));
        this.support.lockAndValidate(before.phaseId());
        if (this.support
                .receipt(operatorId, "AWARD_REVOKE", requestKey, body, Long.class)
                .isPresent()) {
            return;
        }
        final AwardBatch batch = this.repository.batch(id).orElseThrow();
        if ("REVOKED".equals(batch.status())) {
            throw BizException.conflict("批次已撤销，请刷新");
        }
        this.repository.transitionBatch(id, expectedVersion, "REVOKED", reason.strip());
        this.support.complete(operatorId, "AWARD_REVOKE", requestKey, body, id);
    }

    private List<Long> ids(final AwardBatch batch) {
        return this.support.read(batch.snapshotJson(), Snapshot.class).candidates().stream()
                .map(AwardCandidate::targetId)
                .sorted()
                .toList();
    }

    private AwardBatchView batchView(final AwardBatch batch) {
        return new AwardBatchView(
                batch.id(),
                batch.phaseId(),
                batch.status(),
                batch.inputVersion(),
                batch.previewId(),
                batch.supersedesBatchId(),
                batch.reason(),
                batch.version(),
                batch.createdBy(),
                batch.createdAt(),
                batch.updatedAt(),
                this.support.read(batch.snapshotJson(), Snapshot.class).rows());
    }

    private static AwardRuleView ruleView(final AwardRule rule) {
        return new AwardRuleView(
                rule.ruleId(),
                rule.id(),
                rule.phaseId(),
                rule.targetType(),
                rule.firstCategoryId(),
                rule.secondCategoryId(),
                rule.groupName(),
                rule.awardName(),
                rule.minScore(),
                rule.maxScore(),
                rule.enabled(),
                rule.revision(),
                rule.revision(),
                rule.createdAt());
    }

    private static void numericPhase(final WorkflowPhase phase) {
        if (!Set.of("PROVINCIAL", "NATIONAL").contains(phase.code())) {
            throw BizException.badRequest("奖项规则仅支持省审和国审阶段");
        }
    }

    private static void required(final @Nullable String value, final String field) {
        if (value == null || value.isBlank() || value.length() > 2000) {
            throw BizException.badRequest(field + "不能为空或过长");
        }
    }

    private static void key(final @Nullable String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw BizException.badRequest("幂等键不能为空且最多100字");
        }
    }

    private static void token(final @Nullable String value) {
        if (value == null
                || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw BizException.badRequest("预览标识格式无效");
        }
    }

    private String json(final Object value) {
        try {
            return this.mapper.writeValueAsString(value);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("评奖快照序列化失败", ex);
        }
    }

    private static Map<String, Integer> summary(final List<AwardPreviewRow> rows) {
        final Map<String, Integer> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        for (final String state : List.of("MATCHED", "UNMATCHED", "BLOCKED")) {
            result.put(state.toLowerCase(java.util.Locale.ROOT), (int)
                    rows.stream().filter(r -> state.equals(r.status())).count());
        }
        return Map.copyOf(result);
    }
}
