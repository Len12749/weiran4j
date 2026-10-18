package com.weiran.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weiran.system.infrastructure.persistence.entity.SysSiteConfigDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 站点配置 Mapper。 */
@Mapper
public interface SysSiteConfigMapper extends BaseMapper<SysSiteConfigDO> {

    @Select("select * from sys_site_config order by ident asc, id asc for update")
    List<SysSiteConfigDO> lockAll();
}
