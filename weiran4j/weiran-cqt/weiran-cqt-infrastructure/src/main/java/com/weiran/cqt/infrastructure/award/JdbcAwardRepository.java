package com.weiran.cqt.infrastructure.award;

import com.weiran.cqt.domain.award.AwardBatch;
import com.weiran.cqt.domain.award.AwardRepository;
import com.weiran.cqt.domain.award.AwardRule;
import com.weiran.cqt.domain.review.AwardCandidate;
import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/** 参数化SQL操作批次专属关系；调用方在同一事务持有共享阶段锁。 */
public class JdbcAwardRepository implements AwardRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAwardRepository(final DataSource source) {
        this.jdbc = new NamedParameterJdbcTemplate(source);
    }

    private static MapSqlParameterSource params() {
        return new MapSqlParameterSource();
    }

    private long insert(final String sql, final MapSqlParameterSource values) {
        final GeneratedKeyHolder holder = new GeneratedKeyHolder();
        this.jdbc.update(sql, values, holder, new String[] {"id"});
        return Objects.requireNonNull(holder.getKey()).longValue();
    }

    @Override
    public PageResult<AwardRule> rules(
            final @Nullable Long competitionId, final @Nullable Long phaseId, final PageQuery page) {
        final MapSqlParameterSource values = JdbcAwardRepository.params()
                .addValue("competition", competitionId)
                .addValue("phase", phaseId)
                .addValue("limit", page.pageSize())
                .addValue("offset", (long) (page.page() - 1) * page.pageSize());
        final String where =
                " FROM cqt_award_rules r JOIN cqt_review_phases p ON p.id=r.phase_id WHERE r.is_current=1 AND "
                        + "(:competition IS NULL OR p.competition_id=:competition) AND (:phase IS NULL OR "
                        + "r.phase_id=:phase)";
        final long total =
                Objects.requireNonNull(this.jdbc.queryForObject("SELECT COUNT(*)" + where, values, Long.class));
        return new PageResult<>(
                this.jdbc.query(
                        "SELECT r.*" + where + " ORDER BY r.phase_id,r.rule_id LIMIT :limit OFFSET :offset",
                        values,
                        (rs, index) -> JdbcAwardRepository.rule(rs)),
                total,
                page.page(),
                page.pageSize());
    }

    @Override
    public Optional<AwardRule> currentRule(final long ruleId) {
        return this.jdbc
                .query(
                        "SELECT * FROM cqt_award_rules WHERE rule_id=:id AND is_current=1",
                        JdbcAwardRepository.params().addValue("id", ruleId),
                        (rs, index) -> JdbcAwardRepository.rule(rs))
                .stream()
                .findFirst();
    }

    @Override
    public List<AwardRule> currentRules(final long phaseId) {
        return this.jdbc.query(
                "SELECT * FROM cqt_award_rules WHERE phase_id=:phase AND is_current=1 ORDER BY rule_id",
                JdbcAwardRepository.params().addValue("phase", phaseId),
                (rs, index) -> JdbcAwardRepository.rule(rs));
    }

    @Override
    public long insertRule(final AwardRule value) {
        final MapSqlParameterSource values = JdbcAwardRepository.params()
                .addValue("rule", value.ruleId())
                .addValue("phase", value.phaseId())
                .addValue("type", value.targetType())
                .addValue("first", value.firstCategoryId())
                .addValue("second", value.secondCategoryId())
                .addValue("group", value.groupName())
                .addValue("name", value.awardName())
                .addValue("min", value.minScore())
                .addValue("max", value.maxScore())
                .addValue("enabled", value.enabled())
                .addValue("revision", value.revision())
                .addValue("operator", value.createdBy());
        final long id = this.insert(
                "INSERT INTO cqt_award_rules(rule_id,phase_id,target_type,first_category_id,second_category_i"
                        + "d,group_name,award_name,min_score,max_score,enabled,revision,is_current,created_by,created_a"
                        + "t,updated_at) "
                        + "VALUES(:rule,:phase,:type,:first,:second,:group,:name,:min,:max,:enabled,:revision,1,:operat"
                        + "or,NOW(),NOW())",
                values);
        if (value.ruleId() == 0) {
            this.jdbc.update(
                    "UPDATE cqt_award_rules SET rule_id=:id WHERE id=:id",
                    JdbcAwardRepository.params().addValue("id", id));
        }
        return value.ruleId() == 0 ? id : value.ruleId();
    }

    @Override
    public void retireRule(final long id) {
        this.jdbc.update(
                "UPDATE cqt_award_rules SET is_current=0 WHERE rule_id=:id AND is_current=1",
                JdbcAwardRepository.params().addValue("id", id));
    }

    @Override
    public BigDecimal[] phaseBounds(final long currentRuleId) {
        return Objects.requireNonNull(this.jdbc.queryForObject(
                "SELECT min_score,max_score FROM cqt_review_phase_rules WHERE id=:id",
                JdbcAwardRepository.params().addValue("id", currentRuleId),
                (rs, index) -> new BigDecimal[] {
                    Objects.requireNonNull(rs.getBigDecimal("min_score")),
                    Objects.requireNonNull(rs.getBigDecimal("max_score"))
                }));
    }

    @Override
    public boolean validCategory(
            final long competitionId, final long first, final @Nullable Long second, final @Nullable String group) {
        final MapSqlParameterSource values = JdbcAwardRepository.params()
                .addValue("competition", competitionId)
                .addValue("first", first)
                .addValue("second", second)
                .addValue("group", group);
        final Long count = this.jdbc.queryForObject(
                "SELECT COUNT(*) FROM cqt_competition_categories f WHERE f.legacy_id=:first AND "
                        + "f.parent_legacy_id=0 AND f.status=1 AND (:second IS NULL OR EXISTS(SELECT 1 FROM "
                        + "cqt_competition_categories s WHERE s.legacy_id=:second AND s.parent_legacy_id=:first AND "
                        + "s.status=1)) AND (:group IS NULL OR EXISTS(SELECT 1 FROM cqt_competition_groups g WHERE "
                        + "g.competition_id=:competition AND g.name=:group AND g.status=1 AND g.first_category_legacy_id "
                        + "IN(0,:first) AND (:second IS NULL OR g.second_category_legacy_id=0 OR "
                        + "g.second_category_legacy_id=:second)))",
                values,
                Long.class);
        return count != null && count > 0;
    }

    @Override
    public PageResult<AwardBatch> batches(
            final @Nullable Long competitionId,
            final @Nullable Long phaseId,
            final @Nullable String status,
            final PageQuery page) {
        final MapSqlParameterSource values = JdbcAwardRepository.params()
                .addValue("competition", competitionId)
                .addValue("phase", phaseId)
                .addValue("status", status)
                .addValue("limit", page.pageSize())
                .addValue("offset", (long) (page.page() - 1) * page.pageSize());
        final String where =
                " FROM cqt_award_batches b JOIN cqt_review_phases p ON p.id=b.phase_id WHERE (:competition IS "
                        + "NULL OR p.competition_id=:competition) AND (:phase IS NULL OR b.phase_id=:phase) AND (:status "
                        + "IS NULL OR b.status=:status)";
        final long total =
                Objects.requireNonNull(this.jdbc.queryForObject("SELECT COUNT(*)" + where, values, Long.class));
        return new PageResult<>(
                this.jdbc.query(
                        "SELECT b.*" + where + " ORDER BY b.id DESC LIMIT :limit OFFSET :offset",
                        values,
                        (rs, index) -> JdbcAwardRepository.batch(rs)),
                total,
                page.page(),
                page.pageSize());
    }

    @Override
    public Optional<AwardBatch> batch(final long id) {
        return this.jdbc
                .query(
                        "SELECT * FROM cqt_award_batches WHERE id=:id",
                        JdbcAwardRepository.params().addValue("id", id),
                        (rs, index) -> JdbcAwardRepository.batch(rs))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<AwardBatch> batchByPreview(final String token) {
        return this.jdbc
                .query(
                        "SELECT * FROM cqt_award_batches WHERE preview_id=:token",
                        JdbcAwardRepository.params().addValue("token", token),
                        (rs, index) -> JdbcAwardRepository.batch(rs))
                .stream()
                .findFirst();
    }

    @Override
    public List<AwardBatch> effectiveBatches(final long phaseId) {
        return this.jdbc.query(
                "SELECT * FROM cqt_award_batches WHERE phase_id=:phase AND status='CONFIRMED' ORDER BY id",
                JdbcAwardRepository.params().addValue("phase", phaseId),
                (rs, index) -> JdbcAwardRepository.batch(rs));
    }

    @Override
    public long insertBatch(
            final long phaseId,
            final long inputVersion,
            final String token,
            final @Nullable Long replaces,
            final long operatorId,
            final String candidates,
            final String snapshot) {
        return this.insert(
                "INSERT INTO cqt_award_batches(phase_id,status,input_version,preview_id,supersedes_batch_id,v"
                        + "ersion,created_by,candidate_json,snapshot_json,created_at,updated_at) "
                        + "VALUES(:phase,'CONFIRMED',:input,:token,:replaces,1,:operator,:candidates,:snapshot,NOW(),NO"
                        + "W())",
                JdbcAwardRepository.params()
                        .addValue("phase", phaseId)
                        .addValue("input", inputVersion)
                        .addValue("token", token)
                        .addValue("replaces", replaces)
                        .addValue("operator", operatorId)
                        .addValue("candidates", candidates)
                        .addValue("snapshot", snapshot));
    }

    @Override
    public void insertAward(
            final long batchId, final AwardCandidate candidate, final AwardRule rule, final String snapshot) {
        final MapSqlParameterSource values = JdbcAwardRepository.params()
                .addValue("batch", batchId)
                .addValue("target", candidate.targetId())
                .addValue("rule", rule.id())
                .addValue("revision", rule.revision())
                .addValue("scoreVersion", candidate.finalVersion())
                .addValue("name", rule.awardName())
                .addValue("score", candidate.score())
                .addValue("type", candidate.targetType())
                .addValue("snapshot", snapshot);
        final long id = this.insert(
                "INSERT INTO cqt_awards(batch_id,target_id,rule_id,rule_revision,score_version,award_name,fin"
                        + "al_score,target_type,status,snapshot_json,created_at,updated_at) "
                        + "VALUES(:batch,:target,:rule,:revision,:scoreVersion,:name,:score,:type,'CONFIRMED',:snapshot"
                        + ",NOW(),NOW())",
                values);
        this.jdbc.update(
                "INSERT INTO " + "cqt_award_grants(batch_id,award_id,target_id,recipient_type,created_at,updated_at) "
                        + "VALUES(:batch,:award,:target,:type,NOW(),NOW())",
                values.addValue("award", id));
    }

    @Override
    public void transitionBatch(final long id, final long version, final String status, final String reason) {
        final MapSqlParameterSource values = JdbcAwardRepository.params()
                .addValue("id", id)
                .addValue("version", version)
                .addValue("status", status)
                .addValue("reason", reason);
        if (this.jdbc.update(
                        "UPDATE cqt_award_batches SET status=:status,reason=:reason,version=version+1,updated_at=NOW() "
                                + "WHERE id=:id AND version=:version",
                        values)
                != 1) {
            throw BizException.conflict("批次已变化，请刷新后重试");
        }
        this.jdbc.update("UPDATE cqt_awards SET status=:status,updated_at=NOW() WHERE batch_id=:id", values);
    }

    @Override
    public void invalidateBatches(final long phaseId, final String reason) {
        final MapSqlParameterSource values =
                JdbcAwardRepository.params().addValue("phase", phaseId).addValue("reason", reason);
        this.jdbc.update(
                "UPDATE cqt_awards a JOIN cqt_award_batches b ON b.id=a.batch_id SET "
                        + "a.status='STALE',a.updated_at=NOW() WHERE b.phase_id=:phase AND b.status='CONFIRMED'",
                values);
        this.jdbc.update(
                "UPDATE cqt_award_batches SET status='STALE',reason=:reason,version=version+1,updated_at=NOW() "
                        + "WHERE phase_id=:phase AND status='CONFIRMED'",
                values);
    }

    private static AwardRule rule(final ResultSet rs) throws SQLException {
        return new AwardRule(
                rs.getLong("id"),
                rs.getLong("rule_id"),
                rs.getLong("phase_id"),
                Objects.requireNonNull(rs.getString("target_type")),
                rs.getLong("first_category_id"),
                rs.getObject("second_category_id", Long.class),
                rs.getString("group_name"),
                Objects.requireNonNull(rs.getString("award_name")),
                Objects.requireNonNull(rs.getBigDecimal("min_score")),
                Objects.requireNonNull(rs.getBigDecimal("max_score")),
                rs.getBoolean("enabled"),
                rs.getLong("revision"),
                rs.getLong("created_by"),
                Objects.requireNonNull(rs.getObject("created_at", LocalDateTime.class)));
    }

    private static AwardBatch batch(final ResultSet rs) throws SQLException {
        return new AwardBatch(
                rs.getLong("id"),
                rs.getLong("phase_id"),
                Objects.requireNonNull(rs.getString("status")),
                rs.getLong("input_version"),
                Objects.requireNonNull(rs.getString("preview_id")),
                rs.getObject("supersedes_batch_id", Long.class),
                rs.getString("reason"),
                rs.getLong("version"),
                rs.getLong("created_by"),
                Objects.requireNonNull(rs.getString("candidate_json")),
                Objects.requireNonNull(rs.getString("snapshot_json")),
                Objects.requireNonNull(rs.getObject("created_at", LocalDateTime.class)),
                Objects.requireNonNull(rs.getObject("updated_at", LocalDateTime.class)));
    }
}
