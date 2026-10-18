package com.weiran.cqt.infrastructure.review;

import com.weiran.cqt.domain.review.AwardCandidate;
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
import com.weiran.cqt.domain.review.ReviewRepository;
import com.weiran.cqt.domain.review.ReviewWorkflowRepository;
import com.weiran.cqt.domain.review.ReviewerSnapshot;
import com.weiran.cqt.domain.review.WorkflowPhase;
import com.weiran.cqt.domain.review.WorkflowPreview;
import com.weiran.cqt.domain.review.WorkflowPreviewStore;
import com.weiran.cqt.domain.review.WorkflowReceipt;
import com.weiran.framework.error.BizException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullUnmarked;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/** 明确关系SQL与领域投影；不通过会打印敏感绑定参数的MyBatis mapper。 */
@NullUnmarked
public class JdbcReviewRepository implements ReviewRepository, ReviewWorkflowRepository, WorkflowPreviewStore {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcReviewRepository(final NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Map<String, Object> p(final Object... values) {
        final Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put((String) values[i], values[i + 1]);
        }
        return result;
    }

    private int update(final String sql, final Map<String, ?> params) {
        return this.jdbc.update(sql, params);
    }

    private long insert(final String sql, final Map<String, ?> params) {
        final GeneratedKeyHolder keys = new GeneratedKeyHolder();
        this.jdbc.update(sql, new MapSqlParameterSource(params), keys, new String[] {"id"});
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    private <T> List<T> list(final String sql, final Map<String, ?> params, final Class<T> type) {
        return this.jdbc.query(sql, params, (rs, index) -> {
            final RecordComponent[] components = type.getRecordComponents();
            final Class<?>[] types = new Class<?>[components.length];
            final Object[] args = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
                final String col = components[i]
                        .getName()
                        .replaceAll("([a-z])([A-Z])", "$1_$2")
                        .toLowerCase(Locale.ROOT);
                if (types[i] == LocalDateTime.class) {
                    args[i] = rs.getObject(col, LocalDateTime.class);
                } else if (types[i] == long.class) {
                    args[i] = rs.getLong(col);
                } else if (types[i] == int.class) {
                    args[i] = rs.getInt(col);
                } else if (types[i] == boolean.class) {
                    args[i] = rs.getBoolean(col);
                } else if (types[i] == Long.class) {
                    final Object v = rs.getObject(col);
                    args[i] = v == null ? null : ((Number) v).longValue();
                } else if (types[i] == BigDecimal.class) {
                    args[i] = rs.getBigDecimal(col);
                } else {
                    args[i] = rs.getString(col);
                }
            }
            try {
                return type.getDeclaredConstructor(types).newInstance(args);
            } catch (final ReflectiveOperationException e) {
                throw new IllegalStateException("评审投影映射失败", e);
            }
        });
    }

    private <T> Optional<T> optional(final String sql, final Map<String, ?> params, final Class<T> type) {
        return this.list(sql, params, type).stream().findFirst();
    }

    private <T> T one(final String sql, final Map<String, ?> params, final Class<T> type) {
        return this.optional(sql, params, type).orElseThrow(() -> BizException.notFound("评审记录不存在"));
    }

    private static void changed(final int rows) {
        if (rows != 1) {
            throw BizException.conflict("记录版本已变化，请刷新后重试");
        }
    }

    private static final String PHASE =
            "SELECT id,competition_id,code,state,current_rule_id,input_version,version FROM " + "cqt_review_phases";
    private static final String ENTRY = "SELECT id,competition_id,title,source_database "
            + "source,scoring_scope,stage_scope,status,COALESCE(legacy_status,0) "
            + "legacy_status,review_version version,first_category_legacy_id "
            + "first_category_id,second_category_legacy_id second_category_id,region_id,"
            + "COALESCE(group_name,'') group_name,COALESCE(attachment_url,'') "
            + "attachment_url,COALESCE(description,'') description FROM cqt_entries";
    private static final String TARGET =
            "SELECT t.id,t.entry_id,t.entry_participant_id " + "participant_id,e.competition_id,e.source_database "
                    + "source,t.target_type,COALESCE(p.name_value,'') name,e.title,e.first_category_legacy_id "
                    + "first_category_id,e.second_category_legacy_id second_category_id,e.region_id,"
                    + "COALESCE(ep.group_snapshot,e.group_name,'') group_name,e.stage_scope,CASE WHEN "
                    + "t.target_type='PARTICIPANT' AND (ep.status IS NULL OR ep.status<>'ACTIVE') THEN 'INACTIVE' "
                    + "ELSE t.status END status,e.status entry_status,COALESCE(e.legacy_status,0) "
                    + "legacy_status,t.review_version version FROM cqt_evaluation_targets t JOIN cqt_entries e ON "
                    + "e.id=t.entry_id LEFT JOIN cqt_entry_participants ep ON ep.id=t.entry_participant_id LEFT JOIN "
                    + "cqt_people p ON p.id=ep.person_id";

