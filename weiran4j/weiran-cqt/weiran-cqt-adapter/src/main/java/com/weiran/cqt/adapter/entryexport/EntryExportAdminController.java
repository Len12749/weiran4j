package com.weiran.cqt.adapter.entryexport;

import com.weiran.cqt.api.entryexport.EntryExportCommand;
import com.weiran.cqt.api.entryexport.EntryExportService;
import com.weiran.framework.auth.RequiresPermission;
import com.weiran.framework.log.OperationLog;
import com.weiran.framework.web.SkipApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntryExportAdminController {
    private final EntryExportService service;

    public EntryExportAdminController(final EntryExportService service) {
        this.service = service;
    }

    @PostMapping("/api/cqt/review/entries/export")
    @SkipApiResponse
    @RequiresPermission("cqt:entry-review:export")
    @OperationLog(module = "作品初审", description = "导出作品清单")
    public ResponseEntity<byte[]> export(@RequestBody final EntryExportCommand command) {
        final byte[] workbook = this.service.export(command);
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=cqt-entries-" + command.competitionId() + "-" + command.phaseId()
                                + ".xlsx")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(workbook);
    }
}
