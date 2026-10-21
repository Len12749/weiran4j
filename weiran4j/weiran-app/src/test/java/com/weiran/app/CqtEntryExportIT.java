package com.weiran.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.weiran.cqt.domain.entryexport.EntryExportRepository;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class CqtEntryExportIT extends CqtIntegrationTestSupport {
    @Autowired
    EntryExportRepository exports;

    @Test
    @DisplayName("作品导出保留未准备与来源冲突问题行并直出Excel")
    void exportsProblemRowsAndRawWorkbook() {
        final String name = IntegrationTestSupport.unique("export");
        this.jdbc.update("INSERT INTO cqt_competitions(name) VALUES(?)", name);
        final long competition = Objects.requireNonNull(
                this.jdbc.queryForObject("SELECT id FROM cqt_competitions WHERE name=?", Long.class, name));
        this.jdbc.update("INSERT INTO cqt_review_phases(competition_id,code) VALUES(?,'PROVINCIAL')", competition);
        final long phase = Objects.requireNonNull(this.jdbc.queryForObject(
                "SELECT id FROM cqt_review_phases WHERE competition_id=?", Long.class, competition));
        final String insert = "INSERT INTO cqt_entries(entry_no,source_channel_id,source_database,source_table,"
                + "source_id,competition_id,title,scoring_scope,legacy_status) VALUES(?,1,?,'export_test',?,?,?, ?,3)";
        this.jdbc.update(insert, name + "_1", "zhongxi", competition, competition, "正常作品 Résumé", "ENTRY");
        this.jdbc.update(insert, name + "_2", "qudao", competition, competition, "无有效参赛人", "PARTICIPANT");
        this.jdbc.update(insert, name + "_3", "zhongxi", competition + 1, competition, "来源冲突", "PARTICIPANT");
        final var rows = this.exports.rows(competition, phase, null, null, null);
        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(row -> row.targetStatus()).containsExactly("未准备", "缺少有效参赛人", "来源与评分对象类型冲突");
        assertThat(rows).allSatisfy(row -> assertThat(row.targetId()).isNull());
        assertThat(this.exports.rows(competition, phase, "RÉSUMÉ", null, null)).hasSize(1);
        assertThat(this.exports.rows(competition, phase, "resume", null, null)).isEmpty();
        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.tokenOf("admin", "admin123"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        final var response = this.rest.exchange(
                "/api/cqt/review/entries/export",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("competitionId", competition, "phaseId", phase), headers),
                byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("cqt-entries-" + competition + "-" + phase + ".xlsx");
        assertThat(Objects.requireNonNull(response.getBody())).startsWith((byte) 'P', (byte) 'K');
        final var invalid = this.post(
                "/api/cqt/review/entries/export",
                this.tokenOf("admin", "admin123"),
                Map.of("competitionId", competition + 1, "phaseId", phase));
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(invalid.getBody()).path("code").asInt())
                .isEqualTo(100001);
    }
}
