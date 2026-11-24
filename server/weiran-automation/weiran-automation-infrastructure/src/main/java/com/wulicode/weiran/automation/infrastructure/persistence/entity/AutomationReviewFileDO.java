package com.wulicode.weiran.automation.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wulicode.weiran.framework.persistence.AuditableDO;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** {@code automation_review_file} 表映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("automation_review_file")
public class AutomationReviewFileDO extends AuditableDO {

    private Long reviewId;

    private String path;

    private Integer addLines;

    private Integer delLines;

    private String level;

    private String commentText;

    private String skipReason;
}
