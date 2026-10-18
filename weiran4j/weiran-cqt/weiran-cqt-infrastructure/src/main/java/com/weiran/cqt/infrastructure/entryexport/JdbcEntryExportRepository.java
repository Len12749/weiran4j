package com.weiran.cqt.infrastructure.entryexport;

import com.weiran.cqt.domain.entryexport.EntryExportRepository;
import com.weiran.cqt.domain.entryexport.EntryExportRow;
import java.util.List;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcEntryExportRepository implements EntryExportRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcEntryExportRepository(final DataSource source) {
        this.jdbc = new NamedParameterJdbcTemplate(source);
    }

    @Override
    public List<EntryExportRow> rows(
            final long competitionId,
            final long phaseId,
            final @Nullable String keyword,
            final @Nullable String source,
            final @Nullable Integer status) {
        final MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("c", competitionId)
                .addValue("ph", phaseId)
                .addValue("keyword", keyword == null ? "" : keyword)
                .addValue("source", source)
                .addValue("status", status);
        final String sql = """
 SELECT e.competition_id,c.name competition,ph.id phase_id,ph.code phase_code,
 e.id entry_id,COALESCE(e.entry_no,'') entry_no,COALESCE(e.title,'') title,e.source_database source,
 t.id target_id,CASE WHEN e.source_database='zhongxi' THEN 'ENTRY' ELSE 'PARTICIPANT' END target_type,
 ep.id participant_id,COALESCE(pe.name_value,'') participant_name,
 COALESCE(r.name,'') region,COALESCE(fc.name,'') category,
 COALESCE(sc.name,e.major_name,'') major,COALESCE(ep.group_snapshot,e.group_name,'') group_name,
 COALESCE(e.legacy_status,0) review_status,
 CASE WHEN e.scoring_scope<>CASE WHEN e.source_database='zhongxi' THEN 'ENTRY' ELSE 'PARTICIPANT' END
 THEN '来源与评分对象类型冲突'
 WHEN e.source_database<>'zhongxi' AND ep.id IS NULL THEN '缺少有效参赛人'
 WHEN t.id IS NULL THEN '未准备' ELSE t.status END target_status,
 COALESCE(f.state,'MISSING') score_status,f.final_score,f.version score_version
 FROM cqt_entries e JOIN cqt_competitions c ON c.id=e.competition_id
 JOIN cqt_review_phases ph ON ph.competition_id=e.competition_id AND ph.id=:ph
 LEFT JOIN cqt_entry_participants ep ON ep.entry_id=e.id AND ep.status='ACTIVE'
 AND e.source_database<>'zhongxi'
 LEFT JOIN cqt_people pe ON pe.id=ep.person_id
 LEFT JOIN cqt_evaluation_targets t ON t.entry_id=e.id AND t.status='ACTIVE'
 AND e.scoring_scope=CASE WHEN e.source_database='zhongxi' THEN 'ENTRY' ELSE 'PARTICIPANT' END
 AND t.target_type=CASE WHEN e.source_database='zhongxi' THEN 'ENTRY' ELSE 'PARTICIPANT' END
 AND ((e.source_database='zhongxi' AND t.entry_participant_id IS NULL)
 OR (e.source_database<>'zhongxi' AND t.entry_participant_id=ep.id))
 LEFT JOIN cqt_final_scores f ON f.phase_id=ph.id AND f.target_id=t.id
 LEFT JOIN region r ON r.id=e.region_id
 LEFT JOIN cqt_competition_categories fc ON fc.legacy_id=e.first_category_legacy_id
 LEFT JOIN cqt_competition_categories sc ON sc.legacy_id=e.second_category_legacy_id
 WHERE e.competition_id=:c AND e.status<>'DELETED'
 AND ((ph.code='PROVINCIAL' AND e.stage_scope<>'NATIONAL')
 OR (ph.code='NATIONAL' AND e.stage_scope<>'PROVINCIAL'))
 AND (:keyword='' OR LOCATE(LOWER(:keyword) COLLATE utf8mb4_bin,
 LOWER(e.title) COLLATE utf8mb4_bin)>0)
 AND (:source IS NULL OR e.source_database=:source)
 AND (:status IS NULL OR COALESCE(e.legacy_status,0)=:status)
 ORDER BY e.id,ep.id LIMIT 10001
 """;
        return this.jdbc.query(
                sql,
                p,
                (r, n) -> new EntryExportRow(
                        r.getLong("competition_id"),
                        r.getString("competition"),
                        r.getLong("phase_id"),
                        r.getString("phase_code"),
                        "PROVINCIAL".equals(r.getString("phase_code")) ? "省审" : "国审",
                        r.getLong("entry_id"),
                        r.getString("entry_no"),
                        r.getString("title"),
                        r.getString("source"),
                        r.getObject("target_id", Long.class),
                        r.getString("target_type"),
                        r.getObject("participant_id", Long.class),
                        r.getString("participant_name"),
                        r.getString("region"),
                        r.getString("category"),
                        r.getString("major"),
                        r.getString("group_name"),
                        r.getInt("review_status"),
                        r.getString("target_status"),
                        r.getString("score_status"),
                        r.getBigDecimal("final_score"),
                        r.getObject("score_version", Long.class)));
    }
}
