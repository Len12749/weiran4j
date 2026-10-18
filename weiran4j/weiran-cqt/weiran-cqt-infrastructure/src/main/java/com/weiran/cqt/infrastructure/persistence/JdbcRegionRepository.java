package com.weiran.cqt.infrastructure.persistence;

import com.weiran.cqt.domain.region.Region;
import com.weiran.cqt.domain.region.RegionRepository;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** {@link RegionRepository} 的 JDBC 实现；读取 weiran-region 维护的行政区划表。 */
public class JdbcRegionRepository implements RegionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** 构造仓储。 */
    public JdbcRegionRepository(final DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public List<Region> findAll() {
        return this.jdbc.query(
                "SELECT child.id, COALESCE(parent.id, 0) parent_id, child.name, child.code "
                        + "FROM region child LEFT JOIN region parent ON parent.code = child.parent_code "
                        + "WHERE child.status = 'enabled' AND (parent.id IS NULL OR parent.status = 'enabled') "
                        + "ORDER BY parent.id, child.sort, child.code",
                (row, rowNumber) -> new Region(
                        row.getLong("id"), row.getLong("parent_id"), row.getString("name"), row.getString("code")));
    }

    @Override
    public Optional<String> findNameById(final long id) {
        return this.jdbc
                .query(
                        "SELECT name FROM region WHERE id = :id",
                        java.util.Map.of("id", id),
                        (row, rowNumber) -> row.getString("name"))
                .stream()
                .findFirst();
    }
}
