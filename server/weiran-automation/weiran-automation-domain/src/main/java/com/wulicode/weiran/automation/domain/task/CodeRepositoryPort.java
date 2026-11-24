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
     * @param projectId 代码库 ID，同时作为本地目录名
     * @param repositoryPath 云效返回的代码库路径（形如 {@code qyd/lianmeng/kr/web}）
     * @throws RepositorySyncException 同步失败（凭据缺失、网络失败、git 不存在等）
     */
    Path synchronize(String projectId, @Nullable String repositoryPath);

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
