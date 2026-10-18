package com.weiran.cqt.application.review;

import com.weiran.cqt.api.review.PreviewResponse;
import com.weiran.cqt.api.review.ReviewCommands;
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
import com.weiran.cqt.domain.review.ReviewModels.UserOption;
import com.weiran.cqt.domain.review.ReviewRepository;
import com.weiran.cqt.domain.review.ReviewRules;
import com.weiran.cqt.domain.review.ReviewWorkflowRepository;
import com.weiran.cqt.domain.review.ReviewerSnapshot;
import com.weiran.cqt.domain.review.WorkflowPhase;
import com.weiran.cqt.domain.review.WorkflowPreview;
import com.weiran.framework.auth.CurrentUser;
import com.weiran.framework.error.BizException;
import com.weiran.framework.error.CommonErrors;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.system.api.user.UserService;
import com.weiran.system.api.user.UserView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/** 初审到晋级的明确领域用例；所有影响输入的命令共用阶段锁。 */
public class ReviewApplicationService {
    private final ReviewRepository repository;
    private final ReviewWorkflowRepository workflow;
    private final ReviewWorkflowSupport support;
    private final UserService users;

    public ReviewApplicationService(
            final ReviewRepository repository,
            final ReviewWorkflowRepository workflow,
            final ReviewWorkflowSupport support,
            final UserService users) {
        this.repository = repository;
        this.workflow = workflow;
        this.support = support;
        this.users = users;
    }

    public record Options(
            List<Option> competitions,
            List<Option> categories,
            List<Option> regions,
            List<UserOption> users,
            List<WorkflowPhase> phases) {}

    public record PhaseView(
            long id,
            long competitionId,
            String code,
            String state,
            long currentRuleId,
            long inputVersion,
            long version,
            @Nullable Rule rule,
            List<Dimension> dimensions) {}

    public record EntryDetail(Entry entry, List<Member> members, List<Target> targets, List<InitialDecision> history) {}

    public record TaskView(
            long id,
            long phaseId,
            long targetId,
            long reviewerId,
            String state,
            long version,
            String scoreStatus,
            Target target) {}

    public record TaskDetail(
            Assignment assignment,
            Target target,
            Entry entry,
            Rule rule,
            List<Dimension> dimensions,
            @Nullable Score score,
            @Nullable ScoreRevision current,
            List<DimensionScore> dimensionScores,
            List<ScoreRevision> history,
            boolean canSubmit,
            String blockedReason) {}

    public record Candidate(
            long id,
            long targetId,
            long entryId,
            @Nullable Long participantId,
            long competitionId,
            String name,
            String title,
            String source,
            String targetType,
            long firstCategoryId,
            @Nullable Long secondCategoryId,
            @Nullable Long regionId,
            String groupName,
            long version,
            boolean eligible,
            String blockedReason,
            long assignedCount) {}

    public record ScoreView(
            long id,
            long phaseId,
            long targetId,
            @Nullable BigDecimal finalScore,
            @Nullable BigDecimal averageScore,
            int validReviewerCount,
            String state,
            long version,
            long finalVersion,
            Target target,
            @Nullable TargetDecision decision) {}

    public record ScoreHistory(List<Snapshot> snapshots, List<Correction> corrections) {}

    public record PrepareRow(long entryId, long version, String action, String blockedReason, String targetHash) {}

    public record PrepareSnapshot(ReviewCommands.Prepare command, List<PrepareRow> rows) {}

    public record StateSnapshot(long phaseId, long version, String state, String reason) {}

    public record Impact(long id, String kind, String description) {}

    public record ScopeSnapshot(long id, ReviewCommands.Scope command, Scope old, List<Long> assignments) {}

    public record AssignmentRow(
            long targetId, long userId, String action, String blockedReason, long load, long userVersion) {}

    public record AssignmentSnapshot(
            ReviewCommands.AssignmentPreview command,
            List<AssignmentRow> rows,
            List<ReviewerSnapshot> users,
            List<Assignment> existing) {}

    public record AssignmentChange(long id, ReviewCommands.AssignmentState command, Assignment old) {}

    public record ScoreInput(
            long assignmentId, long reviewerId, long scoreRevisionId, long revision, BigDecimal total) {}

    public record CalculationRow(
            long targetId,
            int validReviewerCount,
            int assignedCount,
            @Nullable BigDecimal average,
            String state,
            String blockedReason,
            List<ScoreInput> inputs) {}

    public record Calculation(
            long phaseId, long ruleId, List<CalculationRow> rows, List<Scope> scopes, List<Target> targets) {}

    public record AdvancementRow(long targetId, long finalVersion, String decision, String blockedReason) {}

    public record Advancement(long phaseId, ReviewCommands.Advance command, List<AdvancementRow> rows) {}

    public record AttachSnapshot(
            long qualificationId,
            ReviewCommands.Attach command,
            long sourceVersion,
            long nationalVersion,
            List<String> credentialHashes) {}

    private static long operator() {
        return CurrentUser.require().id();
    }

    private static void version(final long actual, final long expected) {
        if (actual != expected) {
            throw BizException.conflict("记录版本已变化，请刷新后重试");
        }
    }

    private static String reason(final @Nullable String text) {
        return ReviewRules.text(text, "原因", true, 2000);
    }

    private static String comment(final @Nullable String text) {
        return ReviewRules.text(text, "评语", false, 2000);
    }

    private void numeric(final WorkflowPhase p) {
        if (!Set.of("PROVINCIAL", "NATIONAL").contains(p.code())) {
            throw BizException.conflict("初审仅支持管理员定性审核");
        }
    }

    private void validateUser(final long userId) {
        if (!"enabled".equals(this.users.get(userId).status())) {
            throw BizException.conflict("评委账号已停用");
        }
    }

    private void bumpCompetition(final long competition) {
        for (final WorkflowPhase phase : this.workflow.lockCompetition(competition)) {
            this.workflow.bumpInput(phase.id());
        }
    }

    private WorkflowPhase anchor(final long competition) {
        if (this.repository.competitions().stream().noneMatch(c -> c.id() == competition)) {
            throw BizException.notFound("赛事不存在");
        }
        this.support.lockCompetitionAndValidate(competition);
        return this.workflow.lockPhase(this.repository.createPhase(competition, "INITIAL"));
    }

    private <T> PreviewResponse<T> preview(
            final String purpose,
            final WorkflowPhase phase,
            final Object snapshot,
            final List<T> rows,
            final List<T> blocked) {
        final WorkflowPreview p = this.support.preview(purpose, operator(), phase.id(), snapshot);
        return new PreviewResponse<>(
                p.token(),
                p.expiresAt(),
                phase.inputVersion(),
                "候选" + rows.size() + "，阻断" + blocked.size(),
                rows,
                blocked);
    }

    private <T> T payload(final String token, final String purpose, final Class<T> type) {
        final T value = this.support.read(
                this.support.requirePreview(token, purpose, operator()).snapshot(), type);
        this.support.consumePreview(token);
        return value;
    }

    private void same(final Object expected, final Object current) {
        if (!this.support.hash(expected).equals(this.support.hash(current))) {
            throw BizException.conflict("预览输入已变化，请重新预览");
        }
    }

    private static boolean keyword(final String text, final @Nullable String k) {
        return k == null
                || k.isBlank()
                || text.toLowerCase(Locale.ROOT).contains(k.strip().toLowerCase(Locale.ROOT));
    }

    private static <T> PageResult<T> page(final List<T> all, final PageQuery q) {
        final int from = (int) Math.min(all.size(), (long) (q.page() - 1) * q.pageSize());
        final int to = Math.min(all.size(), from + q.pageSize());
        return new PageResult<>(all.subList(from, to), all.size(), q.page(), q.pageSize());
    }

