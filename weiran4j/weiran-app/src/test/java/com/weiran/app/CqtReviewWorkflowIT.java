package com.weiran.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** 真HTTP、隔离MySQL的评审主闭环回归；只生成本用例独有赛事数据。 */
class CqtReviewWorkflowIT extends CqtIntegrationTestSupport {
    private static final String BASE = "/api/cqt/review";

    @Test
    @DisplayName("作品和人员对象完成初审评分汇总评奖，缺评及替代批次撤销保持隔离")
    void completesReviewAndAwardWorkflowWithIsolation() {
        final String admin = this.adminToken();
        final String name = IntegrationTestSupport.unique("workflow");
        this.jdbc.update("INSERT INTO cqt_competitions(name) VALUES(?)", name);
        final long competition = this.number("SELECT id FROM cqt_competitions WHERE name=?", name);
        final long category = 10_000_000 + competition;
        this.jdbc.update("INSERT INTO cqt_competition_categories(legacy_id,name) VALUES(?,?)", category, name);
        final long entry = this.entry(name + "_entry", "zhongxi", competition, category, 1);
        final long peopleEntry = this.entry(name + "_people", "houtai", competition, category, 2);
        final long missingEntry = this.entry(name + "_missing", "zhongxi", competition, category, 3);
        this.participant(peopleEntry, name + "_one", competition * 10 + 1);
        this.participant(peopleEntry, name + "_two", competition * 10 + 2);
        for (final long id : List.of(entry, peopleEntry, missingEntry)) {
            this.okPost(
                    CqtReviewWorkflowIT.BASE + "/entries/" + id + "/decision",
                    admin,
                    Map.of("decision", "APPROVED", "expectedVersion", 0));
        }
        final JsonNode preparation = this.okPost(
                CqtReviewWorkflowIT.BASE + "/targets/prepare-preview",
                admin,
                Map.of("competitionId", competition, "entryIds", List.of(entry, peopleEntry, missingEntry)));
        this.okPost(
                CqtReviewWorkflowIT.BASE + "/targets/prepare",
                admin,
                CqtReviewWorkflowIT.confirm(preparation, "prepare"));
        assertThat(this.number(
                        "SELECT COUNT(*) FROM cqt_evaluation_targets WHERE entry_id=? AND target_type='ENTRY'", entry))
                .isEqualTo(1);
        assertThat(this.number(
                        "SELECT COUNT(*) FROM cqt_evaluation_targets WHERE entry_id=? AND target_type='PARTICIPANT'",
                        peopleEntry))
                .isEqualTo(2);
        this.okPost(CqtReviewWorkflowIT.BASE + "/phases", admin, Map.of("competitionId", competition));
        final long phase = this.number(
                "SELECT id FROM cqt_review_phases WHERE competition_id=? AND code='PROVINCIAL'", competition);
        this.okPut(
                CqtReviewWorkflowIT.BASE + "/phases/" + phase,
                admin,
                Map.of(
                        "minScore",
                        0,
                        "maxScore",
                        100,
                        "minimumReviewers",
                        1,
                        "expectedVersion",
                        this.phaseVersion(phase)));
        this.okPut(
                CqtReviewWorkflowIT.BASE + "/phases/" + phase + "/dimensions",
                admin,
                Map.of(
                        "dimensions",
                        List.of(Map.of(
                                "firstCategoryId", category, "name", "表现", "minScore", 0, "maxScore", 100, "sort", 1)),
                        "expectedVersion",
                        this.phaseVersion(phase)));
        this.changeState(phase, "OPEN", admin);
        this.okPost(
                CqtReviewWorkflowIT.BASE + "/reviewers",
                admin,
                Map.of(
                        "userId",
                        1,
                        "competitionId",
                        competition,
                        "phaseId",
                        phase,
                        "firstCategoryId",
                        category,
                        "enabled",
                        true));
        final List<Long> targets = this.jdbc.queryForList(
                "SELECT t.id FROM cqt_evaluation_targets t JOIN cqt_entries e ON e.id=t.entry_id "
                        + "WHERE e.competition_id=? AND t.status='ACTIVE' ORDER BY t.id",
                Long.class,
                competition);
        final JsonNode assignment = this.okPost(
                CqtReviewWorkflowIT.BASE + "/assignments/preview",
                admin,
                Map.of(
                        "phaseId",
                        phase,
                        "mode",
                        "MANUAL",
                        "targetIds",
                        targets,
                        "userIds",
                        List.of(1),
                        "reviewersPerTarget",
                        1));
        this.okPost(
                CqtReviewWorkflowIT.BASE + "/assignments/confirm",
                admin,
                CqtReviewWorkflowIT.confirm(assignment, "assign"));
        final List<Long> tasks = this.jdbc.queryForList(
                "SELECT id FROM cqt_review_assignments WHERE phase_id=? ORDER BY id", Long.class, phase);
        assertThat(tasks).hasSize(4);
        final long outsiderId = this.create(
                "/api/users",
                admin,
                Map.of("username", name, "nickname", name, "password", "Passw0rd1", "roleIds", List.of(1)));
        assertThat(outsiderId).isNotEqualTo(1);
        final String outsider = this.tokenOf(name, "Passw0rd1");
        final var denied = this.get(CqtReviewWorkflowIT.BASE + "/my-tasks/" + tasks.getFirst(), outsider);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        final List<Long> scored = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            final long task = tasks.get(index);
            final JsonNode detail = this.okGet(CqtReviewWorkflowIT.BASE + "/my-tasks/" + task, admin);
            final long dimension = detail.path("dimensions").get(0).path("id").asLong();
            final Map<String, Object> score = Map.of(
                    "status",
                    "SUBMITTED",
                    "total",
                    88.5,
                    "dimensions",
                    List.of(Map.of("dimensionId", dimension, "score", 75, "comment", " 表现完整 ")),
                    "expectedVersion",
                    0,
                    "requestKey",
                    "score-" + task);
            this.okPut(CqtReviewWorkflowIT.BASE + "/my-tasks/" + task + "/score", admin, score);
            this.okPut(CqtReviewWorkflowIT.BASE + "/my-tasks/" + task + "/score", admin, score);
            final var conflict = this.put(
                    CqtReviewWorkflowIT.BASE + "/my-tasks/" + task + "/score",
                    admin,
                    Map.of(
                            "status",
                            "DRAFT",
                            "total",
                            80,
                            "dimensions",
                            List.of(),
                            "expectedVersion",
                            0,
                            "requestKey",
                            "stale-" + task));
            assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            scored.add(detail.path("target").path("id").asLong());
        }
        assertThat(this.number(
                        "SELECT COUNT(*) FROM cqt_review_score_revisions r JOIN cqt_review_scores s ON s.id=r.score_id "
                                + "JOIN cqt_review_assignments a ON a.id=s.assignment_id WHERE a.phase_id=?",
                        phase))
                .isEqualTo(3);
        this.changeState(phase, "CLOSED", admin);
        final JsonNode calculation =
                this.okPost(CqtReviewWorkflowIT.BASE + "/phases/" + phase + "/calculate-preview", admin, Map.of());
        assertThat(calculation.path("rows")).hasSize(4);
        assertThat(calculation.path("blockedRows")).hasSize(1);
        final Map<String, Object> withoutAck =
                Map.of("previewToken", calculation.path("previewToken").asText(), "requestKey", "calculate");
        assertThat(this.post(CqtReviewWorkflowIT.BASE + "/phases/" + phase + "/calculate", admin, withoutAck)
                        .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        this.okPost(
                CqtReviewWorkflowIT.BASE + "/phases/" + phase + "/calculate",
                admin,
                CqtReviewWorkflowIT.confirm(calculation, "calculate"));
        assertThat(this.number(
                        "SELECT COUNT(*) FROM cqt_final_scores WHERE phase_id=? AND state='VALID' AND final_score=88.50",
                        phase))
                .isEqualTo(3);
        for (final String type : List.of("ENTRY", "PARTICIPANT")) {
            this.okPost(
                    "/api/cqt/award/rules",
                    admin,
                    Map.of(
                            "phaseId",
                            phase,
                            "targetType",
                            type,
                            "firstCategoryId",
                            category,
                            "awardName",
                            "一等奖",
                            "minScore",
                            80,
                            "maxScore",
                            100,
                            "enabled",
                            true));
        }
        final JsonNode awardPreview = this.okPost(
                "/api/cqt/award/preview",
                admin,
                Map.of("competitionId", competition, "phaseId", phase, "targetIds", scored));
        final Map<String, Object> awardCommand = CqtReviewWorkflowIT.confirm(awardPreview, "award-first");
        final long first = this.okPost("/api/cqt/award/batches", admin, awardCommand)
                .path("id")
                .asLong();
        assertThat(this.okPost("/api/cqt/award/batches", admin, awardCommand)
                        .path("id")
                        .asLong())
                .isEqualTo(first);
        final JsonNode replacement = this.okPost(
                "/api/cqt/award/preview",
                admin,
                Map.of(
                        "competitionId",
                        competition,
                        "phaseId",
                        phase,
                        "targetIds",
                        scored,
                        "supersedesBatchId",
                        first));
        final long second = this.okPost(
                        "/api/cqt/award/batches",
                        admin,
                        Map.of(
                                "previewToken",
                                replacement.path("previewToken").asText(),
                                "requestKey",
                                "award-second-" + first,
                                "supersedesBatchId",
                                first))
                .path("id")
                .asLong();
        final long oldVersion = this.number("SELECT version FROM cqt_award_batches WHERE id=?", first);
        this.okPost(
                "/api/cqt/award/batches/" + first + "/revoke",
                admin,
                Map.of("reason", "撤销旧批次", "expectedVersion", oldVersion, "requestKey", "revoke-old-" + first));
        assertThat(this.number("SELECT COUNT(*) FROM cqt_awards WHERE batch_id=? AND status='CONFIRMED'", second))
                .isEqualTo(3);
        assertThat(this.number("SELECT COUNT(*) FROM cqt_awards WHERE batch_id=? AND status='REVOKED'", first))
                .isEqualTo(3);
    }

