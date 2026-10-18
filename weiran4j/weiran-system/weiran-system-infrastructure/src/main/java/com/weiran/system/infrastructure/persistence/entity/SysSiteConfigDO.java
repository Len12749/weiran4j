package com.weiran.system.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** {@code sys_site_config} 表映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("sys_site_config")
public class SysSiteConfigDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer ident;

    private String configKey;

    private String contents;

    private Instant createdAt;

    private Instant updatedAt;
}
