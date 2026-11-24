package com.wulicode.weiran.automation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wulicode.weiran.automation.infrastructure.persistence.entity.AutomationTaskDO;
import org.apache.ibatis.annotations.Mapper;

/** {@code automation_task} 数据访问。 */
@Mapper
public interface AutomationTaskMapper extends BaseMapper<AutomationTaskDO> {}
