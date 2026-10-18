package com.weiran.cqt.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weiran.cqt.infrastructure.persistence.entity.CqtSchoolImportTaskDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jspecify.annotations.Nullable;

/** 学校导入任务查询。 */
public interface CqtSchoolImportTaskMapper extends BaseMapper<CqtSchoolImportTaskDO> {

    /** 事务内锁定一个可领取任务。 */
    @Select("SELECT * FROM cqt_school_import_tasks WHERE status = 'QUEUED'"
            + " OR (status = 'PROCESSING' AND lease_until < #{now})"
            + " ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED")
    CqtSchoolImportTaskDO lockNext(@Param("now") LocalDateTime now);

    /** 当前账号拥有的指定会话/任务。 */
    @Select("SELECT * FROM cqt_school_import_tasks WHERE id = #{id} AND account_id = #{accountId}")
    CqtSchoolImportTaskDO findOwned(@Param("id") String id, @Param("accountId") long accountId);

    /** 当前学校指定赛事任务。 */
    @Select("SELECT * FROM cqt_school_import_tasks WHERE account_id = #{accountId}"
            + " AND competition_id = #{competitionId} ORDER BY created_at DESC")
    List<CqtSchoolImportTaskDO> listOwned(
            @Param("accountId") long accountId, @Param("competitionId") long competitionId);

    /** 判断学校是否有待提交或处理中任务。 */
    @Select(
            "SELECT COUNT(*) > 0 FROM cqt_school_import_tasks WHERE account_id = #{accountId}"
                    + " AND ((status = 'UPLOADING' AND upload_expires_at > #{now})"
                    + " OR (status IN ('QUEUED','PROCESSING') AND (#{competitionId} IS NULL OR competition_id = #{competitionId})))")
    boolean hasPending(
            @Param("accountId") long accountId,
            @Param("competitionId") @Nullable Long competitionId,
            @Param("now") LocalDateTime now);

    /** 将过期上传会话标记为失败，便于用户查看并重新提交。 */
    @Update("UPDATE cqt_school_import_tasks SET status = 'FAILED', error_message = '上传会话已过期，请重新上传', updated_at = #{now}"
            + " WHERE status = 'UPLOADING' AND upload_expires_at <= #{now}")
    int expireUploading(@Param("now") LocalDateTime now);

    /** 登记附件并更新时间。 */
    @Update("UPDATE cqt_school_import_tasks SET zip_url = COALESCE(#{zipUrl}, zip_url),"
            + " zip_name = COALESCE(#{zipName}, zip_name), excel_url = COALESCE(#{excelUrl}, excel_url),"
            + " excel_name = COALESCE(#{excelName}, excel_name),"
            + " upload_expires_at = DATE_ADD(#{now}, INTERVAL 2 HOUR), updated_at = #{now}"
            + " WHERE id = #{id} AND account_id = #{accountId} AND status = 'UPLOADING'"
            + " AND upload_expires_at > #{now}")
    int updateUpload(
            @Param("id") String id,
            @Param("accountId") long accountId,
            @Param("zipUrl") @Nullable String zipUrl,
            @Param("zipName") @Nullable String zipName,
            @Param("excelUrl") @Nullable String excelUrl,
            @Param("excelName") @Nullable String excelName,
            @Param("now") LocalDateTime now);

    /** 将上传完成的会话排队；调用两次只允许第一次成功。 */
    @Update("UPDATE cqt_school_import_tasks SET competition_id = #{competitionId}, import_type = #{importType},"
            + " status = 'QUEUED', upload_expires_at = NULL, updated_at = #{now}"
            + " WHERE id = #{id} AND account_id = #{accountId}"
            + " AND status = 'UPLOADING' AND upload_expires_at > #{now} AND excel_url IS NOT NULL")
    int queue(
            @Param("id") String id,
            @Param("accountId") long accountId,
            @Param("competitionId") long competitionId,
            @Param("importType") String importType,
            @Param("now") LocalDateTime now);

    /** 原子更新租约、状态与尝试次数。 */
    @Update("UPDATE cqt_school_import_tasks SET status = 'PROCESSING', lease_until = #{leaseUntil},"
            + " attempt_count = attempt_count + 1, updated_at = #{now} WHERE id = #{id}")
    int claim(@Param("id") String id, @Param("leaseUntil") LocalDateTime leaseUntil, @Param("now") LocalDateTime now);

    /** 更新任务总组数。 */
    @Update("UPDATE cqt_school_import_tasks SET total_rows = #{totalRows}, updated_at = #{now} WHERE id = #{id}")
    int setTotalRows(@Param("id") String id, @Param("totalRows") int totalRows, @Param("now") LocalDateTime now);

    /** 行结果提交时同步真实计数，进度查询无需加载行。 */
    @Update("UPDATE cqt_school_import_tasks t SET updated_at = #{now},"
            + " processed_rows = (SELECT COUNT(*) FROM cqt_school_import_rows r"
            + " WHERE r.task_id = t.id AND r.status IN ('SUCCESS','FAILED')),"
            + " success_count = (SELECT COUNT(*) FROM cqt_school_import_rows r WHERE r.task_id = t.id AND r.status = 'SUCCESS'),"
            + " failure_count = (SELECT COUNT(*) FROM cqt_school_import_rows r WHERE r.task_id = t.id AND r.status = 'FAILED')"
            + " WHERE id = #{id}")
    int syncCounts(@Param("id") String id, @Param("now") LocalDateTime now);

    /** 保存已脱敏的任务错误。 */
    @Update("UPDATE cqt_school_import_tasks SET error_message = #{error} WHERE id = #{id}")
    int setError(@Param("id") String id, @Param("error") String error);

    /** 完成任务并根据行状态同步进度。 */
    @Update("UPDATE cqt_school_import_tasks t SET status = #{status}, lease_until = NULL, updated_at = #{now},"
            + " processed_rows = (SELECT COUNT(*) FROM cqt_school_import_rows r WHERE r.task_id = t.id AND r.status <> 'PENDING'),"
            + " success_count = (SELECT COUNT(*) FROM cqt_school_import_rows r WHERE r.task_id = t.id AND r.status = 'SUCCESS'),"
            + " failure_count = (SELECT COUNT(*) FROM cqt_school_import_rows r WHERE r.task_id = t.id AND r.status = 'FAILED')"
            + " WHERE id = #{id}")
    int finish(@Param("id") String id, @Param("status") String status, @Param("now") LocalDateTime now);
}
