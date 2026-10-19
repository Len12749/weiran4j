package com.weiran.platform.domain.config;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/** 系统配置的内置分组目录。新增分组通过代码扩展，不由管理页面创建。 */
@NullMarked
public final class ConfigGroupDefinition {

    public record Item(String key, String label) {}

    private static final List<Item> ITEMS = List.of(new Item("general", "通用设置"), new Item("user", "用户"));

    private ConfigGroupDefinition() {}

    public static List<Item> items() {
        return ITEMS;
    }

    public static Optional<Item> find(final String key) {
        return ITEMS.stream().filter(item -> item.key().equals(key)).findFirst();
    }
}
