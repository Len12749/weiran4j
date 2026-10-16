package com.weiran.cqt.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** {@code cqt_school_import_tasks} 行映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("cqt_school_import_tasks")
public class CqtSchoolImportTaskDO {

    @TableId(type = IdType.INPUT)
    private String id;

    private Long accountId;

    private String schoolId;

    private Long competitionId;

    private String zipUrl;

    private String zipName;

    private String excelUrl;

    private String excelName;

    private String importType;

    private String status;

    private Integer totalRows;

    private Integer processedRows;

    private Integer successCount;

    private Integer failureCount;

    private LocalDateTime leaseUntil;

    private LocalDateTime uploadExpiresAt;

    private Integer attemptCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
