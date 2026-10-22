package com.weiran.system.adapter.web;

import com.weiran.framework.auth.CurrentUser;
import com.weiran.framework.auth.RequiresPermission;
import com.weiran.framework.error.BizException;
import com.weiran.framework.log.OperationLog;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.framework.response.ApiResponse;
import com.weiran.system.api.media.FileUploadView;
import com.weiran.system.api.media.MediaService;
import com.weiran.system.api.media.MediaView;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 系统文件上传和资源库管理接口。 */
@RestController
public class MediaController {

    private final MediaService service;

    public MediaController(final MediaService service) {
        this.service = service;
    }

    @PostMapping({"/api-backend/system/media/uploadImage", "/api-backend/system/media/uploadFile"})
    @RequiresPermission("system:media-library:upload")
    @OperationLog(module = "资源库", description = "上传文件")
    public FileUploadView upload(@RequestParam("file") final MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw BizException.badRequest("文件无效");
        }
        return this.service.upload(
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                CurrentUser.require().id());
    }

    @GetMapping("/api-backend/system/mediaLibrary/list")
    @RequiresPermission("system:media-library:list")
    public PageResult<MediaView> page(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(required = false) final @Nullable String keyword,
            @RequestParam(required = false) final @Nullable String category) {
        if (category != null && !category.matches("image|video|document|archive")) {
            throw BizException.badRequest("文件类型无效");
        }
        return this.service.page(keyword, category, new PageQuery(page, pageSize));
    }

    @DeleteMapping("/api-backend/system/mediaLibrary/delete/{id}")
    @RequiresPermission("system:media-library:delete")
    @OperationLog(module = "资源库", description = "删除文件")
    public ApiResponse<Void> delete(@PathVariable final long id) {
        this.service.delete(id);
        return ApiResponse.ok();
    }
}
