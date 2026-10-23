package com.weiran.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.weiran.cqt.domain.entry.SchoolImportRowResult;
import com.weiran.cqt.domain.entry.SchoolImportTask;
import com.weiran.cqt.domain.entry.SchoolImportTaskRepository;
import com.weiran.cqt.domain.entry.SchoolImportTaskStatus;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 学校任务轻量进度、持久计数、终态结果与跨账号隔离。 */
class CqtSchoolImportProgressIT extends CqtIntegrationTestSupport {
    @Autowired
    SchoolImportTaskRepository taskRepository;

    @Autowired
    com.weiran.cqt.application.entry.SchoolImportApplicationService importService;

    @Test
    @DisplayName("进行中计数随行事务更新，部分成功与失败返回真实结果，隔离其它学校")
    void tracksPersistedSourceRowsAndOwnership() {
        final String phone = this.registerSchool();
        final long account = this.accountId(phone);
        final String token = this.portalToken(phone, CqtIntegrationTestSupport.PASSWORD);
        final String taskId = UUID.randomUUID().toString();
        final LocalDateTime now = LocalDateTime.now(java.time.ZoneOffset.UTC).withNano(0);
        this.taskRepository.create(new SchoolImportTask(
                taskId,
                account,
                null,
                2027L,
                null,
                null,
                null,
                null,
                null,
                SchoolImportTaskStatus.PROCESSING,
                0,
                0,
                0,
                0,
                now.plusMinutes(15),
                null,
                1,
                now,
                now,
                null));
        this.taskRepository.insertRows(
                taskId,
                List.of(
                        new SchoolImportRowResult(0, taskId, 3, 3, null, null, "PENDING", "{}", null),
                        new SchoolImportRowResult(0, taskId, 4, 4, null, null, "FAILED", "{}", "赛项无效"),
                        new SchoolImportRowResult(0, taskId, 5, 5, null, null, "PENDING", "{}", null)),
                3,
                now);
        final JsonNode initial =
                IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importProgress/" + taskId, token));
        assertThat(initial.path("progress").asInt()).isEqualTo(33);
        assertThat(initial.path("processed_rows").asInt()).isEqualTo(1);
        assertThat(initial.path("finished").asBoolean()).isFalse();
        assertThat(initial.has("results")).isFalse();
        this.taskRepository.markSuccess(taskId, 3, 3, "fingerprint", 1L, now.plusSeconds(1));
        final JsonNode intermediate =
                IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importProgress/" + taskId, token));
        assertThat(intermediate.path("progress").asInt()).isEqualTo(66);
        assertThat(intermediate.path("success_count").asInt()).isEqualTo(1);
        final JsonNode processingRows =
                IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importResult/" + taskId, token));
        assertThat(processingRows.path("results").get(2).path("status").asInt()).isZero();
        assertThat(processingRows.path("failed_rows")).hasSize(1);
        assertThat(processingRows.path("completed_at").isNull()).isTrue();
        this.taskRepository.markSuccess(taskId, 5, 5, "fingerprint-2", 2L, now.plusSeconds(2));
        assertThat(IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importProgress/" + taskId, token))
                        .path("progress")
                        .asInt())
                .isEqualTo(99);
        this.taskRepository.finish(taskId, SchoolImportTaskStatus.PARTIAL, now.plusSeconds(3));
        final JsonNode completed =
                IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importResult/" + taskId, token));
        assertThat(completed.path("status").asInt()).isEqualTo(2);
        assertThat(completed.path("state").asText()).isEqualTo("PARTIAL");
        assertThat(completed.path("finished").asBoolean()).isTrue();
        assertThat(completed.path("progress").asInt()).isEqualTo(100);
        assertThat(completed.path("success_rows")).hasSize(2);
        assertThat(completed.path("failed_rows")).hasSize(1);
        assertThat(completed.path("completed_at").asText()).isNotBlank();
        this.taskRepository.finish(taskId, SchoolImportTaskStatus.FAILED, now.plusSeconds(4));
        final JsonNode failed =
                IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importResult/" + taskId, token));
        assertThat(failed.path("status").asInt()).isEqualTo(3);
        assertThat(failed.path("failed_rows").get(0).path("error_msg").asText()).isEqualTo("赛项无效");

        final String otherSchool = this.portalToken(this.registerSchool(), CqtIntegrationTestSupport.PASSWORD);
        for (final String endpoint : List.of("importProgress/", "importResult/")) {
            final String path = "/api-web/dramacompetition/" + endpoint;
            CqtIntegrationTestSupport.assertPortalError(this.get(path + taskId, null), 401, "请求参数缺token");
            CqtIntegrationTestSupport.assertPortalError(this.get(path + taskId, otherSchool), 404, "导入记录不存在");
            CqtIntegrationTestSupport.assertPortalError(this.get(path + "missing", token), 404, "导入记录不存在");
        }
        final String student = this.portalToken(this.registerPersonal(), CqtIntegrationTestSupport.PASSWORD);
        CqtIntegrationTestSupport.assertPortalError(
                this.get("/api-web/dramacompetition/importProgress/" + taskId, student), 400, "仅学校账号可使用批量导入");
    }

