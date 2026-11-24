package com.wulicode.weiran.automation.infrastructure.agent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * 只读的代码仓库工具箱。
 *
 * <p>给评审 Agent 用：读文件、搜代码、看目录、查 git 历史。所有路径都限制在传入的仓库根目录内，
 * 命令只读，输出统一截断，避免把整仓库塞进模型上下文。
 */
public class RepositoryToolbox {

    private static final int MAX_FILE_CHARS = 20_000;

    private static final int MAX_OUTPUT_CHARS = 6_000;

    private static final int MAX_LISTED_FILES = 200;

    private static final long MAX_FILE_BYTES = 400_000L;

    private static final long GIT_TIMEOUT_SECONDS = 60L;

    private final Path root;

    /** 构造工具箱。 */
    public RepositoryToolbox(final Path repositoryRoot) {
        this.root = repositoryRoot.toAbsolutePath().normalize();
    }

    /** 读取仓库内文件全文。 */
    public String readFile(final String relativePath) {
        try {
            final Path target = this.resolve(relativePath);
            if (target == null) {
                return "路径越界：" + relativePath;
            }
            if (!Files.isRegularFile(target)) {
                return "文件不存在：" + relativePath;
            }
            if (Files.size(target) > RepositoryToolbox.MAX_FILE_BYTES) {
                return "文件过大，未读取（超过 400KB）：" + relativePath;
            }
            return RepositoryToolbox.clip(Files.readString(target, StandardCharsets.UTF_8), MAX_FILE_CHARS);
        } catch (final IOException ex) {
            return "读取失败：" + ex.getMessage();
        }
    }

    /** 用 git grep 在仓库里搜代码，返回带行号的匹配。 */
    public String grep(final String pattern, final @Nullable String relativePath) {
        final List<String> command = new ArrayList<>(
                List.of("git", "-C", this.root.toString(), "grep", "-n", "-I", "-E", "-m", "20", pattern));
        final String scoped = relativePath == null || relativePath.isBlank() ? null : relativePath.strip();
        if (scoped != null) {
            command.add("--");
            command.add(scoped);
        }
        return this.git(command);
    }

    /** 列出仓库内某个目录下的文件（最多 2 层）。 */
    public String listFiles(final @Nullable String relativePath) {
        final Path dir =
                relativePath == null || relativePath.isBlank() ? this.root : this.resolve(relativePath.strip());
        if (dir == null || !Files.isDirectory(dir)) {
            return "目录不存在：" + relativePath;
        }
        try (Stream<Path> stream = Files.walk(dir, 2)) {
            final List<String> files = stream.filter(Files::isRegularFile)
                    .map(path -> this.root.relativize(path).toString())
                    .filter(name -> !name.contains("/.git/") && !name.startsWith(".git/"))
                    .sorted()
                    .limit(RepositoryToolbox.MAX_LISTED_FILES)
                    .toList();
            return files.isEmpty() ? "目录为空" : String.join("\n", files);
        } catch (final IOException ex) {
            return "列目录失败：" + ex.getMessage();
        }
    }

    /** 最近提交记录；可限定到某个路径。 */
    public String gitLog(final @Nullable String relativePath, final int limit) {
        final int size = Math.min(Math.max(limit, 1), 50);
        final List<String> command = new ArrayList<>(
                List.of("git", "-C", this.root.toString(), "log", "--oneline", "-n", String.valueOf(size)));
        final String scoped = relativePath == null || relativePath.isBlank() ? null : relativePath.strip();
        if (scoped != null) {
            command.add("--");
            command.add(scoped);
        }
        return this.git(command);
    }

    /** 查看某个提交的改动概要。 */
    public String gitShow(final String sha) {
        return this.git(List.of("git", "-C", this.root.toString(), "show", "--stat", "--oneline", sha.strip()));
    }

    private String git(final List<String> command) {
        final ProcessBuilder builder = new ProcessBuilder(command).directory(this.root.toFile());
        builder.redirectErrorStream(true);
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        try {
            final Process process = builder.start();
            final boolean finished = process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "git 命令超时";
            }
            final String output;
            try (InputStream input = process.getInputStream()) {
                output = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            final String normalized = output.strip();
            return RepositoryToolbox.clip(normalized.isEmpty() ? "（没有结果）" : normalized, MAX_OUTPUT_CHARS);
        } catch (final IOException ex) {
            return "git 执行失败：" + ex.getMessage();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "git 命令被中断";
        }
    }

    private @Nullable Path resolve(final String relativePath) {
        final String cleaned = relativePath == null ? "" : relativePath.strip();
        if (cleaned.isEmpty()) {
            return this.root;
        }
        final Path target = this.root.resolve(cleaned).normalize();
        return target.startsWith(this.root) ? target : null;
    }

    private static String clip(final String value, final int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "\n…（输出已截断）";
    }
}
