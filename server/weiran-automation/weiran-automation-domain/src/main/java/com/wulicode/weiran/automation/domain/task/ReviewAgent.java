package com.wulicode.weiran.automation.domain.task;

import java.nio.file.Path;
import java.util.List;

/**
 * 代码评审 Agent 端口。
 *
 * <p>实现负责「先检索知识库规范，再调用大模型」的完整链路；本层只拿到结论。
 */
public interface ReviewAgent {

    /**
     * 对一次合并请求的变更文件产出评论草稿；失败时抛出带可读原因的异常。
     *
     * @param repositoryPath 已同步到最新的本地代码库根目录，供 Agent 读取变更文件之外的上文
     */
    ReviewDraft review(ChangeRequestSummary summary, List<ChangedFile> files, Path repositoryPath);
}
