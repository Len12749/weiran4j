package com.weiran.system.application.batch;

import com.weiran.system.api.batch.BatchFileException;
import com.weiran.system.api.batch.BatchProcessor;
import com.weiran.system.api.batch.BatchProgress;
import com.weiran.system.domain.batch.BatchFileStorage;
import com.weiran.system.domain.batch.BatchJobRepository;
import com.weiran.system.domain.batch.BatchJobRepository.BatchWork;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;

/** 单实例串行消费；数据库原子领取允许多个应用实例并行处理不同任务。 */
public class BatchJobWorker implements AutoCloseable, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchJobWorker.class);
    private final BatchJobRepository jobs;
    private final BatchFileStorage files;
    private final List<BatchProcessor> processors;
    private @Nullable ScheduledFuture<?> cleanupTask;
    private final ScheduledExecutorService renewer = Executors.newScheduledThreadPool(2, task -> {
        final Thread thread = new Thread(task, "weiran-batch-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public BatchJobWorker(
            final BatchJobRepository jobs,
            final BatchFileStorage files,
            final ObjectProvider<BatchProcessor> processors) {
        this.jobs = jobs;
        this.files = files;
        this.processors = processors.orderedStream().toList();
    }

    @Override
    public void afterPropertiesSet() {
        this.cleanupTask = this.renewer.scheduleWithFixedDelay(
                () -> {
                    try {
                        this.cleanupExpiredFiles();
                    } catch (final RuntimeException ex) {
                        LOGGER.warn("批处理文件保留期清理暂不可用");
                    }
                },
                1,
                1,
                TimeUnit.HOURS);
    }

    @Scheduled(fixedDelay = 1500)
    public void consume() {
        final String worker = UUID.randomUUID().toString();
        final BatchWork work = this.jobs.claim(worker);
        if (work == null) {
            return;
        }
        final BatchProcessor processor = this.processors.stream()
                .filter(candidate -> candidate.kind().equals(work.kind()))
                .findFirst()
                .orElse(null);
        if (processor == null) {
            this.jobs.fail(work.id(), worker, "处理器未安装");
            return;
        }
        final ScheduledFuture<?> lease = this.renewer.scheduleAtFixedRate(
                () -> {
                    try {
                        this.jobs.heartbeat(work.id(), worker);
                    } catch (final RuntimeException ex) {
                        LOGGER.warn("批处理续租失败 id={}", work.id());
                    }
                },
                30,
                30,
                TimeUnit.SECONDS);
        final AtomicBoolean validationPassed = new AtomicBoolean();
        try {
            final BatchProgress progress = new BatchProgress() {
                @Override
                public long jobId() {
                    return work.id();
                }

                @Override
                public void precheckFailure(final long itemNo, final String itemName, final String message) {
                    BatchJobWorker.this.jobs.precheckFailure(
                            work.id(),
                            worker,
                            itemNo,
                            BatchJobWorker.limit(itemName, 255),
                            BatchJobWorker.limit(message, 500));
                }

                @Override
                public void total(final long value) {
                    BatchJobWorker.this.jobs.setTotal(work.id(), worker, value);
                    final long errors = BatchJobWorker.this.jobs.precheckErrors(work.id());
                    if (errors > 0) {
                        throw new BatchFileException("预检发现 " + errors + " 项错误");
                    }
                    BatchJobWorker.this.jobs.beginProcessing(work.id(), worker);
                    validationPassed.set(true);
                }

                @Override
                public void success(final long itemNo, final String itemName) {
                    BatchJobWorker.this.jobs.success(work.id(), worker, itemNo, BatchJobWorker.limit(itemName, 255));
                }

                @Override
                public void failure(final long itemNo, final String itemName, final String message) {
                    final String name = BatchJobWorker.limit(itemName, 255);
                    final String reason = BatchJobWorker.limit(message, 500);
                    BatchJobWorker.this.jobs.failure(work.id(), worker, itemNo, name, reason);
                }

                @Override
                public boolean cancelled() {
                    return BatchJobWorker.this.jobs.stopped(work.id(), worker);
                }
            };
            this.jobs.beginValidation(work.id(), worker);
            processor.process(() -> this.files.open(work.sourceKey()), progress);
            this.jobs.finish(work.id(), worker, this.jobs.hasFailures(work.id()) ? "partial_failed" : "succeeded");
        } catch (final Exception ex) {
            LOGGER.error("批处理任务失败 id={} type={}", work.id(), ex.getClass().getSimpleName());
            final String message = ex instanceof BatchFileException fileError
                    ? BatchJobWorker.limit(fileError.getMessage(), 500)
                    : "文件处理失败，请检查模板或文件内容";
            if (!validationPassed.get()) {
                try {
                    if (this.jobs.precheckErrors(work.id()) == 0) {
                        this.jobs.precheckFailure(work.id(), worker, 0, work.sourceName(), message);
                    }
                } catch (final RuntimeException detailError) {
                    LOGGER.error("批处理预检错误记录失败 id={}", work.id());
                }
            }
            this.jobs.fail(work.id(), worker, message);
        } finally {
            lease.cancel(false);
        }
    }

    public void cleanupExpiredFiles() {
        for (final BatchJobRepository.BatchCleanup job : this.jobs.expiredFiles()) {
            try {
                if (job.sourceKey() != null) {
                    this.files.delete(job.sourceKey());
                }
                this.jobs.clearFiles(job.id(), job.sourceKey());
            } catch (final RuntimeException ex) {
                LOGGER.warn("批处理文件保留期清理失败 id={}", job.id());
            }
        }
    }

    @Override
    public void close() {
        if (this.cleanupTask != null) {
            this.cleanupTask.cancel(false);
        }
        this.renewer.shutdownNow();
    }

    private static String limit(final String value, final int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
