package com.weiran.cqt.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.weiran.framework.persistence.AuditableDO;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** 学术评审团表映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("cqt_experts")
public class CqtExpertDO extends AuditableDO {

    private String name;
    private String image;
    private String content;
    private Boolean showOnHome;
    private Boolean enabled;
    private Integer sort;
}
