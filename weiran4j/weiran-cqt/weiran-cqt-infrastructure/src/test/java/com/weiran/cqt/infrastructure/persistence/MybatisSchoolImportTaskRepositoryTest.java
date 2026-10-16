package com.weiran.cqt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weiran.cqt.domain.entry.SchoolImportTask;
import com.weiran.cqt.domain.entry.SchoolImportTaskStatus;
import com.weiran.cqt.infrastructure.persistence.entity.CqtSchoolImportTaskDO;
import com.weiran.cqt.infrastructure.persistence.mapper.CqtSchoolImportRowMapper;
import com.weiran.cqt.infrastructure.persistence.mapper.CqtSchoolImportTaskMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MybatisSchoolImportTaskRepositoryTest {

    @Test
    @DisplayName("领取租约过期的处理中任务并递增尝试次数")
    void reclaimsExpiredProcessingTask() {
        final CqtSchoolImportTaskMapper taskMapper = mock(CqtSchoolImportTaskMapper.class);
        final LocalDateTime now = LocalDateTime.of(2026, 10, 16, 12, 0);
        final LocalDateTime leaseUntil = now.plusMinutes(15);
        final CqtSchoolImportTaskDO row = new CqtSchoolImportTaskDO();
        row.setId("task-1");
        row.setAccountId(10L);
        row.setStatus("PROCESSING");
        row.setTotalRows(4);
        row.setProcessedRows(1);
        row.setSuccessCount(1);
        row.setFailureCount(0);
        row.setAttemptCount(2);
        row.setCreatedAt(now.minusHours(1));
        row.setUpdatedAt(now.minusMinutes(20));
        row.setLeaseUntil(now.minusMinutes(5));
        when(taskMapper.lockNext(now)).thenReturn(row);
        when(taskMapper.claim("task-1", leaseUntil, now)).thenReturn(1);
        final MybatisSchoolImportTaskRepository repository =
                new MybatisSchoolImportTaskRepository(taskMapper, mock(CqtSchoolImportRowMapper.class));

        final var claimed = repository.claimNext(now, leaseUntil);

        assertThat(claimed).isPresent().get().satisfies(task -> {
            assertThat(task.status()).isEqualTo(SchoolImportTaskStatus.PROCESSING);
            assertThat(task.attemptCount()).isEqualTo(3);
            assertThat(task.leaseUntil()).isEqualTo(leaseUntil);
        });
        verify(taskMapper).lockNext(now);
        verify(taskMapper).claim("task-1", leaseUntil, now);
    }

    @Test
    @DisplayName("没有待处理或过期任务时返回空")
    void returnsEmptyWhenNoTaskCanBeClaimed() {
        final CqtSchoolImportTaskMapper taskMapper = mock(CqtSchoolImportTaskMapper.class);
        final LocalDateTime now = LocalDateTime.of(2026, 10, 16, 12, 0);
        when(taskMapper.lockNext(now)).thenReturn(null);
        final MybatisSchoolImportTaskRepository repository =
                new MybatisSchoolImportTaskRepository(taskMapper, mock(CqtSchoolImportRowMapper.class));

        final Optional<SchoolImportTask> claimed = repository.claimNext(now, now.plusMinutes(15));

        assertThat(claimed).isEmpty();
    }
}
