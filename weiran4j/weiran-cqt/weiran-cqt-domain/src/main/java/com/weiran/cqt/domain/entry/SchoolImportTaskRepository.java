package com.weiran.cqt.domain.entry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 学校导入会话、任务与逐组结果持久化端口。 */
public interface SchoolImportTaskRepository {

    /** 创建等待文件上传的会话。 */
    void create(SchoolImportTask task);

    /** 查询指定账号拥有的会话/任务。 */
    Optional<SchoolImportTask> findOwned(String taskId, long accountId);

    /** 更新上传对象信息，仅允许 UPLOADING 状态且当前账号拥有。 */
    boolean updateUpload(
            String taskId,
            long accountId,
            @Nullable String zipUrl,
            @Nullable String zipName,
            @Nullable String excelUrl,
            @Nullable String excelName,
            LocalDateTime now);

    /** 确认会话并排队，同一任务重复确认返回 false。 */
    boolean queue(String taskId, long accountId, long competitionId, String importType, LocalDateTime now);

    /** 原子领取一个未处理或租约过期的任务。 */
    Optional<SchoolImportTask> claimNext(LocalDateTime now, LocalDateTime leaseUntil);

    /** 保存解析出的作品组行，重复调用不重复插入。 */
    void insertRows(String taskId, List<SchoolImportRowResult> rows, int totalRows, LocalDateTime now);

    /** 取任务所有结果行。 */
    List<SchoolImportRowResult> rows(String taskId);

    /** 更新作品组全部源行的成功状态与作品主键。 */
    void markSuccess(
            String taskId, int firstSourceRow, int lastSourceRow, String fingerprint, long entryId, LocalDateTime now);

    /** 更新作品组全部源行的失败状态与可回显错误。 */
    void markFailure(String taskId, int firstSourceRow, int lastSourceRow, String error, LocalDateTime now);

    /** 按行结果重算计数并结束任务。 */
    void finish(String taskId, SchoolImportTaskStatus status, LocalDateTime now);

    /** 查当前学校账号指定赛事的任务。 */
    List<SchoolImportTask> listOwned(long accountId, long competitionId);

    /** 当前学校是否存在待处理上传或导入任务。 */
    boolean hasPending(long accountId, @Nullable Long competitionId, LocalDateTime now);

    /** 失效超时未完成的文件上传会话。 */
    int expireUploading(LocalDateTime now);
}
