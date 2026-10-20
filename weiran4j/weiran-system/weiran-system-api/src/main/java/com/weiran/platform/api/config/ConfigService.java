package com.weiran.platform.api.config;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** 系统配置服务。 */
public interface ConfigService {

    /** 分页查询，keyword 匹配键与描述。 */
    PageResult<ConfigView> page(@Nullable String keyword, PageQuery pageQuery);

    /** 全量系统配置，用于可视化表单。 */
    List<ConfigView> all();

    /** 查询启用的系统配置分组。 */
    List<ConfigGroupView> groups();

    /** 新增配置分组，返回分组 ID。 */
    long createGroup(CreateConfigGroupCommand command);

    /** 修改配置分组名称，分组编码保持不变。 */
    void updateGroup(UpdateConfigGroupCommand command);

    /** 删除空的自定义配置分组；内置分组或含配置项的分组不可删。 */
    void deleteGroup(String key);

    /** 详情；不存在抛 40400。 */
    ConfigView get(long id);

    /** 新增，返回 ID。 */
    long create(SaveConfigCommand command);

    /** 修改；内置项键不可改。 */
    void update(long id, SaveConfigCommand command);

    /** 删除；内置不可删。 */
    void delete(long id);

    /** 读取公开配置：非 {@code system.site.*} 前缀抛 40300，不存在抛 40400。 */
    PublicConfigView getPublic(String configKey);
}
