package com.weiran.cqt.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** {@code cqt_school_import_rows} 行映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("cqt_school_import_rows")
public class CqtSchoolImportRowDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private Integer sourceRow;

    private Integer lastSourceRow;

    private String entryFingerprint;

    private Long entryId;

    private String status;

    private String displayJson;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
