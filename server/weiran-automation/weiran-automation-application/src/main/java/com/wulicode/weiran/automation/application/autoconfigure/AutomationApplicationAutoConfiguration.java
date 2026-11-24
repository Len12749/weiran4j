package com.wulicode.weiran.automation.application.autoconfigure;

import com.wulicode.weiran.automation.application.task.AutomationTaskApplicationService;
import com.wulicode.weiran.automation.application.task.AutomationTaskExecutor;
import com.wulicode.weiran.automation.application.task.AutomationTaskLauncher;
import com.wulicode.weiran.automation.application.task.AutomationTaskScheduler;
import com.wulicode.weiran.automation.domain.task.AutomationReadiness;
import com.wulicode.weiran.automation.domain.task.AutomationRunRepository;
import com.wulicode.weiran.automation.domain.task.AutomationTaskRepository;
import com.wulicode.weiran.automation.domain.task.ChangeRequestSource;
import com.wulicode.weiran.automation.domain.task.CodeRepositoryPort;
import com.wulicode.weiran.automation.domain.task.ReviewAgent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * weiran-automation 应用层自动配置。
 *
 * <p>调度开关由 {@code weiran.automation.enabled} 控制（默认开启）；任务本身的启停与间隔在数据里，
 * 只能由管理员通过界面调整启停，定时配置不开放修改。
 */
@AutoConfiguration
@EnableScheduling
public class AutomationApplicationAutoConfiguration {

    /** 合并请求并行分析的线程池；单轮最多 3 个合并请求，3 个线程足够。 */
    @Bean(name = "automationReviewExecutor", destroyMethod = "shutdownNow")
    public ExecutorService automationReviewExecutor() {
        return Executors.newFixedThreadPool(3, runnable -> {
            final Thread thread = new Thread(runnable, "automation-review");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** 评审工具线程池：同一轮里模型可能一次要求多个工具，最多 8 个并行。 */
    @Bean(name = "automationToolExecutor", destroyMethod = "shutdownNow")
    public ExecutorService automationToolExecutor() {
        return Executors.newFixedThreadPool(8, runnable -> {
            final Thread thread = new Thread(runnable, "automation-tool");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** 任务执行入口线程池：调度与手动触发共用，避免阻塞调度线程与 HTTP 请求。 */
    @Bean(name = "automationLaunchExecutor", destroyMethod = "shutdownNow")
    public ExecutorService automationLaunchExecutor() {
        return Executors.newFixedThreadPool(2, runnable -> {
            final Thread thread = new Thread(runnable, "automation-launch");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** 任务启动器。 */
    @Bean
    public AutomationTaskLauncher automationTaskLauncher(
            final AutomationTaskRepository tasks,
            final AutomationTaskExecutor executor,
            @Qualifier("automationLaunchExecutor") final ExecutorService launchExecutor) {
        return new AutomationTaskLauncher(tasks, executor, launchExecutor);
    }

    /** 任务查询与启停用例。 */
    @Bean
    public AutomationTaskApplicationService automationTaskApplicationService(
            final AutomationTaskRepository tasks,
            final AutomationRunRepository runs,
            final AutomationTaskLauncher launcher) {
        return new AutomationTaskApplicationService(tasks, runs, launcher);
    }

    /** 执行器：编排一轮评审的全部步骤。 */
    @Bean
    public AutomationTaskExecutor automationTaskExecutor(
            final AutomationTaskRepository tasks,
            final AutomationRunRepository runs,
            final ChangeRequestSource source,
            final CodeRepositoryPort repositories,
            final ReviewAgent agent,
            final AutomationReadiness readiness,
            @Qualifier("automationReviewExecutor") final ExecutorService reviewExecutor) {
        return new AutomationTaskExecutor(tasks, runs, source, repositories, agent, readiness, reviewExecutor);
    }

    /** 调度器：每 15 秒检查到期任务，执行频率由任务表里的间隔决定。 */
    @Bean
    public AutomationTaskScheduler automationTaskScheduler(
            final AutomationTaskRepository tasks,
            final AutomationTaskLauncher launcher,
            final AutomationRunRepository runs,
            final Environment environment) {
        return new AutomationTaskScheduler(tasks, launcher, runs, environment);
    }
}
