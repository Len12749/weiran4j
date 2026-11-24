package com.wulicode.weiran.automation.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wulicode.weiran.framework.persistence.AuditableDO;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** {@code automation_task_run} 表映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("automation_task_run")
public class AutomationTaskRunDO extends AuditableDO {

    private Long taskId;

    private String triggerType;

    private String outputMode;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Long durationMs;

    private String summary;

    private String errorMessage;

    private Integer mergeRequestCount;

    private Integer reviewedCount;

    private Integer commentCount;
}
