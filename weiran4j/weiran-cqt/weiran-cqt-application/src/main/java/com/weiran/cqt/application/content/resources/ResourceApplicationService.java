package com.weiran.cqt.application.content.resources;

import com.weiran.cqt.api.content.resources.ResourceCommand;
import com.weiran.cqt.api.content.resources.ResourceService;
import com.weiran.cqt.api.content.resources.ResourceView;
import com.weiran.cqt.domain.content.ContentValidation;
import com.weiran.cqt.domain.content.resources.Resource;
import com.weiran.cqt.domain.content.resources.ResourceRepository;
import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/** 指导资料管理与公开查询。 */
public class ResourceApplicationService implements ResourceService {
    private final ResourceRepository repository;

    public ResourceApplicationService(final ResourceRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<ResourceView> page(
            final PageQuery query,
            final @Nullable String keyword,
            final @Nullable String category,
            final @Nullable Boolean enabled,
            final boolean homeOnly) {
        ResourceApplicationService.validateCategory(category);
        return this.repository.page(query, keyword, category, enabled, homeOnly).map(ResourceApplicationService::view);
    }

    @Override
    public ResourceView get(final long id) {
        return ResourceApplicationService.view(this.find(id));
    }

    @Override
    public ResourceView getPublic(final long id) {
        final Resource value = this.find(id);
        if (!value.enabled()) {
            throw BizException.notFound("内容不存在");
        }
        return ResourceApplicationService.view(value);
    }

    @Override
    @Transactional
    public long create(final ResourceCommand command) {
        return this.repository.insert(ResourceApplicationService.value(command));
    }

    @Override
    @Transactional
    public void update(final long id, final ResourceCommand command) {
        this.find(id);
        this.repository.update(id, ResourceApplicationService.value(command));
    }

    @Override
    @Transactional
    public void delete(final long id) {
        this.find(id);
        this.repository.delete(id);
    }

    private Resource find(final long id) {
        return this.repository.findById(id).orElseThrow(() -> BizException.notFound("内容不存在"));
    }

    private static Resource value(final ResourceCommand command) {
        ContentValidation.required(command.title(), "名称或标题", 255);
        ContentValidation.url(command.fileUrl(), true);
        ContentValidation.required(command.fileName(), "fileName", 255);
        validateCategory(command.category());
        if (command.sort() < 0) {
            throw BizException.badRequest("排序值不能小于0");
        }
        return new Resource(
                0,
                command.title(),
                command.category(),
                command.fileUrl(),
                command.fileName(),
                command.content(),
                !Boolean.FALSE.equals(command.enabled()),
                command.sort(),
                java.time.LocalDateTime.MIN,
                java.time.LocalDateTime.MIN);
    }

    private static ResourceView view(final Resource value) {
        return new ResourceView(
                value.id(),
                value.title(),
                value.category(),
                value.fileUrl(),
                value.fileName(),
                value.content(),
                value.enabled(),
                value.sort(),
                value.createdAt(),
                value.updatedAt());
    }

    private static void validateCategory(final @Nullable String category) {
        if (category != null
                && !java.util.Set.of("SCRIPT_PAPER", "DRAMA_MATERIAL").contains(category)) {
            throw BizException.badRequest("分类参数无效");
        }
    }
}
