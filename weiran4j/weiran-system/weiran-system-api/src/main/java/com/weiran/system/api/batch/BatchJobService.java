package com.weiran.system.api.batch;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.io.InputStream;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** 批处理用例接口。 */
public interface BatchJobService {
    BatchJobView create(String kind, String filename, InputStream input, long size, long userId);

    /** 在调用方事务内创建业务引用任务；引用与清单必须先由业务模块持久化。 */
    BatchJobView createReference(String kind, String name, String reference, long userId);

    BatchJobView get(long id, long userId);

    default List<BatchJobView> recent(final long userId) {
        return this.recent(userId, List.of());
    }

    List<BatchJobView> recent(long userId, List<String> kinds);

    BatchJobView cancel(long id, long userId);

    PageResult<BatchTaskDetailView> details(
            long id, long userId, @Nullable String stage, @Nullable String status, PageQuery page);
}
