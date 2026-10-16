package com.weiran.cqt.adapter.portal;

import com.weiran.cqt.api.entry.SchoolImportService;
import com.weiran.cqt.api.entry.SchoolImportTaskView;
import com.weiran.framework.error.BizException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** 学校报名表批量导入接口，兼容 uniapp 既有参数与响应字段。 */
@PortalController
public class SchoolImportController {

    private static final String XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final SchoolImportService service;

    /** 构造学校导入接口。 */
    public SchoolImportController(final SchoolImportService service) {
        this.service = service;
    }

    /** 返回不含示例数据及奖项列的空白模板。 */
    @PortalPublic
    @GetMapping("/api-web/cqt-import/template.xlsx")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(SchoolImportController.XLSX_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=school-entry-template.xlsx")
                .body(this.service.templateWorkbook());
    }

    /** 旧页面模板链接接口。 */
    @PortalPublic
    @GetMapping("/api-web/auth/getexinfo")
    public PortalResult<String> templateUrl() {
        return PortalResult.ok("/api-web/cqt-import/template.xlsx");
    }

    /** 查询学校是否存在待处理任务。 */
    @GetMapping("/api-web/competition/checksignup")
    public PortalResult<Boolean> checkSignup(
            @RequestParam(value = "competitionid", required = false) final @Nullable String competitionId) {
        return PortalResult.ok(this.service.checkSignup(
                PortalAccount.current().orElseThrow(), PortalParams.longOrNull(competitionId)));
    }

    /** 学校已导入作品。 */
    @GetMapping("/api-web/competcategory/schoolproductlists")
    public PortalResult<List<Map<String, @Nullable Object>>> products(
            @RequestParam(value = "competitionid", required = false) final @Nullable String competitionId) {
        return PortalResult.ok(this.service.schoolProducts(
                PortalAccount.current().orElseThrow(), PortalParams.longOrNull(competitionId)));
    }

    /** 登记 ZIP 上传对象；当前 uniapp 只发送 oss_zip_url。 */
    @PostMapping("/api-web/dramacompetition/uploadAttachmentZiposs")
    public PortalResult<Map<String, Object>> registerZip(@RequestBody final Map<String, Object> body) {
        final String id = this.service.registerZip(
                PortalAccount.current().orElseThrow(), this.requiredText("oss_zip_url", body.get("oss_zip_url")), null);
        return PortalResult.ok(Map.of("session_id", id));
    }

    /** 登记 Excel 上传对象并返回旧页面使用的 excel_id。 */
    @PostMapping("/api-web/dramacompetition/uploadexceloss")
    public PortalResult<Map<String, Object>> registerExcel(@RequestBody final Map<String, Object> body) {
        final String id = this.service.registerExcel(
                PortalAccount.current().orElseThrow(),
                this.requiredText("session_id", body.get("session_id")),
                this.requiredText("oss_excel_url", body.get("oss_excel_url")),
                PortalParams.text(body.get("excel_name")));
        return PortalResult.ok(Map.of("excel_id", id));
    }

    /** 确认上传并排队导入；userid 仅为旧客户端字段，不参与身份判断。 */
    @PostMapping("/api-web/dramacompetition/confirmImportoss")
    public PortalResult<Map<String, Object>> confirm(@RequestBody final Map<String, Object> body) {
        final long competitionId = this.requiredLong("competitionid", body.get("competitionid"));
        final String taskId = this.service.confirm(
                PortalAccount.current().orElseThrow(),
                competitionId,
                this.requiredText("session_id", body.get("session_id")),
                this.requiredText("excel_id", body.get("excel_id")),
                PortalParams.text(body.get("type")),
                PortalParams.text(body.get("zip_name")),
                PortalParams.text(body.get("excel_name")));
        return PortalResult.ok(Map.of("task_id", taskId, "competitionid", competitionId, "message", "已提交导入任务"));
    }

    /** 任务列表沿用旧接口的 {@code data.data[]} 包装。 */
    @PostMapping("/api-web/competition/getTaskResult")
    public PortalResult<Map<String, Object>> tasks(@RequestBody final Map<String, Object> body) {
        final long competitionId = this.requiredLong("competitionid", body.get("competitionid"));
        final List<Map<String, @Nullable Object>> values =
                this.service.tasks(PortalAccount.current().orElseThrow(), competitionId).stream()
                        .map(this::taskMap)
                        .toList();
        return PortalResult.ok(Map.of("data", values));
    }

    /** 指定学校任务及逐作品组结果。 */
    @GetMapping("/api-web/competition/getTaskDetail")
    public PortalResult<Map<String, @Nullable Object>> detail(
            @RequestParam("Competitionid") final String competitionId, @RequestParam("id") final String taskId) {
        final SchoolImportTaskView view = this.service.detail(
                PortalAccount.current().orElseThrow(), this.requiredLong("Competitionid", competitionId), taskId);
        final Map<String, @Nullable Object> result = this.taskMap(view);
        result.put("results", view.results());
        return PortalResult.ok(result);
    }

    private Map<String, @Nullable Object> taskMap(final SchoolImportTaskView view) {
        final Map<String, @Nullable Object> result = new LinkedHashMap<>();
        result.put("id", view.id());
        result.put("competitionid", view.competitionId());
        result.put("oss_zip_url", view.zipUrl());
        result.put("zip_name", view.zipName());
        result.put("excel_url", view.excelUrl());
        result.put("excel_name", view.excelName());
        result.put("total_rows", view.totalRows());
        result.put("success_count", view.successCount());
        result.put("failed_count", view.failureCount());
        result.put(
                "status",
                switch (view.status()) {
                    case "UPLOADING", "QUEUED" -> 0;
                    case "PROCESSING" -> 1;
                    case "COMPLETED" -> 2;
                    default -> 3;
                });
        result.put("create_ad", view.createdAt());
        return result;
    }

    private String requiredText(final String field, final @Nullable Object value) {
        final String text = PortalParams.text(value);
        if (text == null || text.isBlank()) {
            throw BizException.badRequest(field + "不能为空");
        }
        return text;
    }

    private long requiredLong(final String field, final @Nullable Object value) {
        final Long number = PortalParams.longValue(field, value);
        if (number == null) {
            throw BizException.badRequest(field + "不能为空");
        }
        return number;
    }
}
