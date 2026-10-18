package com.weiran.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** 独立赛事fixture、真实MySQL及HTTP的阶段和失效方向边界回归。 */
class CqtReviewBoundaryIT extends CqtIntegrationTestSupport {
    private static final String BASE = "/api/cqt/review";

    private record Fixture(long competition, long province, long national, long target, long other, long task) {}

    @Test
    @DisplayName("关闭阶段拒绝草稿和提交且不写评分或回执")
    void closedPhaseRejectsDraftAndSubmission() {
        final String admin = this.adminToken();
        final Fixture f = this.fixture(1, "CLOSED");
        for (final String status : List.of("DRAFT", "SUBMITTED")) {
            final var response = this.put(
                    CqtReviewBoundaryIT.BASE + "/my-tasks/" + f.task() + "/score",
                    admin,
                    Map.of(
                            "status",
                            status,
                            "total",
                            88,
                            "dimensions",
                            List.of(),
                            "expectedVersion",
                            0,
                            "requestKey",
                            "closed-" + f.task() + "-" + status));
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
        assertThat(this.number("SELECT COUNT(*) FROM cqt_review_scores WHERE assignment_id=?", f.task()))
                .isZero();
        assertThat(this.number(
                        "SELECT COUNT(*) FROM cqt_review_command_receipts WHERE operation=?", "SCORE:" + f.task()))
                .isZero();
    }

    @Test
    @DisplayName("国审重开不逆向失效省审同对象与无关对象成绩")
    void nationalReopenDoesNotInvalidateProvince() {
        final String admin = this.adminToken();
        final Fixture f = this.fixture(1, "FROZEN");
        this.finalScore(f.province(), f.target());
        this.finalScore(f.province(), f.other());
        this.finalScore(f.national(), f.target());
        this.jdbc.update("UPDATE cqt_review_phases SET state='FROZEN' WHERE id=?", f.national());
        final long version = this.number("SELECT version FROM cqt_review_phases WHERE id=?", f.national());
        final var preview = this.post(
                CqtReviewBoundaryIT.BASE + "/phases/" + f.national() + "/state-preview",
                admin,
                Map.of("state", "REOPEN", "reason", "补充国赛评分", "expectedVersion", version));
        IntegrationTestSupport.assertOk(preview);
        final var result = this.post(
                CqtReviewBoundaryIT.BASE + "/phases/" + f.national() + "/state",
                admin,
                Map.of(
                        "previewToken",
                        IntegrationTestSupport.data(preview)
                                .path("previewToken")
                                .asText(),
                        "requestKey",
                        "reopen-" + f.national()));
        IntegrationTestSupport.assertOk(result);
        assertThat(this.state(f.national(), f.target())).isEqualTo("STALE");
        assertThat(this.state(f.province(), f.target())).isEqualTo("VALID");
        assertThat(this.state(f.province(), f.other())).isEqualTo("VALID");
        assertThat(this.jdbc.queryForObject(
                        "SELECT state FROM cqt_review_phases WHERE id=?", String.class, f.national()))
                .isEqualTo("OPEN");
        assertThat(this.number(
                        "SELECT COUNT(*) FROM cqt_score_snapshots WHERE phase_id=? AND kind='REOPEN'", f.national()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("停用评委后业务读取只失效其相关对象与国审下游")
    void disabledReviewerInvalidatesOnlyRelatedTargets() {
        final String admin = this.adminToken();
        final String username = IntegrationTestSupport.unique("boundary");
        final long reviewer = this.create(
                "/api/users",
                admin,
                Map.of("username", username, "nickname", username, "password", "Passw0rd1", "roleIds", List.of(1)));
        final Fixture f = this.fixture(reviewer, "FROZEN");
        this.finalScore(f.province(), f.target());
        this.finalScore(f.province(), f.other());
        this.finalScore(f.national(), f.target());
        this.finalScore(f.national(), f.other());
        this.jdbc.update("UPDATE sys_user SET status='disabled',token_version=token_version+1 WHERE id=?", reviewer);
        final var response = this.get(CqtReviewBoundaryIT.BASE + "/scores?competitionId=" + f.competition(), admin);
        IntegrationTestSupport.assertOk(response);
        assertThat(this.state(f.province(), f.target())).isEqualTo("STALE");
        assertThat(this.state(f.national(), f.target())).isEqualTo("STALE");
        assertThat(this.state(f.province(), f.other())).isEqualTo("VALID");
        assertThat(this.state(f.national(), f.other())).isEqualTo("VALID");
        final JsonNode rows = IntegrationTestSupport.data(response).path("list");
        assertThat(rows).anySatisfy(row -> {
            assertThat(row.path("phaseId").asLong()).isEqualTo(f.province());
            assertThat(row.path("targetId").asLong()).isEqualTo(f.target());
            assertThat(row.path("state").asText()).isEqualTo("STALE");
        });
    }

    private Fixture fixture(final long reviewer, final String state) {
        final String name = IntegrationTestSupport.unique("boundary");
        this.jdbc.update("INSERT INTO cqt_competitions(name) VALUES(?)", name);
        final long competition = this.number("SELECT id FROM cqt_competitions WHERE name=?", name);
        this.jdbc.update(
                "INSERT INTO cqt_review_phases(competition_id,code,state) VALUES(?,'PROVINCIAL',?)",
                competition,
                state);
        this.jdbc.update(
                "INSERT INTO cqt_review_phases(competition_id,code,state) VALUES(?,'NATIONAL','FROZEN')", competition);
        final long province = this.number(
                "SELECT id FROM cqt_review_phases WHERE competition_id=? AND code='PROVINCIAL'", competition);
        final long national =
                this.number("SELECT id FROM cqt_review_phases WHERE competition_id=? AND code='NATIONAL'", competition);
        for (final long phase : List.of(province, national)) {
            this.jdbc.update(
                    "INSERT INTO cqt_review_phase_rules(phase_id,revision,min_score,max_score,minimum_reviewers,created_by)"
                            + " VALUES(?,1,0,100,1,1)",
                    phase);
            this.jdbc.update(
                    "UPDATE cqt_review_phases SET current_rule_id=(SELECT id FROM cqt_review_phase_rules WHERE phase_id=?)"
                            + " WHERE id=?",
                    phase,
                    phase);
        }
        final long target = this.target(competition, name + "_a");
        final long other = this.target(competition, name + "_b");
        this.jdbc.update(
                "INSERT INTO cqt_reviewer_scopes(user_id,competition_id,phase_id,first_category_id,enabled) VALUES(?,?,?,5,1)",
                reviewer,
                competition,
                province);
        this.jdbc.update(
                "INSERT INTO cqt_review_assignments(phase_id,target_id,reviewer_id,user_state,user_updated_at)"
                        + " SELECT ?,?,id,status,updated_at FROM sys_user WHERE id=?",
                province,
                target,
                reviewer);
        final long task = this.number(
                "SELECT id FROM cqt_review_assignments WHERE phase_id=? AND target_id=? AND reviewer_id=?",
                province,
                target,
                reviewer);
        return new Fixture(competition, province, national, target, other, task);
    }

    private long target(final long competition, final String name) {
        this.jdbc.update(
                "INSERT INTO cqt_entries(entry_no,source_channel_id,source_database,source_table,source_id,competition_id,"
                        + "title,scoring_scope,legacy_status,first_category_legacy_id) VALUES(?,1,'zhongxi',?,1,?,?,'ENTRY',3,5)",
                name,
                name,
                competition,
                name);
        final long entry = this.number("SELECT id FROM cqt_entries WHERE entry_no=?", name);
        this.jdbc.update("INSERT INTO cqt_evaluation_targets(entry_id,target_type) VALUES(?,'ENTRY')", entry);
        return this.number("SELECT id FROM cqt_evaluation_targets WHERE entry_id=?", entry);
    }

    private void finalScore(final long phase, final long target) {
        this.jdbc.update(
                "INSERT INTO cqt_final_scores(phase_id,target_id,final_score,average_score,valid_reviewer_count,state,version,"
                        + "snapshot_id) VALUES(?,?,88,88,1,'VALID',1,0)",
                phase,
                target);
    }

    private String state(final long phase, final long target) {
        return Objects.requireNonNull(this.jdbc.queryForObject(
                "SELECT state FROM cqt_final_scores WHERE phase_id=? AND target_id=?", String.class, phase, target));
    }

    private long number(final String sql, final Object... values) {
        return Objects.requireNonNull(this.jdbc.queryForObject(sql, Long.class, values));
    }
}
