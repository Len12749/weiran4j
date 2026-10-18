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

    @Test
    @DisplayName("轻量进度不读取结果行，详情保持待处理0及真实updatedAt")
    void summaryAvoidsRowsAndDetailPreservesPending() {
        final AccountRepository accounts = mock(AccountRepository.class);
        final SchoolImportTaskRepository tasks = mock(SchoolImportTaskRepository.class);
        when(accounts.findById(10L))
                .thenReturn(Optional.of(
                        Account.builder().userType(AccountTypes.SCHOOL).build()));
        final SchoolImportTask task = SchoolImportApplicationServiceTest.task(SchoolImportTaskStatus.PROCESSING, 2027L);
        when(tasks.findOwned("task-1", 10L)).thenReturn(Optional.of(task));
        final SchoolImportApplicationService service =
                SchoolImportApplicationServiceTest.service(accounts, mock(CompetitionRepository.class), tasks);
        assertThat(service.summary(10L, "task-1").updatedAt()).isEqualTo(task.updatedAt());
        verify(tasks, never()).rows("task-1");
        when(tasks.rows("task-1"))
                .thenReturn(java.util.List.of(new com.weiran.cqt.domain.entry.SchoolImportRowResult(
                        1, "task-1", 3, 3, null, null, "PENDING", "{}", null)));
        assertThat(service.detailById(10L, "task-1").results().getFirst()).containsEntry("status", 0);
    }

    @Test
    @DisplayName("新任务ID查询统一拒绝非学校及其它学校/不存在任务")
    void rejectsNonSchoolAndUnownedTasks() {
        final AccountRepository accounts = mock(AccountRepository.class);
        final SchoolImportTaskRepository tasks = mock(SchoolImportTaskRepository.class);
        when(accounts.findById(10L))
                .thenReturn(Optional.of(
                        Account.builder().userType(AccountTypes.SCHOOL).build()));
        when(tasks.findOwned("missing", 10L)).thenReturn(Optional.empty());
        final SchoolImportApplicationService service =
                SchoolImportApplicationServiceTest.service(accounts, mock(CompetitionRepository.class), tasks);
        assertThatThrownBy(() -> service.summary(10L, "missing"))
                .isInstanceOf(BizException.class)
                .hasMessage("导入记录不存在");
        assertThatThrownBy(() -> service.detailById(10L, "missing"))
                .isInstanceOf(BizException.class)
                .hasMessage("导入记录不存在");
        when(accounts.findById(10L))
                .thenReturn(Optional.of(
                        Account.builder().userType(AccountTypes.PERSONAL).build()));
        assertThatThrownBy(() -> service.summary(10L, "missing"))
                .isInstanceOf(BizException.class)
                .hasMessage("仅学校账号可使用批量导入");
        verify(tasks, never()).rows("missing");
    }

    @Test
    @DisplayName("重试耗尽保留成功行、失败剩余行并记部分成功及任务级错误")
    void exhaustedRetriesPreservePartialSuccess() {
        final SchoolImportTaskRepository tasks = mock(SchoolImportTaskRepository.class);
        final SchoolImportTask base = SchoolImportApplicationServiceTest.task(SchoolImportTaskStatus.PROCESSING, 2027L);
        final SchoolImportTask task = new SchoolImportTask(
                base.id(),
                base.accountId(),
                base.schoolId(),
                base.competitionId(),
                base.zipUrl(),
                base.zipName(),
                base.excelUrl(),
                base.excelName(),
                base.importType(),
                base.status(),
                2,
                1,
                1,
                0,
                base.leaseUntil(),
                base.uploadExpiresAt(),
                5,
                base.createdAt(),
                base.updatedAt(),
                null);
        when(tasks.claimNext(
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(Optional.of(task));
        when(tasks.rows("task-1"))
                .thenReturn(java.util.List.of(
                        new com.weiran.cqt.domain.entry.SchoolImportRowResult(
                                1, "task-1", 3, 3, null, 1L, "SUCCESS", "{}", null),
                        new com.weiran.cqt.domain.entry.SchoolImportRowResult(
                                2, "task-1", 4, 4, null, null, "PENDING", "{}", null)));
        final SchoolImportApplicationService service = SchoolImportApplicationServiceTest.service(
                mock(AccountRepository.class), mock(CompetitionRepository.class), tasks);
        assertThat(service.processNext()).isTrue();
        verify(tasks)
                .markFailure(
                        org.mockito.ArgumentMatchers.eq("task-1"),
                        org.mockito.ArgumentMatchers.eq(4),
                        org.mockito.ArgumentMatchers.eq(4),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class));
        verify(tasks)
                .fail(
                        org.mockito.ArgumentMatchers.eq("task-1"),
                        org.mockito.ArgumentMatchers.eq(SchoolImportTaskStatus.PARTIAL),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class));
        verify(tasks, never())
                .insertRows(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class));
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
                now,
                null);
    }
}