    public Options options() {
        final var user = CurrentUser.require();
        final boolean manage = List.of(
                        "cqt:reviewer:list",
                        "cqt:reviewer:create",
                        "cqt:reviewer:update",
                        "cqt:assignment:list",
                        "cqt:assignment:create",
                        "cqt:assignment:auto")
                .stream()
                .anyMatch(user::hasPermission);
        final boolean onlyTasks = !List.of(
                        "cqt:entry-review:list",
                        "cqt:review:list",
                        "cqt:reviewer:list",
                        "cqt:assignment:list",
                        "cqt:score:list",
                        "cqt:award-rule:list",
                        "cqt:award-batch:list")
                .stream()
                .anyMatch(user::hasPermission);
        final Set<Long> phases = this.repository.userAssignments(user.id()).stream()
                .filter(a -> "ACTIVE".equals(a.state()))
                .map(Assignment::phaseId)
                .collect(java.util.stream.Collectors.toSet());
        final List<WorkflowPhase> allowed = this.workflow.phases(0).stream()
                .filter(p -> !onlyTasks || phases.contains(p.id()))
                .toList();
        final Set<Long> competitionIds =
                allowed.stream().map(WorkflowPhase::competitionId).collect(java.util.stream.Collectors.toSet());
        final Set<Long> categories = this.repository.userAssignments(user.id()).stream()
                .filter(a -> "ACTIVE".equals(a.state()))
                .map(a -> this.repository.target(a.targetId()).firstCategoryId())
                .collect(java.util.stream.Collectors.toSet());
        return new Options(
                this.repository.competitions().stream()
                        .filter(c -> !onlyTasks || competitionIds.contains(c.id()))
                        .toList(),
                this.repository.categories().stream()
                        .filter(c -> !onlyTasks
                                || categories.contains(c.id())
                                || (c.parentId() != null && categories.contains(c.parentId())))
                        .toList(),
                onlyTasks ? List.of() : this.repository.regions(),
                manage
                        ? this.users.options().stream()
                                .map(u -> {
                                    final UserView v = this.users.get(u.id());
                                    return new UserOption(v.id(), v.username(), v.nickname());
                                })
                                .toList()
                        : List.of(),
                allowed);
    }

    public PageResult<Entry> entries(
            final long competition,
            final @Nullable String key,
            final @Nullable String source,
            final @Nullable String status,
            final PageQuery page) {
        return page(
                this.repository.entries(competition).stream()
                        .filter(e -> keyword(e.title(), key))
                        .filter(e -> source == null || source.isBlank() || source.equals(e.source()))
                        .filter(e -> status == null
                                || status.isBlank()
                                || status.equals(e.status())
                                || status.equals(String.valueOf(e.legacyStatus())))
                        .toList(),
                page);
    }

