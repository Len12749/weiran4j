package com.weiran.cqt.infrastructure.competition.admin;

import com.weiran.cqt.domain.competition.admin.AdminCompetition;
import com.weiran.cqt.domain.competition.admin.CompetitionAdminRepository;
import com.weiran.cqt.domain.competition.admin.CompetitionConfiguration;
import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

public class JdbcCompetitionAdminRepository implements CompetitionAdminRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final RowMapper<AdminCompetition> MAPPER = (rs, index) -> new AdminCompetition(
            rs.getLong("id"),
            rs.getObject("legacy_id", Long.class),
            rs.getString("name"),
            rs.getString("edition"),
            rs.getObject("year", Integer.class),
            rs.getString("description"),
            rs.getString("national_entry_mode"),
            rs.getObject("registration_start", LocalDateTime.class),
            rs.getObject("registration_end", LocalDateTime.class),
            rs.getObject("province_review_start", LocalDateTime.class),
            rs.getObject("province_review_end", LocalDateTime.class),
            rs.getObject("national_review_start", LocalDateTime.class),
            rs.getObject("national_review_end", LocalDateTime.class),
            rs.getObject("result_publish_time", LocalDateTime.class),
            rs.getString("province_certificate_template"),
            rs.getString("national_certificate_template"),
            rs.getString("teacher_org_certificate_template"),
            rs.getInt("status"),
            rs.getLong("version"),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("updated_at", LocalDateTime.class));

    public JdbcCompetitionAdminRepository(final DataSource source) {
        this.jdbc = new NamedParameterJdbcTemplate(source);
    }

    private static MapSqlParameterSource parameters(final CompetitionConfiguration v, final LocalDateTime now) {
        return new MapSqlParameterSource()
                .addValue("name", v.name())
                .addValue("edition", v.edition())
                .addValue("year", v.year())
                .addValue("description", v.description())
                .addValue("nationalEntryMode", v.nationalEntryMode())
                .addValue("registrationStart", v.registrationStart())
                .addValue("registrationEnd", v.registrationEnd())
                .addValue("provinceReviewStart", v.provinceReviewStart())
                .addValue("provinceReviewEnd", v.provinceReviewEnd())
                .addValue("nationalReviewStart", v.nationalReviewStart())
                .addValue("nationalReviewEnd", v.nationalReviewEnd())
                .addValue("resultPublishTime", v.resultPublishTime())
                .addValue("provinceCertificateTemplate", v.provinceCertificateTemplate())
                .addValue("nationalCertificateTemplate", v.nationalCertificateTemplate())
                .addValue("teacherOrgCertificateTemplate", v.teacherOrgCertificateTemplate())
                .addValue("now", now);
    }

    @Override
    public PageResult<AdminCompetition> page(
            final PageQuery q,
            final @Nullable String keyword,
            final @Nullable Integer year,
            final @Nullable Integer status) {
        final MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("keyword", keyword == null ? "" : keyword)
                .addValue("year", year)
                .addValue("status", status)
                .addValue("offset", q.offset())
                .addValue("limit", q.pageSize());
        final String where = " WHERE (:keyword='' OR LOCATE(:keyword,name)>0) AND (:year IS NULL OR year=:year) "
                + "AND (:status IS NULL OR status=:status)";
        final long count = Objects.requireNonNull(
                this.jdbc.queryForObject("SELECT COUNT(*) FROM cqt_competitions" + where, p, Long.class));
        return PageResult.of(
                this.jdbc.query(
                        "SELECT * FROM cqt_competitions" + where
                                + " ORDER BY year DESC,id DESC LIMIT :limit OFFSET :offset",
                        p,
                        MAPPER),
                count,
                q);
    }

    @Override
    public Optional<AdminCompetition> find(final long id) {
        return this.jdbc
                .query("SELECT * FROM cqt_competitions WHERE id=:id", new MapSqlParameterSource("id", id), MAPPER)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<AdminCompetition> lock(final long id) {
        return this.jdbc
                .query(
                        "SELECT * FROM cqt_competitions WHERE id=:id FOR UPDATE",
                        new MapSqlParameterSource("id", id),
                        MAPPER)
                .stream()
                .findFirst();
    }

    @Override
    public long create(final CompetitionConfiguration v, final LocalDateTime now) {
        final GeneratedKeyHolder keys = new GeneratedKeyHolder();
        try {
            this.jdbc.update(
                    "INSERT INTO cqt_competitions(name,edition,year,description,national_entry_mode,"
                            + "registration_start,registration_end,province_review_start,province_review_end,"
                            + "national_review_start,national_review_end,result_publish_time,"
                            + "province_certificate_template,national_certificate_template,"
                            + "teacher_org_certificate_template,status,version,created_at,updated_at) VALUES(:name,"
                            + ":edition,:year,:description,:nationalEntryMode,:registrationStart,:registrationEnd,"
                            + ":provinceReviewStart,:provinceReviewEnd,:nationalReviewStart,:nationalReviewEnd,"
                            + ":resultPublishTime,:provinceCertificateTemplate,:nationalCertificateTemplate,"
                            + ":teacherOrgCertificateTemplate,0,0,:now,:now)",
                    parameters(v, now),
                    keys,
                    new String[] {"id"});
        } catch (final DuplicateKeyException e) {
            throw BizException.conflict("同年份同届次赛事已存在");
        }
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    @Override
    public boolean update(
            final long id, final CompetitionConfiguration v, final long expectedVersion, final LocalDateTime now) {
        try {
            return this.jdbc.update(
                            "UPDATE cqt_competitions SET name=:name,edition=:edition,year=:year,"
                                    + "description=:description,registration_start=:registrationStart,"
                                    + "registration_end=:registrationEnd,province_review_start=:provinceReviewStart,"
                                    + "province_review_end=:provinceReviewEnd,national_review_start=:nationalReviewStart,"
                                    + "national_review_end=:nationalReviewEnd,result_publish_time=:resultPublishTime,"
                                    + "province_certificate_template=:provinceCertificateTemplate,"
                                    + "national_certificate_template=:nationalCertificateTemplate,"
                                    + "teacher_org_certificate_template=:teacherOrgCertificateTemplate,version=version+1,"
                                    + "updated_at=:now WHERE id=:id AND version=:version",
                            parameters(v, now).addValue("id", id).addValue("version", expectedVersion))
                    == 1;
        } catch (final DuplicateKeyException e) {
            throw BizException.conflict("同年份同届次赛事已存在");
        }
    }

    @Override
    public boolean status(final long id, final int status, final long expectedVersion, final LocalDateTime now) {
        return this.jdbc.update(
                        "UPDATE cqt_competitions SET status=:status,version=version+1,updated_at=:now WHERE "
                                + "id=:id AND version=:version",
                        new MapSqlParameterSource("id", id)
                                .addValue("status", status)
                                .addValue("version", expectedVersion)
                                .addValue("now", now))
                == 1;
    }

    @Override
    public void createDefaultGroups(final long competitionId) {
        for (final String name : List.of("小学低年级组", "小学中年级组", "小学高年级组", "初中组", "高中组（含中职）")) {
            this.jdbc.update(
                    "INSERT INTO cqt_competition_groups(competition_id,first_category_legacy_id,"
                            + "second_category_legacy_id,name,status) VALUES(:id,0,0,:name,1)",
                    new MapSqlParameterSource("id", competitionId).addValue("name", name));
        }
    }
}
