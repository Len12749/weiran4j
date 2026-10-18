package com.weiran.system.application.siteconfig;

import com.weiran.framework.error.BizException;
import com.weiran.system.api.siteconfig.SiteConfigItemView;
import com.weiran.system.api.siteconfig.SiteConfigService;
import com.weiran.system.api.siteconfig.SiteConfigValueRequest;
import com.weiran.system.domain.siteconfig.SiteConfigDefinition;
import com.weiran.system.domain.siteconfig.SiteConfigRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/** 站点配置读取和全量保存用例。 */
public class SiteConfigApplicationService implements SiteConfigService {

    private final SiteConfigRepository repository;

    public SiteConfigApplicationService(final SiteConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteConfigItemView> getAll() {
        final Map<Integer, @Nullable String> contents = this.repository.findContents();
        return SiteConfigDefinition.items().stream()
                .map(item -> new SiteConfigItemView(item.key(), item.label(), item.ident(), contents.get(item.ident())))
                .toList();
    }

    @Override
    @Transactional
    public void save(final List<SiteConfigValueRequest> items) {
        if (items == null || items.size() != SiteConfigDefinition.items().size()) {
            throw BizException.badRequest("必须提交全部站点配置项");
        }
        final Set<String> keys = new HashSet<>();
        for (final @Nullable SiteConfigValueRequest item : items) {
            if (item == null || !SiteConfigDefinition.keys().contains(item.key()) || !keys.add(item.key())) {
                throw BizException.badRequest("站点配置项无效或重复");
            }
        }
        if (!keys.equals(SiteConfigDefinition.keys())) {
            throw BizException.badRequest("必须提交全部站点配置项");
        }
        this.repository.lockAndFindContents();
        items.stream()
                .sorted(java.util.Comparator.comparingInt(item -> SiteConfigDefinition.ident(item.key())))
                .forEach(item -> this.repository.saveContents(SiteConfigDefinition.ident(item.key()), item.contents()));
    }
}
