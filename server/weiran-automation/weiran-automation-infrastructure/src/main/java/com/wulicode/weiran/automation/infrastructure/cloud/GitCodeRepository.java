package com.wulicode.weiran.automation.infrastructure.cloud;

import com.wulicode.weiran.automation.domain.task.CodeRepositoryPort;
import com.wulicode.weiran.automation.infrastructure.autoconfigure.AutomationProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于系统 git 的代码仓库同步。
 *
 * <p>凭据走系统 git 的 credential helper（本机已配置 osxkeychain），不在配置或日志里保存密码；
 * 首次 clone、之后 fetch --all --prune。同一个代码库的同步用进程内锁串行化，避免并发写坏工作区。
 */
public class GitCodeRepository implements CodeRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitCodeRepository.class);

    private final AutomationProperties.Repository properties;

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /** 构造仓库同步实现。 */
    public GitCodeRepository(final AutomationProperties properties) {
        this.properties = properties.repository();
    }

    @Override
    public Path synchronize(final String projectId, final @Nullable String repositoryPath) {
        if (repositoryPath == null || repositoryPath.isBlank()) {
            throw new RepositorySyncException("云效没有返回合并请求所属代码库的路径");
        }
        final String url = this.toHttpsUrl(repositoryPath);
        final Path root = Path.of(this.properties.root()).toAbsolutePath().normalize();
        final Path target =
                root.resolve(GitCodeRepository.directoryName(projectId)).normalize();
        final ReentrantLock lock = this.locks.computeIfAbsent(projectId, key -> new ReentrantLock());
        lock.lock();
        try {
            Files.createDirectories(root);
            if (Files.isDirectory(target.resolve(".git"))) {
                this.run(target, "fetch", "--all", "--prune", "--quiet");
                LOGGER.info("Repository {} updated at {}", projectId, target);
            } else {
                this.run(root, "clone", "--quiet", url, target.toString());
                LOGGER.info("Repository {} cloned to {}", projectId, target);
            }
            return target;
        } catch (final IOException ex) {
            throw new RepositorySyncException("创建代码仓库目录失败：" + ex.getMessage(), ex);
        } finally {
            lock.unlock();
        }
    }

    /** 把代码库路径转成 HTTPS 克隆地址；密码由系统 git 凭据提供。 */
    private String toHttpsUrl(final String repositoryPath) {
        final String path = repositoryPath.strip().replace(" ", "");
        if (path.isEmpty()) {
            throw new RepositorySyncException("代码库路径为空");
        }
        final String base = this.properties.baseUrl().strip();
        final String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalizedBase + "/" + path + ".git";
    }

    private void run(final Path workDir, final String... args) {
        final List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        final ProcessBuilder builder = new ProcessBuilder(command).directory(workDir.toFile());
        builder.redirectErrorStream(true);
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        try {
            final Process process = builder.start();
            final boolean finished = process.waitFor(this.properties.timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RepositorySyncException("git 命令超时：" + String.join(" ", args));
            }
            final String output;
            try (InputStream input = process.getInputStream()) {
                output = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (process.exitValue() != 0) {
                throw new RepositorySyncException(this.describeFailure(output));
            }
        } catch (final IOException ex) {
            throw new RepositorySyncException("无法执行 git，请确认服务器已安装 git：" + ex.getMessage(), ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RepositorySyncException("git 命令被中断", ex);
        }
    }

    /** 把 git 输出翻译成使用者能看懂的原因，同时不暴露任何凭据。 */
    private String describeFailure(final String output) {
        final String normalized = output.toLowerCase(Locale.ROOT);
        if (normalized.contains("authentication failed")
                || normalized.contains("could not read username")
                || normalized.contains("terminal prompts disabled")) {
            return "无法认证代码仓库：请在云效「个人设置 → HTTPS密码」配置克隆账号密码，并保证本机 git 凭据可用";
        }
        if (normalized.contains("not found") || normalized.contains("未知的仓库路径")) {
            return "代码仓库地址不存在或没有权限，请检查仓库路径";
        }
        final String trimmed = output.strip();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 299) + "…";
    }

    private static String directoryName(final String projectId) {
        final String safe = projectId.replaceAll("[^A-Za-z0-9_.-]", "_");
        return safe.isBlank() ? "unknown" : safe;
    }
}
