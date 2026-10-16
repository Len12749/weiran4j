package com.weiran.cqt.application.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiran.cqt.domain.account.Account;
import com.weiran.cqt.domain.account.AccountRepository;
import com.weiran.cqt.domain.account.AccountTypes;
import com.weiran.cqt.domain.competition.Competition;
import com.weiran.cqt.domain.competition.CompetitionRepository;
import com.weiran.cqt.domain.entry.EntryRepository;
import com.weiran.cqt.domain.entry.SchoolImportTask;
import com.weiran.cqt.domain.entry.SchoolImportTaskRepository;
import com.weiran.cqt.domain.entry.SchoolImportTaskStatus;
import com.weiran.cqt.domain.file.FileStorage;
import com.weiran.framework.error.BizException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class SchoolImportApplicationServiceTest {

    @Test
    @DisplayName("重复确认已排队的导入任务返回原任务且不再次入队")
    void repeatedConfirmationDoesNotQueueAgain() {
        final AccountRepository accounts = mock(AccountRepository.class);
        final CompetitionRepository competitions = mock(CompetitionRepository.class);
        final SchoolImportTaskRepository tasks = mock(SchoolImportTaskRepository.class);
        final Account school = Account.builder().userType(AccountTypes.SCHOOL).build();
        final SchoolImportTask queued = SchoolImportApplicationServiceTest.task(SchoolImportTaskStatus.QUEUED, 2027L);
        when(accounts.findById(10L)).thenReturn(Optional.of(school));
        when(tasks.findOwned("task-1", 10L)).thenReturn(Optional.of(queued));
        when(competitions.findById(2027L))
                .thenReturn(Optional.of(new Competition(
                        2027L, 2027L, "测试赛事", null, null, null, Competition.STATUS_ENABLED, null, null, false)));
        final SchoolImportApplicationService service =
                SchoolImportApplicationServiceTest.service(accounts, competitions, tasks);

        final String taskId = service.confirm(10L, 2027L, "task-1", "task-1", "", null, null);

        assertThat(taskId).isEqualTo("task-1");
        verify(tasks, never())
                .queue(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    @DisplayName("任务 worker 从仓储领取可处理任务")
    void workerClaimsNextTask() {
        final SchoolImportTaskRepository tasks = mock(SchoolImportTaskRepository.class);
        when(tasks.claimNext(
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        final SchoolImportApplicationService service = SchoolImportApplicationServiceTest.service(
                mock(AccountRepository.class), mock(CompetitionRepository.class), tasks);

        assertThat(service.processNext()).isFalse();

        verify(tasks)
                .claimNext(
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    @DisplayName("任务查询仅能读取当前学校拥有的任务")
    void taskDetailRejectsTaskOwnedByAnotherSchool() {
        final AccountRepository accounts = mock(AccountRepository.class);
        final SchoolImportTaskRepository tasks = mock(SchoolImportTaskRepository.class);
        when(accounts.findById(10L))
                .thenReturn(Optional.of(
                        Account.builder().userType(AccountTypes.SCHOOL).build()));
        when(tasks.findOwned("foreign-task", 10L)).thenReturn(Optional.empty());
        final SchoolImportApplicationService service =
                SchoolImportApplicationServiceTest.service(accounts, mock(CompetitionRepository.class), tasks);

        assertThatThrownBy(() -> service.detail(10L, 2027L, "foreign-task"))
                .isInstanceOf(BizException.class)
                .hasMessage("导入记录不存在");
    }

    private static SchoolImportApplicationService service(
            final AccountRepository accounts,
            final CompetitionRepository competitions,
            final SchoolImportTaskRepository tasks) {
        return new SchoolImportApplicationService(
                accounts,
                competitions,
                mock(EntryRepository.class),
                tasks,
                mock(SchoolImportEntryWriter.class),
                new DefaultListableBeanFactory().getBeanProvider(FileStorage.class),
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"), ZoneOffset.UTC),
                "cqt/");
    }

    private static SchoolImportTask task(final SchoolImportTaskStatus status, final long competitionId) {
        final LocalDateTime now = LocalDateTime.of(2026, 10, 16, 0, 0);
        return new SchoolImportTask(
                "task-1",
                10L,
                "school-1",
                competitionId,
                null,
                null,
                "https://storage.test/import.xlsx",
                "import.xlsx",
                "",
                status,
                0,
                0,
                0,
                0,
                null,
                now.plusHours(2),
                1,
                now,
                now);
    }
}
