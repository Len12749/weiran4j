package com.weiran.system.domain.batch;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** 任务及逐项结果仓储；领取与逐项写入均由数据库原子操作保证。 */
public interface BatchJobRepository {
    long create(String kind, String filename, String sourceKey, long userId);

    @Nullable
    BatchJob get(long id, long userId);

    List<BatchJob> recent(long userId, List<String> kinds);

    PageResult<BatchTaskDetail> details(long id, @Nullable String stage, @Nullable String status, PageQuery page);

    @Nullable
    BatchWork claim(String worker);

    void heartbeat(long id, String worker);

    void beginValidation(long id, String worker);

    void setTotal(long id, String worker, long total);

    void beginProcessing(long id, String worker);

    void precheckFailure(long id, String worker, long itemNo, String itemName, String message);

    long precheckErrors(long id);

    void success(long id, String worker, long itemNo, String itemName);

    void failure(long id, String worker, long itemNo, String itemName, String message);

    boolean hasFailures(long id);

    boolean stopped(long id, String worker);

    void requestCancel(long id, long userId);

    void finish(long id, String worker, String status);

    void fail(long id, String worker, String message);

    List<BatchCleanup> expiredFiles();

    void clearFiles(long id, @Nullable String sourceKey);

    record BatchWork(long id, String kind, String sourceName, String sourceKey) {}

    record BatchCleanup(long id, @Nullable String sourceKey) {}
}
