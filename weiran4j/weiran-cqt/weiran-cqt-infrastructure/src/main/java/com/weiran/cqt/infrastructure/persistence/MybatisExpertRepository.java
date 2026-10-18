package com.weiran.cqt.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weiran.cqt.domain.content.experts.Expert;
import com.weiran.cqt.domain.content.experts.ExpertRepository;
import com.weiran.cqt.infrastructure.persistence.entity.CqtExpertDO;
import com.weiran.cqt.infrastructure.persistence.mapper.CqtExpertMapper;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 学术评审团持久化。 */
public class MybatisExpertRepository implements ExpertRepository {
    private final CqtExpertMapper mapper;

    public MybatisExpertRepository(final CqtExpertMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<Expert> page(
            final PageQuery query,
            final @Nullable String keyword,
            final @Nullable String category,
            final @Nullable Boolean enabled,
            final boolean homeOnly) {
        final Page<CqtExpertDO> page = this.mapper.selectPage(
                new Page<>(query.page(), query.pageSize()),
                Wrappers.lambdaQuery(CqtExpertDO.class)
                        .like(keyword != null && !keyword.isBlank(), CqtExpertDO::getName, keyword)
                        .eq(homeOnly, CqtExpertDO::getShowOnHome, true)
                        .eq(enabled != null, CqtExpertDO::getEnabled, enabled)
                        .orderByAsc(CqtExpertDO::getSort)
                        .orderByAsc(CqtExpertDO::getId));
        return new PageResult<>(
                page.getRecords().stream().map(MybatisExpertRepository::value).toList(),
                page.getTotal(),
                query.page(),
                query.pageSize());
    }

    @Override
    public Optional<Expert> findById(final long id) {
        return Optional.ofNullable(this.mapper.selectById(id)).map(MybatisExpertRepository::value);
    }

    @Override
    public long insert(final Expert value) {
        final CqtExpertDO row = MybatisExpertRepository.row(value);
        this.mapper.insert(row);
        return row.getId();
    }

    @Override
    public void update(final long id, final Expert value) {
        final CqtExpertDO row = MybatisExpertRepository.row(value);
        row.setId(id);
        this.mapper.updateById(row);
    }

    @Override
    public void delete(final long id) {
        this.mapper.deleteById(id);
    }

    private static Expert value(final CqtExpertDO row) {
        return new Expert(
                row.getId(),
                row.getName(),
                row.getImage(),
                row.getContent(),
                Boolean.TRUE.equals(row.getShowOnHome()),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getSort(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private static CqtExpertDO row(final Expert value) {
        final CqtExpertDO row = new CqtExpertDO();
        row.setName(value.name());
        row.setImage(value.image());
        row.setContent(value.content());
        row.setShowOnHome(value.showOnHome());
        row.setEnabled(value.enabled());
        row.setSort(value.sort());
        return row;
    }
}