    @Override
    public WorkflowPhase lockPhase(final long id) {
        return this.one(PHASE + " WHERE id=:id FOR UPDATE", p("id", id), WorkflowPhase.class);
    }

    @Override
    public List<WorkflowPhase> lockCompetition(final long competitionId) {
        return this.list(
                PHASE + " WHERE competition_id=:c ORDER BY id FOR UPDATE", p("c", competitionId), WorkflowPhase.class);
    }

    @Override
    public Optional<WorkflowPhase> phase(final long id) {
        return this.optional(PHASE + " WHERE id=:id", p("id", id), WorkflowPhase.class);
    }

    @Override
    public List<WorkflowPhase> phases(final long c) {
        return this.list(PHASE + " WHERE (:c=0 OR competition_id=:c) ORDER BY id", p("c", c), WorkflowPhase.class);
    }

    @Override
    public void bumpInput(final long id) {
        this.update(
                "UPDATE cqt_review_phases SET input_version=input_version+1,version=version+1 WHERE id=:id",
                p("id", id));
    }

    @Override
    public void invalidate(final long id, final String reason) {
        this.invalidatePhase(id, reason);
    }

    @Override
    public void invalidatePhase(final long id, final String reason) {
        this.invalidatePhaseContents(id, reason, true);
    }

    @Override
    public void invalidateDerived(final long id, final String reason) {
        this.invalidatePhaseContents(id, reason, false);
    }

    private void invalidatePhaseContents(final long phaseId, final String reason, final boolean includeFinal) {
        final List<Long> targets = this.jdbc.queryForList(
                "SELECT target_id FROM cqt_review_assignments WHERE phase_id=:ph "
                        + "UNION SELECT target_id FROM cqt_final_scores WHERE phase_id=:ph "
                        + "UNION SELECT target_id FROM cqt_phase_target_status WHERE phase_id=:ph "
                        + "UNION SELECT source_target_id FROM cqt_review_qualifications WHERE source_phase_id=:ph",
                p("ph", phaseId),
                Long.class);
        for (final long target : targets) {
            this.invalidatePhaseTarget(phaseId, target, reason, includeFinal);
        }
    }

    @Override
    public void invalidateTarget(final long competition, final long target, final String reason) {
        for (final WorkflowPhase phase : this.phases(competition)) {
            this.invalidatePhaseTarget(phase.id(), target, reason, true);
        }
    }

    @Override
    public void invalidateReviewer(final long phaseId, final long reviewerId, final String reason) {
        final List<Long> targets = this.jdbc.queryForList(
                "SELECT DISTINCT target_id FROM cqt_review_assignments "
                        + "WHERE phase_id=:ph AND reviewer_id=:u AND state='ACTIVE' ORDER BY target_id",
                p("ph", phaseId, "u", reviewerId),
                Long.class);
        for (final long target : targets) {
            this.invalidatePhaseTarget(phaseId, target, reason, true);
        }
    }

    @Override
    public void invalidatePhaseTarget(
            final long phaseId, final long targetId, final String reason, final boolean includeCurrentFinal) {
        final WorkflowPhase phase = this.phase(phaseId).orElseThrow(() -> BizException.notFound("阶段不存在"));
        // Capture live associations before marking them STALE. Historical associations remain immutable evidence.
        final List<Long> descendants = this.jdbc.queryForList(
                "SELECT national_target_id FROM cqt_review_qualifications WHERE source_phase_id=:ph "
                        + "AND source_target_id=:t AND status IN('ELIGIBLE','ATTACHED') "
                        + "AND national_target_id IS NOT NULL",
                p("ph", phaseId, "t", targetId),
                Long.class);
        this.invalidateLocalTarget(phaseId, targetId, reason, includeCurrentFinal);
        if ("PROVINCIAL".equals(phase.code())) {
            for (final WorkflowPhase national : this.phases(phase.competitionId())) {
                if ("NATIONAL".equals(national.code())) {
                    this.invalidateLocalTarget(national.id(), targetId, reason, true);
                    for (final long descendant : descendants) {
                        if (descendant != targetId) {
                            this.invalidateLocalTarget(national.id(), descendant, reason, true);
                        }
                    }
                    this.bumpInput(national.id());
                }
            }
        }
    }

