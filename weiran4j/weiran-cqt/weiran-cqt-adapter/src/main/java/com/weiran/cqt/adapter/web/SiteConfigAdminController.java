package com.weiran.cqt.adapter.web;

import com.weiran.cqt.api.file.FileUploadService;
import com.weiran.cqt.api.file.UploadedFileView;
import com.weiran.cqt.api.setting.SaveSiteConfigRequest;
import com.weiran.cqt.api.setting.SiteConfigAdminResponse;
import com.weiran.cqt.api.setting.SiteConfigAdminService;
import com.weiran.cqt.domain.file.UploadedFiles;
import com.weiran.framework.auth.CurrentUser;
import com.weiran.framework.auth.RequiresPermission;
import com.weiran.framework.error.BizException;
import com.weiran.framework.log.OperationLog;
import com.weiran.framework.response.ApiResponse;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 常青藤站点配置管理接口。 */
@RestController
@RequestMapping("/api-backend/cqt/site-config")
public class SiteConfigAdminController {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    private final SiteConfigAdminService adminService;

    private final FileUploadService fileUploadService;

    /** 构造控制器。 */
    public SiteConfigAdminController(
            final SiteConfigAdminService adminService, final FileUploadService fileUploadService) {
        this.adminService = adminService;
        this.fileUploadService = fileUploadService;
    }

    /** 获取后台表单及其原始配置值。 */
    @RequiresPermission("cqt:site-config:list")
    @GetMapping("/get")
    public SiteConfigAdminResponse get() {
        return new SiteConfigAdminResponse(this.adminService.getEditableConfig());
    }

    /** 全量保存表单配置。 */
    @RequiresPermission("cqt:site-config:update")
    @OperationLog(module = "站点配置", description = "更新站点配置")
    @PutMapping("/update")
    public ApiResponse<Void> save(@RequestBody final @Nullable SaveSiteConfigRequest request) {
        if (request == null) {
            throw BizException.badRequest("必须提交全部站点配置项");
        }
        this.adminService.saveConfig(request.items());
        return ApiResponse.ok();
    }

    /** 上传富文本图片；二进制文件不进入操作日志请求体。 */
    @RequiresPermission("cqt:site-config:update")
    @OperationLog(module = "站点配置", description = "上传站点配置图片")
    @PostMapping(value = "/uploadImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadedFileView uploadImage(@RequestParam("file") final MultipartFile file) {
        final String originalName = file.getOriginalFilename();
        if (UploadedFiles.extensionOf(originalName)
                .filter(SiteConfigAdminController.IMAGE_EXTENSIONS::contains)
                .isEmpty()) {
            throw BizException.badRequest("仅支持 jpg、jpeg、png、gif 图片");
        }
        if (file.isEmpty()) {
            throw BizException.badRequest("请选择图片");
        }
        if (file.getSize() > SiteConfigAdminController.MAX_IMAGE_SIZE) {
            throw BizException.badRequest("图片不能超过 10 MB");
        }
        return this.fileUploadService.upload(
                CurrentUser.require().id(), originalName, file.getSize(), file::getInputStream, file.getContentType());
    }
}
