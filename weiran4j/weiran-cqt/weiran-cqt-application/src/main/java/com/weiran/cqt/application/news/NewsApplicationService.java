package com.weiran.cqt.application.news;

import com.weiran.cqt.api.news.NewsAdminService;
import com.weiran.cqt.api.news.NewsCommand;
import com.weiran.cqt.api.news.NewsPortalService;
import com.weiran.cqt.api.news.NewsView;
import com.weiran.cqt.domain.news.News;
import com.weiran.cqt.domain.news.NewsRepository;
import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/** 大赛动态管理与公开查询用例。 */
public class NewsApplicationService implements NewsAdminService, NewsPortalService {

    private static final int MAX_TITLE_LENGTH = 255;

    private static final int MAX_IMAGE_LENGTH = 1024;

    private final NewsRepository newsRepository;

    /** 构造应用服务。 */
    public NewsApplicationService(final NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @Override
    public PageResult<NewsView> page(final PageQuery query, final @Nullable String keyword) {
        return this.newsRepository.page(query, keyword, false).map(NewsApplicationService::view);
    }

    @Override
    public NewsView get(final long id) {
        return NewsApplicationService.view(this.find(id));
    }

    @Override
    @Transactional
    public long create(final NewsCommand command) {
        NewsApplicationService.validate(command);
        return this.newsRepository.insert(
                command.title().trim(),
                command.image().trim(),
                command.showOnHome(),
                command.sort(),
                command.content());
    }

    @Override
    @Transactional
    public void update(final long id, final NewsCommand command) {
        this.find(id);
        NewsApplicationService.validate(command);
        this.newsRepository.update(
                id,
                command.title().trim(),
                command.image().trim(),
                command.showOnHome(),
                command.sort(),
                command.content());
    }

    @Override
    @Transactional
    public void delete(final long id) {
        this.find(id);
        this.newsRepository.delete(id);
    }

    @Override
    public PageResult<NewsView> page(final PageQuery query, final boolean homeOnly) {
        return this.newsRepository.page(query, null, homeOnly).map(NewsApplicationService::view);
    }

    private News find(final long id) {
        return this.newsRepository.findById(id).orElseThrow(() -> BizException.notFound("大赛动态不存在"));
    }

    private static void validate(final NewsCommand command) {
        if (command.title().isBlank() || command.title().trim().length() > NewsApplicationService.MAX_TITLE_LENGTH) {
            throw BizException.badRequest("标题不能为空且不能超过 255 个字符");
        }
        if (command.image().length() > NewsApplicationService.MAX_IMAGE_LENGTH) {
            throw BizException.badRequest("首图地址不能超过 1024 个字符");
        }
        if (command.sort() < 0) {
            throw BizException.badRequest("排序值不能小于 0");
        }
        if (command.content().isBlank()) {
            throw BizException.badRequest("动态内容不能为空");
        }
    }

    private static NewsView view(final News news) {
        return new NewsView(
                news.id(),
                news.title(),
                news.image(),
                news.showOnHome(),
                news.sort(),
                news.content(),
                news.createdAt(),
                news.updatedAt());
    }
}