    private void invalidateLocalTarget(
            final long phaseId, final long targetId, final String reason, final boolean includeFinal) {
        final Map<String, Object> params = p("ph", phaseId, "t", targetId, "r", reason);
        if (includeFinal) {
            this.update(
                    "UPDATE cqt_final_scores SET state='STALE' WHERE phase_id=:ph AND target_id=:t "
                            + "AND state='VALID'",
                    params);
        }
        this.update(
                "UPDATE cqt_award_batches SET status='STALE',reason=:r,version=version+1 "
                        + "WHERE phase_id=:ph AND status='CONFIRMED' "
                        + "AND JSON_CONTAINS(candidate_json,CAST(:t AS JSON))",
                params);
        this.update(
                "UPDATE cqt_awards a JOIN cqt_award_batches b ON b.id=a.batch_id "
                        + "SET a.status='STALE' WHERE b.phase_id=:ph AND b.status='STALE' "
                        + "AND a.status='CONFIRMED'",
                params);
        this.update(
                "UPDATE cqt_phase_target_status SET state='STALE',status_version=status_version+1 "
                        + "WHERE phase_id=:ph AND target_id=:t AND state='VALID'",
                params);
        this.update(
                "UPDATE cqt_review_qualifications SET status='STALE',version=version+1 "
                        + "WHERE source_phase_id=:ph AND source_target_id=:t AND status IN('ELIGIBLE','ATTACHED')",
                params);
    }

    @Override
    public List<ReviewerSnapshot> reviewerSnapshots(final long id) {
        return this.list(
                "SELECT DISTINCT reviewer_id user_id,user_state state,user_updated_at updated_at FROM "
                        + "cqt_review_assignments WHERE phase_id=:id AND state='ACTIVE'",
                p("id", id),
                ReviewerSnapshot.class);
    }

    @Override
    public void refreshReviewer(final long phaseId, final ReviewerSnapshot v) {
        this.update(
                "UPDATE cqt_review_assignments SET user_state=:s,user_updated_at=:u,version=version+1 WHERE "
                        + "phase_id=:p AND reviewer_id=:id",
                p("s", v.state(), "u", v.updatedAt(), "p", phaseId, "id", v.userId()));
    }

    @Override
    public List<AwardCandidate> awardCandidates(final long id) {
        return this.list(
                "SELECT t.id target_id,t.entry_id,t.entry_participant_id "
                        + "participant_id,t.target_type,e.first_category_legacy_id "
                        + "first_category_id,e.second_category_legacy_id "
                        + "second_category_id,COALESCE(ep.group_snapshot,e.group_name,'') "
                        + "group_name,COALESCE(pe.name_value,'') display_name,e.title,f.final_score "
                        + "score,COALESCE(f.version,0) final_version,COALESCE(f.state,'MISSING') "
                        + "final_state,t.review_version target_version,(f.state='VALID' AND t.status='ACTIVE' AND "
                        + "e.status='ACTIVE' AND e.legacy_status=3 AND (t.target_type='ENTRY' OR ep.status='ACTIVE')) "
                        + "eligible,CASE WHEN f.state<>'VALID' OR f.state IS NULL THEN '成绩未确认或已失效' WHEN "
                        + "t.status<>'ACTIVE' OR e.status<>'ACTIVE' THEN '作品或对象失效' ELSE '' END blocked_reason FROM "
                        + "cqt_evaluation_targets t JOIN cqt_entries e ON e.id=t.entry_id JOIN cqt_review_phases ph ON "
                        + "ph.competition_id=e.competition_id LEFT JOIN cqt_entry_participants ep ON "
                        + "ep.id=t.entry_participant_id LEFT JOIN cqt_people pe ON pe.id=ep.person_id LEFT JOIN "
                        + "cqt_final_scores f ON f.target_id=t.id AND f.phase_id=ph.id WHERE ph.id=:id AND "
                        + "t.status='ACTIVE' AND (f.id IS NOT NULL OR e.legacy_status=3) ORDER BY t.id",
                p("id", id),
                AwardCandidate.class);
    }

    @Override
    public void save(final WorkflowPreview v) {
        this.update("DELETE FROM cqt_review_previews WHERE expires_at<CURRENT_TIMESTAMP", p());
        this.insert(
                "INSERT INTO "
                        + "cqt_review_previews(token,purpose,operator_id,phase_id,payload_hash,input_version,snapshot"
                        + ",expires_at) VALUES(:t,:p,:o,:ph,:h,:v,:s,:e)",
                p(
                        "t",
                        v.token(),
                        "p",
                        v.purpose(),
                        "o",
                        v.operatorId(),
                        "ph",
                        v.phaseId(),
                        "h",
                        v.payloadHash(),
                        "v",
                        v.inputVersion(),
                        "s",
                        v.snapshot(),
                        "e",
                        v.expiresAt()));
    }

