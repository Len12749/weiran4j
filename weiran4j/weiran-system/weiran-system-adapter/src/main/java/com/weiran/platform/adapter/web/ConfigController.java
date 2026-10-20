package com.weiran.platform.adapter.web;

import com.weiran.framework.auth.PublicApi;
import com.weiran.framework.auth.RequiresPermission;
import com.weiran.framework.log.OperationLog;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.framework.response.ApiResponse;
import com.weiran.framework.response.IdResult;
import com.weiran.platform.adapter.web.request.CreateConfigGroupRequest;
import com.weiran.platform.adapter.web.request.SaveConfigRequest;
import com.weiran.platform.adapter.web.request.UpdateConfigGroupRequest;
import com.weiran.platform.api.config.ConfigGroupView;
import com.weiran.platform.api.config.ConfigService;
import com.weiran.platform.api.config.ConfigView;
import com.weiran.platform.api.config.CreateConfigGroupCommand;
import com.weiran.platform.api.config.PublicConfigView;
import com.weiran.platform.api.config.SaveConfigCommand;
import com.weiran.platform.api.config.UpdateConfigGroupCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 系统配置接口 {@code /api/system/config}。 */
@RestController
@RequestMapping("/api/system/config")
public class ConfigController {

    private static final String MODULE = "系统配置";

    private final ConfigService configService;

    /** 构造控制器。 */
    public ConfigController(final ConfigService configService) {
        this.configService = configService;
    }

    /** 分页查询。 */
    @RequiresPermission("system:config:list")
    @GetMapping("/list")
    public PageResult<ConfigView> page(
            @RequestParam(required = false) final @Nullable String keyword,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize) {
        return this.configService.page(keyword, new PageQuery(page, pageSize));
    }

    /** 全量配置，用于生成分组表单。 */
    @RequiresPermission("system:config:list")
    @GetMapping("/listAll")
    public List<ConfigView> all() {
        return this.configService.all();
    }

    /** 系统内置分组目录。 */
    @RequiresPermission("system:config:list")
    @GetMapping("/listGroups")
    public List<ConfigGroupView> groups() {
        return this.configService.groups();
    }

    /** 新增配置分组。 */
    @RequiresPermission("system:config:create")
    @OperationLog(module = ConfigController.MODULE, description = "新增配置分组")
    @PostMapping("/createGroup")
    public IdResult createGroup(@Valid @RequestBody final CreateConfigGroupRequest request) {
        return new IdResult(
                this.configService.createGroup(new CreateConfigGroupCommand(request.key(), request.label())));
    }

    /** 修改配置分组名称。 */
    @RequiresPermission("system:config:update")
    @OperationLog(module = ConfigController.MODULE, description = "修改配置分组")
    @PutMapping("/updateGroup/{key}")
    public ApiResponse<Void> updateGroup(
            @PathVariable final String key, @Valid @RequestBody final UpdateConfigGroupRequest request) {
        this.configService.updateGroup(new UpdateConfigGroupCommand(key, request.label()));
        return ApiResponse.ok();
    }

    /** 删除空的自定义配置分组。 */
    @RequiresPermission("system:config:delete")
    @OperationLog(module = ConfigController.MODULE, description = "删除配置分组")
    @DeleteMapping("/deleteGroup/{key}")
    public ApiResponse<Void> deleteGroup(@PathVariable final String key) {
        this.configService.deleteGroup(key);
        return ApiResponse.ok();
    }

    /** 公开配置（仅 {@code system.site.*}），无需登录。 */
    @PublicApi
    @GetMapping("/getPublic/{key}")
    public PublicConfigView getPublic(@PathVariable final String key) {
        return this.configService.getPublic(key);
    }

    /** 详情。 */
    @RequiresPermission("system:config:list")
    @GetMapping("/get/{id}")
    public ConfigView get(@PathVariable final long id) {
        return this.configService.get(id);
    }

    /** 新增。 */
    @RequiresPermission("system:config:create")
    @OperationLog(module = ConfigController.MODULE, description = "新增配置")
    @PostMapping("/create")
    public IdResult create(@Valid @RequestBody final SaveConfigRequest request) {
        return new IdResult(this.configService.create(ConfigController.toCommand(request)));
    }

    /** 修改。 */
    @RequiresPermission("system:config:update")
    @OperationLog(module = ConfigController.MODULE, description = "修改配置")
    @PutMapping("/update/{id}")
    public ApiResponse<Void> update(@PathVariable final long id, @Valid @RequestBody final SaveConfigRequest request) {
        this.configService.update(id, ConfigController.toCommand(request));
        return ApiResponse.ok();
    }

    /** 删除。 */
    @RequiresPermission("system:config:delete")
    @OperationLog(module = ConfigController.MODULE, description = "删除配置")
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete(@PathVariable final long id) {
        this.configService.delete(id);
        return ApiResponse.ok();
    }

    private static SaveConfigCommand toCommand(final SaveConfigRequest request) {
        return new SaveConfigCommand(
                request.configKey(),
                request.configValue(),
                request.configType(),
                request.groupKey(),
                request.fieldType(),
                request.fieldOptions(),
                request.required(),
                request.description());
    }
}
