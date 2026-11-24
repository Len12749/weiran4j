package com.wulicode.weiran.automation.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wulicode.weiran.framework.persistence.AuditableDO;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** {@code automation_task} 表映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("automation_task")
public class AutomationTaskDO extends AuditableDO {

    private String code;

    private String name;

    private String description;

    private String triggerDescription;

    private String scheduleDescription;

    private String scheduleKind;

    private Integer intervalSeconds;

    private String outputMode;

    private Integer enabled;

    private LocalDateTime lastRunAt;

    private String lastRunStatus;

    private String lastRunSummary;

    private Long lastRunId;

    private LocalDateTime nextRunAt;

    private LocalDateTime lockedUntil;
}