    private long entry(
            final String name, final String source, final long competition, final long category, final long sourceId) {
        this.jdbc.update(
                "INSERT INTO cqt_entries(entry_no,source_channel_id,source_database,source_table,source_id,competition_id,"
                        + "first_category_legacy_id,title,scoring_scope,legacy_status) VALUES(?,1,?,'review_workflow',?,?,?,?,?,1)",
                name,
                source,
                competition * 10 + sourceId,
                competition,
                category,
                name,
                "zhongxi".equals(source) ? "ENTRY" : "PARTICIPANT");
        return this.number("SELECT id FROM cqt_entries WHERE entry_no=?", name);
    }

    private void participant(final long entry, final String name, final long sourceId) {
        this.jdbc.update(
                "INSERT INTO cqt_people(source_database,source_table,source_id,name_value) VALUES('houtai','review_workflow',?,?)",
                sourceId,
                name);
        final long person = this.number(
                "SELECT id FROM cqt_people WHERE source_database='houtai' "
                        + "AND source_table='review_workflow' AND source_id=?",
                sourceId);
        this.jdbc.update(
                "INSERT INTO cqt_entry_participants(entry_id,person_id,source_database,source_table,source_id) "
                        + "VALUES(?,?,'houtai','review_workflow',?)",
                entry,
                person,
                sourceId);
    }

