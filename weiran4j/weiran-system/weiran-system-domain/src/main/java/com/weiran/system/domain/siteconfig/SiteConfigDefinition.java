package com.weiran.system.domain.siteconfig;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

/** 固定站点配置目录及键到存储编号的映射。 */
@NullMarked
public final class SiteConfigDefinition {

    public record Item(String key, String label, int ident) {}

    private static final List<Item> ITEMS = List.of(
            new Item("site_introduction", "站点介绍", 1),
            new Item("about", "关于我们", 2),
            new Item("user_agreement", "用户协议", 3),
            new Item("privacy_policy", "隐私政策", 4),
            new Item("disclaimer", "免责声明", 5),
            new Item("contact_address", "联系地址", 6),
            new Item("contact_phone", "联系电话", 7),
            new Item("contact_email", "联系邮箱", 8),
            new Item("working_hours", "工作时间", 9),
            new Item("homepage_image", "首页图片", 10));

    private static final Set<String> KEYS =
            ITEMS.stream().map(Item::key).collect(java.util.stream.Collectors.toUnmodifiableSet());

    private static final Map<String, Integer> IDENTS =
            ITEMS.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Item::key, Item::ident));

    private SiteConfigDefinition() {}

    public static List<Item> items() {
        return ITEMS;
    }

    public static Set<String> keys() {
        return KEYS;
    }

    public static int ident(String key) {
        final Integer ident = IDENTS.get(key);
        if (ident == null) {
            throw new IllegalArgumentException("unknown site config key");
        }
        return ident.intValue();
    }
}
