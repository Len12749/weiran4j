package com.wulicode.weiran.automation.application.task;

import com.wulicode.weiran.automation.domain.task.AutomationTask;
import com.wulicode.weiran.automation.domain.task.AutomationTaskRepository;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 任务启动器：把一轮执行交给独立线程，调度与手动触发共用。
 *
 * <p>执行期间持有任务租约，结束时统一释放；任何异常都不会让租约留在库里。
 */
public class AutomationTaskLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationTaskLauncher.class);

    /** 调度触发。 */
    public static final String TRIGGER_SCHEDULE = "schedule";

    /** 管理员手动触发。 */
    public static final String TRIGGER_MANUAL = "manual";

    private final AutomationTaskRepository tasks;

    private final AutomationTaskExecutor executor;

    private final ExecutorService pool;

    /** 构造启动器。 */
    public AutomationTaskLauncher(
            final AutomationTaskRepository tasks, final AutomationTaskExecutor executor, final ExecutorService pool) {
        this.tasks = tasks;
        this.executor = executor;
        this.pool = pool;
    }

    /** 异步执行一轮；调用方须已抢占租约。 */
    public void submit(final AutomationTask task, final String triggerType) {
        this.pool.execute(() -> {
            try {
                this.executor.execute(task, triggerType);
            } catch (final RuntimeException ex) {
                LOGGER.error("Automation task {} failed unexpectedly", task.code(), ex);
            } finally {
                this.tasks.release(task.id());
            }
        });
    }
}
