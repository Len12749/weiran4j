package com.weiran.cqt.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weiran.cqt.domain.content.resources.Resource;
import com.weiran.cqt.domain.content.resources.ResourceRepository;
import com.weiran.cqt.infrastructure.persistence.entity.CqtResourceDO;
import com.weiran.cqt.infrastructure.persistence.mapper.CqtResourceMapper;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 指导资料持久化。 */
public class MybatisResourceRepository implements ResourceRepository {
    private final CqtResourceMapper mapper;

    public MybatisResourceRepository(final CqtResourceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<Resource> page(
            final PageQuery query,
            final @Nullable String keyword,
            final @Nullable String category,
            final @Nullable Boolean enabled,
            final boolean homeOnly) {
        final Page<CqtResourceDO> page = this.mapper.selectPage(
                new Page<>(query.page(), query.pageSize()),
                Wrappers.lambdaQuery(CqtResourceDO.class)
                        .like(keyword != null && !keyword.isBlank(), CqtResourceDO::getTitle, keyword)
                        .eq(category != null, CqtResourceDO::getCategory, category)
                        .eq(enabled != null, CqtResourceDO::getEnabled, enabled)
                        .orderByAsc(CqtResourceDO::getSort)
                        .orderByAsc(CqtResourceDO::getId));
        return new PageResult<>(
                page.getRecords().stream().map(MybatisResourceRepository::value).toList(),
                page.getTotal(),
                query.page(),
                query.pageSize());
    }

    @Override
    public Optional<Resource> findById(final long id) {
        return Optional.ofNullable(this.mapper.selectById(id)).map(MybatisResourceRepository::value);
    }

    @Override
    public long insert(final Resource value) {
        final CqtResourceDO row = MybatisResourceRepository.row(value);
        this.mapper.insert(row);
        return row.getId();
    }

    @Override
    public void update(final long id, final Resource value) {
        final CqtResourceDO row = MybatisResourceRepository.row(value);
        row.setId(id);
        this.mapper.updateById(row);
    }

    @Override
    public void delete(final long id) {
        this.mapper.deleteById(id);
    }

    private static Resource value(final CqtResourceDO row) {
        return new Resource(
                row.getId(),
                row.getTitle(),
                row.getCategory(),
                row.getFileUrl(),
                row.getFileName(),
                row.getContent(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getSort(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private static CqtResourceDO row(final Resource value) {
        final CqtResourceDO row = new CqtResourceDO();
        row.setTitle(value.title());
        row.setCategory(value.category());
        row.setFileUrl(value.fileUrl());
        row.setFileName(value.fileName());
        row.setContent(value.content());
        row.setEnabled(value.enabled());
        row.setSort(value.sort());
        return row;
    }
}
