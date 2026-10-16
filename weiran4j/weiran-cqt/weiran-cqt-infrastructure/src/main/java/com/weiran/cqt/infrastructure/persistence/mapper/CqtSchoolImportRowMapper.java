package com.weiran.cqt.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weiran.cqt.infrastructure.persistence.entity.CqtSchoolImportRowDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 学校导入作品组结果查询。 */
public interface CqtSchoolImportRowMapper extends BaseMapper<CqtSchoolImportRowDO> {

    /** 任务结果按源行排序。 */
    @Select("SELECT * FROM cqt_school_import_rows WHERE task_id = #{taskId} ORDER BY source_row")
    List<CqtSchoolImportRowDO> findByTask(@Param("taskId") String taskId);

    /** 成功后持久化作品 id 与指纹。 */
    @Update("UPDATE cqt_school_import_rows SET status = 'SUCCESS', entry_id = #{entryId},"
            + " entry_fingerprint = #{fingerprint}, error_message = NULL, updated_at = NOW()"
            + " WHERE task_id = #{taskId} AND source_row BETWEEN #{firstSourceRow} AND #{lastSourceRow}"
            + " AND status = 'PENDING'")
    int markSuccess(
            @Param("taskId") String taskId,
            @Param("firstSourceRow") int firstSourceRow,
            @Param("lastSourceRow") int lastSourceRow,
            @Param("fingerprint") String fingerprint,
            @Param("entryId") long entryId);

    /** 失败后保存脱敏错误摘要。 */
    @Update("UPDATE cqt_school_import_rows SET status = 'FAILED', error_message = #{error}, updated_at = NOW()"
            + " WHERE task_id = #{taskId} AND source_row BETWEEN #{firstSourceRow} AND #{lastSourceRow}"
            + " AND status = 'PENDING'")
    int markFailure(
            @Param("taskId") String taskId,
            @Param("firstSourceRow") int firstSourceRow,
            @Param("lastSourceRow") int lastSourceRow,
            @Param("error") String error);
}
