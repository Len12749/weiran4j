package com.weiran.cqt.domain.competition.admin;

import com.weiran.framework.error.BizException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** 赛事配置纯领域校验，不抓取外部模板或改变评审阶段。 */
public final class CompetitionConfigurationRules {
    private static final Set<String> CREDENTIAL_KEYS = Set.of(
            "accesskeyid",
            "access_key_id",
            "access_key_secret",
            "secret",
            "token",
            "password",
            "signature",
            "security-token",
            "ossaccesskeyid");

    private CompetitionConfigurationRules() {}

    public static CompetitionConfiguration validate(final CompetitionConfiguration v) {
        final String name = CompetitionConfigurationRules.required(v.name(), 300, "名称");
        final String edition = CompetitionConfigurationRules.optional(v.edition());
        if (edition != null && edition.length() > 50) {
            throw BizException.badRequest("届次超过长度");
        }
        if (v.year() != null && (v.year() < 1900 || v.year() > 9999)) {
            throw BizException.badRequest("年份须为1900至9999");
        }
        if (v.nationalEntryMode() == null || !Set.of("SHARED", "SEPARATE").contains(v.nationalEntryMode())) {
            throw BizException.badRequest("省国赛模式无效");
        }
        final String description = v.description();
        if (description != null && description.getBytes(StandardCharsets.UTF_8).length > 65535) {
            throw BizException.badRequest("说明超过存储长度");
        }
        CompetitionConfigurationRules.time(v.registrationStart());
        CompetitionConfigurationRules.time(v.registrationEnd());
        CompetitionConfigurationRules.time(v.provinceReviewStart());
        CompetitionConfigurationRules.time(v.provinceReviewEnd());
        CompetitionConfigurationRules.time(v.nationalReviewStart());
        CompetitionConfigurationRules.time(v.nationalReviewEnd());
        CompetitionConfigurationRules.time(v.resultPublishTime());
        CompetitionConfigurationRules.window(v.registrationStart(), v.registrationEnd());
        CompetitionConfigurationRules.window(v.provinceReviewStart(), v.provinceReviewEnd());
        CompetitionConfigurationRules.window(v.nationalReviewStart(), v.nationalReviewEnd());
        return new CompetitionConfiguration(
                name,
                edition,
                v.year(),
                description,
                v.nationalEntryMode(),
                v.registrationStart(),
                v.registrationEnd(),
                v.provinceReviewStart(),
                v.provinceReviewEnd(),
                v.nationalReviewStart(),
                v.nationalReviewEnd(),
                v.resultPublishTime(),
                CompetitionConfigurationRules.url(v.provinceCertificateTemplate()),
                CompetitionConfigurationRules.url(v.nationalCertificateTemplate()),
                CompetitionConfigurationRules.url(v.teacherOrgCertificateTemplate()));
    }

    public static void filters(
            final @Nullable String keyword, final @Nullable Integer year, final @Nullable Integer status) {
        if (keyword != null && keyword.length() > 300) {
            throw BizException.badRequest("关键字最多300字符");
        }
        if (year != null && (year < 1900 || year > 9999)) {
            throw BizException.badRequest("年份无效");
        }
        if (status != null && (status < 0 || status > 2)) {
            throw BizException.badRequest("状态只能为0、1、2");
        }
    }

    private static String required(final @Nullable String value, final int max, final String label) {
        final String text = value == null ? "" : value.strip();
        if (text.isEmpty() || text.length() > max) {
            throw BizException.badRequest(label + "不能为空或超过长度");
        }
        return text;
    }

    private static @Nullable String optional(final @Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static void time(final @Nullable LocalDateTime value) {
        if (value != null && (value.getYear() < 1000 || value.getYear() > 9999)) {
            throw BizException.badRequest("计划时间超出存储范围");
        }
    }

    private static void window(final @Nullable LocalDateTime start, final @Nullable LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw BizException.badRequest("开始时间不得晚于结束时间");
        }
    }

    private static @Nullable String url(final @Nullable String raw) {
        if (raw != null && raw.codePoints().anyMatch(Character::isISOControl)) {
            throw BizException.badRequest("模板URL格式无效");
        }
        final String value = CompetitionConfigurationRules.optional(raw);
        if (value == null) {
            return null;
        }
        if (value.length() > 1000) {
            throw BizException.badRequest("模板URL最多1000字符");
        }
        try {
            final URI uri = new URI(value);
            if (value.startsWith("/uploads/")) {
                CompetitionConfigurationRules.localImagePath(uri);
                return value;
            }
            final String scheme = uri.getScheme();
            if (scheme == null
                    || !Set.of("http", "https").contains(scheme.toLowerCase(Locale.ROOT))
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null) {
                throw BizException.badRequest("模板URL必须为不含凭据的绝对http或https地址");
            }
            final String query = uri.getRawQuery();
            if (query != null) {
                for (final String pair : query.split("[&;]", -1)) {
                    final String key = URLDecoder.decode(pair.split("=", 2)[0], StandardCharsets.UTF_8)
                            .strip()
                            .toLowerCase(Locale.ROOT);
                    if (CompetitionConfigurationRules.CREDENTIAL_KEYS.contains(key)
                            || key.startsWith("x-oss-")
                            || key.startsWith("x-amz-")) {
                        throw BizException.badRequest("模板URL不得包含凭据或签名参数");
                    }
                }
            }
            return value;
        } catch (final URISyntaxException | IllegalArgumentException ex) {
            throw BizException.badRequest("模板URL格式无效");
        }
    }

    private static void localImagePath(final URI uri) {
        if (uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getRawAuthority() != null) {
            throw BizException.badRequest("本地图片路径格式无效");
        }
        final String rawPath = uri.getRawPath();
        if (rawPath == null || !rawPath.startsWith("/uploads/")) {
            throw BizException.badRequest("本地图片路径格式无效");
        }
        String decoded = rawPath;
        while (decoded.contains("%")) {
            final String next = URLDecoder.decode(decoded.replace("+", "%2B"), StandardCharsets.UTF_8);
            if (next.equals(decoded)) {
                break;
            }
            decoded = next;
        }
        if (!decoded.startsWith("/uploads/")
                || decoded.contains("\\")
                || decoded.contains("?")
                || decoded.contains("#")
                || decoded.codePoints().anyMatch(Character::isISOControl)) {
            throw BizException.badRequest("本地图片路径格式无效");
        }
        for (final String segment : decoded.split("/", -1)) {
            if (segment.equals(".") || segment.equals("..")) {
                throw BizException.badRequest("本地图片路径不得包含点段");
            }
        }
    }
}
