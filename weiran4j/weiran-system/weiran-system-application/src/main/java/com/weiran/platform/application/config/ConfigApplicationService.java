package com.weiran.platform.application.config;

import com.weiran.framework.error.BizException;
import com.weiran.framework.error.CommonErrors;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.framework.text.Texts;
import com.weiran.platform.api.config.ConfigGroupView;
import com.weiran.platform.api.config.ConfigService;
import com.weiran.platform.api.config.ConfigView;
import com.weiran.platform.api.config.CreateConfigGroupCommand;
import com.weiran.platform.api.config.PublicConfigView;
import com.weiran.platform.api.config.SaveConfigCommand;
import com.weiran.platform.api.config.UpdateConfigGroupCommand;
import com.weiran.platform.domain.config.ConfigGroup;
import com.weiran.platform.domain.config.ConfigGroupRepository;
import com.weiran.platform.domain.config.ConfigRepository;
import com.weiran.platform.domain.config.ConfigType;
import com.weiran.platform.domain.config.JsonSyntax;
import com.weiran.platform.domain.config.SystemConfig;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/** 系统配置用例。 */
public class ConfigApplicationService implements ConfigService {

    private final ConfigRepository configRepository;

    private final ConfigGroupRepository configGroupRepository;

    private final JsonSyntax jsonSyntax;

    /** 构造服务。 */
    public ConfigApplicationService(
            final ConfigRepository configRepository,
            final ConfigGroupRepository configGroupRepository,
            final JsonSyntax jsonSyntax) {
        this.configRepository = configRepository;
        this.configGroupRepository = configGroupRepository;
        this.jsonSyntax = jsonSyntax;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ConfigView> page(final @Nullable String keyword, final PageQuery pageQuery) {
        return this.configRepository.page(Texts.trimToNull(keyword), pageQuery).map(ConfigApplicationService::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfigView> all() {
        return this.configRepository.findAll().stream()
                .map(ConfigApplicationService::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfigGroupView> groups() {
        return this.configGroupRepository.findEnabled().stream()
                .map(item -> new ConfigGroupView(
                        item.key(),
                        item.label(),
                        item.builtin(),
                        !item.builtin() && !this.configRepository.existsByGroupKey(item.key()),
                        item.createdAt()))
                .toList();
    }

    @Override
    @Transactional
    public long createGroup(final CreateConfigGroupCommand command) {
        final String key = command.key().strip();
        final String label = command.label().strip();
        if (key.isEmpty() || !key.matches("[a-z][a-z0-9_-]*")) {
            throw BizException.badRequest("分组编码必须以小写字母开头，且只能包含小写字母、数字、下划线和连字符");
        }
        if (label.isEmpty()) {
            throw BizException.badRequest("分组名称不能为空");
        }
        if (this.configGroupRepository.existsByKey(key)) {
            throw BizException.duplicate("配置分组编码已存在");
        }
        final int sort = this.configGroupRepository.findEnabled().size() + 1;
        return this.configGroupRepository.save(new ConfigGroup(key, label, sort, false, null));
    }

    @Override
    @Transactional
    public void updateGroup(final UpdateConfigGroupCommand command) {
        final String key = command.key().strip();
        final String label = command.label().strip();
        if (label.isEmpty()) {
            throw BizException.badRequest("分组名称不能为空");
        }
        if (this.configGroupRepository.findEnabledByKey(key).isEmpty()) {
            throw new BizException(CommonErrors.NOT_FOUND, "配置分组不存在");
        }
        if (!this.configGroupRepository.updateLabel(key, label)) {
            throw new BizException(CommonErrors.NOT_FOUND, "配置分组不存在");
        }
    }

    @Override
    @Transactional
    public void deleteGroup(final String groupKey) {
        final String key = groupKey.strip();
        final ConfigGroup group =
                this.configGroupRepository.findEnabledByKey(key).orElseThrow(() -> BizException.notFound("配置分组不存在"));
        if (group.builtin()) {
            throw BizException.conflict("内置配置分组不可删除");
        }
        if (this.configRepository.existsByGroupKey(key)) {
            throw BizException.conflict("分组下仍有配置项，不能删除");
        }
        if (!this.configGroupRepository.deleteByKey(key)) {
            throw BizException.notFound("配置分组不存在");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigView get(final long id) {
        return ConfigApplicationService.toView(this.requireConfig(id));
    }

    @Override
    @Transactional
    public long create(final SaveConfigCommand command) {
        this.requireConfigGroup(command.groupKey().strip());
        final String key = command.configKey().strip();
        if (this.configRepository.existsByKey(key)) {
            throw BizException.duplicate("配置键已存在");
        }
        final SystemConfig config = SystemConfig.builder()
                .configKey(key)
                .configValue(command.configValue())
                .configType(ConfigType.of(command.configType()))
                .groupKey(command.groupKey().strip())
                .fieldType(command.fieldType())
                .fieldOptions(Texts.trimToNull(command.fieldOptions()))
                .required(command.required())
                .description(Texts.trimToNull(command.description()))
                .builtin(false)
                .build()
                .validated(this.jsonSyntax);
        return this.configRepository.save(config);
    }

    @Override
    @Transactional
    public void update(final long id, final SaveConfigCommand command) {
        this.requireConfigGroup(command.groupKey().strip());
        final SystemConfig config = this.requireConfig(id);
        final String key = command.configKey().strip();
        final SystemConfig updated = config.withDetails(
                key,
                command.configValue(),
                ConfigType.of(command.configType()),
                command.groupKey().strip(),
                command.fieldType(),
                Texts.trimToNull(command.fieldOptions()),
                command.required(),
                Texts.trimToNull(command.description()),
                this.jsonSyntax);
        if (!config.getConfigKey().equals(key) && this.configRepository.existsByKey(key)) {
            throw BizException.duplicate("配置键已存在");
        }
        this.configRepository.save(updated);
    }

    @Override
    @Transactional
    public void delete(final long id) {
        this.requireConfig(id).ensureDeletable();
        this.configRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicConfigView getPublic(final String configKey) {
        if (!SystemConfig.isPublicKey(configKey)) {
            throw new BizException(CommonErrors.FORBIDDEN, "该配置不允许公开读取");
        }
        // 库的排序规则大小写不敏感，查回来的键可能与请求的大小写不同：以库里的真实键再判一次前缀。
        final SystemConfig config = this.configRepository
                .findByKey(configKey)
                .filter(found -> SystemConfig.isPublicKey(found.getConfigKey()))
                .orElseThrow(() -> BizException.notFound("配置不存在"));
        return new PublicConfigView(config.getConfigKey(), config.getConfigValue());
    }

    private SystemConfig requireConfig(final long id) {
        return this.configRepository.findById(id).orElseThrow(() -> BizException.notFound("配置不存在"));
    }

    private void requireConfigGroup(final String groupKey) {
        if (this.configGroupRepository.findEnabledByKey(groupKey).isEmpty()) {
            throw BizException.badRequest("配置分组不存在或未启用");
        }
    }

    private static ConfigView toView(final SystemConfig config) {
        return new ConfigView(
                config.requireId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getConfigType().value(),
                config.getGroupKey(),
                config.getFieldType(),
                config.getFieldOptions(),
                config.isRequired(),
                config.getDescription(),
                config.isBuiltin(),
                config.getCreatedAt(),
                config.getUpdatedAt());
    }
}
