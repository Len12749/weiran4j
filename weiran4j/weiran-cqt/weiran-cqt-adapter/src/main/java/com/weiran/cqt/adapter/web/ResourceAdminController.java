package com.weiran.cqt.adapter.web;

import com.weiran.cqt.api.content.resources.ResourceCommand;
import com.weiran.cqt.api.content.resources.ResourceService;
import com.weiran.cqt.api.content.resources.ResourceView;
import com.weiran.cqt.api.file.FileUploadService;
import com.weiran.cqt.api.file.UploadedFileView;
import com.weiran.cqt.domain.file.UploadedFiles;
import com.weiran.framework.auth.RequiresPermission;
import com.weiran.framework.error.BizException;
import com.weiran.framework.log.OperationLog;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.framework.response.ApiResponse;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 指导资料后台管理接口。 */
@RestController
@RequestMapping("/api/cqt/resources")
public class ResourceAdminController {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    private final ResourceService service;

    private final FileUploadService fileUploadService;

    /** 构造控制器。 */
    public ResourceAdminController(final ResourceService service, final FileUploadService fileUploadService) {
        this.service = service;
        this.fileUploadService = fileUploadService;
    }

    /** 分页查询内容。 */
    @RequiresPermission("cqt:resource:list")
    @GetMapping
    public PageResult<ResourceView> page(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(required = false) final @Nullable String keyword,
            @RequestParam(required = false) final @Nullable String category,
            @RequestParam(required = false) final @Nullable Boolean enabled) {
        return this.service.page(new PageQuery(page, pageSize), keyword, category, enabled, false);
    }

    /** 查询内容详情。 */
    @RequiresPermission("cqt:resource:list")
    @GetMapping("/{id}")
    public ResourceView get(@PathVariable("id") final long id) {
        return this.service.get(id);
    }

    /** 新增内容。 */
    @RequiresPermission("cqt:resource:create")
    @OperationLog(module = "指导资料", description = "新增内容")
    @PostMapping
    public long create(@RequestBody(required = false) final @Nullable ResourceCommand command) {
        if (command == null) {
            throw BizException.badRequest("内容内容不能为空");
        }
        return this.service.create(command);
    }

    /** 更新内容。 */
    @RequiresPermission("cqt:resource:update")
    @OperationLog(module = "指导资料", description = "更新内容")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable("id") final long id, @RequestBody(required = false) final @Nullable ResourceCommand command) {
        if (command == null) {
            throw BizException.badRequest("内容内容不能为空");
        }
        this.service.update(id, command);
        return ApiResponse.ok();
    }

    /** 删除内容。 */
    @RequiresPermission("cqt:resource:delete")
    @OperationLog(module = "指导资料", description = "删除内容")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") final long id) {
        this.service.delete(id);
        return ApiResponse.ok();
    }

    /** 上传内容首图；二进制内容不写入操作日志请求体。 */
    @RequiresPermission({"cqt:resource:create", "cqt:resource:update"})
    @OperationLog(module = "指导资料", description = "上传内容首图")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadedFileView uploadImage(@RequestParam("file") final MultipartFile file) {
        final String originalName = file.getOriginalFilename();
        if (UploadedFiles.extensionOf(originalName)
                .filter(ResourceAdminController.IMAGE_EXTENSIONS::contains)
                .isEmpty()) {
            throw BizException.badRequest("仅支持 jpg、jpeg、png、gif 图片");
        }
        if (file.isEmpty()) {
            throw BizException.badRequest("请选择图片");
        }
        if (file.getSize() > ResourceAdminController.MAX_IMAGE_SIZE) {
            throw BizException.badRequest("图片不能超过 10 MB");
        }
        return this.fileUploadService.upload(
                com.weiran.framework.auth.CurrentUser.require().id(),
                originalName,
                file.getSize(),
                file::getInputStream,
                file.getContentType());
    }
    /** 上传附件，复用存储服务的白名单与大小上限。 */
    @RequiresPermission({"cqt:resource:create", "cqt:resource:update"})
    @OperationLog(module = "指导资料", description = "上传附件")
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadedFileView uploadFile(@RequestParam("file") final MultipartFile file) {
        if (file.isEmpty()
                || file.getSize() > 1024L * 1024 * 1024
                || UploadedFiles.extensionOf(file.getOriginalFilename()).isEmpty()) {
            throw BizException.badRequest("附件为空、超过1GB或扩展名不支持");
        }
        return this.fileUploadService.upload(
                com.weiran.framework.auth.CurrentUser.require().id(),
                file.getOriginalFilename(),
                file.getSize(),
                file::getInputStream,
                file.getContentType());
    }
}
