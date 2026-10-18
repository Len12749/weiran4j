package com.weiran.cqt.application.competition.admin;

import com.weiran.cqt.api.competition.admin.CompetitionAdminCommand;
import com.weiran.cqt.api.competition.admin.CompetitionAdminService;
import com.weiran.cqt.api.competition.admin.CompetitionAdminView;
import com.weiran.cqt.domain.competition.admin.AdminCompetition;
import com.weiran.cqt.domain.competition.admin.CompetitionAdminRepository;
import com.weiran.cqt.domain.competition.admin.CompetitionConfiguration;
import com.weiran.cqt.domain.competition.admin.CompetitionConfigurationRules;
import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.time.Clock;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public class CompetitionAdminApplicationService implements CompetitionAdminService {
    private final CompetitionAdminRepository repository;
    private final Clock clock;

    public CompetitionAdminApplicationService(final CompetitionAdminRepository repository, final Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    private static CompetitionAdminView view(final AdminCompetition c) {
        return new CompetitionAdminView(
                c.id(),
                c.legacyId(),
                c.name(),
                c.edition(),
                c.year(),
                c.description(),
                c.nationalEntryMode(),
                c.registrationStart(),
                c.registrationEnd(),
                c.provinceReviewStart(),
                c.provinceReviewEnd(),
                c.nationalReviewStart(),
                c.nationalReviewEnd(),
                c.resultPublishTime(),
                c.provinceCertificateTemplate(),
                c.nationalCertificateTemplate(),
                c.teacherOrgCertificateTemplate(),
                c.status(),
                c.version(),
                c.createdAt(),
                c.updatedAt());
    }

    private static CompetitionConfiguration configuration(final CompetitionAdminCommand c) {
        return CompetitionConfigurationRules.validate(new CompetitionConfiguration(
                c.name(),
                c.edition(),
                c.year(),
                c.description(),
                c.nationalEntryMode(),
                c.registrationStart(),
                c.registrationEnd(),
                c.provinceReviewStart(),
                c.provinceReviewEnd(),
                c.nationalReviewStart(),
                c.nationalReviewEnd(),
                c.resultPublishTime(),
                c.provinceCertificateTemplate(),
                c.nationalCertificateTemplate(),
                c.teacherOrgCertificateTemplate()));
    }

    @Override
    public PageResult<CompetitionAdminView> page(
            final PageQuery q,
            final @Nullable String keyword,
            final @Nullable Integer year,
            final @Nullable Integer status) {
        CompetitionConfigurationRules.filters(keyword, year, status);
        return this.repository
                .page(q, keyword == null ? null : keyword.strip(), year, status)
                .map(CompetitionAdminApplicationService::view);
    }

    private static void positiveId(final long id) {
        if (id <= 0) {
            throw BizException.badRequest("赛事编号必须为正整数");
        }
    }

    @Override
    public CompetitionAdminView get(final long id) {
        CompetitionAdminApplicationService.positiveId(id);
        return CompetitionAdminApplicationService.view(
                this.repository.find(id).orElseThrow(() -> BizException.notFound("赛事不存在")));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result create(final CompetitionAdminCommand command) {
        final CompetitionConfiguration value = CompetitionAdminApplicationService.configuration(command);
        final long id = this.repository.create(value, LocalDateTime.now(this.clock));
        this.repository.createDefaultGroups(id);
        return new Result(id, 0);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result update(final long id, final CompetitionAdminCommand command) {
        CompetitionAdminApplicationService.positiveId(id);
        final CompetitionConfiguration value = CompetitionAdminApplicationService.configuration(command);
        final Long expected = command.expectedVersion();
        if (expected == null || expected < 0) {
            throw BizException.badRequest("expectedVersion不能为空且必须非负");
        }
        final AdminCompetition old = this.repository.lock(id).orElseThrow(() -> BizException.notFound("赛事不存在"));
        if (!old.nationalEntryMode().equals(value.nationalEntryMode())) {
            throw BizException.conflict("省国赛报名模式创建后不可更改");
        }
        if (!this.repository.update(id, value, expected, LocalDateTime.now(this.clock))) {
            throw BizException.conflict("赛事版本已变化，请刷新重试");
        }
        return new Result(id, expected + 1);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result status(final long id, final int status, final long expectedVersion) {
        CompetitionAdminApplicationService.positiveId(id);
        CompetitionConfigurationRules.filters(null, null, status);
        if (expectedVersion < 0) {
            throw BizException.badRequest("版本无效");
        }
        final AdminCompetition old = this.repository.lock(id).orElseThrow(() -> BizException.notFound("赛事不存在"));
        if (old.version() != expectedVersion) {
            throw BizException.conflict("赛事版本已变化，请刷新重试");
        }
        if (!this.repository.status(id, status, expectedVersion, LocalDateTime.now(this.clock))) {
            throw BizException.conflict("赛事版本已变化，请刷新重试");
        }
        return new Result(id, expectedVersion + 1);
    }
}
