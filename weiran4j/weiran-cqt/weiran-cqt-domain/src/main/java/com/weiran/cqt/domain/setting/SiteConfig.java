package com.weiran.cqt.domain.setting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * 前台站点配置的组装规则：配置项编号 → 前台键名，以及内容清洗。
 *
 * <p>口径沿用原 FastAPI 实现（{@code routes/product.py} 的 {@code CONFIG_IDENT_MAP}），uniapp 各页面按这些键名读取，
 * 键名与取值形状不得随意改动。
 */
public final class SiteConfig {

    /** 读取的编号下限（含）。 */
    public static final int IDENT_FROM = 1;

    /** 读取的编号上限（含）。 */
    public static final int IDENT_TO = 100;

    /** 公众号：唯一返回数组、且不清洗的配置项。 */
    static final int WECHAT_OFFICIAL_ACCOUNT_IDENT = 23;

    /** 前台键名 → 配置项编号；顺序即返回顺序。 */
    static final Map<String, Integer> IDENT_BY_KEY = SiteConfig.identByKey();

    private static final Map<String, String> LABEL_BY_KEY = SiteConfig.labelByKey();

    /** 公开接口以 HTML 富文本形式返回的键。 */
    private static final Set<String> RICH_TEXT_KEYS = Set.of(
            "guanyuwomen",
            "user_agreement",
            "yinsixieyi",
            "about_us",
            "gongsijieshao",
            "hezuohuoban",
            "dasaijieshao",
            "mianzexieyi",
            "shouhoufuwu",
            "shangwuhezuo",
            "dasaizhangcheng");

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private SiteConfig() {}

    /** 管理端使用的配置项定义，不包含数据内容。 */
    public record Definition(String key, String label, int ident) {}

    /** 返回配置键对应的 ident；未知键返回 {@code null}。 */
    public static @Nullable Integer identForKey(final String key) {
        return SiteConfig.IDENT_BY_KEY.get(key);
    }

    /** 固定管理表单目录，顺序与前台响应一致。 */
    public static List<Definition> definitions() {
        return SiteConfig.IDENT_BY_KEY.entrySet().stream()
                .map(entry -> new Definition(
                        entry.getKey(),
                        SiteConfig.LABEL_BY_KEY.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getValue()))
                .toList();
    }

    /**
     * 按前台键名组装配置。
     *
     * @param contentsByIdent 编号 → 原始内容（来自仓储，缺行的编号不在其中）
     * @return 前台键名 → 取值：富文本键保留 HTML；其它字符串去 HTML 标签与首尾空白；内容为 NULL 或缺行时为 null；
     *     公众号为数组（有值时单元素原值，否则空数组）
     */
    public static Map<String, @Nullable Object> build(final Map<Integer, @Nullable String> contentsByIdent) {
        final Map<String, @Nullable Object> result = new LinkedHashMap<>();
        SiteConfig.IDENT_BY_KEY.forEach((key, ident) -> {
            final String contents = contentsByIdent.get(ident);
            if (ident == SiteConfig.WECHAT_OFFICIAL_ACCOUNT_IDENT) {
                result.put(key, contents == null || contents.isEmpty() ? List.of() : List.of(contents));
            } else if (SiteConfig.RICH_TEXT_KEYS.contains(key)) {
                result.put(key, contents);
            } else {
                result.put(key, contents == null ? null : SiteConfig.stripHtml(contents));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /** 去掉全部 HTML 标签与首尾空白。 */
    static String stripHtml(final String value) {
        return SiteConfig.HTML_TAG.matcher(value).replaceAll("").strip();
    }

    private static Map<String, Integer> identByKey() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("guanyuwomen", 2);
        map.put("user_agreement", 71);
        map.put("yinsixieyi", 3);
        map.put("about_us", 4);
        map.put("dizhi", 7);
        map.put("shouji", 8);
        map.put("weixin", 9);
        map.put("youxiang", 10);
        map.put("gongsijieshao", 15);
        map.put("hezuohuoban", 16);
        map.put("dasaijieshao", 17);
        map.put("mianzexieyi", 18);
        map.put("shouhoufuwu", 19);
        map.put("shangwuhezuo", 20);
        map.put("gongzuoshijian", 21);
        map.put("dasaizhangcheng", 22);
        map.put("gongzhonghao", SiteConfig.WECHAT_OFFICIAL_ACCOUNT_IDENT);
        map.put("certificate_visibility", 67);
        map.put("sheng_certificate_visibility", 68);
        map.put("guo_certificate_visibility", 70);
        map.put("teacher_org_certificate_competition_id", 69);
        map.put("shouyeimage", 66);
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> labelByKey() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("guanyuwomen", "关于我们");
        map.put("user_agreement", "用户协议");
        map.put("yinsixieyi", "隐私协议");
        map.put("about_us", "关于我们（英文）");
        map.put("dizhi", "公司地址");
        map.put("shouji", "联系电话");
        map.put("weixin", "微信号");
        map.put("youxiang", "联系邮箱");
        map.put("gongsijieshao", "公司介绍");
        map.put("hezuohuoban", "合作伙伴");
        map.put("dasaijieshao", "大赛介绍");
        map.put("mianzexieyi", "免责声明");
        map.put("shouhoufuwu", "售后服务");
        map.put("shangwuhezuo", "商务合作");
        map.put("gongzuoshijian", "工作时间");
        map.put("dasaizhangcheng", "大赛章程");
        map.put("gongzhonghao", "公众号图片路径");
        map.put("certificate_visibility", "证书显示设置");
        map.put("sheng_certificate_visibility", "省级证书显示设置");
        map.put("teacher_org_certificate_competition_id", "教师/机构证书赛事编号");
        map.put("guo_certificate_visibility", "国家级证书显示设置");
        map.put("shouyeimage", "首页图片路径");
        return Collections.unmodifiableMap(map);
    }
}
