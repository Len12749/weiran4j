package com.wulicode.weiran.automation.domain.task;

import java.util.List;

/**
 * 云效变更请求来源端口。
 *
 * <p>实现负责调用云效 OpenAPI；失败时抛出带可读原因的异常，由应用层记录为一次失败执行。
 */
public interface ChangeRequestSource {

    /** 拉取状态为待评审、且云效侧没有任何评论的合并请求。 */
    List<ChangeRequestSummary> listPendingReviews();

    /** 读取合并请求在当前源分支与目标分支之间的变更文件。 */
    List<ChangedFile> loadChangedFiles(ChangeRequestSummary summary);
}
