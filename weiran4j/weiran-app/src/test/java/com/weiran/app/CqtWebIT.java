package com.weiran.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.weiran.cqt.adapter.portal.PortalAccount;
import com.weiran.cqt.adapter.portal.PortalController;
import com.weiran.cqt.adapter.portal.PortalResult;
import com.weiran.cqt.application.entry.SchoolImportApplicationService;
import com.weiran.cqt.domain.portal.PortalTokenCodec;
import com.weiran.framework.error.BizException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 常青藤前台接口（{@code /api-web/**}）集成测试：响应包络、错误语义、前台令牌，以及站点配置接口。
 *
 * <p>下游测试：放在 {@code com.weiran.app} 包只为继承包级私有的 {@link IntegrationTestSupport}，不改任何上游文件。
 * 测试专用 Controller 用类上的 {@code @Import} 注册——{@code @SpringBootTest} 显式指定了启动类，嵌套的
 * {@code @TestConfiguration} 不会被发现。
 */
@Import(CqtWebIT.TestPortalController.class)
class CqtWebIT extends CqtIntegrationTestSupport {

    private static final List<String> CONFIG_KEYS = List.of(
            "guanyuwomen",
            "user_agreement",
            "yinsixieyi",
            "about_us",
            "dizhi",
            "shouji",
            "weixin",
            "youxiang",
            "gongsijieshao",
            "hezuohuoban",
            "dasaijieshao",
            "mianzexieyi",
            "shouhoufuwu",
            "shangwuhezuo",
            "gongzuoshijian",
            "dasaizhangcheng",
            "gongzhonghao",
            "certificate_visibility",
            "sheng_certificate_visibility",
            "guo_certificate_visibility",
            "teacher_org_certificate_competition_id",
            "shouyeimage");

    @Autowired
    PortalTokenCodec portalTokenCodec;

    @Autowired
    SchoolImportApplicationService schoolImportService;

    @BeforeEach
    void resetSettings() {
        this.jdbc.update("UPDATE sys_config SET config_value = '' WHERE config_key LIKE 'system.cqt_site.%'");
        this.jdbc.update("DELETE FROM cqt_news");
        this.jdbc.update("DELETE FROM cqt_experts");
        this.jdbc.update("DELETE FROM cqt_showcases");
        this.jdbc.update("DELETE FROM cqt_resources");
        this.jdbc.update(
                "UPDATE sys_config SET config_value = ? WHERE config_key = 'system.cqt_site.guanyuwomen'",
                "<p>关于我们原文</p>");
        this.jdbc.update(
                "UPDATE sys_config SET config_value = ? WHERE config_key = 'system.cqt_site.user_agreement'",
                "<p>用户协议原文</p>");
        this.jdbc.update(
                "UPDATE sys_config SET config_value = ? WHERE config_key = 'system.cqt_site.dizhi'",
                "<p>北京市 <b>东城区</b></p> ");
        this.jdbc.update(
                "UPDATE sys_config SET config_value = ? WHERE config_key = 'system.cqt_site.gongzhonghao'",
                "/uploads/qr.png");
    }

    @Test
    @DisplayName("getconfig 免登录：富文本保留 HTML、普通文本清洗、缺行为 null、公众号为数组")
    void getConfig() {
        final ResponseEntity<JsonNode> response = this.get("/api-web/n/product/getconfig", null);

        CqtWebIT.assertPortalOk(response);
        final JsonNode data = IntegrationTestSupport.data(response);
        final List<String> keys = new ArrayList<>();
        data.fieldNames().forEachRemaining(keys::add);
        assertThat(keys).containsExactlyInAnyOrderElementsOf(CqtWebIT.CONFIG_KEYS);
        assertThat(data.path("dizhi").asText()).isEqualTo("北京市 东城区");
        assertThat(data.path("weixin").isNull()).isTrue();
        assertThat(data.path("shouji").isNull()).isTrue();
        assertThat(data.path("gongzhonghao").isArray()).isTrue();
        assertThat(data.path("gongzhonghao").get(0).asText()).isEqualTo("/uploads/qr.png");
        assertThat(data.path("guanyuwomen").asText()).isEqualTo("<p>关于我们原文</p>");
        assertThat(data.path("user_agreement").asText()).isEqualTo("<p>用户协议原文</p>");
    }

    @Test
    @DisplayName("系统配置修改站点富文本后，前台读取新值")
    void managesRawSiteConfig() {
        final String admin = this.adminToken();
        final Long id = this.jdbc.queryForObject(
                "SELECT id FROM sys_config WHERE config_key = 'system.cqt_site.guanyuwomen'", Long.class);
        IntegrationTestSupport.assertOk(this.put(
                "/api-backend/system/config/update/" + id,
                admin,
                Map.of(
                        "configKey", "system.cqt_site.guanyuwomen",
                        "configValue", "<div>更新后的介绍</div>",
                        "configType", "string",
                        "groupKey", "cqt_site",
                        "fieldType", "richtext",
                        "required", false)));
        final ResponseEntity<JsonNode> publicResponse = this.get("/api-web/n/product/getconfig", null);
        CqtWebIT.assertPortalOk(publicResponse);
        final JsonNode publicConfig = IntegrationTestSupport.data(publicResponse);
        assertThat(publicConfig.path("guanyuwomen").asText()).isEqualTo("<div>更新后的介绍</div>");
        assertThat(publicConfig.path("user_agreement").asText()).isEqualTo("<p>用户协议原文</p>");
    }

    @Test
    @DisplayName("独立内容公开查询只展示启用项，首页筛选与资源ID隔离")
    void isolatesContentResourcesAndVisibility() {
        final String admin = this.adminToken();
        final Map<String, Object> expert =
                Map.of("name", "评审甲", "image", "/uploads/expert.png", "content", "<p>介绍</p>", "sort", 2);
        final ResponseEntity<JsonNode> created = this.post("/api-backend/cqt/experts/create", admin, expert);
        IntegrationTestSupport.assertOk(created);
        final long id = IntegrationTestSupport.data(created).asLong();
        final ResponseEntity<JsonNode> publicDetail = this.get("/api-web/n/content/experts/" + id, null);
        CqtWebIT.assertPortalOk(publicDetail);
        assertThat(IntegrationTestSupport.data(publicDetail).path("enabled").asBoolean())
                .isTrue();
        assertThat(IntegrationTestSupport.data(publicDetail).path("showOnHome").asBoolean())
                .isTrue();
        final ResponseEntity<JsonNode> home = this.get("/api-web/n/content/experts?homeOnly=true", null);
        CqtWebIT.assertPortalOk(home);
        assertThat(IntegrationTestSupport.data(home).path("list")).hasSize(1);
        assertThat(IntegrationTestSupport.body(this.get("/api-web/n/content/showcases/" + id, null))
                        .path("code")
                        .asInt())
                .isEqualTo(404);
        assertThat(IntegrationTestSupport.body(this.get("/api-web/n/content/resources/" + id, null))
                        .path("code")
                        .asInt())
                .isEqualTo(404);
        final Map<String, Object> disabled = new LinkedHashMap<>(expert);
        disabled.put("enabled", false);
        IntegrationTestSupport.assertOk(this.put("/api-backend/cqt/experts/update/" + id, admin, disabled));
        assertThat(IntegrationTestSupport.body(this.get("/api-web/n/content/experts/" + id, null))
                        .path("code")
                        .asInt())
                .isEqualTo(404);
        assertThat(IntegrationTestSupport.data(this.get("/api-web/n/content/experts", null))
                        .path("list"))
                .isEmpty();
        CqtIntegrationTestSupport.assertError(
                this.post("/api-backend/cqt/experts/create", null, expert), HttpStatus.UNAUTHORIZED, 100101);
        final ResponseEntity<JsonNode> showcase = this.post(
                "/api-backend/cqt/showcases/create",
                admin,
                Map.of("title", "剧本甲", "image", "/uploads/showcase.png", "category", "SCRIPT"));
        IntegrationTestSupport.assertOk(showcase);
        final long showcaseId = IntegrationTestSupport.data(showcase).asLong();
        assertThat(IntegrationTestSupport.data(this.get("/api-web/n/content/showcases/" + showcaseId, null))
                        .path("title")
                        .asText())
                .isEqualTo("剧本甲");
        final ResponseEntity<JsonNode> resource = this.post(
                "/api-backend/cqt/resources/create",
                admin,
                Map.of("title", "稿纸甲", "category", "SCRIPT_PAPER", "materialUrl", "/uploads/paper.pdf"));
        IntegrationTestSupport.assertOk(resource);
        final JsonNode publicResource = IntegrationTestSupport.data(this.get(
                "/api-web/n/content/resources/"
                        + IntegrationTestSupport.data(resource).asLong(),
                null));
        assertThat(publicResource.path("materialUrl").asText()).isEqualTo("/uploads/paper.pdf");
        assertThat(publicResource.has("fileUrl")).isFalse();
        assertThat(publicResource.has("fileName")).isFalse();
        assertThat(IntegrationTestSupport.data(this.get("/api-web/n/content/resources?category=DRAMA_MATERIAL", null))
                        .path("list"))
                .isEmpty();
    }

    @Test
    @DisplayName("内容写入拒绝未知分类、危险URL及缺少必填字段")
    void rejectsInvalidContent() {
        final String admin = this.adminToken();
        CqtIntegrationTestSupport.assertError(
                this.post("/api-backend/cqt/experts/create", admin, Map.of()), HttpStatus.BAD_REQUEST, 100001);
        CqtIntegrationTestSupport.assertError(
                this.post(
                        "/api-backend/cqt/experts/create",
                        admin,
                        Map.of("name", "甲", "image", "javascript:alert(1)", "content", "简介")),
                HttpStatus.BAD_REQUEST,
                100001);
        CqtIntegrationTestSupport.assertError(
                this.post(
                        "/api-backend/cqt/showcases/create",
                        admin,
                        Map.of("title", "甲", "image", "/a.png", "category", "UNKNOWN")),
                HttpStatus.BAD_REQUEST,
                100001);
        CqtIntegrationTestSupport.assertError(
                this.post(
                        "/api-backend/cqt/resources/create",
                        admin,
                        Map.of("title", "甲", "category", "SCRIPT_PAPER", "materialUrl", "//evil.test/a")),
                HttpStatus.BAD_REQUEST,
                100001);
        assertThat(IntegrationTestSupport.body(this.get("/api-web/n/content/resources?category=UNKNOWN", null))
                        .path("code")
                        .asInt())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("动态后台增删改查、首页展示筛选与公开详情保持一致")
    void managesNewsAndServesPortalPages() {
        final String admin = this.adminToken();
        final Map<String, Object> first = Map.of(
                "title", "首页动态",
                "image", "/uploads/news/cover.png",
                "showOnHome", true,
                "sort", 2,
                "content", "<p>正文原文</p>");
        final Map<String, Object> second = Map.of(
                "title", "普通动态",
                "image", "",
                "showOnHome", false,
                "sort", 1,
                "content", "<p>不在首页</p>");

        final ResponseEntity<JsonNode> firstCreated = this.post("/api-backend/cqt/news/create", admin, first);
        IntegrationTestSupport.assertOk(firstCreated);
        final long firstId = IntegrationTestSupport.data(firstCreated).asLong();
        final ResponseEntity<JsonNode> secondCreated = this.post("/api-backend/cqt/news/create", admin, second);
        IntegrationTestSupport.assertOk(secondCreated);
        final long secondId = IntegrationTestSupport.data(secondCreated).asLong();

        final ResponseEntity<JsonNode> adminPage =
                this.get("/api-backend/cqt/news/list?page=1&pageSize=10&keyword=动态", admin);
        IntegrationTestSupport.assertOk(adminPage);
        assertThat(IntegrationTestSupport.data(adminPage).path("total").asLong())
                .isEqualTo(2);

        final ResponseEntity<JsonNode> homePage =
                this.get("/api-web/n/product/news?page=1&pageSize=3&homeOnly=true", null);
        CqtWebIT.assertPortalOk(homePage);
        final JsonNode homeList = IntegrationTestSupport.data(homePage).path("list");
        assertThat(homeList).hasSize(1);
        assertThat(homeList.get(0).path("id").asLong()).isEqualTo(firstId);

        final ResponseEntity<JsonNode> publicPage = this.get("/api-web/n/product/news?page=1&pageSize=10", null);
        CqtWebIT.assertPortalOk(publicPage);
        assertThat(IntegrationTestSupport.data(publicPage).path("list")).hasSize(2);
        final ResponseEntity<JsonNode> detail = this.get("/api-web/n/product/newsdetail?id=" + firstId, null);
        CqtWebIT.assertPortalOk(detail);
        assertThat(IntegrationTestSupport.data(detail).path("content").asText()).isEqualTo("<p>正文原文</p>");

        final Map<String, Object> updated =
                Map.of("title", "更新后动态", "image", "", "showOnHome", false, "sort", 0, "content", "<p>已更新</p>");
        IntegrationTestSupport.assertOk(this.put("/api-backend/cqt/news/update/" + firstId, admin, updated));
        final ResponseEntity<JsonNode> emptyHome =
                this.get("/api-web/n/product/news?page=1&pageSize=3&homeOnly=true", null);
        CqtWebIT.assertPortalOk(emptyHome);
        assertThat(IntegrationTestSupport.data(emptyHome).path("total").asLong())
                .isZero();

        IntegrationTestSupport.assertOk(this.delete("/api-backend/cqt/news/delete/" + secondId, admin));
        CqtIntegrationTestSupport.assertError(
                this.get("/api-backend/cqt/news/get/" + secondId, admin), HttpStatus.NOT_FOUND, 100002);
        CqtIntegrationTestSupport.assertError(
                this.post("/api-backend/cqt/news/create", null, first), HttpStatus.UNAUTHORIZED, 100101);
        CqtIntegrationTestSupport.assertError(
                this.post(
                        "/api-backend/cqt/news/create",
                        admin,
                        Map.of("title", " ", "image", "", "showOnHome", false, "sort", 0, "content", "x")),
                HttpStatus.BAD_REQUEST,
                100001);
    }

    @Test
    @DisplayName("动态首图上传校验图片扩展名并返回文件地址")
    void uploadsNewsImage() {
        final String admin = this.adminToken();
        CqtIntegrationTestSupport.assertError(this.uploadNewsImage(admin, "cover.svg"), HttpStatus.BAD_REQUEST, 100001);
        final ResponseEntity<JsonNode> response = this.uploadNewsImage(admin, "cover.png");
        IntegrationTestSupport.assertOk(response);
        assertThat(IntegrationTestSupport.data(response).path("url").asText())
                .startsWith("/uploads/cqt/user_files/")
                .endsWith(".png");
    }

    @Test
    @DisplayName("学校导入模板可公开下载，任务查询要求登录且只允许学校账号")
    void schoolImportTemplateAndAccessBoundary() {
        final ResponseEntity<JsonNode> templateLink = this.get("/api-web/n/auth/getexinfo", null);
        CqtIntegrationTestSupport.assertPortalOk(templateLink);
        final String templateUrl = IntegrationTestSupport.data(templateLink).asText();
        assertThat(templateUrl).isEqualTo("/api-web/n/cqt-import/template.xlsx");

        final ResponseEntity<byte[]> template = this.rest.getForEntity(templateUrl, byte[].class);
        assertThat(template.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(template.getHeaders().getContentType())
                        .toString())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        final byte[] templateBytes = Objects.requireNonNull(template.getBody());
        assertThat(templateBytes[0]).isEqualTo((byte) 'P');
        assertThat(templateBytes[1]).isEqualTo((byte) 'K');

        CqtIntegrationTestSupport.assertPortalError(
                this.get("/api-web/competition/checksignup", null), 401, "请求参数缺token");

        final String phone = this.registerPersonal();
        final String token = this.portalToken(phone, CqtIntegrationTestSupport.PASSWORD);
        CqtIntegrationTestSupport.assertPortalError(
                this.get("/api-web/competition/checksignup", token), 400, "仅学校账号可使用批量导入");
    }

    @Test
    @DisplayName("学校通过 portal 上传并导入作品、附件和逐行结果，另一学校不能读取任务")
    void importsSchoolWorkbookAndEnforcesTaskOwnership() throws Exception {
        final long competitionId = 9302;
        this.jdbc.update(
                "INSERT IGNORE INTO cqt_competitions (id, legacy_id, name, edition, year, status, national_entry_mode)"
                        + " VALUES (?, 9302, '导入集成赛事', '9302', 2026, 1, 'SHARED')",
                competitionId);
        this.jdbc.update("INSERT IGNORE INTO cqt_competition_categories"
                + " (legacy_id, parent_legacy_id, name, sort_order, status, attachment_required)"
                + " VALUES (93001, 0, '导入戏剧表演', 1, 1, 0), (93002, 93001, '导入话剧', 1, 1, 0)");
        this.jdbc.update(
                "INSERT IGNORE INTO cqt_competition_groups"
                        + " (competition_id, first_category_legacy_id, second_category_legacy_id, name, status)"
                        + " VALUES (?, 93001, 93002, '导入初中组', 1)",
                competitionId);

        final String schoolPhone = this.registerSchool("导入测试学校");
        final long schoolAccountId = this.accountId(schoolPhone);
        this.jdbc.update("UPDATE cqt_portal_accounts SET audit_status = 0 WHERE id = ?", schoolAccountId);
        final String schoolToken = this.portalToken(schoolPhone, CqtIntegrationTestSupport.PASSWORD);

        final ResponseEntity<JsonNode> zipUpload = this.uploadPortalFile(schoolToken, "works.zip", CqtWebIT.zip());
        CqtWebIT.assertPortalOk(zipUpload);
        final String zipUrl = IntegrationTestSupport.data(zipUpload).path("url").asText();
        final ResponseEntity<JsonNode> excelUpload = this.uploadPortalFile(
                schoolToken,
                "school.xlsx",
                CqtWebIT.workbook(
                        "导入学生",
                        CqtIntegrationTestSupport.uniqueIdCard(),
                        "导入学生二",
                        CqtIntegrationTestSupport.uniqueIdCard(),
                        "导入作品"));
        CqtWebIT.assertPortalOk(excelUpload);
        final String excelUrl =
                IntegrationTestSupport.data(excelUpload).path("url").asText();

        final JsonNode zipSession = IntegrationTestSupport.data(this.post(
                "/api-web/dramacompetition/uploadAttachmentZiposs", schoolToken, Map.of("oss_zip_url", zipUrl)));
        final String sessionId = zipSession.path("session_id").asText();
        final JsonNode excelSession = IntegrationTestSupport.data(this.post(
                "/api-web/dramacompetition/uploadexceloss",
                schoolToken,
                Map.of("session_id", sessionId, "oss_excel_url", excelUrl)));
        final String excelId = excelSession.path("excel_id").asText();
        final ResponseEntity<JsonNode> confirmation = this.post(
                "/api-web/dramacompetition/confirmImportoss",
                schoolToken,
                Map.of(
                        "type",
                        "school",
                        "competitionid",
                        competitionId,
                        "session_id",
                        sessionId,
                        "excel_id",
                        excelId,
                        "userid",
                        Long.MAX_VALUE,
                        "zip_name",
                        "works.zip",
                        "excel_name",
                        "school.xlsx"));
        CqtWebIT.assertPortalOk(confirmation);
        final String taskId =
                IntegrationTestSupport.data(confirmation).path("task_id").asText();

        assertThat(IntegrationTestSupport.data(confirmation).path("job_id").asText())
                .isEqualTo(taskId);
        final JsonNode queuedProgress = IntegrationTestSupport.data(
                this.get("/api-web/dramacompetition/importProgress/" + taskId, schoolToken));
        assertThat(queuedProgress.path("state").asText()).isEqualTo("QUEUED");
        assertThat(queuedProgress.path("progress").asInt()).isZero();
        this.schoolImportService.processNext();
        final JsonNode finalResult =
                IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importResult/" + taskId, schoolToken));
        assertThat(finalResult.path("state").asText()).isEqualTo("COMPLETED");
        assertThat(finalResult.path("progress").asInt()).isEqualTo(100);
        assertThat(finalResult.path("success_rows")).hasSize(2);
        assertThat(finalResult.path("failed_rows")).isEmpty();

        final ResponseEntity<JsonNode> detailResponse = this.get(
                "/api-web/competition/getTaskDetail?Competitionid=" + competitionId + "&id=" + taskId, schoolToken);
        CqtWebIT.assertPortalOk(detailResponse);
        final JsonNode detail = IntegrationTestSupport.data(detailResponse);
        assertThat(detail.path("status").asInt()).isEqualTo(2);
        assertThat(detail.path("total_rows").asInt()).isEqualTo(2);
        assertThat(detail.path("success_count").asInt()).isEqualTo(2);
        assertThat(detail.path("results")).hasSize(2);
        assertThat(detail.path("results").get(0).path("status").asInt()).isEqualTo(1);
        assertThat(detail.path("results").get(0).path("row_number").asInt()).isEqualTo(3);
        assertThat(detail.path("results").get(1).path("status").asInt()).isEqualTo(1);
        assertThat(detail.path("results").get(1).path("row_number").asInt()).isEqualTo(4);
        assertThat(detail.path("results").get(0).path("product_id").asLong())
                .isEqualTo(detail.path("results").get(1).path("product_id").asLong());

        final Map<String, Object> entry =
                this.jdbc.queryForMap("SELECT id, attachment_type, attachment_url, attachment_name, stage_scope"
                        + " FROM cqt_entries WHERE title = '导入作品'");
        assertThat(entry).containsEntry("attachment_type", "FILE").containsEntry("attachment_name", "作品.pdf");
        assertThat(Objects.requireNonNull(entry.get("attachment_url")).toString())
                .contains("/user_files/" + schoolAccountId + "/");
        assertThat(entry).containsEntry("stage_scope", "BOTH");
        final long entryId = Objects.requireNonNull((Number) entry.get("id")).longValue();
        assertThat(this.jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cqt_entry_participants WHERE entry_id = ?", Integer.class, entryId))
                .isEqualTo(2);

        final String expiringSession = IntegrationTestSupport.data(this.post(
                        "/api-web/dramacompetition/uploadAttachmentZiposs", schoolToken, Map.of("oss_zip_url", zipUrl)))
                .path("session_id")
                .asText();
        this.jdbc.update(
                "UPDATE cqt_school_import_tasks SET upload_expires_at = DATE_SUB(NOW(), INTERVAL 1 MINUTE) WHERE id = ?",
                expiringSession);
        final ResponseEntity<JsonNode> pending =
                this.get("/api-web/competition/checksignup?competitionid=" + competitionId, schoolToken);
        CqtWebIT.assertPortalOk(pending);
        assertThat(IntegrationTestSupport.data(pending).asBoolean()).isFalse();
        assertThat(this.jdbc.queryForObject(
                        "SELECT status FROM cqt_school_import_tasks WHERE id = ?", String.class, expiringSession))
                .isEqualTo("FAILED");
        CqtIntegrationTestSupport.assertPortalError(
                this.post(
                        "/api-web/dramacompetition/uploadexceloss",
                        schoolToken,
                        Map.of("session_id", expiringSession, "oss_excel_url", excelUrl)),
                400,
                "上传会话不存在、已失效或不属于当前学校");

        final String otherSchoolPhone = this.registerSchool("另一所测试学校");
        final long otherAccountId = this.accountId(otherSchoolPhone);
        this.jdbc.update("UPDATE cqt_portal_accounts SET audit_status = 0 WHERE id = ?", otherAccountId);
        final String otherToken = this.portalToken(otherSchoolPhone, CqtIntegrationTestSupport.PASSWORD);
        CqtIntegrationTestSupport.assertPortalError(
                this.get(
                        "/api-web/competition/getTaskDetail?Competitionid=" + competitionId + "&id=" + taskId,
                        otherToken),
                404,
                "导入记录不存在");
    }

    private String registerSchool(final String school) {
        final String phone = CqtIntegrationTestSupport.uniquePhone();
        final Map<String, Object> form = new LinkedHashMap<>();
        form.put("type", "2");
        form.put("name", school);
        form.put("school", school);
        form.put("contact", "王老师");
        form.put("phone", phone);
        form.put("code", this.smsCode(phone));
        form.put("schoolid", "TEST-" + phone);
        form.put("email", "school@example.com");
        form.put("zhizhao", "/uploads/license.png");
        form.put("chengnuoshu", "/uploads/commitment.pdf");
        form.put("password", CqtIntegrationTestSupport.PASSWORD);
        form.put("password_confirmation", CqtIntegrationTestSupport.PASSWORD);
        CqtWebIT.assertPortalOk(this.post("/api-web/n/auth/register", null, form));
        return phone;
    }

    private ResponseEntity<JsonNode> uploadPortalFile(final String token, final String name, final byte[] content) {
        final MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return name;
            }
        });
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return this.rest.exchange(
                "/api-web/local-files/upload",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(form, headers),
                JsonNode.class);
    }

    private static byte[] zip() throws Exception {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("作品.pdf"));
            zip.write(new byte[] {1, 2, 3});
            zip.closeEntry();
            zip.finish();
            return bytes.toByteArray();
        }
    }

    private static byte[] workbook(
            final String name,
            final String idCard,
            final String secondName,
            final String secondIdCard,
            final String title)
            throws Exception {
        final List<String> headers = List.of(
                "序号",
                "*团体/个人",
                "*团体人数",
                "*姓名",
                "*身份证号/护照号",
                "*所在省/市/自治区",
                "*所在学校",
                "联系方式",
                "*组别",
                "*赛项",
                "*专业",
                "*作品名称",
                "*作品简介",
                "*作品形式",
                "*附件名称/百度网盘链接",
                "指导教师1",
                "指导教师1身份证号",
                "指导教师2",
                "指导教师2身份证号",
                "渠道标识");
        final List<String> values = List.of(
                "1",
                "团体",
                "2",
                name,
                idCard,
                "北京市",
                "导入测试学校",
                "13900000000",
                "导入初中组",
                "导入戏剧表演",
                "导入话剧",
                title,
                "导入集成测试作品",
                "文件",
                "作品.pdf",
                "王老师",
                "",
                "",
                "",
                "web");
        final List<String> secondValues = List.of(
                "2",
                "团体",
                "2",
                secondName,
                secondIdCard,
                "北京市",
                "导入测试学校",
                "13900000000",
                "导入初中组",
                "导入戏剧表演",
                "导入话剧",
                title,
                "导入集成测试作品",
                "文件",
                "作品.pdf",
                "王老师",
                "",
                "",
                "",
                "web");
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"
                + CqtWebIT.sheetRow(2, headers)
                + CqtWebIT.sheetRow(3, values)
                + CqtWebIT.sheetRow(4, secondValues)
                + "</sheetData></worksheet>";
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(bytes)) {
            CqtWebIT.zipText(
                    zip,
                    "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument."
                            + "spreadsheetml.sheet.main+xml\"/>"
                            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-"
                            + "officedocument."
                            + "spreadsheetml.worksheet+xml\"/>"
                            + "</Types>");
            CqtWebIT.zipText(
                    zip,
                    "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/"
                            + "officeDocument\" Target=\"xl/workbook.xml\"/>"
                            + "</Relationships>");
            CqtWebIT.zipText(
                    zip,
                    "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
                            + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"
                            + "<sheet name=\"报名表\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            CqtWebIT.zipText(
                    zip,
                    "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/"
                            + "worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                            + "</Relationships>");
            CqtWebIT.zipText(zip, "xl/worksheets/sheet1.xml", xml);
            zip.finish();
            return bytes.toByteArray();
        }
    }

    private static String sheetRow(final int row, final List<String> values) {
        final StringBuilder xml = new StringBuilder("<row r=\"").append(row).append("\">");
        for (int index = 0; index < values.size(); index++) {
            int column = index;
            final StringBuilder reference = new StringBuilder();
            do {
                reference.insert(0, (char) ('A' + column % 26));
                column = column / 26 - 1;
            } while (column >= 0);
            xml.append("<c r=\"")
                    .append(reference)
                    .append(row)
                    .append("\" t=\"inlineStr\"><is><t>")
                    .append(CqtWebIT.xmlEscape(values.get(index)))
                    .append("</t></is></c>");
        }
        return xml.append("</row>").toString();
    }

    private static String xmlEscape(final String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static void zipText(final ZipOutputStream zip, final String name, final String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private ResponseEntity<JsonNode> uploadNewsImage(
            final @org.jspecify.annotations.Nullable String token, final String fileName) {
        return this.uploadImage("/api-backend/cqt/news/uploadImage", token, fileName);
    }

    private ResponseEntity<JsonNode> uploadImage(
            final String path, final @org.jspecify.annotations.Nullable String token, final String fileName) {
        final MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(new byte[] {1, 2, 3}) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return this.rest.exchange(
                path, HttpMethod.POST, new org.springframework.http.HttpEntity<>(form, headers), JsonNode.class);
    }

    @Test
    @DisplayName("旧站点配置表已迁移并删除")
    void settingTableRemoved() {
        final Integer count = this.jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'cqt_setting'",
                Integer.class);
        assertThat(count).isZero();
        final Integer configs = this.jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_config WHERE config_key LIKE 'system.cqt_site.%'", Integer.class);
        assertThat(configs).isEqualTo(22);
    }

    @Test
    @DisplayName("需登录接口：无令牌 / 无效令牌 / 账号不存在 / 版本过时 → code 401；有效令牌 → 取得账号 ID")
    void requiresPortalToken() {
        CqtWebIT.assertPortalError(this.get("/api-web/__test/me", null), 401, "请求参数缺token");
        CqtWebIT.assertPortalError(this.get("/api-web/__test/me", "not-a-jwt"), 401, "登录失效,请重新登录");
        CqtWebIT.assertPortalError(
                this.get("/api-web/__test/me", this.portalTokenCodec.issue(Long.MAX_VALUE, 0)), 401, "登录失效,请重新登录");

        final String phone = this.registerPersonal();
        final long accountId = this.accountId(phone);
        CqtWebIT.assertPortalError(
                this.get("/api-web/__test/me", this.portalTokenCodec.issue(accountId, 1)), 401, "登录失效,请重新登录");

        final ResponseEntity<JsonNode> response =
                this.get("/api-web/__test/me", this.portalToken(phone, CqtIntegrationTestSupport.PASSWORD));
        CqtWebIT.assertPortalOk(response);
        assertThat(IntegrationTestSupport.data(response).asLong()).isEqualTo(accountId);
    }

    @Test
    @DisplayName("公开接口：无令牌可访问，带令牌也不解析账号；登录后前缀不允许匿名访问")
    void publicEndpointDoesNotResolveAccount() {
        final ResponseEntity<JsonNode> anonymous = this.get("/api-web/n/__test/public-me", null);
        CqtWebIT.assertPortalOk(anonymous);
        assertThat(IntegrationTestSupport.data(anonymous).asLong()).isEqualTo(-1L);

        final String phone = this.registerPersonal();
        final ResponseEntity<JsonNode> signedIn =
                this.get("/api-web/n/__test/public-me", this.portalToken(phone, CqtIntegrationTestSupport.PASSWORD));
        CqtWebIT.assertPortalOk(signedIn);
        assertThat(IntegrationTestSupport.data(signedIn).asLong()).isEqualTo(-1L);
        CqtWebIT.assertPortalOk(this.get("/api-web/n/__test/public-me", "not-a-jwt"));
    }

    @Test
    @DisplayName("前后台令牌互不通用")
    void portalAndAdminTokensAreSeparate() {
        final String portalToken = this.portalToken(this.registerPersonal(), CqtIntegrationTestSupport.PASSWORD);
        IntegrationTestSupport.assertError(
                this.get("/api-backend/system/auth/getCurrentUser", portalToken), HttpStatus.UNAUTHORIZED, 100101);

        CqtWebIT.assertPortalError(this.get("/api-web/__test/me", this.adminToken()), 401, "登录失效,请重新登录");
    }

    @Test
    @DisplayName("后台接口错误语义不变：无令牌 → HTTP 401 + 100101")
    void adminErrorsUnchanged() {
        IntegrationTestSupport.assertError(
                this.get("/api-backend/system/auth/getCurrentUser", null), HttpStatus.UNAUTHORIZED, 100101);
    }

    @Test
    @DisplayName("业务异常折成三位码；参数缺失与类型不匹配为 400；未预期异常为 500 且不泄露细节")
    void errorSemantics() {
        CqtWebIT.assertPortalError(this.get("/api-web/n/__test/not-found", null), 404, "资源不存在");

        final ResponseEntity<JsonNode> missing = this.get("/api-web/n/__test/required", null);
        CqtWebIT.assertPortalError(missing, 400, "page: 不能为空");
        final ResponseEntity<JsonNode> mismatch = this.get("/api-web/n/__test/required?page=abc", null);
        CqtWebIT.assertPortalError(mismatch, 400, "page: 参数类型不正确");

        final ResponseEntity<JsonNode> boom = this.get("/api-web/n/__test/boom", null);
        CqtWebIT.assertPortalError(boom, 500, "服务器内部错误");
        assertThat(IntegrationTestSupport.body(boom).toString()).doesNotContain("internal-detail");
    }

    /** 只存在于测试 classpath 的前台接口，用来覆盖无法用真实业务接口触发的分支。 */
    @PortalController
    @RequestMapping("/api-web")
    static class TestPortalController {

        @GetMapping("/__test/me")
        PortalResult<Long> me() {
            return PortalResult.ok(PortalAccount.current().orElseThrow());
        }

        @GetMapping("/n/__test/public-me")
        PortalResult<Long> publicMe() {
            return PortalResult.ok(PortalAccount.current().orElse(-1L));
        }

        @GetMapping("/n/__test/not-found")
        PortalResult<Void> notFound() {
            throw BizException.notFound("资源不存在");
        }

        @GetMapping("/n/__test/required")
        PortalResult<Integer> required(@RequestParam final int page) {
            return PortalResult.ok(page);
        }

        @GetMapping("/n/__test/boom")
        PortalResult<Void> boom() {
            throw new IllegalStateException("internal-detail");
        }
    }
}
