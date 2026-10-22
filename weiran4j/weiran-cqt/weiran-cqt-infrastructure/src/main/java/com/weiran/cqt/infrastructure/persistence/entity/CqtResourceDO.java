package com.weiran.cqt.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.weiran.framework.persistence.AuditableDO;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** 指导资料表映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("cqt_resources")
public class CqtResourceDO extends AuditableDO {

    private String title;
    private String category;
    private String materialUrl;
    private String content;
    private Boolean enabled;
    private Integer sort;
}
