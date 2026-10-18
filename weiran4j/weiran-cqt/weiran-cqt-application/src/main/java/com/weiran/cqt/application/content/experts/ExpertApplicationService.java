package com.weiran.cqt.application.content.experts;

import com.weiran.cqt.api.content.experts.ExpertCommand;
import com.weiran.cqt.api.content.experts.ExpertService;
import com.weiran.cqt.api.content.experts.ExpertView;
import com.weiran.cqt.domain.content.ContentValidation;
import com.weiran.cqt.domain.content.experts.Expert;
import com.weiran.cqt.domain.content.experts.ExpertRepository;
import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/** 学术评审团管理与公开查询。 */
public class ExpertApplicationService implements ExpertService {
    private final ExpertRepository repository;

    public ExpertApplicationService(final ExpertRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<ExpertView> page(
            final PageQuery query,
            final @Nullable String keyword,
            final @Nullable String category,
            final @Nullable Boolean enabled,
            final boolean homeOnly) {

        return this.repository.page(query, keyword, category, enabled, homeOnly).map(ExpertApplicationService::view);
    }

    @Override
    public ExpertView get(final long id) {
        return ExpertApplicationService.view(this.find(id));
    }

    @Override
    public ExpertView getPublic(final long id) {
        final Expert value = this.find(id);
        if (!value.enabled()) {
            throw BizException.notFound("内容不存在");
        }
        return ExpertApplicationService.view(value);
    }

    @Override
    @Transactional
    public long create(final ExpertCommand command) {
        return this.repository.insert(ExpertApplicationService.value(command));
    }

    @Override
    @Transactional
    public void update(final long id, final ExpertCommand command) {
        this.find(id);
        this.repository.update(id, ExpertApplicationService.value(command));
    }

    @Override
    @Transactional
    public void delete(final long id) {
        this.find(id);
        this.repository.delete(id);
    }

    private Expert find(final long id) {
        return this.repository.findById(id).orElseThrow(() -> BizException.notFound("内容不存在"));
    }

    private static Expert value(final ExpertCommand command) {
        ContentValidation.required(command.name(), "名称或标题", 255);
        ContentValidation.url(command.image(), true);
        ContentValidation.required(command.content(), "content", 2147483647);
        if (command.sort() < 0) {
            throw BizException.badRequest("排序值不能小于0");
        }
        return new Expert(
                0,
                command.name(),
                command.image(),
                command.content(),
                !Boolean.FALSE.equals(command.showOnHome()),
                !Boolean.FALSE.equals(command.enabled()),
                command.sort(),
                java.time.LocalDateTime.MIN,
                java.time.LocalDateTime.MIN);
    }

    private static ExpertView view(final Expert value) {
        return new ExpertView(
                value.id(),
                value.name(),
                value.image(),
                value.content(),
                value.showOnHome(),
                value.enabled(),
                value.sort(),
                value.createdAt(),
                value.updatedAt());
    }
}
