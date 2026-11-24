package com.wulicode.weiran.automation.infrastructure.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wulicode.weiran.automation.domain.task.ChangeRequestSource;
import com.wulicode.weiran.automation.domain.task.ChangeRequestSummary;
import com.wulicode.weiran.automation.domain.task.ChangedFile;
import com.wulicode.weiran.automation.infrastructure.autoconfigure.AutomationProperties;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * 云效 Codeup OpenAPI 客户端：拉取待评审合并请求与变更文件。
 *
 * <p>只做两件事：{@code GET changeRequests}（筛出总评论数为 0 的待评审请求）与
 * {@code GET compares}（拿到文件级 diff）。接口细节以官方文档为准，改动时先核对文档。
 */
public class YunxiaoChangeRequestClient implements ChangeRequestSource {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final AutomationProperties.Cloud properties;

    private final AutomationProperties.Review reviewProperties;

    private final HttpJson http;

    /** 构造云效客户端。 */
    public YunxiaoChangeRequestClient(
            final AutomationProperties properties, final HttpClient client, final ObjectMapper mapper) {
        this.properties = properties.cloud();
        this.reviewProperties = properties.review();
        this.http = new HttpJson(client, mapper);
    }

    @Override
    public List<ChangeRequestSummary> listPendingReviews() {
        final List<ChangeRequestSummary> result = new ArrayList<>();
        final int pageSize = Math.max(this.properties.pageSize(), 1);
        final int maxPages = Math.max(this.properties.maxPages(), 1);
        for (int page = 1; page <= maxPages; page++) {
            final JsonNode rows =
                    this.http.get(this.listUrl(page, pageSize), this.headers(), YunxiaoChangeRequestClient.TIMEOUT);
            if (!rows.isArray() || rows.isEmpty()) {
                break;
            }
            for (final JsonNode row : rows) {
                if (row.path("totalCommentCount").asInt(0) > 0) {
                    continue;
                }
                final ChangeRequestSummary summary = YunxiaoChangeRequestClient.toSummary(row);
                if (this.allowed(summary.repositoryPath())) {
                    result.add(summary);
                }
            }
            if (rows.size() < pageSize) {
                break;
            }
        }
        return result;
    }

    @Override
    public List<ChangedFile> loadChangedFiles(final ChangeRequestSummary summary) {
        final String url = this.apiBase() + "/repositories/" + encode(summary.projectId()) + "/compares?from="
                + encode(summary.targetBranch()) + "&to=" + encode(summary.sourceBranch())
                + "&sourceType=branch&targetType=branch&straight=false";
        final JsonNode root = this.http.get(url, this.headers(), YunxiaoChangeRequestClient.TIMEOUT);
        final List<ChangedFile> files = new ArrayList<>();
        for (final JsonNode diff : root.path("diffs")) {
            files.add(new ChangedFile(
                    diff.path("newPath").asText(diff.path("oldPath").asText("")),
                    diff.hasNonNull("oldPath") ? diff.path("oldPath").asText() : null,
                    0,
                    0,
                    diff.path("isBinary").asBoolean(false),
                    diff.path("deletedFile").asBoolean(false),
                    diff.path("renamedFile").asBoolean(false),
                    diff.path("diff").asText("")));
        }
        // compares 接口不返回逐文件增删行数，这里从 diff 文本里统计，保证展示有数字。
        return files.stream()
                .map(file -> new ChangedFile(
                        file.path(),
                        file.oldPath(),
                        countLines(file.diff(), "+"),
                        countLines(file.diff(), "-"),
                        file.binary(),
                        file.deleted(),
                        file.renamed(),
                        file.diff()))
                .toList();
    }

    /**
     * 只评审白名单内的代码库。
     *
     * <p>白名单为空表示不过滤；配置后，不在名单内的合并请求直接忽略，避免去拉没有权限的仓库。
     */
    private boolean allowed(final String repositoryPath) {
        final String allowPaths = this.reviewProperties.allowPaths();
        if (allowPaths == null || allowPaths.isBlank()) {
            return true;
        }
        for (final String allowed : allowPaths.split(",")) {
            final String prefix = allowed.strip();
            if (!prefix.isEmpty() && repositoryPath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String listUrl(final int page, final int pageSize) {
        final StringBuilder url = new StringBuilder(this.apiBase())
                .append("/changeRequests?page=")
                .append(page)
                .append("&perPage=")
                .append(pageSize)
                .append("&state=opened&orderBy=updated_at&sort=desc");
        return url.toString();
    }

    private String apiBase() {
        final String endpoint = this.properties.endpoint().strip();
        final String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        final String organizationId = this.properties.organizationId();
        if (organizationId == null || organizationId.isBlank()) {
            return base + "/oapi/v1/codeup";
        }
        return base + "/oapi/v1/codeup/organizations/" + encode(organizationId.strip());
    }

    private Map<String, String> headers() {
        return Map.of("x-yunxiao-token", this.properties.token().strip());
    }

    private static ChangeRequestSummary toSummary(final JsonNode row) {
        return new ChangeRequestSummary(
                row.path("projectId").asText(""),
                row.path("localId").asLong(0L),
                row.path("title").asText(""),
                textOrNull(row, "detailUrl"),
                row.hasNonNull("author") ? textOrNull(row.path("author"), "name") : null,
                row.path("sourceBranch").asText(""),
                row.path("targetBranch").asText(""),
                row.path("totalCommentCount").asInt(0),
                YunxiaoChangeRequestClient.repositoryPath(row),
                textOrNull(row, "updatedAt"));
    }

    /** 云效返回的是展示名 {@code qyd / lianmeng / kr / web}，这里还原成仓库路径。 */
    private static String repositoryPath(final JsonNode row) {
        final String withNamespace = row.path("nameWithNamespace").asText("");
        if (!withNamespace.isBlank()) {
            return withNamespace.replace(" ", "");
        }
        // 兜底：从详情地址里截出 host 之后、/change/ 之前的部分。
        final String detailUrl = row.path("detailUrl").asText("");
        final int changeIndex = detailUrl.indexOf("/change/");
        final int schemeIndex = detailUrl.indexOf("//");
        if (changeIndex > 0 && schemeIndex > 0) {
            final int pathStart = detailUrl.indexOf('/', schemeIndex + 2);
            if (pathStart > 0 && pathStart < changeIndex) {
                return detailUrl.substring(pathStart + 1, changeIndex);
            }
        }
        return "";
    }

    private static @Nullable String textOrNull(final JsonNode node, final String field) {
        final JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static int countLines(final String diff, final String prefix) {
        if (diff.isBlank()) {
            return 0;
        }
        int count = 0;
        for (final String line : diff.split("\n")) {
            if (line.startsWith(prefix) && !line.startsWith(prefix + prefix + prefix)) {
                count++;
            }
        }
        return count;
    }
}
