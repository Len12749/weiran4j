package com.weiran.cqt.application.entryexport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.weiran.cqt.api.entryexport.EntryExportCommand;
import com.weiran.cqt.application.review.ReviewWorkflowSupport;
import com.weiran.cqt.domain.entryexport.EntryExportRepository;
import com.weiran.cqt.domain.entryexport.EntryExportRow;
import com.weiran.cqt.domain.review.WorkflowPhase;
import com.weiran.framework.error.BizException;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntryExportApplicationServiceTest {
    @Test
    @DisplayName("验证赛事阶段和筛选，空清单可下载")
    void validatesScopeAndExportsEmpty() {
        final EntryExportRepository repo = mock(EntryExportRepository.class);
        final ReviewWorkflowSupport support = mock(ReviewWorkflowSupport.class);
        final EntryExportApplicationService service =
                new EntryExportApplicationService(repo, support, Clock.systemUTC());
        assertThatThrownBy(() -> service.export(new EntryExportCommand(0, 2, null, null, null)))
                .isInstanceOf(BizException.class);
        verifyNoInteractions(repo, support);
        when(support.lockAndValidate(2)).thenReturn(new WorkflowPhase(2, 1, "INITIAL", "DRAFT", 0, 0, 0));
        assertThatThrownBy(() -> service.export(new EntryExportCommand(1, 2, null, null, null)))
                .isInstanceOf(BizException.class);
        when(support.lockAndValidate(2)).thenReturn(new WorkflowPhase(2, 1, "NATIONAL", "DRAFT", 0, 0, 0));
        when(repo.rows(1, 2, null, null, null)).thenReturn(List.of());
        assertThat(service.export(new EntryExportCommand(1, 2, null, null, null)))
                .isNotEmpty();
    }

    @Test
    @DisplayName("超过一万行明确拒绝而不截断")
    void rejectsOversize() {
        final EntryExportRepository repo = mock(EntryExportRepository.class);
        final ReviewWorkflowSupport support = mock(ReviewWorkflowSupport.class);
        when(support.lockAndValidate(2)).thenReturn(new WorkflowPhase(2, 1, "PROVINCIAL", "CLOSED", 1, 0, 0));
        final EntryExportRow row = mock(EntryExportRow.class);
        when(repo.rows(1, 2, null, null, null)).thenReturn(Collections.nCopies(10001, row));
        assertThatThrownBy(() -> new EntryExportApplicationService(repo, support, Clock.systemUTC())
                        .export(new EntryExportCommand(1, 2, null, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("10000");
    }
}
