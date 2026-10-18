package com.weiran.cqt.application.content.showcases;

import com.weiran.cqt.api.content.showcases.ShowcaseCommand;
import com.weiran.cqt.api.content.showcases.ShowcaseService;
import com.weiran.cqt.api.content.showcases.ShowcaseView;
import com.weiran.cqt.domain.content.ContentValidation;
import com.weiran.cqt.domain.content.showcases.Showcase;
import com.weiran.cqt.domain.content.showcases.ShowcaseRepository;
import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/** 佳作欣赏管理与公开查询。 */
public class ShowcaseApplicationService implements ShowcaseService {
    private final ShowcaseRepository repository;

    public ShowcaseApplicationService(final ShowcaseRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<ShowcaseView> page(
            final PageQuery query,
            final @Nullable String keyword,
            final @Nullable String category,
            final @Nullable Boolean enabled,
            final boolean homeOnly) {
        ShowcaseApplicationService.validateCategory(category);
        return this.repository.page(query, keyword, category, enabled, homeOnly).map(ShowcaseApplicationService::view);
    }

    @Override
    public ShowcaseView get(final long id) {
        return ShowcaseApplicationService.view(this.find(id));
    }

    @Override
    public ShowcaseView getPublic(final long id) {
        final Showcase value = this.find(id);
        if (!value.enabled()) {
            throw BizException.notFound("内容不存在");
        }
        return ShowcaseApplicationService.view(value);
    }

    @Override
    @Transactional
    public long create(final ShowcaseCommand command) {
        return this.repository.insert(ShowcaseApplicationService.value(command));
    }

    @Override
    @Transactional
    public void update(final long id, final ShowcaseCommand command) {
        this.find(id);
        this.repository.update(id, ShowcaseApplicationService.value(command));
    }

    @Override
    @Transactional
    public void delete(final long id) {
        this.find(id);
        this.repository.delete(id);
    }

    private Showcase find(final long id) {
        return this.repository.findById(id).orElseThrow(() -> BizException.notFound("内容不存在"));
    }

    private static Showcase value(final ShowcaseCommand command) {
        ContentValidation.required(command.title(), "名称或标题", 255);
        ContentValidation.url(command.image(), true);
        ContentValidation.optional(command.author(), "author", 255);
        ContentValidation.optional(command.teacher(), "teacher", 255);
        ContentValidation.optional(command.school(), "school", 255);
        ContentValidation.url(command.attachmentUrl(), false);
        ContentValidation.optional(command.attachmentName(), "attachmentName", 255);
        ContentValidation.url(command.videoUrl(), false);
        validateCategory(command.category());
        if (command.sort() < 0) {
            throw BizException.badRequest("排序值不能小于0");
        }
        return new Showcase(
                0,
                command.title(),
                command.image(),
                command.category(),
                command.author(),
                command.teacher(),
                command.school(),
                command.description(),
                command.content(),
                command.attachmentUrl(),
                command.attachmentName(),
                command.videoUrl(),
                !Boolean.FALSE.equals(command.enabled()),
                command.sort(),
                java.time.LocalDateTime.MIN,
                java.time.LocalDateTime.MIN);
    }

    private static ShowcaseView view(final Showcase value) {
        return new ShowcaseView(
                value.id(),
                value.title(),
                value.image(),
                value.category(),
                value.author(),
                value.teacher(),
                value.school(),
                value.description(),
                value.content(),
                value.attachmentUrl(),
                value.attachmentName(),
                value.videoUrl(),
                value.enabled(),
                value.sort(),
                value.createdAt(),
                value.updatedAt());
    }

    private static void validateCategory(final @Nullable String category) {
        if (category != null && !java.util.Set.of("SCRIPT", "PERFORMANCE").contains(category)) {
            throw BizException.badRequest("分类参数无效");
        }
    }
}
