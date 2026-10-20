package com.weiran.system.adapter.web;

import com.weiran.framework.auth.CurrentUser;
import com.weiran.framework.auth.RequiresPermission;
import com.weiran.framework.error.BizException;
import com.weiran.framework.log.OperationLog;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.framework.response.ApiResponse;
import com.weiran.system.api.media.ImageUploadView;
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

/** 系统图片上传和媒体库管理接口。 */
@RestController
public class MediaController {

    private final MediaService service;

    public MediaController(final MediaService service) {
        this.service = service;
    }

    @PostMapping("/api/system/media/uploadImage")
    @RequiresPermission("system:media-library:upload")
    @OperationLog(module = "图片库", description = "上传图片")
    public ImageUploadView upload(@RequestParam("file") final MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw BizException.badRequest("图片文件无效");
        }
        return this.service.upload(
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                CurrentUser.require().id());
    }

    @GetMapping("/api/system/mediaLibrary/list")
    @RequiresPermission("system:media-library:list")
    public PageResult<MediaView> page(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(required = false) final @Nullable String keyword) {
        return this.service.page(keyword, new PageQuery(page, pageSize));
    }

    @DeleteMapping("/api/system/mediaLibrary/delete/{id}")
    @RequiresPermission("system:media-library:delete")
    @OperationLog(module = "图片库", description = "删除图片")
    public ApiResponse<Void> delete(@PathVariable final long id) {
        this.service.delete(id);
        return ApiResponse.ok();
    }
}
