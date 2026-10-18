package com.weiran.cqt.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.weiran.framework.persistence.AuditableDO;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** 佳作欣赏表映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("cqt_showcases")
public class CqtShowcaseDO extends AuditableDO {

    private String title;
    private String image;
    private String category;
    private String author;
    private String teacher;
    private String school;
    private String description;
    private String content;
    private String attachmentUrl;
    private String attachmentName;
    private String videoUrl;
    private Boolean enabled;
    private Integer sort;
}