    public EntryDetail entry(final long id) {
        final Entry e = this.repository.entry(id);
        return new EntryDetail(
                e,
                this.repository.members(id),
                this.repository.targets(e.competitionId()).stream()
                        .filter(t -> t.entryId() == id)
                        .toList(),
                this.repository.decisions(id));
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.IdVersion decideEntry(final long id, final ReviewCommands.Decision c) {
        final Entry e = this.repository.entry(id);
        this.anchor(e.competitionId());
        version(e.version(), c.expectedVersion());
        if (!Set.of("APPROVED", "REJECTED").contains(c.decision())) {
            throw BizException.badRequest("初审决定只能通过或驳回");
        }
        final String reason = ReviewRules.text(c.reason(), "原因", "REJECTED".equals(c.decision()), 2000);
        if (e.legacyStatus() != 1 && this.repository.hasLiveReview(id)) {
            throw BizException.conflict("作品已有下游评审引用，请先显式失效复核");
        }
        this.repository.decideEntry(id, c.decision(), reason, operator(), c.expectedVersion());
        for (final Target t : this.repository.targets(e.competitionId())) {
            if (t.entryId() == id) {
                this.workflow.invalidateTarget(e.competitionId(), t.id(), "初审决定已变化");
            }
        }
        this.bumpCompetition(e.competitionId());
        return new ReviewCommands.IdVersion(id, e.version() + 1);
    }

    private PrepareSnapshot preparation(final ReviewCommands.Prepare c) {
        if (c.entryIds() == null || c.entryIds().isEmpty() || c.entryIds().size() > 500) {
            throw BizException.badRequest("请选择1至500件作品");
        }
        final List<PrepareRow> rows = new ArrayList<>();
        for (final long id : new TreeSet<>(c.entryIds())) {
            final Entry e = this.repository.entry(id);
            if (e.competitionId() != c.competitionId()) {
                throw BizException.badRequest("作品不属于所选赛事");
            }
            final String type = "zhongxi".equals(e.source()) ? "ENTRY" : "PARTICIPANT";
            final List<Target> targets = this.repository.targets(c.competitionId()).stream()
                    .filter(t -> t.entryId() == id && "ACTIVE".equals(t.status()))
                    .toList();
            final int expected =
                    "ENTRY".equals(type) ? 1 : this.repository.members(id).size();
            final Set<Long> expectedMembers =
                    this.repository.members(id).stream().map(Member::id).collect(java.util.stream.Collectors.toSet());
            final Set<Long> actualMembers = targets.stream()
                    .map(Target::participantId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            final boolean mismatch = !e.scoringScope().equals(type)
                    || targets.size() != expected
                    || targets.stream().anyMatch(t -> !t.targetType().equals(type))
                    || ("PARTICIPANT".equals(type) && !actualMembers.equals(expectedMembers));
            final String blocked = !"ACTIVE".equals(e.status())
                    ? "作品已失效"
                    : mismatch && this.repository.referenced(id) ? "已有任务或评分引用，禁止自动转换" : "";
            rows.add(new PrepareRow(
                    id,
                    e.version(),
                    mismatch ? "REPAIR" : "UNCHANGED",
                    blocked,
                    this.support.hash(List.of(targets, this.repository.members(id)))));
        }
        return new PrepareSnapshot(c, rows);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PreviewResponse<PrepareRow> preparePreview(final ReviewCommands.Prepare c) {
        final WorkflowPhase p = this.anchor(c.competitionId());
        final PrepareSnapshot v = this.preparation(c);
        return this.preview(
                "PREPARE",
                p,
                v,
                v.rows(),
                v.rows().stream().filter(x -> !x.blockedReason().isBlank()).toList());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.Count prepare(final ReviewCommands.Confirm c) {
        final Optional<ReviewCommands.Count> receipt =
                this.support.receipt(operator(), "PREPARE", c.requestKey(), c, ReviewCommands.Count.class);
        if (receipt.isPresent()) {
            return receipt.get();
        }
        final PrepareSnapshot old = this.payload(c.previewToken(), "PREPARE", PrepareSnapshot.class);
        final PrepareSnapshot current = this.preparation(old.command());
        this.same(old, current);
        if (current.rows().stream().anyMatch(x -> !x.blockedReason().isBlank())) {
            throw BizException.conflict("存在不可修正的评审对象");
        }
        int changed = 0;
        for (final PrepareRow row : current.rows()) {
            if ("REPAIR".equals(row.action())) {
                this.repository.prepareTargets(row.entryId());
                changed++;
            }
        }
        if (changed > 0) {
            for (final PrepareRow row : current.rows()) {
                if ("REPAIR".equals(row.action())) {
                    for (final Target t : this.repository.targets(old.command().competitionId())) {
                        if (t.entryId() == row.entryId()) {
                            this.workflow.invalidateTarget(old.command().competitionId(), t.id(), "评审对象已重新准备");
                        }
                    }
                }
            }
            this.bumpCompetition(old.command().competitionId());
        }
        final ReviewCommands.Count result =
                new ReviewCommands.Count(changed, current.rows().size() - changed, 0);
        this.support.complete(operator(), "PREPARE", c.requestKey(), c, result);
        return result;
    }

    public List<PhaseView> phases(final long competition) {
        return this.workflow.phases(competition).stream()
                .map(p -> {
                    this.support.lockAndValidate(p.id());
                    return this.phaseView(this.workflow.phase(p.id()).orElseThrow());
                })
                .toList();
    }

    private PhaseView phaseView(final WorkflowPhase p) {
        return new PhaseView(
                p.id(),
                p.competitionId(),
                p.code(),
                p.state(),
                p.currentRuleId(),
                p.inputVersion(),
                p.version(),
                p.currentRuleId() == 0 ? null : this.repository.rule(p.currentRuleId()),
                p.currentRuleId() == 0 ? List.of() : this.repository.dimensions(p.currentRuleId()));
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public List<PhaseView> createPhases(final ReviewCommands.PhaseCreate c) {
        this.anchor(c.competitionId());
        for (final String code : List.of("PROVINCIAL", "NATIONAL")) {
            this.repository.createPhase(c.competitionId(), code);
        }
        return this.phases(c.competitionId());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.IdVersion rule(final long id, final ReviewCommands.Rule c) {
        final WorkflowPhase p = this.support.lockAndValidate(id);
        this.numeric(p);
        version(p.version(), c.expectedVersion());
        if (!Set.of("DRAFT", "CLOSED").contains(p.state())) {
            throw BizException.conflict("规则须在草稿或关闭状态修改");
        }
        if (c.minimumReviewers() < 1
                || c.minScore() == null
                || c.maxScore() == null
                || c.minScore().compareTo(c.maxScore()) >= 0) {
            throw BizException.badRequest("评分范围或最低人数无效");
        }
        ReviewRules.number(c.minScore(), BigDecimal.ZERO, new BigDecimal("99999"));
        ReviewRules.number(c.maxScore(), BigDecimal.ZERO, new BigDecimal("99999"));
        if (c.passingScore() != null) {
            ReviewRules.number(c.passingScore(), c.minScore(), c.maxScore());
        }
        final List<Dimension> old = p.currentRuleId() == 0 ? List.of() : this.repository.dimensions(p.currentRuleId());
        final long r = this.repository.saveRule(
                id, c.minScore(), c.maxScore(), c.minimumReviewers(), c.passingScore(), operator());
        for (final Dimension d : old) {
            this.repository.addDimension(new Dimension(
                    0, r, d.firstCategoryId(), d.name(), d.description(), d.minScore(), d.maxScore(), d.sort()));
        }
        this.workflow.invalidatePhase(id, "评分规则已更新");
        return new ReviewCommands.IdVersion(id, p.version() + 1);
    }

    public List<Dimension> dimensions(final long id) {
        final WorkflowPhase p = this.support.lockAndValidate(id);
        return p.currentRuleId() == 0 ? List.of() : this.repository.dimensions(p.currentRuleId());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.IdVersion dimensions(final long id, final ReviewCommands.Dimensions c) {
        final WorkflowPhase p = this.support.lockAndValidate(id);
        this.numeric(p);
        version(p.version(), c.expectedVersion());
        if (p.currentRuleId() == 0 || !Set.of("DRAFT", "CLOSED").contains(p.state())) {
            throw BizException.conflict("请先配置评分规则并关闭阶段");
        }
        if (c.dimensions() == null || c.dimensions().size() > 100) {
            throw BizException.badRequest("维度最多100项");
        }
        final Rule rule = this.repository.rule(p.currentRuleId());
        final long next = this.repository.saveRule(
                id, rule.minScore(), rule.maxScore(), rule.minimumReviewers(), rule.passingScore(), operator());
        final Set<String> names = new HashSet<>();
        for (final ReviewCommands.Dimension d : c.dimensions()) {
            this.category(d.firstCategoryId(), null);
            final String name = ReviewRules.text(d.name(), "维度名称", true, 255);
            if (!names.add(d.firstCategoryId() + ":" + name)
                    || d.minScore() == null
                    || d.maxScore() == null
                    || d.minScore().compareTo(d.maxScore()) >= 0) {
                throw BizException.badRequest("维度重名或范围无效");
            }
            ReviewRules.number(d.minScore(), BigDecimal.ZERO, new BigDecimal("99999"));
            ReviewRules.number(d.maxScore(), BigDecimal.ZERO, new BigDecimal("99999"));
            this.repository.addDimension(new Dimension(
                    0,
                    next,
                    d.firstCategoryId(),
                    name,
                    ReviewRules.text(d.description(), "说明", false, 2000),
                    d.minScore(),
                    d.maxScore(),
                    d.sort()));
        }
        this.workflow.invalidatePhase(id, "评分维度已更新");
        return new ReviewCommands.IdVersion(id, p.version() + 1);
    }

    private void validateState(final WorkflowPhase p, final ReviewCommands.State c) {
        version(p.version(), c.expectedVersion());
        this.numeric(p);
        if ("CLOSED".equals(c.state()) && "OPEN".equals(p.state())) {
            return;
        }
        if ("OPEN".equals(c.state()) && "DRAFT".equals(p.state())) {
            if (p.currentRuleId() == 0) {
                throw BizException.conflict("请先配置评分规则");
            }
            if (this.repository.entries(p.competitionId()).stream()
                    .filter(e -> e.legacyStatus() == 3)
                    .anyMatch(e ->
                            this.preparation(new ReviewCommands.Prepare(p.competitionId(), List.of(e.id())))
                                    .rows()
                                    .stream()
                                    .anyMatch(r -> !"UNCHANGED".equals(r.action())
                                            || !r.blockedReason().isBlank()))) {
                throw BizException.conflict("请先准备完整评审对象");
            }
            return;
        }
        if ("REOPEN".equals(c.state()) && Set.of("CLOSED", "FROZEN").contains(p.state())) {
            reason(c.reason());
            return;
        }
        throw BizException.conflict("当前阶段不可执行该状态变化");
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PreviewResponse<Impact> statePreview(final long id, final ReviewCommands.State c) {
        final WorkflowPhase p = this.support.lockAndValidate(id);
        this.validateState(p, c);
        final StateSnapshot s = new StateSnapshot(id, p.version(), c.state(), comment(c.reason()));
        final List<Impact> rows = this.workflow.phases(p.competitionId()).stream()
                .map(v -> new Impact(v.id(), v.code(), "REOPEN".equals(c.state()) ? "该阶段及依赖结果将失效" : "阶段状态改变"))
                .toList();
        return this.preview("PHASE_STATE", p, s, rows, List.of());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.IdVersion state(final long id, final ReviewCommands.Confirm c) {
        final Optional<ReviewCommands.IdVersion> cached =
                this.support.receipt(operator(), "PHASE_STATE", c.requestKey(), c, ReviewCommands.IdVersion.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        final StateSnapshot s = this.payload(c.previewToken(), "PHASE_STATE", StateSnapshot.class);
        if (s.phaseId() != id) {
            throw BizException.conflict("预览阶段不一致");
        }
        final WorkflowPhase p = this.workflow.lockPhase(id);
        this.validateState(p, new ReviewCommands.State(s.state(), s.reason(), s.version()));
        if ("REOPEN".equals(s.state())) {
            this.workflow.invalidatePhase(p.id(), s.reason());
            this.bumpCompetition(p.competitionId());
            this.repository.snapshot(id, 0, p.inputVersion(), "REOPEN", "{}", "{}", s.reason(), operator());
        }
        this.repository.phaseState(
                id,
                "REOPEN".equals(s.state()) ? "OPEN" : s.state(),
                this.workflow.lockPhase(id).version());
        final ReviewCommands.IdVersion result =
                new ReviewCommands.IdVersion(id, this.workflow.lockPhase(id).version());
        this.support.complete(operator(), "PHASE_STATE", c.requestKey(), c, result);
        return result;
    }

    private void category(final long first, final @Nullable Long second) {
        if (this.repository.categories().stream().noneMatch(x -> x.id() == first && x.parentId() == null)) {
            throw BizException.badRequest("一级赛项无效");
        }
        if (second != null
                && second != 0
                && this.repository.categories().stream()
                        .noneMatch(x -> x.id() == second && Objects.equals(x.parentId(), first))) {
            throw BizException.badRequest("专业不属于当前赛项");
        }
    }

    private Scope scopeValue(final long id, final ReviewCommands.Scope c) {
        final WorkflowPhase p = this.workflow.phase(c.phaseId()).orElseThrow(() -> BizException.notFound("阶段不存在"));
        this.numeric(p);
        if (p.competitionId() != c.competitionId()) {
            throw BizException.badRequest("阶段不属于所选赛事");
        }
        this.category(c.firstCategoryId(), c.secondCategoryId());
        if (c.regionId() != null
                && c.regionId() != 0
                && this.repository.regions().stream().noneMatch(r -> r.id() == c.regionId())) {
            throw BizException.badRequest("赛区不存在");
        }
        this.validateUser(c.userId());
        return new Scope(
                id,
                c.userId(),
                c.competitionId(),
                c.phaseId(),
                c.firstCategoryId(),
                c.secondCategoryId(),
                c.regionId(),
                c.enabled(),
                c.expectedVersion());
    }

    public PageResult<Scope> scopes(
            final long competition,
            final long phase,
            final @Nullable Long user,
            final @Nullable String key,
            final PageQuery q) {
        if (phase > 0) {
            this.support.lockAndValidate(phase);
        }
        return page(
                this.repository.scopes(phase).stream()
                        .filter(s -> competition == 0 || s.competitionId() == competition)
                        .filter(s -> user == null || s.userId() == user)
                        .filter(s -> key == null
                                || key.isBlank()
                                || keyword(
                                        this.users.get(s.userId()).nickname()
                                                + this.users.get(s.userId()).username(),
                                        key))
                        .toList(),
                q);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.IdVersion createScope(final ReviewCommands.Scope c) {
        this.support.lockAndValidate(c.phaseId());
        final Scope value = this.scopeValue(0, c);
        final long id = this.repository.createScope(value);
        this.workflow.invalidateReviewer(c.phaseId(), c.userId(), "评委范围已新增，需重新复核");
        this.workflow.bumpInput(c.phaseId());
        return new ReviewCommands.IdVersion(id, 0);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PreviewResponse<Impact> scopePreview(final long id, final ReviewCommands.Scope c) {
        final Scope old = this.repository.scope(id);
        final WorkflowPhase p = this.support.lockAndValidate(old.phaseId());
        version(old.version(), c.expectedVersion());
        this.scopeValue(id, c);
        if (old.phaseId() != c.phaseId()) {
            throw BizException.badRequest("已有范围不能迁移阶段，请另建范围");
        }
        final List<Long> assignments = this.repository.assignments(old.phaseId()).stream()
                .filter(a -> a.reviewerId() == old.userId() || a.reviewerId() == c.userId())
                .map(Assignment::id)
                .toList();
        final ScopeSnapshot snapshot = new ScopeSnapshot(id, c, old, assignments);
        return this.preview(
                "SCOPE_UPDATE",
                p,
                snapshot,
                assignments.stream()
                        .map(a -> new Impact(a, "ASSIGNMENT", "评分有效性及派生结果将重新核验"))
                        .toList(),
                List.of());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.IdVersion updateScope(final long id, final ReviewCommands.Scope c) {
        final Scope old = this.repository.scope(id);
        this.support.lockAndValidate(old.phaseId());
        final ScopeSnapshot snapshot = this.payload(
                ReviewRules.text(c.previewToken(), "预览token", true, 36), "SCOPE_UPDATE", ScopeSnapshot.class);
        version(old.version(), c.expectedVersion());
        if (snapshot.id() != id) {
            throw BizException.conflict("预览范围不匹配");
        }
        final ReviewCommands.Scope saved = snapshot.command();
        if (!this.support
                .hash(new Scope(
                        id,
                        c.userId(),
                        c.competitionId(),
                        c.phaseId(),
                        c.firstCategoryId(),
                        c.secondCategoryId(),
                        c.regionId(),
                        c.enabled(),
                        c.expectedVersion()))
                .equals(this.support.hash(new Scope(
                        id,
                        saved.userId(),
                        saved.competitionId(),
                        saved.phaseId(),
                        saved.firstCategoryId(),
                        saved.secondCategoryId(),
                        saved.regionId(),
                        saved.enabled(),
                        saved.expectedVersion())))) {
            throw BizException.conflict("范围与预览不同");
        }
        if (!reason(c.reason()).equals(reason(saved.reason()))) {
            throw BizException.conflict("原因与预览不同");
        }
        this.repository.updateScope(this.scopeValue(id, c), c.expectedVersion());
        this.workflow.invalidateReviewer(old.phaseId(), old.userId(), "评委范围已变化");
        if (old.userId() != c.userId()) {
            this.workflow.invalidateReviewer(c.phaseId(), c.userId(), "评委范围已变化");
        }
        this.bumpCompetition(old.competitionId());
        return new ReviewCommands.IdVersion(id, c.expectedVersion() + 1);
    }

    private boolean advanced(final WorkflowPhase p, final Target t) {
        return this.repository.targetDecisions(p.id()).stream()
                .anyMatch(d ->
                        d.targetId() == t.id() && Set.of("ELIGIBLE", "ADVANCED").contains(d.decision()));
    }

    private String eligible(final WorkflowPhase p, final Target t) {
        return ReviewRules.eligibility(t, p, this.advanced(p, t));
    }

    private String assignmentBlocked(final WorkflowPhase p, final Assignment a) {
        final Target t = this.repository.target(a.targetId());
        final String problem = this.eligible(p, t);
        if (!problem.isBlank()) {
            return problem;
        }
        if (!"ACTIVE".equals(a.state())) {
            return "任务已取消";
        }
        try {
            this.validateUser(a.reviewerId());
        } catch (final BizException e) {
            if (e.getErrorCode().httpStatus() >= 500) {
                throw e;
            }
            return "评委账号不可用";
        }
        return this.repository.scopes(p.id()).stream()
                        .anyMatch(s -> s.userId() == a.reviewerId() && ReviewRules.matches(s, t, p.id()))
                ? ""
                : "评委范围不匹配";
    }

    private TaskView taskView(final Assignment a) {
        return new TaskView(
                a.id(),
                a.phaseId(),
                a.targetId(),
                a.reviewerId(),
                a.state(),
                a.version(),
                this.repository
                        .score(a.id())
                        .filter(score -> score.ruleId()
                                == this.workflow
                                        .phase(a.phaseId())
                                        .orElseThrow()
                                        .currentRuleId())
                        .map(Score::status)
                        .orElse("EMPTY"),
                this.repository.target(a.targetId()));
    }

    public PageResult<TaskView> assignments(
            final long competition,
            final long phase,
            final @Nullable Long reviewer,
            final @Nullable String state,
            final @Nullable String key,
            final PageQuery q) {
        final List<TaskView> rows = new ArrayList<>();
        for (final WorkflowPhase p : this.workflow.phases(competition)) {
            if (phase > 0 && p.id() != phase) {
                continue;
            }
            this.support.lockAndValidate(p.id());
            for (final Assignment a : this.repository.assignments(p.id())) {
                final TaskView v = this.taskView(a);
                if ((reviewer == null || a.reviewerId() == reviewer)
                        && (state == null || state.isBlank() || state.equals(a.state()))
                        && keyword(v.target().name() + v.target().title(), key)) {
                    rows.add(v);
                }
            }
        }
        return page(rows, q);
    }

    public PageResult<Candidate> candidates(
            final long competition,
            final long phase,
            final @Nullable String source,
            final @Nullable String type,
            final @Nullable Long category,
            final @Nullable String key,
            final PageQuery q) {
        final WorkflowPhase p = this.support.lockAndValidate(phase);
        if (p.competitionId() != competition) {
            throw BizException.badRequest("赛事与阶段不匹配");
        }
        final List<Assignment> assigned = this.repository.assignments(phase);
        return page(
                this.repository.targets(competition).stream()
                        .filter(t -> source == null || source.isBlank() || source.equals(t.source()))
                        .filter(t -> type == null || type.isBlank() || type.equals(t.targetType()))
                        .filter(t -> category == null || t.firstCategoryId() == category)
                        .filter(t -> keyword(t.name() + t.title(), key))
                        .map(t -> {
                            final String blocked = this.eligible(p, t);
                            return new Candidate(
                                    t.id(),
                                    t.id(),
                                    t.entryId(),
                                    t.participantId(),
                                    t.competitionId(),
                                    t.name(),
                                    t.title(),
                                    t.source(),
                                    t.targetType(),
                                    t.firstCategoryId(),
                                    t.secondCategoryId(),
                                    t.regionId(),
                                    t.groupName(),
                                    t.version(),
                                    blocked.isBlank(),
                                    blocked,
                                    assigned.stream()
                                            .filter(a -> a.targetId() == t.id() && "ACTIVE".equals(a.state()))
                                            .count());
                        })
                        .toList(),
                q);
    }

    private AssignmentSnapshot distribution(final ReviewCommands.AssignmentPreview c) {
        final WorkflowPhase p = this.workflow.lockPhase(c.phaseId());
        this.numeric(p);
        if (!"OPEN".equals(p.state()) || p.currentRuleId() == 0) {
            throw BizException.conflict("仅开放且已配置规则阶段可分配");
        }
        if (c.targetIds() == null
                || c.targetIds().isEmpty()
                || c.targetIds().size() > 500
                || c.userIds() == null
                || !Set.of("MANUAL", "AUTO").contains(c.mode())
                || c.reviewersPerTarget() < 1
                || c.reviewersPerTarget() > 100) {
            throw BizException.badRequest("分配参数无效或超出批量上限");
        }
        if ("MANUAL".equals(c.mode()) && c.userIds().isEmpty()) {
            throw BizException.badRequest("请选择评委");
        }
        final List<Scope> scopes = this.repository.scopes(p.id());
        final List<Assignment> existing = this.repository.assignments(p.id());
        final Set<Long> pool = new TreeSet<>();
        if (c.userIds().isEmpty()) {
            scopes.forEach(s -> pool.add(s.userId()));
        } else {
            pool.addAll(c.userIds());
        }
        final List<ReviewerSnapshot> snapshots = new ArrayList<>();
        for (final long id : pool) {
            try {
                final UserView u = this.users.get(id);
                snapshots.add(new ReviewerSnapshot(id, u.status(), u.updatedAt()));
            } catch (final BizException e) {
                if (e.getErrorCode().httpStatus() >= 500) {
                    throw e;
                }
                snapshots.add(new ReviewerSnapshot(id, "missing", null));
            }
        }
        final Map<Long, Long> loads = new HashMap<>();
        for (final Assignment a : existing) {
            if ("ACTIVE".equals(a.state())) {
                loads.merge(a.reviewerId(), 1L, Long::sum);
            }
        }
        final List<AssignmentRow> rows = new ArrayList<>();
        for (final long targetId : new TreeSet<>(c.targetIds())) {
            final Target t = this.repository.target(targetId);
            final String blocked = this.eligible(p, t);
            if (!blocked.isBlank()) {
                rows.add(new AssignmentRow(targetId, 0, "BLOCKED", blocked, 0, 0));
                continue;
            }
            final Set<Long> current = new HashSet<>();
            existing.stream()
                    .filter(a -> a.targetId() == targetId && "ACTIVE".equals(a.state()))
                    .forEach(a -> current.add(a.reviewerId()));
            final List<Long> ordered = new ArrayList<>(pool);
            ordered.sort(Comparator.comparingLong((Long id) -> loads.getOrDefault(id, 0L))
                    .thenComparingLong(Long::longValue));
            int needed = "MANUAL".equals(c.mode())
                    ? c.userIds().size()
                    : Math.max(0, c.reviewersPerTarget() - current.size());
            for (final long user : ordered) {
                if ("AUTO".equals(c.mode()) && needed == 0) {
                    break;
                }
                final ReviewerSnapshot snapshot = snapshots.stream()
                        .filter(s -> s.userId() == user)
                        .findFirst()
                        .orElseThrow();
                final boolean allowed = "enabled".equals(snapshot.state())
                        && scopes.stream().anyMatch(s -> s.userId() == user && ReviewRules.matches(s, t, p.id()));
                if (current.contains(user)) {
                    if ("MANUAL".equals(c.mode())) {
                        rows.add(new AssignmentRow(targetId, user, "UNCHANGED", "", loads.getOrDefault(user, 0L), 0));
                        needed--;
                    }
                    continue;
                }
                if (!allowed) {
                    if ("MANUAL".equals(c.mode())) {
                        rows.add(new AssignmentRow(
                                targetId, user, "BLOCKED", "评委已停用或范围不匹配", loads.getOrDefault(user, 0L), 0));
                        needed--;
                    }
                    continue;
                }
                rows.add(new AssignmentRow(targetId, user, "ASSIGN", "", loads.getOrDefault(user, 0L), 0));
                loads.merge(user, 1L, Long::sum);
                needed--;
            }
            if (needed > 0) {
                rows.add(new AssignmentRow(targetId, 0, "BLOCKED", "合法评委不足，缺少" + needed + "人", 0, 0));
            }
        }
        if (rows.stream().filter(r -> "ASSIGN".equals(r.action())).count() > 5000) {
            throw BizException.badRequest("新任务超过5000，请缩小批次");
        }
        return new AssignmentSnapshot(c, rows, snapshots, existing);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PreviewResponse<AssignmentRow> assignmentPreview(final ReviewCommands.AssignmentPreview c) {
        final WorkflowPhase p = this.support.lockAndValidate(c.phaseId());
        if (!CurrentUser.require()
                .hasPermission("AUTO".equals(c.mode()) ? "cqt:assignment:auto" : "cqt:assignment:create")) {
            throw new BizException(CommonErrors.FORBIDDEN);
        }
        final AssignmentSnapshot s = this.distribution(c);
        return this.preview(
                "ASSIGN",
                p,
                s,
                s.rows(),
                s.rows().stream().filter(r -> !r.blockedReason().isBlank()).toList());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.Count assign(final ReviewCommands.Confirm c) {
        final Optional<ReviewCommands.Count> cached =
                this.support.receipt(operator(), "ASSIGN", c.requestKey(), c, ReviewCommands.Count.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        final AssignmentSnapshot old = this.payload(c.previewToken(), "ASSIGN", AssignmentSnapshot.class);
        if (!CurrentUser.require()
                .hasPermission("AUTO".equals(old.command().mode()) ? "cqt:assignment:auto" : "cqt:assignment:create")) {
            throw new BizException(CommonErrors.FORBIDDEN);
        }
        final AssignmentSnapshot now = this.distribution(old.command());
        this.same(old, now);
        if (now.rows().stream().anyMatch(r -> !r.blockedReason().isBlank())) {
            throw BizException.conflict("存在缺人或越界对象，请调整后重新预览");
        }
        int created = 0;
        int unchanged = 0;
        for (final AssignmentRow row : now.rows()) {
            if ("UNCHANGED".equals(row.action())) {
                unchanged++;
                continue;
            }
            this.repository.assign(
                    old.command().phaseId(),
                    row.targetId(),
                    row.userId(),
                    now.users().stream()
                            .filter(u -> u.userId() == row.userId())
                            .findFirst()
                            .orElseThrow());
            created++;
        }
        this.workflow.bumpInput(old.command().phaseId());
        final ReviewCommands.Count result = new ReviewCommands.Count(created, unchanged, 0);
        this.support.complete(operator(), "ASSIGN", c.requestKey(), c, result);
        return result;
    }

    private String assignmentState(final ReviewCommands.AssignmentState c) {
        if (Set.of("CANCELLED", "cancel", "CANCEL").contains(c.state())) {
            return "CANCELLED";
        }
        if (Set.of("ACTIVE", "restore", "RESTORE").contains(c.state())) {
            return "ACTIVE";
        }
        throw BizException.badRequest("任务状态无效");
    }

    private void validateAssignmentChange(final Assignment a, final ReviewCommands.AssignmentState c) {
        version(a.version(), c.expectedVersion());
        final String state = this.assignmentState(c);
        if (!CurrentUser.require()
                .hasPermission("ACTIVE".equals(state) ? "cqt:assignment:restore" : "cqt:assignment:cancel")) {
            throw new BizException(CommonErrors.FORBIDDEN);
        }
        reason(c.reason());
        if ("ACTIVE".equals(state)) {
            final WorkflowPhase p = this.workflow.lockPhase(a.phaseId());
            final Assignment restored = new Assignment(
                    a.id(),
                    a.phaseId(),
                    a.targetId(),
                    a.reviewerId(),
                    "ACTIVE",
                    a.version(),
                    a.userState(),
                    a.userUpdatedAt());
            if (!"OPEN".equals(p.state())
                    || !this.assignmentBlocked(p, restored).isBlank()) {
                throw BizException.conflict("当前任务不能恢复");
            }
        }
        if ("CANCELLED".equals(state)
                && this.repository
                        .score(a.id())
                        .map(s -> "SUBMITTED".equals(s.status()))
                        .orElse(false)
                && !CurrentUser.require().hasPermission("cqt:assignment:cancel")) {
            throw new BizException(CommonErrors.FORBIDDEN);
        }
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PreviewResponse<Impact> assignmentStatePreview(final long id, final ReviewCommands.AssignmentState c) {
        final Assignment a = this.repository.assignment(id);
        final WorkflowPhase p = this.support.lockAndValidate(a.phaseId());
        this.validateAssignmentChange(a, c);
        return this.preview(
                "ASSIGNMENT_STATE",
                p,
                new AssignmentChange(id, c, a),
                List.of(new Impact(id, "ASSIGNMENT", "任务状态及派生结果有效性变化")),
                List.of());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.IdVersion assignmentState(final long id, final ReviewCommands.AssignmentState c) {
        final Optional<ReviewCommands.IdVersion> cached =
                this.support.receipt(operator(), "ASSIGNMENT_STATE", c.requestKey(), c, ReviewCommands.IdVersion.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        final Assignment a = this.repository.assignment(id);
        final AssignmentChange old = this.payload(
                ReviewRules.text(c.previewToken(), "预览token", true, 36), "ASSIGNMENT_STATE", AssignmentChange.class);
        if (old.id() != id
                || !old.command().state().equals(c.state())
                || !Objects.equals(old.command().reason(), c.reason())) {
            throw BizException.conflict("预览任务或输入不同");
        }
        this.validateAssignmentChange(a, c);
        this.repository.assignmentState(id, this.assignmentState(c), c.expectedVersion());
        final WorkflowPhase p = this.workflow.lockPhase(a.phaseId());
        this.workflow.invalidatePhaseTarget(p.id(), a.targetId(), reason(c.reason()), true);
        this.bumpCompetition(p.competitionId());
        final ReviewCommands.IdVersion result = new ReviewCommands.IdVersion(id, a.version() + 1);
        this.support.complete(operator(), "ASSIGNMENT_STATE", c.requestKey(), c, result);
        return result;
    }

    public PageResult<TaskView> myTasks(
            final long competition,
            final long phase,
            final @Nullable String state,
            final @Nullable String key,
            final PageQuery q) {
        final List<TaskView> rows = new ArrayList<>();
        for (final Assignment a : this.repository.userAssignments(operator())) {
            final WorkflowPhase p = this.support.lockAndValidate(a.phaseId());
            final Target t = this.repository.target(a.targetId());
            if ((competition == 0 || p.competitionId() == competition)
                    && (phase == 0 || phase == p.id())
                    && (state == null
                            || state.isBlank()
                            || state.equals(a.state())
                            || state.equals(this.taskView(a).scoreStatus()))
                    && keyword(t.name() + t.title(), key)
                    && this.assignmentBlocked(p, a).isBlank()) {
                rows.add(this.taskView(a));
            }
        }
        return page(rows, q);
    }

    private Assignment own(final long id) {
        final Assignment a = this.repository.assignment(id);
        if (a.reviewerId() != operator()) {
            throw BizException.notFound("任务不存在");
        }
        return a;
    }

    public TaskDetail task(final long id) {
        final Assignment a = this.own(id);
        final WorkflowPhase p = this.support.lockAndValidate(a.phaseId());
        final String blocked = this.assignmentBlocked(p, a);
        if (!blocked.isBlank()) {
            throw BizException.notFound("任务不可用");
        }
        final Target t = this.repository.target(a.targetId());
        final Score score = this.repository.score(id).orElse(null);
        final List<ScoreRevision> history = score == null ? List.of() : this.repository.revisions(score.id());
        final ScoreRevision current = history.stream()
                .filter(r ->
                        score != null && r.revision() == score.currentRevision() && r.ruleId() == p.currentRuleId())
                .findFirst()
                .orElse(null);
        return new TaskDetail(
                a,
                t,
                this.repository.entry(t.entryId()),
                this.repository.rule(p.currentRuleId()),
                this.repository.dimensions(p.currentRuleId()).stream()
                        .filter(d -> d.firstCategoryId() == t.firstCategoryId())
                        .toList(),
                score,
                current,
                current == null ? List.of() : this.repository.dimensionScores(current.id()),
                history,
                "OPEN".equals(p.state()),
                "OPEN".equals(p.state()) ? "" : "阶段已关闭或冻结");
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.IdVersion score(final long id, final ReviewCommands.Score c) {
        final Optional<ReviewCommands.IdVersion> cached =
                this.support.receipt(operator(), "SCORE:" + id, c.requestKey(), c, ReviewCommands.IdVersion.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        final Assignment a = this.own(id);
        final WorkflowPhase p = this.support.lockAndValidate(a.phaseId());
        if (!"OPEN".equals(p.state()) || !this.assignmentBlocked(p, a).isBlank()) {
            throw BizException.conflict("阶段或任务当前不可评分");
        }
        final Score old = this.repository.score(id).orElse(null);
        version(old == null ? 0 : old.version(), c.expectedVersion());
        if (!Set.of("DRAFT", "SUBMITTED").contains(c.status()) || c.dimensions() == null) {
            throw BizException.badRequest("评分状态或维度无效");
        }
        final boolean submitted = "SUBMITTED".equals(c.status());
        final Rule rule = this.repository.rule(p.currentRuleId());
        if (submitted || c.total() != null) {
            ReviewRules.number(c.total(), rule.minScore(), rule.maxScore());
        }
        final String text = ReviewRules.text(c.comment(), "综合评语", false, 2000);
        final List<Dimension> definitions = this.repository.dimensions(p.currentRuleId()).stream()
                .filter(d -> d.firstCategoryId()
                        == this.repository.target(a.targetId()).firstCategoryId())
                .toList();
        final List<DimensionScore> values = new ArrayList<>();
        final Set<Long> ids = new HashSet<>();
        for (final ReviewCommands.DimensionScore item : c.dimensions()) {
            if (!ids.add(item.dimensionId())) {
                throw BizException.badRequest("维度重复");
            }
            final Dimension d = definitions.stream()
                    .filter(x -> x.id() == item.dimensionId())
                    .findFirst()
                    .orElseThrow(() -> BizException.badRequest("维度不属于当前规则与赛项"));
            if (submitted || item.score() != null) {
                ReviewRules.number(item.score(), d.minScore(), d.maxScore());
            }
            values.add(new DimensionScore(
                    d.id(), item.score(), ReviewRules.text(item.comment(), "维度评语", submitted, 2000)));
        }
        if (submitted && ids.size() != definitions.size()) {
            throw BizException.badRequest("请完成全部维度评分与评语");
        }
        boolean unchanged = false;
        if (old != null && old.ruleId() == p.currentRuleId()) {
            final ScoreRevision current = this.repository.revisions(old.id()).stream()
                    .filter(r -> r.revision() == old.currentRevision())
                    .findFirst()
                    .orElseThrow();
            unchanged = (current.total() == null
                            ? c.total() == null
                            : c.total() != null && current.total().compareTo(c.total()) == 0)
                    && current.comment().equals(text)
                    && current.status().equals(c.status())
                    && this.repository.dimensionScores(current.id()).equals(values);
        }
        final long scoreId = unchanged
                ? Objects.requireNonNull(old).id()
                : this.repository.saveScore(
                        id, p.currentRuleId(), c.status(), c.total(), text, values, operator(), c.expectedVersion());
        if (!unchanged) {
            this.workflow.invalidatePhaseTarget(p.id(), a.targetId(), "评委评分已修订", true);
            this.bumpCompetition(p.competitionId());
        }
        final ReviewCommands.IdVersion result =
                new ReviewCommands.IdVersion(scoreId, unchanged ? c.expectedVersion() : c.expectedVersion() + 1);
        this.support.complete(operator(), "SCORE:" + id, c.requestKey(), c, result);
        return result;
    }

    private Calculation calculation(final long id) {
        final WorkflowPhase p = this.workflow.lockPhase(id);
        this.numeric(p);
        if (!"CLOSED".equals(p.state())) {
            throw BizException.conflict("请先关闭阶段评分再汇总");
        }
        final Rule rule = this.repository.rule(p.currentRuleId());
        final List<Target> targets = this.repository.targets(p.competitionId());
        final Set<Long> candidates = new TreeSet<>();
        targets.stream().filter(t -> this.eligible(p, t).isBlank()).forEach(t -> candidates.add(t.id()));
        this.repository.finals(id).forEach(f -> candidates.add(f.targetId()));
        final List<Assignment> assignments = this.repository.assignments(id);
        assignments.forEach(a -> candidates.add(a.targetId()));
        final List<CalculationRow> rows = new ArrayList<>();
        for (final long targetId : candidates) {
            final Target t = this.repository.target(targetId);
            final String blocked = this.eligible(p, t);
            final List<Assignment> assigned = assignments.stream()
                    .filter(a -> a.targetId() == targetId && "ACTIVE".equals(a.state()))
                    .toList();
            final List<ScoreInput> inputs = new ArrayList<>();
            if (blocked.isBlank()) {
                for (final Assignment a : assigned) {
                    if (!this.assignmentBlocked(p, a).isBlank()) {
                        continue;
                    }
                    final Score s = this.repository.score(a.id()).orElse(null);
                    if (s == null || !"SUBMITTED".equals(s.status()) || s.ruleId() != rule.id()) {
                        continue;
                    }
                    final ScoreRevision r = this.repository.revisions(s.id()).stream()
                            .filter(v -> v.revision() == s.currentRevision()
                                    && v.ruleId() == rule.id()
                                    && "SUBMITTED".equals(v.status()))
                            .findFirst()
                            .orElse(null);
                    if (r != null && r.total() != null) {
                        inputs.add(new ScoreInput(a.id(), a.reviewerId(), r.id(), r.revision(), r.total()));
                    }
                }
            }
            final boolean ready = blocked.isBlank() && inputs.size() >= rule.minimumReviewers();
            rows.add(new CalculationRow(
                    targetId,
                    inputs.size(),
                    assigned.size(),
                    ready
                            ? ReviewRules.average(
                                    inputs.stream().map(ScoreInput::total).toList())
                            : null,
                    ready ? "READY" : "SKIPPED",
                    blocked.isBlank() && !ready ? "有效提交不足最低评委人数" : blocked,
                    inputs));
        }
        return new Calculation(id, rule.id(), rows, this.repository.scopes(id), targets);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PreviewResponse<CalculationRow> calculatePreview(final long id) {
        final WorkflowPhase p = this.support.lockAndValidate(id);
        final Calculation c = this.calculation(id);
        return this.preview(
                "CALCULATE",
                p,
                c,
                c.rows(),
                c.rows().stream().filter(r -> !"READY".equals(r.state())).toList());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.Count calculate(final long id, final ReviewCommands.Confirm c) {
        final Optional<ReviewCommands.Count> cached =
                this.support.receipt(operator(), "CALCULATE:" + id, c.requestKey(), c, ReviewCommands.Count.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        final Calculation old = this.payload(c.previewToken(), "CALCULATE", Calculation.class);
        if (old.phaseId() != id) {
            throw BizException.conflict("预览阶段不匹配");
        }
        final Calculation now = this.calculation(id);
        this.same(old, now);
        final long ready =
                now.rows().stream().filter(r -> "READY".equals(r.state())).count();
        if (ready == 0) {
            throw BizException.conflict("没有达到最低人数的有效对象，不能冻结");
        }
        if (ready < now.rows().size() && !c.acknowledgeSkipped()) {
            throw BizException.conflict("请显式接受缺评及失效对象清单");
        }
        final WorkflowPhase p = this.workflow.lockPhase(id);
        this.workflow.invalidateDerived(id, "阶段已重新汇总");
        for (final CalculationRow row : now.rows()) {
            final long snapshot = this.repository.snapshot(
                    id,
                    row.targetId(),
                    p.inputVersion(),
                    "CALCULATE",
                    this.support.json(now),
                    this.support.json(row),
                    row.blockedReason(),
                    operator());
            if ("READY".equals(row.state())) {
                this.repository.saveFinal(
                        id, row.targetId(), Objects.requireNonNull(row.average()), row.validReviewerCount(), snapshot);
            } else {
                this.repository.staleFinal(id, row.targetId());
            }
        }
        this.repository.phaseState(id, "FROZEN", p.version());
        final ReviewCommands.Count result =
                new ReviewCommands.Count((int) ready, 0, now.rows().size() - (int) ready);
        this.support.complete(operator(), "CALCULATE:" + id, c.requestKey(), c, result);
        return result;
    }

    public PageResult<ScoreView> scores(
            final long competition,
            final long phase,
            final @Nullable String state,
            final @Nullable String key,
            final PageQuery q) {
        final List<ScoreView> rows = new ArrayList<>();
        for (final WorkflowPhase initial : this.workflow.phases(competition)) {
            if ("INITIAL".equals(initial.code()) || (phase > 0 && phase != initial.id())) {
                continue;
            }
            final WorkflowPhase p = this.support.lockAndValidate(initial.id());
            final List<FinalScore> finals = this.repository.finals(p.id());
            final List<TargetDecision> decisions = this.repository.targetDecisions(p.id());
            for (final Target t : this.repository.targets(p.competitionId())) {
                final FinalScore f = finals.stream()
                        .filter(v -> v.targetId() == t.id())
                        .findFirst()
                        .orElse(null);
                if (f == null && !this.eligible(p, t).isBlank()) {
                    continue;
                }
                final String status = f == null ? "MISSING" : f.state();
                if ((state == null || state.isBlank() || state.equals(status)) && keyword(t.name() + t.title(), key)) {
                    rows.add(new ScoreView(
                            f == null ? 0 : f.id(),
                            p.id(),
                            t.id(),
                            f == null ? null : f.finalScore(),
                            f == null ? null : f.averageScore(),
                            f == null ? 0 : f.validReviewerCount(),
                            status,
                            f == null ? 0 : f.version(),
                            f == null ? 0 : f.version(),
                            t,
                            decisions.stream()
                                    .filter(d -> d.targetId() == t.id())
                                    .findFirst()
                                    .orElse(null)));
                }
            }
        }
        return page(rows, q);
    }

    public ScoreHistory history(final long id) {
        final FinalScore f = this.repository.finalScore(id);
        this.support.lockAndValidate(f.phaseId());
        return new ScoreHistory(this.repository.snapshots(f.phaseId(), f.targetId()), this.repository.corrections(id));
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.IdVersion correct(final long id, final ReviewCommands.Correction c) {
        final FinalScore before = this.repository.finalScore(id);
        final WorkflowPhase p = this.support.lockAndValidate(before.phaseId());
        final FinalScore f = this.repository.finalScore(id);
        version(f.version(), c.expectedVersion());
        if (!"FROZEN".equals(p.state()) || !"VALID".equals(f.state())) {
            throw BizException.conflict("仅冻结阶段有效成绩可更正");
        }
        final Rule rule = this.repository.rule(p.currentRuleId());
        ReviewRules.number(c.finalScore(), rule.minScore(), rule.maxScore());
        final String why = reason(c.reason());
        this.workflow.invalidatePhaseTarget(p.id(), f.targetId(), why, false);
        this.repository.correct(f, c.finalScore(), why, operator(), c.expectedVersion());
        this.repository.snapshot(
                p.id(),
                f.targetId(),
                p.inputVersion(),
                "CORRECT",
                this.support.json(f),
                this.support.json(c),
                why,
                operator());
        this.workflow.bumpInput(p.id());
        return new ReviewCommands.IdVersion(id, f.version() + 1);
    }

    private Advancement advancement(final long id, final ReviewCommands.Advance c) {
        final WorkflowPhase p = this.workflow.lockPhase(id);
        if (!"FROZEN".equals(p.state()) || !"PROVINCIAL".equals(p.code())) {
            throw BizException.conflict("仅冻结省审阶段可决定晋级");
        }
        if (!Set.of("ADVANCED", "ELIMINATED", "LOCKED").contains(c.decision())) {
            throw BizException.badRequest("晋级决定无效");
        }
        final List<FinalScore> finals = this.repository.finals(id);
        final Set<Long> selected = new TreeSet<>();
        if (c.threshold() != null) {
            final Rule rule = this.repository.rule(p.currentRuleId());
            ReviewRules.number(c.threshold(), rule.minScore(), rule.maxScore());
            finals.stream()
                    .filter(f -> "VALID".equals(f.state())
                            && f.finalScore() != null
                            && f.finalScore().compareTo(c.threshold()) >= 0)
                    .forEach(f -> selected.add(f.targetId()));
        } else {
            reason(c.reason());
            if (c.targetIds() == null) {
                throw BizException.badRequest("请选择目标");
            }
            selected.addAll(c.targetIds());
        }
        if (selected.isEmpty() || selected.size() > 500) {
            throw BizException.badRequest("晋级对象须为1至500个");
        }
        final List<AdvancementRow> rows = new ArrayList<>();
        for (final long target : selected) {
            final FinalScore f = finals.stream()
                    .filter(v -> v.targetId() == target)
                    .findFirst()
                    .orElse(null);
            final String blocked = f == null || !"VALID".equals(f.state())
                    ? "无有效最终成绩"
                    : this.eligible(p, this.repository.target(target));
            rows.add(new AdvancementRow(target, f == null ? 0 : f.version(), c.decision(), blocked));
        }
        return new Advancement(id, c, rows);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PreviewResponse<AdvancementRow> advancePreview(final long id, final ReviewCommands.Advance c) {
        final WorkflowPhase p = this.support.lockAndValidate(id);
        final Advancement v = this.advancement(id, c);
        return this.preview(
                "ADVANCE",
                p,
                v,
                v.rows(),
                v.rows().stream().filter(r -> !r.blockedReason().isBlank()).toList());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.Count advance(final long id, final ReviewCommands.Confirm c) {
        final Optional<ReviewCommands.Count> cached =
                this.support.receipt(operator(), "ADVANCE:" + id, c.requestKey(), c, ReviewCommands.Count.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        final Advancement old = this.payload(c.previewToken(), "ADVANCE", Advancement.class);
        if (old.phaseId() != id) {
            throw BizException.conflict("预览阶段不匹配");
        }
        final Advancement current = this.advancement(id, old.command());
        this.same(old, current);
        if (current.rows().stream().anyMatch(r -> !r.blockedReason().isBlank())) {
            throw BizException.conflict("存在失效成绩，不可晋级");
        }
        for (final AdvancementRow row : current.rows()) {
            this.workflow.invalidatePhaseTarget(id, row.targetId(), "晋级决定已变化", false);
            this.repository.decideTarget(
                    id, row.targetId(), row.decision(), comment(old.command().reason()), row.finalVersion());
        }
        this.workflow.bumpInput(id);
        final ReviewCommands.Count result =
                new ReviewCommands.Count(current.rows().size(), 0, 0);
        this.support.complete(operator(), "ADVANCE:" + id, c.requestKey(), c, result);
        return result;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.Count promote(final long id, final ReviewCommands.Promote c) {
        final Optional<ReviewCommands.Count> cached =
                this.support.receipt(operator(), "PROMOTE:" + id, c.requestKey(), c, ReviewCommands.Count.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        final WorkflowPhase p = this.support.lockAndValidate(id);
        version(p.version(), c.expectedVersion());
        if (!"FROZEN".equals(p.state())
                || !"PROVINCIAL".equals(p.code())
                || c.targetIds() == null
                || c.targetIds().isEmpty()
                || c.targetIds().size() > 500) {
            throw BizException.conflict("晋级阶段或候选无效");
        }
        final long national = this.repository.createPhase(p.competitionId(), "NATIONAL");
        final boolean separate = this.repository.separateNational(p.competitionId());
        for (final long target : new TreeSet<>(c.targetIds())) {
            final FinalScore f = this.repository.finals(id).stream()
                    .filter(v -> v.targetId() == target && "VALID".equals(v.state()))
                    .findFirst()
                    .orElseThrow(() -> BizException.conflict("最终成绩已失效"));
            if (this.repository.targetDecisions(id).stream()
                    .noneMatch(d -> d.targetId() == target
                            && "ADVANCED".equals(d.decision())
                            && d.inputFinalVersion() == f.version())) {
                throw BizException.conflict("请先确认有效晋级名单");
            }
            if (separate) {
                this.repository.qualify(id, target, f.version());
            } else {
                if (this.repository.targetDecisions(national).stream()
                        .noneMatch(d -> d.targetId() == target && d.inputFinalVersion() == f.version())) {
                    this.repository.decideTarget(national, target, "ELIGIBLE", "省审晋级", f.version());
                }
            }
        }
        this.workflow.bumpInput(national);
        this.workflow.bumpInput(id);
        final ReviewCommands.Count result = new ReviewCommands.Count(new HashSet<>(c.targetIds()).size(), 0, 0);
        this.support.complete(operator(), "PROMOTE:" + id, c.requestKey(), c, result);
        return result;
    }

    public record QualificationView(
            Qualification qualification, Target sourceTarget, List<Target> matchingTargets, String blockedReason) {}

    private List<String> credentials(final Target t) {
        final List<String> keys = this.repository.credentials(t.id());
        if (keys.isEmpty()
                || keys.stream().anyMatch(v -> v.isBlank() || v.endsWith(":"))
                || new HashSet<>(keys).size() != keys.size()) {
            throw BizException.conflict("参赛人缺少或重复规范化证件，不能关联资格");
        }
        return keys.stream().map(this.support::hash).sorted().toList();
    }

    private String attachmentBlocked(final Qualification q, final Target national) {
        final WorkflowPhase p =
                this.workflow.phase(q.sourcePhaseId()).orElseThrow(() -> BizException.notFound("阶段不存在"));
        final Target source = this.repository.target(q.sourceTargetId());
        if (!this.eligible(p, source).isBlank()
                || !"ELIGIBLE".equals(q.status())
                || this.repository.finals(p.id()).stream()
                        .noneMatch(f -> f.targetId() == source.id()
                                && "VALID".equals(f.state())
                                && f.version() == q.finalVersion())) {
            return "资格或来源成绩已失效";
        }
        if (source.entryId() == national.entryId()
                || source.competitionId() != national.competitionId()
                || !source.source().equals(national.source())
                || source.firstCategoryId() != national.firstCategoryId()
                || !source.targetType().equals(national.targetType())
                || !Set.of("NATIONAL", "BOTH").contains(national.stageScope())
                || !"ACTIVE".equals(national.status())
                || !"ACTIVE".equals(national.entryStatus())
                || national.legacyStatus() != 3) {
            return "国赛作品归属、来源、赛项或状态不匹配";
        }
        try {
            return this.credentials(source).equals(this.credentials(national)) ? "" : "规范化证件集合不匹配";
        } catch (final BizException e) {
            return Objects.requireNonNullElse(e.getMessage(), "资格校验失败");
        }
    }

    public PageResult<QualificationView> qualifications(final long competition, final PageQuery qpage) {
        final List<QualificationView> rows = new ArrayList<>();
        for (final Qualification q : this.repository.qualifications(competition)) {
            final WorkflowPhase p = this.support.lockAndValidate(q.sourcePhaseId());
            final Qualification fresh = this.repository.qualification(q.id());
            final Target source = this.repository.target(q.sourceTargetId());
            rows.add(new QualificationView(
                    fresh,
                    source,
                    this.repository.targets(p.competitionId()).stream()
                            .filter(t -> this.attachmentBlocked(fresh, t).isBlank())
                            .toList(),
                    Set.of("ELIGIBLE", "ATTACHED").contains(fresh.status()) ? "" : "资格已失效"));
        }
        return page(rows, qpage);
    }

    private AttachSnapshot attachment(final long id, final ReviewCommands.Attach c) {
        final Qualification q = this.repository.qualification(id);
        version(q.version(), c.expectedVersion());
        reason(c.reason());
        final Target national = this.repository.target(c.nationalTargetId());
        final String blocked = this.attachmentBlocked(q, national);
        if (!blocked.isBlank()) {
            throw BizException.conflict(blocked);
        }
        if (q.nationalTargetId() != null && q.nationalTargetId() != national.id()) {
            throw BizException.conflict("资格已关联其他作品");
        }
        if (this.repository.qualifications(national.competitionId()).stream()
                .anyMatch(other -> other.id() != id
                        && Objects.equals(other.nationalTargetId(), national.id())
                        && Set.of("ELIGIBLE", "ATTACHED").contains(other.status()))) {
            throw BizException.conflict("国赛对象已关联其他资格");
        }
        return new AttachSnapshot(id, c, q.version(), national.version(), this.credentials(national));
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PreviewResponse<Impact> attachPreview(final long id, final ReviewCommands.Attach c) {
        final Qualification q = this.repository.qualification(id);
        final WorkflowPhase p = this.support.lockAndValidate(q.sourcePhaseId());
        final AttachSnapshot s = this.attachment(id, c);
        return this.preview(
                "ATTACH",
                p,
                s,
                List.of(new Impact(c.nationalTargetId(), "QUALIFICATION", "关联已有国赛对象，不复制作品")),
                List.of());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReviewCommands.IdVersion attach(final long id, final ReviewCommands.Attach c) {
        final Optional<ReviewCommands.IdVersion> cached =
                this.support.receipt(operator(), "ATTACH:" + id, c.requestKey(), c, ReviewCommands.IdVersion.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        final AttachSnapshot old =
                this.payload(ReviewRules.text(c.previewToken(), "预览token", true, 36), "ATTACH", AttachSnapshot.class);
        final ReviewCommands.Attach original = new ReviewCommands.Attach(
                c.nationalTargetId(),
                c.reason(),
                c.expectedVersion(),
                old.command().previewToken(),
                old.command().requestKey());
        this.same(old, this.attachment(id, original));
        final Qualification q = this.repository.qualification(id);
        final WorkflowPhase source = this.workflow.lockPhase(q.sourcePhaseId());
        final long national = this.repository.createPhase(source.competitionId(), "NATIONAL");
        this.repository.attach(id, c.nationalTargetId(), reason(c.reason()), c.expectedVersion());
        this.repository.decideTarget(national, c.nationalTargetId(), "ELIGIBLE", reason(c.reason()), q.finalVersion());
        this.workflow.bumpInput(national);
        this.workflow.bumpInput(source.id());
        final ReviewCommands.IdVersion result = new ReviewCommands.IdVersion(id, q.version() + 1);
        this.support.complete(operator(), "ATTACH:" + id, c.requestKey(), c, result);
        return result;
    }

    public List<Target> qualificationCandidates(final long id) {
        final Qualification q = this.repository.qualification(id);
        final WorkflowPhase p = this.support.lockAndValidate(q.sourcePhaseId());
        final Qualification fresh = this.repository.qualification(id);
        return this.repository.targets(p.competitionId()).stream()
                .filter(t -> this.attachmentBlocked(fresh, t).isBlank())
                .toList();
    }
}
