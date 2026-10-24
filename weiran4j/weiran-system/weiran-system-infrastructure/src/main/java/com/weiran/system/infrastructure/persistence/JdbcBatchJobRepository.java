package com.weiran.system.infrastructure.persistence;

import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.system.domain.batch.BatchJob;
import com.weiran.system.domain.batch.BatchJobRepository;
import com.weiran.system.domain.batch.BatchTaskDetail;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

/** MySQL 8 任务队列与逐项明细。领取通过一条 UPDATE 原子完成，过期租约可重新领取。 */
public class JdbcBatchJobRepository implements BatchJobRepository {

    private static final RowMapper<BatchJob> JOB_MAPPER = (row, index) -> JdbcBatchJobRepository.map(row);
    private final JdbcTemplate jdbc;

    public JdbcBatchJobRepository(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public long create(final String kind, final String filename, final String sourceKey, final long userId) {
        this.jdbc.queryForObject("select id from sys_user where id = ? for update", Long.class, userId);
        final Integer active = this.jdbc.queryForObject(
                "select count(*) from sys_task where created_by = ? "
                        + "and status in ('queued', 'running', 'cancel_requested')",
                Integer.class,
                userId);
        if (active != null && active >= 3) {
            throw BizException.conflict("同一账号最多同时保留 3 个待处理任务");
        }
        final KeyHolder keys = new GeneratedKeyHolder();
        this.jdbc.update(
                connection -> {
                    final var statement = connection.prepareStatement(
                            "insert into sys_task (kind, source_name, source_key, created_by) values (?, ?, ?, ?)",
                            java.sql.Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, kind);
                    statement.setString(2, filename);
                    statement.setString(3, sourceKey);
                    statement.setLong(4, userId);
                    return statement;
                },
                keys);
        return java.util.Objects.requireNonNull(keys.getKey()).longValue();
    }

    @Override
    public @Nullable BatchJob get(final long id, final long userId) {
        return this.jdbc
                .query("select * from sys_task where id = ? and created_by = ?", JOB_MAPPER, id, userId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<BatchJob> recent(final long userId, final List<String> kinds) {
        if (kinds.isEmpty()) {
            return this.jdbc.query(
                    "select * from sys_task where created_by = ? order by id desc limit 30", JOB_MAPPER, userId);
        }
        final String placeholders = String.join(",", java.util.Collections.nCopies(kinds.size(), "?"));
        final List<Object> args = new ArrayList<>();
        args.add(userId);
        args.addAll(kinds);
        return this.jdbc.query(
                "select * from sys_task where created_by = ? and kind in (" + placeholders
                        + ") order by id desc limit 30",
                JOB_MAPPER,
                args.toArray());
    }

    @Override
    public PageResult<BatchTaskDetail> details(
            final long id, final @Nullable String stage, final @Nullable String status, final PageQuery page) {
        final StringBuilder filter = new StringBuilder("task_id = ?");
        final List<Object> args = new ArrayList<>();
        args.add(id);
        if (stage != null) {
            filter.append(" and stage = ?");
            args.add(stage);
        }
        if (status != null) {
            filter.append(" and status = ?");
            args.add(status);
        }
        final Long total = this.jdbc.queryForObject(
                "select count(*) from sys_task_detail where " + filter, Long.class, args.toArray());
        args.add(page.pageSize());
        args.add(page.offset());
        final List<BatchTaskDetail> items = this.jdbc.query(
                "select * from sys_task_detail where " + filter
                        + " order by case stage when 'validating' then 0 else 1 end, item_no, id limit ? offset ?",
                (row, index) -> JdbcBatchJobRepository.mapDetail(row),
                args.toArray());
        return PageResult.of(items, total == null ? 0 : total, page);
    }

    @Override
    @Transactional
    public @Nullable BatchWork claim(final String worker) {
        this.jdbc.update("update sys_task set status = 'cancelled', finished_at = now() "
                + "where status = 'cancel_requested' and heartbeat_at < date_sub(now(), interval 2 minute)");
        final int claimed = this.jdbc.update(
                "update sys_task set status = 'running', phase = 'preparing', worker = ?, heartbeat_at = now(), "
                        + "total = null, processed = 0, succeeded = 0, failed = 0, error_message = null "
                        + "where status = 'queued' or (status = 'running' and heartbeat_at < date_sub(now(), interval 2 minute)) "
                        + "order by id limit 1",
                worker);
        if (claimed == 0) {
            return null;
        }
        final BatchWork work = this.jdbc
                .query(
                        "select id, kind, source_name, source_key from sys_task where worker = ? and status = 'running'",
                        (row, index) -> new BatchWork(
                                row.getLong("id"),
                                row.getString("kind"),
                                row.getString("source_name"),
                                row.getString("source_key")),
                        worker)
                .stream()
                .findFirst()
                .orElse(null);
        if (work != null) {
            this.jdbc.update("delete from sys_task_detail where task_id = ?", work.id());
        }
        return work;
    }

    @Override
    public void heartbeat(final long id, final String worker) {
        this.jdbc.update(
                "update sys_task set heartbeat_at = now() where id = ? and worker = ? and status in ('running', 'cancel_requested')",
                id,
                worker);
    }

    @Override
    public void beginValidation(final long id, final String worker) {
        this.jdbc.update(
                "update sys_task set phase = 'validating', heartbeat_at = now() where id = ? and worker = ? and status = 'running'",
                id,
                worker);
    }

    @Override
    public void setTotal(final long id, final String worker, final long total) {
        this.jdbc.update(
                "update sys_task set total = ?, heartbeat_at = now() where id = ? and worker = ? and status = 'running' and phase = 'validating'",
                total,
                id,
                worker);
    }

    @Override
    public void beginProcessing(final long id, final String worker) {
        this.jdbc.update(
                "update sys_task set phase = 'processing', heartbeat_at = now() where id = ? and worker = ? and status = 'running' and phase = 'validating'",
                id,
                worker);
    }

    @Override
    @Transactional
    public void precheckFailure(
            final long id, final String worker, final long itemNo, final String itemName, final String message) {
        final int updated = this.jdbc.update(
                "update sys_task set heartbeat_at = now() where id = ? and worker = ? and status = 'running' and phase = 'validating'",
                id,
                worker);
        if (updated != 1) {
            throw new IllegalStateException("批处理预检已停止");
        }
        this.jdbc.update(
                "insert into sys_task_detail (task_id, item_no, item_name, stage, status, message) "
                        + "values (?, ?, ?, 'validating', 'failed', ?)",
                id,
                itemNo,
                itemName,
                message);
    }

    @Override
    public long precheckErrors(final long id) {
        final Long count = this.jdbc.queryForObject(
                "select count(*) from sys_task_detail where task_id = ? and stage = 'validating' and status = 'failed'",
                Long.class,
                id);
        return count == null ? 0 : count;
    }

    @Override
    @Transactional
    public void success(final long id, final String worker, final long itemNo, final String itemName) {
        final int updated = this.jdbc.update(
                "update sys_task set processed = processed + 1, succeeded = succeeded + 1, heartbeat_at = now() "
                        + "where id = ? and worker = ? and status = 'running'",
                id,
                worker);
        if (updated != 1) {
            throw new IllegalStateException("批处理任务已停止");
        }
        this.jdbc.update(
                "insert into sys_task_detail (task_id, item_no, item_name, stage, status) "
                        + "values (?, ?, ?, 'processing', 'succeeded')",
                id,
                itemNo,
                itemName);
    }

    @Override
    @Transactional
    public void failure(
            final long id, final String worker, final long itemNo, final String itemName, final String message) {
        final int updated = this.jdbc.update(
                "update sys_task set processed = processed + 1, failed = failed + 1, heartbeat_at = now() "
                        + "where id = ? and worker = ? and status = 'running'",
                id,
                worker);
        if (updated != 1) {
            throw new IllegalStateException("批处理任务已停止");
        }
        this.jdbc.update(
                "insert into sys_task_detail (task_id, item_no, item_name, stage, status, message) "
                        + "values (?, ?, ?, 'processing', 'failed', ?)",
                id,
                itemNo,
                itemName,
                message);
    }

    @Override
    public boolean hasFailures(final long id) {
        final Long failed = this.jdbc.queryForObject("select failed from sys_task where id = ?", Long.class, id);
        return failed != null && failed > 0;
    }

    @Override
    public boolean stopped(final long id, final String worker) {
        return !Boolean.TRUE.equals(this.jdbc.queryForObject(
                "select status = 'running' and worker = ? from sys_task where id = ?", Boolean.class, worker, id));
    }

    @Override
    public void requestCancel(final long id, final long userId) {
        this.jdbc.update(
                "update sys_task set status = case when status = 'queued' then 'cancelled' else 'cancel_requested' end, "
                        + "finished_at = case when status = 'queued' then now() else finished_at end "
                        + "where id = ? and created_by = ? and status in ('queued', 'running')",
                id,
                userId);
    }

    @Override
    public void finish(final long id, final String worker, final String status) {
        this.jdbc.update(
                "update sys_task set phase = case when status = 'cancel_requested' then phase else 'finished' end, "
                        + "status = case when status = 'cancel_requested' then 'cancelled' else ? end, "
                        + "finished_at = now(), worker = null "
                        + "where id = ? and worker = ?",
                status,
                id,
                worker);
    }

    @Override
    public void fail(final long id, final String worker, final String message) {
        this.jdbc.update(
                "update sys_task set status = case when status = 'cancel_requested' then 'cancelled' else 'failed' end, "
                        + "finished_at = now(), worker = null, error_message = ? "
                        + "where id = ? and worker = ?",
                message,
                id,
                worker);
    }

    @Override
    public List<BatchCleanup> expiredFiles() {
        return this.jdbc.query(
                "select id, source_key from sys_task "
                        + "where finished_at < date_sub(now(), interval 7 day) "
                        + "and source_key is not null order by id limit 100",
                (row, index) -> new BatchCleanup(row.getLong("id"), row.getString("source_key")));
    }

    @Override
    public void clearFiles(final long id, final @Nullable String sourceKey) {
        this.jdbc.update("update sys_task set source_key = null where id = ? and source_key <=> ?", id, sourceKey);
    }

    private static BatchJob map(final ResultSet row) throws SQLException {
        return new BatchJob(
                row.getLong("id"),
                row.getString("kind"),
                row.getString("source_name"),
                row.getString("status"),
                row.getString("phase"),
                row.getObject("total", Long.class),
                row.getLong("processed"),
                row.getLong("succeeded"),
                row.getLong("failed"),
                row.getString("error_message"),
                row.getTimestamp("created_at").toLocalDateTime(),
                row.getTimestamp("finished_at") == null
                        ? null
                        : row.getTimestamp("finished_at").toLocalDateTime());
    }

    private static BatchTaskDetail mapDetail(final ResultSet row) throws SQLException {
        return new BatchTaskDetail(
                row.getLong("id"),
                row.getLong("item_no"),
                row.getString("item_name"),
                row.getString("stage"),
                row.getString("status"),
                row.getString("message"),
                row.getTimestamp("created_at").toLocalDateTime());
    }
}