    @Test
    @DisplayName("排队且总行数未知时进度为0，任务失败不虚构源行")
    void queuedUnknownTotalStaysZero() {
        final String phone = this.registerSchool();
        final long account = this.accountId(phone);
        final String token = this.portalToken(phone, CqtIntegrationTestSupport.PASSWORD);
        final String taskId = UUID.randomUUID().toString();
        final LocalDateTime now = LocalDateTime.now(java.time.ZoneOffset.UTC);
        this.taskRepository.create(new SchoolImportTask(
                taskId,
                account,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SchoolImportTaskStatus.QUEUED,
                0,
                0,
                0,
                0,
                null,
                null,
                0,
                now,
                now,
                null));
        final JsonNode queued =
                IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importProgress/" + taskId, token));
        assertThat(queued.path("progress").asInt()).isZero();
        assertThat(queued.path("finished").asBoolean()).isFalse();
        this.taskRepository.fail(taskId, SchoolImportTaskStatus.FAILED, "文件无法解析", now);
        final JsonNode failed =
                IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importResult/" + taskId, token));
        assertThat(failed.path("progress").asInt()).isEqualTo(100);
        assertThat(failed.path("message").asText()).isEqualTo("文件无法解析");
        assertThat(failed.path("total_rows").asInt()).isZero();
        assertThat(failed.path("results")).isEmpty();
    }

    @Test
    @DisplayName("无ZIP学校经上传确认与worker处理空模板后保留任务级错误且不伪造源行")
    void importsEmptyTemplateThroughApiAndReturnsTaskError() {
        final String phone = this.registerSchool();
        final String token = this.portalToken(phone, CqtIntegrationTestSupport.PASSWORD);
        this.jdbc.update(
                "INSERT IGNORE INTO cqt_competitions (id,legacy_id,name,edition,year,status,national_entry_mode) "
                        + "VALUES (9399,9399,'空模板赛事','9399',2026,1,'SHARED')");
        final org.springframework.util.MultiValueMap<String, Object> form =
                new org.springframework.util.LinkedMultiValueMap<>();
        form.add("file", new org.springframework.core.io.ByteArrayResource(this.importService.templateWorkbook()) {
            @Override
            public String getFilename() {
                return "empty.xlsx";
            }
        });
        final org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        final var upload = this.rest.exchange(
                "/api-web/local-files/upload",
                org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(form, headers),
                JsonNode.class);
        CqtIntegrationTestSupport.assertPortalOk(upload);
        final String url = IntegrationTestSupport.data(upload).path("url").asText();
        final var excel = this.post(
                "/api-web/dramacompetition/uploadexceloss",
                token,
                Map.of("session_id", "3session_id", "oss_excel_url", url));
        CqtIntegrationTestSupport.assertPortalOk(excel);
        final String excelId =
                IntegrationTestSupport.data(excel).path("excel_id").asText();
        final var confirmation = this.post(
                "/api-web/dramacompetition/confirmImportoss",
                token,
                Map.of("competitionid", 9399, "session_id", "3session_id", "excel_id", excelId));
        CqtIntegrationTestSupport.assertPortalOk(confirmation);
        final String taskId =
                IntegrationTestSupport.data(confirmation).path("task_id").asText();
        assertThat(IntegrationTestSupport.data(confirmation).path("job_id").asText())
                .isEqualTo(taskId);
        assertThat(IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importProgress/" + taskId, token))
                        .path("progress")
                        .asInt())
                .isZero();
        assertThat(this.importService.processNext()).isTrue();
        final JsonNode result =
                IntegrationTestSupport.data(this.get("/api-web/dramacompetition/importResult/" + taskId, token));
        assertThat(result.path("state").asText()).isEqualTo("FAILED");
        assertThat(result.path("message").asText()).isEqualTo("报名表没有可导入的作品行");
        assertThat(result.path("total_rows").asInt()).isZero();
        assertThat(result.path("results")).isEmpty();
    }

    private String registerSchool() {
        final String phone = CqtIntegrationTestSupport.uniquePhone();
        final Map<String, Object> form = new LinkedHashMap<>();
        form.put("type", "2");
        form.put("name", "进度测试学校");
        form.put("school", "进度测试学校");
        form.put("contact", "王老师");
        form.put("phone", phone);
        form.put("code", this.smsCode(phone));
        form.put("schoolid", "TEST-" + phone);
        form.put("password", CqtIntegrationTestSupport.PASSWORD);
        form.put("password_confirmation", CqtIntegrationTestSupport.PASSWORD);
        CqtIntegrationTestSupport.assertPortalOk(this.post("/api-web/n/auth/register", null, form));
        return phone;
    }
}
