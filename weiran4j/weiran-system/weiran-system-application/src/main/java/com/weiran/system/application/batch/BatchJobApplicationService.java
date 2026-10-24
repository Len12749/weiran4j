package com.weiran.system.application.batch;

import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.system.api.batch.BatchJobService;
import com.weiran.system.api.batch.BatchJobView;
import com.weiran.system.api.batch.BatchProcessor;
import com.weiran.system.api.batch.BatchTaskDetailView;
import com.weiran.system.domain.batch.BatchFileStorage;
import com.weiran.system.domain.batch.BatchJob;
import com.weiran.system.domain.batch.BatchJobRepository;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;

/** 上传文件并建立持久化任务，查询始终按创建者隔离。 */
public class BatchJobApplicationService implements BatchJobService {

    private final BatchJobRepository jobs;
    private final BatchFileStorage files;
    private final List<BatchProcessor> processors;

    public BatchJobApplicationService(
            final BatchJobRepository jobs,
            final BatchFileStorage files,
            final ObjectProvider<BatchProcessor> processors) {
        this.jobs = jobs;
        this.files = files;
        this.processors = processors.orderedStream().toList();
    }

    @Override
    public BatchJobView create(
            final String kind, final String filename, final InputStream input, final long size, final long userId) {
        final BatchProcessor processor = this.processors.stream()
                .filter(candidate -> candidate.kind().equals(kind))
                .findFirst()
                .orElseThrow(() -> BizException.badRequest("不支持的批处理类型"));
        if (size <= 0 || size > processor.maxBytes()) {
            throw BizException.badRequest("批处理文件超过该类型的大小限制");
        }
        processor.authorize(userId);
        final String safeName = filename.replace('\\', '/');
        final String basename = safeName.substring(safeName.lastIndexOf('/') + 1);
        if (basename.isBlank()
                || basename.length() > 255
                || !basename.toLowerCase(Locale.ROOT).endsWith("." + processor.extension())) {
            throw BizException.badRequest("文件名与批处理类型不匹配");
        }
        final String key = this.files.store(basename, input, size);
        try {
            final long id = this.jobs.create(kind, basename, key, userId);
            return this.get(id, userId);
        } catch (final RuntimeException ex) {
            this.files.delete(key);
            throw ex;
        }
    }

    @Override
    public BatchJobView get(final long id, final long userId) {
        final BatchJob job = this.jobs.get(id, userId);
        if (job == null) {
            throw BizException.notFound("批处理任务不存在");
        }
        return this.view(job);
    }

    @Override
    public List<BatchJobView> recent(final long userId, final List<String> kinds) {
        if (kinds.size() > 20 || kinds.stream().anyMatch(kind -> !kind.matches("[A-Za-z0-9._-]{1,64}"))) {
            throw BizException.badRequest("批处理类型筛选无效");
        }
        return this.jobs.recent(userId, kinds.stream().distinct().toList()).stream()
                .map(this::view)
                .toList();
    }

    @Override
    public BatchJobView cancel(final long id, final long userId) {
        this.get(id, userId);
        this.jobs.requestCancel(id, userId);
        return this.get(id, userId);
    }

    @Override
    public PageResult<BatchTaskDetailView> details(
            final long id,
            final long userId,
            final @Nullable String stage,
            final @Nullable String status,
            final PageQuery page) {
        this.get(id, userId);
        if (stage != null && !stage.equals("validating") && !stage.equals("processing")) {
            throw BizException.badRequest("明细阶段无效");
        }
        if (status != null && !status.equals("succeeded") && !status.equals("failed")) {
            throw BizException.badRequest("明细状态无效");
        }
        return this.jobs
                .details(id, stage, status, page)
                .map(item -> new BatchTaskDetailView(
                        item.id(),
                        item.itemNo(),
                        item.itemName(),
                        item.stage(),
                        item.status(),
                        item.message(),
                        item.createdAt()));
    }

    private BatchJobView view(final BatchJob job) {
        return new BatchJobView(
                job.id(),
                job.kind(),
                job.sourceName(),
                job.status(),
                job.phase(),
                job.total(),
                job.processed(),
                job.succeeded(),
                job.failed(),
                job.errorMessage(),
                job.createdAt(),
                job.finishedAt());
    }
}
