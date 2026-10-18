package com.weiran.cqt.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weiran.cqt.domain.news.News;
import com.weiran.cqt.domain.news.NewsRepository;
import com.weiran.cqt.infrastructure.persistence.entity.CqtNewsDO;
import com.weiran.cqt.infrastructure.persistence.mapper.CqtNewsMapper;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** {@link NewsRepository} 的 MyBatis-Plus 实现。 */
public class MybatisNewsRepository implements NewsRepository {

    private final CqtNewsMapper newsMapper;

    /** 构造仓储。 */
    public MybatisNewsRepository(final CqtNewsMapper newsMapper) {
        this.newsMapper = newsMapper;
    }

    @Override
    public PageResult<News> page(final PageQuery query, final @Nullable String keyword, final boolean homeOnly) {
        final Page<CqtNewsDO> page = this.newsMapper.selectPage(
                new Page<>(query.page(), query.pageSize()),
                Wrappers.lambdaQuery(CqtNewsDO.class)
                        .like(keyword != null && !keyword.isBlank(), CqtNewsDO::getTitle, keyword)
                        .eq(homeOnly, CqtNewsDO::getShowOnHome, true)
                        .orderByAsc(CqtNewsDO::getSort)
                        .orderByAsc(CqtNewsDO::getId));
        return new PageResult<>(
                page.getRecords().stream().map(MybatisNewsRepository::news).toList(),
                page.getTotal(),
                query.page(),
                query.pageSize());
    }

    @Override
    public Optional<News> findById(final long id) {
        return Optional.ofNullable(this.newsMapper.selectById(id)).map(MybatisNewsRepository::news);
    }

    @Override
    public long insert(
            final String title, final String image, final boolean showOnHome, final int sort, final String content) {
        final CqtNewsDO row = MybatisNewsRepository.row(title, image, showOnHome, sort, content);
        this.newsMapper.insert(row);
        return row.getId();
    }

    @Override
    public void update(
            final long id,
            final String title,
            final String image,
            final boolean showOnHome,
            final int sort,
            final String content) {
        final CqtNewsDO row = MybatisNewsRepository.row(title, image, showOnHome, sort, content);
        row.setId(id);
        this.newsMapper.updateById(row);
    }

    @Override
    public void delete(final long id) {
        this.newsMapper.deleteById(id);
    }

    private static News news(final CqtNewsDO row) {
        return new News(
                row.getId(),
                row.getTitle(),
                row.getImage(),
                Boolean.TRUE.equals(row.getShowOnHome()),
                row.getSort(),
                row.getContent(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private static CqtNewsDO row(
            final String title, final String image, final boolean showOnHome, final int sort, final String content) {
        final CqtNewsDO row = new CqtNewsDO();
        row.setTitle(title);
        row.setImage(image);
        row.setShowOnHome(showOnHome);
        row.setSort(sort);
        row.setContent(content);
        return row;
    }
}
