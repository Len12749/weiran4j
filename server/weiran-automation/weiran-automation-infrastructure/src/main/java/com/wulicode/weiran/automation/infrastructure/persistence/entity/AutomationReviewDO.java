package com.wulicode.weiran.automation.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wulicode.weiran.framework.persistence.AuditableDO;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** {@code automation_review} 表映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("automation_review")
public class AutomationReviewDO extends AuditableDO {

    private Long runId;

    private Long taskId;

    private String projectId;

    private Long mergeRequestId;

    private String mergeRequestTitle;

    private String webUrl;

    private String authorName;

    private String sourceBranch;

    private String targetBranch;

    private String status;

    private String commentText;

    private String model;

    private Integer fileCount;

    private Integer addLines;

    private Integer delLines;

    private String errorMessage;
}
