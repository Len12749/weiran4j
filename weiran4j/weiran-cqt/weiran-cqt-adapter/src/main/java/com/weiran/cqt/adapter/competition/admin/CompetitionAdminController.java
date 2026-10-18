package com.weiran.cqt.adapter.competition.admin;

import com.weiran.cqt.api.competition.admin.CompetitionAdminService;
import com.weiran.cqt.api.competition.admin.CompetitionAdminView;
import com.weiran.cqt.api.file.FileUploadService;
import com.weiran.cqt.api.file.UploadedFileView;
import com.weiran.cqt.domain.file.UploadedFiles;
import com.weiran.framework.auth.CurrentUser;
import com.weiran.framework.auth.RequiresPermission;
import com.weiran.framework.error.BizException;
import com.weiran.framework.log.OperationLog;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cqt/competitions")
public class CompetitionAdminController {
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private final CompetitionAdminService service;
    private final FileUploadService fileUploadService;

    public CompetitionAdminController(
            final CompetitionAdminService service, final FileUploadService fileUploadService) {
        this.service = service;
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission({"cqt:competition:create", "cqt:competition:update"})
    @OperationLog(module = "赛事管理", description = "上传赛事模板图片")
    public UploadedFileView uploadImage(@RequestParam("file") final MultipartFile file) {
        final String originalName = file.getOriginalFilename();
        if (UploadedFiles.extensionOf(originalName)
                .filter(CompetitionAdminController.IMAGE_EXTENSIONS::contains)
                .isEmpty()) {
            throw BizException.badRequest("仅支持 jpg、jpeg、png、gif 图片");
        }
        if (file.isEmpty()) {
            throw BizException.badRequest("请选择图片");
        }
        if (file.getSize() > CompetitionAdminController.MAX_IMAGE_SIZE) {
            throw BizException.badRequest("图片不能超过 10 MB");
        }
        return this.fileUploadService.upload(
                CurrentUser.require().id(), originalName, file.getSize(), file::getInputStream, file.getContentType());
    }

    @GetMapping
    @RequiresPermission("cqt:competition:list")
    public PageResult<CompetitionAdminView> page(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(required = false) final @Nullable String keyword,
            @RequestParam(required = false) final @Nullable Integer year,
            @RequestParam(required = false) final @Nullable Integer status) {
        return this.service.page(new PageQuery(page, pageSize), keyword, year, status);
    }

    @GetMapping("/{id}")
    @RequiresPermission("cqt:competition:list")
    public CompetitionAdminView get(@PathVariable final long id) {
        return this.service.get(id);
    }

    @PostMapping
    @RequiresPermission("cqt:competition:create")
    @OperationLog(module = "赛事管理", description = "新增赛事")
    public CompetitionAdminService.Result create(
            @RequestBody(required = false) final @Nullable CompetitionAdminRequest command) {
        if (command == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        return this.service.create(command.command());
    }

    @PutMapping("/{id}")
    @RequiresPermission("cqt:competition:update")
    @OperationLog(module = "赛事管理", description = "编辑赛事")
    public CompetitionAdminService.Result update(
            @PathVariable final long id,
            @RequestBody(required = false) final @Nullable CompetitionAdminRequest command) {
        if (command == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        return this.service.update(id, command.command());
    }

    public record StatusRequest(
            @Nullable Integer status, @Nullable Long expectedVersion) {}

    @PostMapping("/{id}/status")
    @RequiresPermission("cqt:competition:status")
    @OperationLog(module = "赛事管理", description = "变更赛事状态")
    public CompetitionAdminService.Result status(
            @PathVariable final long id,
            @RequestBody(required = false) final CompetitionAdminController.@Nullable StatusRequest command) {
        if (command == null || command.status() == null || command.expectedVersion() == null) {
            throw BizException.badRequest("状态和版本不能为空");
        }
        return this.service.status(id, command.status(), command.expectedVersion());
    }
}
