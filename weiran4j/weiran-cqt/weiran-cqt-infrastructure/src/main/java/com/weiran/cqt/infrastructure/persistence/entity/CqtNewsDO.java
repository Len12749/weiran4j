package com.weiran.cqt.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/** {@code cqt_news} 动态表映射。 */
@NullUnmarked
@Getter
@Setter
@TableName("cqt_news")
public class CqtNewsDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String image;

    private Boolean showOnHome;

    private Integer sort;

    private String content;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
