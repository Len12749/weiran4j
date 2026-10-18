package com.weiran.cqt.application.entryexport;

import com.weiran.cqt.api.entryexport.EntryExportCommand;
import com.weiran.cqt.api.entryexport.EntryExportService;
import com.weiran.cqt.application.review.ReviewWorkflowSupport;
import com.weiran.cqt.domain.entryexport.EntryExportRepository;
import com.weiran.cqt.domain.entryexport.EntryExportRow;
import com.weiran.cqt.domain.review.WorkflowPhase;
import com.weiran.framework.error.BizException;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public class EntryExportApplicationService implements EntryExportService {
    private final EntryExportRepository repository;
    private final ReviewWorkflowSupport support;
    private final Clock clock;

    public EntryExportApplicationService(
            final EntryExportRepository repository, final ReviewWorkflowSupport support, final Clock clock) {
        this.repository = repository;
        this.support = support;
        this.clock = clock;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public byte[] export(final EntryExportCommand c) {
        if (c.competitionId() <= 0 || c.phaseId() <= 0) {
            throw BizException.badRequest("请选择有效赛事和阶段");
        }
        if (c.keyword() != null && c.keyword().length() > 200) {
            throw BizException.badRequest("关键字最多200字符");
        }
        if (c.source() != null && !Set.of("zhongxi", "qudao", "houtai").contains(c.source())) {
            throw BizException.badRequest("数据来源无效");
        }
        if (c.status() != null && (c.status() < 0 || c.status() > 3)) {
            throw BizException.badRequest("初审状态无效");
        }
        final WorkflowPhase phase = this.support.lockAndValidate(c.phaseId());
        if (phase.competitionId() != c.competitionId()
                || !Set.of("PROVINCIAL", "NATIONAL").contains(phase.code())) {
            throw BizException.badRequest("请选择所属赛事的省审或国审阶段");
        }
        final List<EntryExportRow> rows = this.repository.rows(
                c.competitionId(),
                c.phaseId(),
                c.keyword() == null ? null : c.keyword().strip(),
                c.source(),
                c.status());
        if (rows.size() > 10000) {
            throw BizException.badRequest("导出超过10000行，请缩小筛选范围");
        }
        return new EntryExportWorkbookWriter().write(c.competitionId(), c.phaseId(), phase.code(), rows, this.clock);
    }
}
