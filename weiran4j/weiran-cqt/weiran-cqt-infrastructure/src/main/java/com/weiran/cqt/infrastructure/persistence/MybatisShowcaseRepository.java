package com.weiran.cqt.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weiran.cqt.domain.content.showcases.Showcase;
import com.weiran.cqt.domain.content.showcases.ShowcaseRepository;
import com.weiran.cqt.infrastructure.persistence.entity.CqtShowcaseDO;
import com.weiran.cqt.infrastructure.persistence.mapper.CqtShowcaseMapper;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 佳作欣赏持久化。 */
public class MybatisShowcaseRepository implements ShowcaseRepository {
    private final CqtShowcaseMapper mapper;

    public MybatisShowcaseRepository(final CqtShowcaseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<Showcase> page(
            final PageQuery query,
            final @Nullable String keyword,
            final @Nullable String category,
            final @Nullable Boolean enabled,
            final boolean homeOnly) {
        final Page<CqtShowcaseDO> page = this.mapper.selectPage(
                new Page<>(query.page(), query.pageSize()),
                Wrappers.lambdaQuery(CqtShowcaseDO.class)
                        .like(keyword != null && !keyword.isBlank(), CqtShowcaseDO::getTitle, keyword)
                        .eq(category != null, CqtShowcaseDO::getCategory, category)
                        .eq(enabled != null, CqtShowcaseDO::getEnabled, enabled)
                        .orderByAsc(CqtShowcaseDO::getSort)
                        .orderByAsc(CqtShowcaseDO::getId));
        return new PageResult<>(
                page.getRecords().stream().map(MybatisShowcaseRepository::value).toList(),
                page.getTotal(),
                query.page(),
                query.pageSize());
    }

    @Override
    public Optional<Showcase> findById(final long id) {
        return Optional.ofNullable(this.mapper.selectById(id)).map(MybatisShowcaseRepository::value);
    }

    @Override
    public long insert(final Showcase value) {
        final CqtShowcaseDO row = MybatisShowcaseRepository.row(value);
        this.mapper.insert(row);
        return row.getId();
    }

    @Override
    public void update(final long id, final Showcase value) {
        final CqtShowcaseDO row = MybatisShowcaseRepository.row(value);
        row.setId(id);
        this.mapper.updateById(row);
    }

    @Override
    public void delete(final long id) {
        this.mapper.deleteById(id);
    }

    private static Showcase value(final CqtShowcaseDO row) {
        return new Showcase(
                row.getId(),
                row.getTitle(),
                row.getImage(),
                row.getCategory(),
                row.getAuthor(),
                row.getTeacher(),
                row.getSchool(),
                row.getDescription(),
                row.getContent(),
                row.getAttachmentUrl(),
                row.getAttachmentName(),
                row.getVideoUrl(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getSort(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private static CqtShowcaseDO row(final Showcase value) {
        final CqtShowcaseDO row = new CqtShowcaseDO();
        row.setTitle(value.title());
        row.setImage(value.image());
        row.setCategory(value.category());
        row.setAuthor(value.author());
        row.setTeacher(value.teacher());
        row.setSchool(value.school());
        row.setDescription(value.description());
        row.setContent(value.content());
        row.setAttachmentUrl(value.attachmentUrl());
        row.setAttachmentName(value.attachmentName());
        row.setVideoUrl(value.videoUrl());
        row.setEnabled(value.enabled());
        row.setSort(value.sort());
        return row;
    }
}
