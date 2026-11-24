package com.wulicode.weiran.automation.application.task;

import com.wulicode.weiran.automation.domain.task.AutomationRunRepository;
import com.wulicode.weiran.automation.domain.task.AutomationTask;
import com.wulicode.weiran.automation.domain.task.AutomationTaskRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 自动化任务调度器。
 *
 * <p>固定每 15 秒检查一次，是否真正执行由任务表里的 {@code next_run_at} 与执行租约决定：
 * 任务间隔（例如 60 秒）写在数据里而不是注解里，重启后不会重置节奏；多实例同时检查时，
 * 数据库行级租约保证只有一个实例进入执行。
 */
public class AutomationTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationTaskScheduler.class);

    /** 检查间隔：15 秒，只用于发现到期任务，不决定任务本身的执行频率。 */
    private static final long TICK_DELAY_MS = 15_000L;

    /** 首次检查延迟：等应用启动完成后再跑。 */
    private static final long INITIAL_DELAY_MS = 10_000L;

    /** 执行租约时长：单轮最长处理 3 个合并请求，30 分钟足够；进程崩溃后由租约自动释放。 */
    private static final long LEASE_MINUTES = 30L;

    /** 任务间隔下限，避免脏数据把云效接口打成高频轮询。 */
    private static final int MIN_INTERVAL_SECONDS = 30;

    /** 启动清理的宽限：比这个更早的未收尾记录视为中断。 */
    private static final long STALE_GRACE_MINUTES = 10L;

    /** 业务时区：与数据库 datetime 列的写入口径保持一致。 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AutomationTaskRepository tasks;

    private final AutomationTaskLauncher launcher;

    private final AutomationRunRepository runs;

    private final boolean enabled;

    /** 构造调度器。 */
    public AutomationTaskScheduler(
            final AutomationTaskRepository tasks,
            final AutomationTaskLauncher launcher,
            final AutomationRunRepository runs,
            final Environment environment) {
        this.tasks = tasks;
        this.launcher = launcher;
        this.runs = runs;
        this.enabled = Boolean.parseBoolean(environment.getProperty("weiran.automation.enabled", "true"));
    }

    /** 启动时把上一个进程留下的「执行中」记录标记为中断，避免界面一直转圈。 */
    @EventListener(ApplicationReadyEvent.class)
    public void cleanupStaleRuns() {
        final int cleaned = this.runs.failStaleRuns(LocalDateTime.now(AutomationTaskScheduler.ZONE)
                .minusMinutes(AutomationTaskScheduler.STALE_GRACE_MINUTES));
        if (cleaned > 0) {
            LOGGER.warn("Marked {} stale automation runs as interrupted", cleaned);
        }
    }

    /** 检查并执行到期的任务。 */
    @Scheduled(
            fixedDelay = AutomationTaskScheduler.TICK_DELAY_MS,
            initialDelay = AutomationTaskScheduler.INITIAL_DELAY_MS)
    public void tick() {
        if (!this.enabled) {
            return;
        }
        final LocalDateTime now = LocalDateTime.now(AutomationTaskScheduler.ZONE);
        for (final AutomationTask task : this.tasks.findAll()) {
            if (!task.enabled()) {
                continue;
            }
            final int interval = Math.max(task.intervalSeconds(), AutomationTaskScheduler.MIN_INTERVAL_SECONDS);
            final boolean claimed = this.tasks.claim(
                    task.id(),
                    now,
                    now.plusSeconds(interval),
                    now.plusMinutes(AutomationTaskScheduler.LEASE_MINUTES),
                    true);
            if (!claimed) {
                continue;
            }
            this.launcher.submit(task, AutomationTaskLauncher.TRIGGER_SCHEDULE);
        }
    }
}