    private long number(final String sql, final Object parameter) {
        return Objects.requireNonNull(this.jdbc.queryForObject(sql, Long.class, parameter));
    }

    private long phaseVersion(final long phase) {
        return this.number("SELECT version FROM cqt_review_phases WHERE id=?", phase);
    }

    private void changeState(final long phase, final String state, final String token) {
        final JsonNode preview = this.okPost(
                CqtReviewWorkflowIT.BASE + "/phases/" + phase + "/state-preview",
                token,
                Map.of("state", state, "expectedVersion", this.phaseVersion(phase)));
        this.okPost(
                CqtReviewWorkflowIT.BASE + "/phases/" + phase + "/state",
                token,
                CqtReviewWorkflowIT.confirm(preview, "state-" + phase + "-" + state));
    }

    private JsonNode okPost(final String path, final String token, final Object body) {
        return CqtReviewWorkflowIT.ok(this.post(path, token, body));
    }

    private JsonNode okPut(final String path, final String token, final Object body) {
        return CqtReviewWorkflowIT.ok(this.put(path, token, body));
    }

    private JsonNode okGet(final String path, final String token) {
        return CqtReviewWorkflowIT.ok(this.get(path, token));
    }

    private static JsonNode ok(final ResponseEntity<JsonNode> response) {
        IntegrationTestSupport.assertOk(response);
        return IntegrationTestSupport.data(response);
    }

    private static Map<String, Object> confirm(final JsonNode preview, final String key) {
        return Map.of(
                "previewToken",
                preview.path("previewToken").asText(),
                "requestKey",
                IntegrationTestSupport.unique(key),
                "acknowledgeSkipped",
                true);
    }
}