    @Override
    public Optional<WorkflowPreview> find(final String token) {
        return this.optional(
                "SELECT token,purpose,operator_id,phase_id,input_version,payload_hash,snapshot,expires_at FROM "
                        + "cqt_review_previews WHERE token=:t",
                p("t", token),
                WorkflowPreview.class);
    }

    @Override
    public Optional<WorkflowReceipt> receipt(final long o, final String op, final String key) {
        return this.optional(
                "SELECT operator_id,operation,request_key,payload_hash,result_json result FROM "
                        + "cqt_review_command_receipts WHERE operator_id=:o AND operation=:op AND request_key=:k",
                p("o", o, "op", op, "k", key),
                WorkflowReceipt.class);
    }

    @Override
    public void consume(final String token) {
        changed(this.update(
                "UPDATE cqt_review_previews SET consumed_at=CURRENT_TIMESTAMP WHERE token=:t AND consumed_at "
                        + "IS NULL",
                p("t", token)));
    }

    @Override
    public void saveReceipt(final WorkflowReceipt v) {
        this.insert(
                "INSERT INTO "
                        + "cqt_review_command_receipts(operator_id,operation,request_key,payload_hash,result_json) "
                        + "VALUES(:o,:op,:k,:h,:r)",
                p(
                        "o",
                        v.operatorId(),
                        "op",
                        v.operation(),
                        "k",
                        v.requestKey(),
                        "h",
                        v.payloadHash(),
                        "r",
                        v.result()));
    }

    @Override
    public List<Entry> entries(final long c) {
        return this.list(ENTRY + " WHERE (:c=0 OR competition_id=:c) ORDER BY id", p("c", c), Entry.class);
    }

    @Override
    public Entry entry(final long id) {
        return this.one(ENTRY + " WHERE id=:id", p("id", id), Entry.class);
    }

    @Override
    public List<Member> members(final long id) {
        return this.list(
                "SELECT ep.id,p.name_value name,COALESCE(ep.school_snapshot,p.school_value,'') "
                        + "school,COALESCE(ep.group_snapshot,'') group_name FROM cqt_entry_participants ep JOIN "
                        + "cqt_people p ON p.id=ep.person_id WHERE ep.entry_id=:id AND ep.status='ACTIVE' ORDER BY "
                        + "ep.sort_order,ep.id",
                p("id", id),
                Member.class);
    }

    @Override
    public List<Target> targets(final long c) {
        return this.list(TARGET + " WHERE e.competition_id=:c ORDER BY t.id", p("c", c), Target.class);
    }

    @Override
    public Target target(final long id) {
        return this.one(TARGET + " WHERE t.id=:id", p("id", id), Target.class);
    }

    @Override
    public List<String> credentials(final long id) {
        return this.jdbc
                .queryForList(
                        "SELECT "
                                + "CONCAT(COALESCE(p.credential_type,''),':',UPPER(TRIM(COALESCE(p.normalized_id_card,p.id_ca"
                                + "rd_value,'')))) FROM cqt_evaluation_targets t JOIN cqt_entry_participants ep ON ep.entry_i"
                                + "d=t.entry_id JOIN cqt_people p ON p.id=ep.person_id WHERE t.id=:id AND ep.status='ACTIVE' "
                                + "AND (t.target_type='ENTRY' OR ep.id=t.entry_participant_id) ORDER BY p.normalized_id_card,"
                                + "ep.id",
                        p("id", id),
                        String.class)
                .stream()
                .sorted()
                .toList();
    }

    @Override
    public boolean hasLiveReview(final long entry) {
        final Long count = this.jdbc.queryForObject(
                "SELECT COUNT(*) FROM cqt_evaluation_targets t WHERE t.entry_id=:e AND (EXISTS(SELECT 1 FROM "
                        + "cqt_review_assignments a WHERE a.target_id=t.id AND a.state='ACTIVE') OR EXISTS(SELECT 1 FROM "
                        + "cqt_final_scores f WHERE f.target_id=t.id AND f.state='VALID') OR EXISTS(SELECT 1 FROM "
                        + "cqt_awards a WHERE a.target_id=t.id AND a.status='CONFIRMED'))",
                p("e", entry),
                Long.class);
        return count != null && count > 0;
    }

