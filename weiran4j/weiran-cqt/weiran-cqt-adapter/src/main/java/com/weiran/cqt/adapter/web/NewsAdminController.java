package com.weiran.cqt.adapter.web;

import com.weiran.cqt.api.file.FileUploadService;
import com.weiran.cqt.api.file.UploadedFileView;
import com.weiran.cqt.api.news.NewsAdminService;
import com.weiran.cqt.api.news.NewsCommand;
import com.weiran.cqt.api.news.NewsView;
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

/** 大赛动态后台管理接口。 */
@RestController
@RequestMapping("/api/cqt/news")
public class NewsAdminController {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    private final NewsAdminService newsService;

    private final FileUploadService fileUploadService;

    /** 构造控制器。 */
    public NewsAdminController(final NewsAdminService newsService, final FileUploadService fileUploadService) {
        this.newsService = newsService;
        this.fileUploadService = fileUploadService;
    }

    /** 分页查询动态。 */
    @RequiresPermission("cqt:news:list")
    @GetMapping
    public PageResult<NewsView> page(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(required = false) final @Nullable String keyword) {
        return this.newsService.page(new PageQuery(page, pageSize), keyword);
    }

    /** 查询动态详情。 */
    @RequiresPermission("cqt:news:list")
    @GetMapping("/{id}")
    public NewsView get(@PathVariable("id") final long id) {
        return this.newsService.get(id);
    }

    /** 新增动态。 */
    @RequiresPermission("cqt:news:create")
    @OperationLog(module = "大赛动态", description = "新增动态")
    @PostMapping
    public long create(@RequestBody final @Nullable NewsCommand command) {
        if (command == null) {
            throw BizException.badRequest("动态内容不能为空");
        }
        return this.newsService.create(command);
    }

    /** 更新动态。 */
    @RequiresPermission("cqt:news:update")
    @OperationLog(module = "大赛动态", description = "更新动态")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable("id") final long id, @RequestBody final @Nullable NewsCommand command) {
        if (command == null) {
            throw BizException.badRequest("动态内容不能为空");
        }
        this.newsService.update(id, command);
        return ApiResponse.ok();
    }

    /** 删除动态。 */
    @RequiresPermission("cqt:news:delete")
    @OperationLog(module = "大赛动态", description = "删除动态")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") final long id) {
        this.newsService.delete(id);
        return ApiResponse.ok();
    }

    /** 上传动态首图；二进制内容不写入操作日志请求体。 */
    @RequiresPermission({"cqt:news:create", "cqt:news:update"})
    @OperationLog(module = "大赛动态", description = "上传动态首图")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadedFileView uploadImage(@RequestParam("file") final MultipartFile file) {
        final String originalName = file.getOriginalFilename();
        if (UploadedFiles.extensionOf(originalName)
                .filter(NewsAdminController.IMAGE_EXTENSIONS::contains)
                .isEmpty()) {
            throw BizException.badRequest("仅支持 jpg、jpeg、png、gif 图片");
        }
        if (file.isEmpty()) {
            throw BizException.badRequest("请选择图片");
        }
        if (file.getSize() > NewsAdminController.MAX_IMAGE_SIZE) {
            throw BizException.badRequest("图片不能超过 10 MB");
        }
        return this.fileUploadService.upload(
                com.weiran.framework.auth.CurrentUser.require().id(),
                originalName,
                file.getSize(),
                file::getInputStream,
                file.getContentType());
    }
}
