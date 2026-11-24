package com.wulicode.weiran.automation.domain.task;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * 本地代码仓库同步端口。
 *
 * <p>评审前先同步代码库，保证 Agent 读到的文件是最新版本；实现负责 clone / fetch 与并发互斥。
 */
public interface CodeRepositoryPort {

    /**
     * 同步指定代码库到本地并返回根目录。
     *
     * @param projectId 代码库 ID，按它缓存一份主克隆
     * @param repositoryPath 云效返回的代码库路径（形如 {@code qyd/lianmeng/kr/web}）
     * @param sourceBranch 合并请求的源分支；工具会读取这个分支的工作区，而不是默认分支
     * @param mergeRequestId 合并请求局部 ID，用于给每个 PR 单独开一个 git worktree
     * @throws RepositorySyncException 同步失败（凭据缺失、网络失败、git 不存在等）
     */
    Path synchronize(
            String projectId, @Nullable String repositoryPath, @Nullable String sourceBranch, long mergeRequestId);

    /** 仓库同步失败；消息可直接展示给使用者。 */
    final class RepositorySyncException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /** 构造带有可读原因的同步异常。 */
        public RepositorySyncException(final String message) {
            super(message);
        }

        /** 构造带有可读原因与底层异常的同步异常。 */
        public RepositorySyncException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