    @Override
    public boolean referenced(final long id) {
        return Boolean.TRUE.equals(this.jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM cqt_review_assignments a JOIN cqt_evaluation_targets t ON "
                        + "t.id=a.target_id WHERE t.entry_id=:id) OR EXISTS(SELECT 1 FROM cqt_final_scores f JOIN "
                        + "cqt_evaluation_targets t ON t.id=f.target_id WHERE t.entry_id=:id) OR EXISTS(SELECT 1 FROM "
                        + "cqt_awards a JOIN cqt_evaluation_targets t ON t.id=a.target_id WHERE t.entry_id=:id)",
                p("id", id),
                Boolean.class));
    }

    @Override
    public int prepareTargets(final long id) {
        final Entry e = this.entry(id);
        final String type = "zhongxi".equals(e.source()) ? "ENTRY" : "PARTICIPANT";
        this.update(
                "UPDATE cqt_entries SET scoring_scope=:type,review_version=review_version+1 WHERE id=:id AND "
                        + "scoring_scope<>:type",
                p("type", type, "id", id));
        this.update(
                "UPDATE cqt_evaluation_targets SET status='HISTORICAL',review_version=review_version+1 WHERE "
                        + "entry_id=:id AND target_type<>:type AND status='ACTIVE'",
                p("id", id, "type", type));
        if ("ENTRY".equals(type)) {
            return this.update(
                    "INSERT INTO cqt_evaluation_targets(entry_id,target_type,status) VALUES(:id,'ENTRY','ACTIVE') "
                            + "ON DUPLICATE KEY UPDATE status='ACTIVE'",
                    p("id", id));
        }
        return this.update(
                "INSERT INTO cqt_evaluation_targets(entry_id,entry_participant_id,target_type,status) SELECT "
                        + "ep.entry_id,ep.id,'PARTICIPANT','ACTIVE' FROM cqt_entry_participants ep WHERE ep.entry_id=:id "
                        + "AND ep.status='ACTIVE' ON DUPLICATE KEY UPDATE status='ACTIVE'",
                p("id", id));
    }

    @Override
    public void decideEntry(
            final long id, final String decision, final String reason, final long operator, final long version) {
        changed(this.update(
                "UPDATE cqt_entries SET legacy_status=:s,review_version=review_version+1 WHERE id=:id AND "
                        + "review_version=:v AND status='ACTIVE'",
                p("s", "APPROVED".equals(decision) ? 3 : 2, "id", id, "v", version)));
        this.insert(
                "INSERT INTO cqt_entry_review_decisions(entry_id,decision,reason,operator_id,version) "
                        + "VALUES(:id,:d,:r,:o,:v)",
                p("id", id, "d", decision, "r", reason, "o", operator, "v", version + 1));
    }

    @Override
    public List<InitialDecision> decisions(final long id) {
        return this.list(
                "SELECT id,entry_id,decision,reason,operator_id,version,created_at FROM "
                        + "cqt_entry_review_decisions WHERE entry_id=:id ORDER BY version",
                p("id", id),
                InitialDecision.class);
    }

    @Override
    public long createPhase(final long c, final String code) {
        this.update(
                "INSERT IGNORE INTO cqt_review_phases(competition_id,code) VALUES(:c,:code)", p("c", c, "code", code));
        return this.jdbc.queryForObject(
                "SELECT id FROM cqt_review_phases WHERE competition_id=:c AND code=:code",
                p("c", c, "code", code),
                Long.class);
    }

    @Override
    public void phaseState(final long id, final String state, final long version) {
        changed(this.update(
                "UPDATE cqt_review_phases SET state=:s,version=version+1,input_version=input_version+1 WHERE "
                        + "id=:id AND version=:v",
                p("s", state, "id", id, "v", version)));
    }

    @Override
    public Rule rule(final long id) {
        return this.one("SELECT * FROM cqt_review_phase_rules WHERE id=:id", p("id", id), Rule.class);
    }

    @Override
    public List<Dimension> dimensions(final long rule) {
        return this.list(
                "SELECT * FROM cqt_review_dimensions WHERE rule_id=:id ORDER BY sort,id",
                p("id", rule),
                Dimension.class);
    }

    @Override
    public long saveRule(
            final long phase,
            final BigDecimal min,
            final BigDecimal max,
            final int minimum,
            final BigDecimal passing,
            final long operator) {
        final long revision = this.jdbc.queryForObject(
                "SELECT COALESCE(MAX(revision),0)+1 FROM cqt_review_phase_rules WHERE phase_id=:p",
                p("p", phase),
                Long.class);
        final long id = this.insert(
                "INSERT INTO "
                        + "cqt_review_phase_rules(phase_id,revision,min_score,max_score,minimum_reviewers,passing_sco"
                        + "re,created_by) VALUES(:p,:v,:min,:max,:n,:passing,:o)",
                p("p", phase, "v", revision, "min", min, "max", max, "n", minimum, "passing", passing, "o", operator));
        this.update(
                "UPDATE cqt_review_phases SET "
                        + "current_rule_id=:r,input_version=input_version+1,version=version+1 WHERE id=:p",
                p("r", id, "p", phase));
        return id;
    }

    @Override
    public void addDimension(final Dimension d) {
        this.insert(
                "INSERT INTO "
                        + "cqt_review_dimensions(rule_id,first_category_id,name,description,min_score,max_score,sort) "
                        + "VALUES(:r,:c,:n,:d,:min,:max,:s)",
                p(
                        "r",
                        d.ruleId(),
                        "c",
                        d.firstCategoryId(),
                        "n",
                        d.name(),
                        "d",
                        d.description(),
                        "min",
                        d.minScore(),
                        "max",
                        d.maxScore(),
                        "s",
                        d.sort()));
    }

    @Override
    public List<Scope> scopes(final long phase) {
        return this.list(
                "SELECT * FROM cqt_reviewer_scopes WHERE (:p=0 OR phase_id=:p) ORDER BY id",
                p("p", phase),
                Scope.class);
    }

    @Override
    public Scope scope(final long id) {
        return this.one("SELECT * FROM cqt_reviewer_scopes WHERE id=:id", p("id", id), Scope.class);
    }

    private static Map<String, Object> scopeParams(final Scope s) {
        return p(
                "u",
                s.userId(),
                "c",
                s.competitionId(),
                "p",
                s.phaseId(),
                "fc",
                s.firstCategoryId(),
                "sc",
                s.secondCategoryId() == null ? 0 : s.secondCategoryId(),
                "r",
                s.regionId() == null ? 0 : s.regionId(),
                "e",
                s.enabled(),
                "id",
                s.id(),
                "v",
                s.version());
    }

    @Override
    public long createScope(final Scope v) {
        return this.insert(
                "INSERT INTO "
                        + "cqt_reviewer_scopes(user_id,competition_id,phase_id,first_category_id,second_category_id,r"
                        + "egion_id,enabled) VALUES(:u,:c,:p,:fc,:sc,:r,:e)",
                scopeParams(v));
    }

    @Override
    public void updateScope(final Scope v, final long version) {
        changed(this.update(
                "UPDATE cqt_reviewer_scopes SET "
                        + "user_id=:u,competition_id=:c,phase_id=:p,first_category_id=:fc,second_category_id=:sc,regi"
                        + "on_id=:r,enabled=:e,version=version+1 WHERE id=:id AND version=:v",
                scopeParams(v)));
    }

    @Override
    public List<Assignment> assignments(final long p) {
        return this.list(
                "SELECT * FROM cqt_review_assignments WHERE (:p=0 OR phase_id=:p) ORDER BY id",
                p("p", p),
                Assignment.class);
    }

    @Override
    public List<Assignment> userAssignments(final long user) {
        return this.list(
                "SELECT * FROM cqt_review_assignments WHERE reviewer_id=:u ORDER BY id",
                p("u", user),
                Assignment.class);
    }

    @Override
    public Assignment assignment(final long id) {
        return this.one("SELECT * FROM cqt_review_assignments WHERE id=:id", p("id", id), Assignment.class);
    }

    @Override
    public long assign(final long phase, final long target, final long user, final ReviewerSnapshot snapshot) {
        this.update(
                "INSERT INTO "
                        + "cqt_review_assignments(phase_id,target_id,reviewer_id,state,user_state,user_updated_at) "
                        + "VALUES(:p,:t,:u,'ACTIVE',:s,:at) ON DUPLICATE KEY UPDATE "
                        + "state='ACTIVE',version=version+1,user_state=:s,user_updated_at=:at",
                p("p", phase, "t", target, "u", user, "s", snapshot.state(), "at", snapshot.updatedAt()));
        return this.jdbc.queryForObject(
                "SELECT id FROM cqt_review_assignments WHERE phase_id=:p AND target_id=:t AND reviewer_id=:u",
                p("p", phase, "t", target, "u", user),
                Long.class);
    }

    @Override
    public void assignmentState(final long id, final String state, final long version) {
        changed(this.update(
                "UPDATE cqt_review_assignments SET state=:s,version=version+1 WHERE id=:id AND version=:v",
                p("s", state, "id", id, "v", version)));
    }

    @Override
    public Optional<Score> score(final long assignment) {
        return this.optional(
                "SELECT * FROM cqt_review_scores WHERE assignment_id=:id", p("id", assignment), Score.class);
    }

    @Override
    public List<ScoreRevision> revisions(final long score) {
        return this.list(
                "SELECT * FROM cqt_review_score_revisions WHERE score_id=:id ORDER BY revision",
                p("id", score),
                ScoreRevision.class);
    }

    @Override
    public List<DimensionScore> dimensionScores(final long rev) {
        return this.list(
                "SELECT dimension_id,value score,comment FROM cqt_review_dimension_scores WHERE "
                        + "score_revision_id=:id ORDER BY dimension_id",
                p("id", rev),
                DimensionScore.class);
    }

    @Override
    public long saveScore(
            final long assignment,
            final long rule,
            final String status,
            final BigDecimal total,
            final String comment,
            final List<DimensionScore> dimensions,
            final long operator,
            final long expected) {
        final Optional<Score> old = this.score(assignment);
        final long scoreId;
        if (old.isEmpty()) {
            if (expected != 0) {
                throw BizException.conflict("评分版本冲突");
            }
            scoreId = this.insert(
                    "INSERT INTO cqt_review_scores(assignment_id,current_revision,status,rule_id,version) "
                            + "VALUES(:a,1,:s,:r,1)",
                    p("a", assignment, "s", status, "r", rule));
        } else {
            scoreId = old.get().id();
            changed(this.update(
                    "UPDATE cqt_review_scores SET "
                            + "current_revision=current_revision+1,status=:s,rule_id=:r,version=version+1 WHERE id=:id AND "
                            + "version=:v",
                    p("s", status, "r", rule, "id", scoreId, "v", expected)));
        }
        final long revision = old.map(Score::currentRevision).orElse(0L) + 1;
        final long revId = this.insert(
                "INSERT INTO "
                        + "cqt_review_score_revisions(score_id,revision,total,comment,status,rule_id,operator_id) "
                        + "VALUES(:s,:v,:t,:c,:state,:r,:o)",
                p("s", scoreId, "v", revision, "t", total, "c", comment, "state", status, "r", rule, "o", operator));
        for (final DimensionScore d : dimensions) {
            this.insert(
                    "INSERT INTO cqt_review_dimension_scores(score_revision_id,dimension_id,value,comment) "
                            + "VALUES(:r,:d,:s,:c)",
                    p("r", revId, "d", d.dimensionId(), "s", d.score(), "c", d.comment()));
        }
        return revision;
    }

    @Override
    public List<FinalScore> finals(final long phase) {
        return this.list(
                "SELECT * FROM cqt_final_scores WHERE phase_id=:p ORDER BY target_id", p("p", phase), FinalScore.class);
    }

    @Override
    public FinalScore finalScore(final long id) {
        return this.one("SELECT * FROM cqt_final_scores WHERE id=:id", p("id", id), FinalScore.class);
    }

    @Override
    public long saveFinal(
            final long phase, final long target, final BigDecimal avg, final int count, final long snapshot) {
        this.update(
                "INSERT INTO "
                        + "cqt_final_scores(phase_id,target_id,final_score,average_score,valid_reviewer_count,state,v"
                        + "ersion,snapshot_id) VALUES(:p,:t,:a,:a,:n,'VALID',1,:s) ON DUPLICATE KEY UPDATE final_scor"
                        + "e=:a,average_score=:a,valid_reviewer_count=:n,state='VALID',version=version+1,snapshot_id="
                        + ":s",
                p("p", phase, "t", target, "a", avg, "n", count, "s", snapshot));
        return this.jdbc.queryForObject(
                "SELECT id FROM cqt_final_scores WHERE phase_id=:p AND target_id=:t",
                p("p", phase, "t", target),
                Long.class);
    }

    @Override
    public void staleFinal(final long phase, final long target) {
        this.update(
                "UPDATE cqt_final_scores SET state='STALE' WHERE phase_id=:p AND target_id=:t",
                p("p", phase, "t", target));
    }

    @Override
    public long snapshot(
            final long phase,
            final long target,
            final long input,
            final String kind,
            final String in,
            final String out,
            final String reason,
            final long op) {
        return this.insert(
                "INSERT INTO "
                        + "cqt_score_snapshots(phase_id,target_id,input_version,kind,input_json,output_json,reason,op"
                        + "erator_id) VALUES(:p,:t,:v,:k,:i,:out,:r,:o)",
                p("p", phase, "t", target, "v", input, "k", kind, "i", in, "out", out, "r", reason, "o", op));
    }

    @Override
    public List<Snapshot> snapshots(final long phase, final long target) {
        return this.list(
                "SELECT * FROM cqt_score_snapshots WHERE phase_id=:p AND target_id=:t ORDER BY id",
                p("p", phase, "t", target),
                Snapshot.class);
    }

    @Override
    public void correct(
            final FinalScore f, final BigDecimal value, final String reason, final long operator, final long expected) {
        changed(this.update(
                "UPDATE cqt_final_scores SET final_score=:n,version=version+1 WHERE id=:id AND version=:v AND "
                        + "state='VALID'",
                p("n", value, "id", f.id(), "v", expected)));
        this.insert(
                "INSERT INTO "
                        + "cqt_score_corrections(final_score_id,before_score,after_score,reason,operator_id,final_ver"
                        + "sion) VALUES(:id,:b,:a,:r,:o,:v)",
                p("id", f.id(), "b", f.finalScore(), "a", value, "r", reason, "o", operator, "v", expected + 1));
    }

    @Override
    public List<Correction> corrections(final long id) {
        return this.list(
                "SELECT * FROM cqt_score_corrections WHERE final_score_id=:id ORDER BY final_version",
                p("id", id),
                Correction.class);
    }

    @Override
    public List<TargetDecision> targetDecisions(final long phase) {
        return this.list(
                "SELECT phase_id,target_id,decision,status_version version,reason,input_final_version FROM "
                        + "cqt_phase_target_status WHERE phase_id=:p AND state='VALID' ORDER BY target_id",
                p("p", phase),
                TargetDecision.class);
    }

    @Override
    public void decideTarget(
            final long phase, final long target, final String decision, final String reason, final long finalVersion) {
        this.update(
                "INSERT INTO "
                        + "cqt_phase_target_status(phase_id,target_id,decision,reason,input_final_version,status_vers"
                        + "ion,state) VALUES(:p,:t,:d,:r,:v,1,'VALID') ON DUPLICATE KEY UPDATE decision=:d,reason=:r,"
                        + "input_final_version=:v,status_version=status_version+1,state='VALID'",
                p("p", phase, "t", target, "d", decision, "r", reason, "v", finalVersion));
    }

    @Override
    public long qualify(final long phase, final long target, final long version) {
        this.update(
                "INSERT IGNORE INTO cqt_review_qualifications(source_phase_id,source_target_id,final_version) "
                        + "VALUES(:p,:t,:v)",
                p("p", phase, "t", target, "v", version));
        return this.jdbc.queryForObject(
                "SELECT id FROM cqt_review_qualifications WHERE source_phase_id=:p AND source_target_id=:t AND "
                        + "final_version=:v",
                p("p", phase, "t", target, "v", version),
                Long.class);
    }

    @Override
    public List<Qualification> qualifications(final long c) {
        return this.list(
                "SELECT q.* FROM cqt_review_qualifications q JOIN cqt_review_phases ph ON "
                        + "ph.id=q.source_phase_id WHERE (:c=0 OR ph.competition_id=:c) ORDER BY q.id",
                p("c", c),
                Qualification.class);
    }

    @Override
    public Qualification qualification(final long id) {
        return this.one("SELECT * FROM cqt_review_qualifications WHERE id=:id", p("id", id), Qualification.class);
    }

    @Override
    public void attach(final long id, final long target, final String reason, final long version) {
        changed(this.update(
                "UPDATE cqt_review_qualifications SET "
                        + "national_target_id=:t,status='ATTACHED',reason=:r,version=version+1 WHERE id=:id AND "
                        + "version=:v AND status='ELIGIBLE'",
                p("t", target, "r", reason, "id", id, "v", version)));
    }

    @Override
    public boolean separateNational(final long id) {
        return "SEPARATE"
                .equals(this.jdbc.queryForObject(
                        "SELECT national_entry_mode FROM cqt_competitions WHERE id=:id", p("id", id), String.class));
    }

    @Override
    public List<Option> competitions() {
        return this.list(
                "SELECT id,name,NULL parent_id FROM cqt_competitions ORDER BY year DESC,id", p(), Option.class);
    }

    @Override
    public List<Option> categories() {
        return this.list(
                "SELECT legacy_id id,name,NULLIF(parent_legacy_id,0) parent_id FROM cqt_competition_categories "
                        + "WHERE status=1 ORDER BY sort_order,id",
                p(),
                Option.class);
    }

    @Override
    public List<Option> regions() {
        return this.list(
                "SELECT id,name,NULL parent_id FROM region WHERE status='enabled' ORDER BY sort,code",
                p(),
                Option.class);
    }
}
