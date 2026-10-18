package com.weiran.cqt.adapter.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weiran.cqt.api.entry.SchoolImportService;
import com.weiran.cqt.api.entry.SchoolImportTaskView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SchoolImportControllerTest {

    @AfterEach
    void clearCurrentAccount() {
        PortalAccount.clear();
    }

    @Test
    @DisplayName("任务列表保持 uniapp 消费的双层 data 包装与字段")
    void taskListMatchesUniappShape() {
        final SchoolImportService service = mock(SchoolImportService.class);
        final SchoolImportTaskView task = SchoolImportControllerTest.task("QUEUED");
        when(service.tasks(77L, 2027L)).thenReturn(List.of(task));
        PortalAccount.set(77L);

        final PortalResult<Map<String, Object>> result =
                new SchoolImportController(service).tasks(Map.of("competitionid", "2027"));

        assertThat(result.code()).isEqualTo(PortalResult.SUCCESS_CODE);
        final Map<String, Object> data = Objects.requireNonNull(result.data());
        assertThat(data).containsKey("data");
        final List<?> values = (List<?>) data.get("data");
        assertThat(values)
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("id", "task-1")
                .containsEntry("competitionid", 2027L)
                .containsEntry("status", 0)
                .containsEntry("failed_count", 1);
    }

    @Test
    @DisplayName("任务详情返回逐行结果数组")
    void taskDetailIncludesResults() {
        final SchoolImportService service = mock(SchoolImportService.class);
        final List<Map<String, @Nullable Object>> rows =
                List.of(Map.of("row_number", 3, "status", 2, "error_msg", "赛项无效"));
        when(service.detail(77L, 2027L, "task-1")).thenReturn(SchoolImportControllerTest.task("PARTIAL", rows));
        PortalAccount.set(77L);

        final PortalResult<Map<String, @Nullable Object>> result =
                new SchoolImportController(service).detail("2027", "task-1");

        final Map<String, @Nullable Object> data = Objects.requireNonNull(result.data());
        assertThat(data).containsEntry("id", "task-1");
        assertThat(data.get("results")).isEqualTo(rows);
        verify(service).detail(77L, 2027L, "task-1");
    }

    @Test
    @DisplayName("确认导入忽略旧客户端 userid 并使用当前 portal 账号")
    void confirmationIgnoresRequestUserId() {
        final SchoolImportService service = mock(SchoolImportService.class);
        when(service.confirm(77L, 2027L, "session-1", "session-1", "school", "作品.zip", "报名.xlsx"))
                .thenReturn("task-1");
        PortalAccount.set(77L);

        final PortalResult<Map<String, Object>> result = new SchoolImportController(service)
                .confirm(Map.of(
                        "competitionid", "2027",
                        "session_id", "session-1",
                        "excel_id", "session-1",
                        "type", "school",
                        "zip_name", "作品.zip",
                        "excel_name", "报名.xlsx",
                        "userid", 999L));

        final Map<String, Object> data = Objects.requireNonNull(result.data());
        assertThat(data)
                .containsEntry("task_id", "task-1")
                .containsEntry("job_id", "task-1")
                .containsEntry("competitionid", 2027L);
        assertThat(data).containsEntry("message", "已提交导入任务");
        verify(service).confirm(77L, 2027L, "session-1", "session-1", "school", "作品.zip", "报名.xlsx");
    }

    @Test
    @DisplayName("进度与结果区分排队、部分成功和失败，待处理行不计失败")
    void progressAndResultReflectTerminalStates() {
        final SchoolImportService service = mock(SchoolImportService.class);
        final SchoolImportController controller = new SchoolImportController(service);
        PortalAccount.set(77L);
        for (final String state : List.of("QUEUED", "PROCESSING", "PARTIAL", "FAILED", "COMPLETED")) {
            final List<Map<String, @Nullable Object>> rows = List.of(
                    Map.of("row_number", 3, "status", 0),
                    Map.of("row_number", 4, "status", 1),
                    Map.of("row_number", 5, "status", 2));
            final SchoolImportTaskView task = SchoolImportControllerTest.task(state, rows);
            when(service.summary(77L, "task-1")).thenReturn(task);
            when(service.detailById(77L, "task-1")).thenReturn(task);
            final Map<String, @Nullable Object> progress =
                    Objects.requireNonNull(controller.progress("task-1").data());
            final boolean terminal = List.of("PARTIAL", "FAILED", "COMPLETED").contains(state);
            assertThat(progress)
                    .containsEntry("state", state)
                    .containsEntry("finished", terminal)
                    .containsEntry("progress", terminal ? 100 : 66)
                    .containsEntry("processed_rows", 2);
            final Map<String, @Nullable Object> result =
                    Objects.requireNonNull(controller.result("task-1").data());
            assertThat((List<?>) result.get("success_rows")).hasSize(1);
            assertThat((List<?>) result.get("failed_rows")).hasSize(1);
            assertThat(result.get("completed_at")).isEqualTo(terminal ? task.updatedAt() : null);
            if ("PARTIAL".equals(state)) {
                assertThat(result).containsEntry("status", 2);
            }
        }
    }

    @Test
    @DisplayName("非终态百分比不溢出且最多99，未知总数为0")
    void progressBoundsRemainSafe() {
        final SchoolImportService service = mock(SchoolImportService.class);
        final SchoolImportController controller = new SchoolImportController(service);
        PortalAccount.set(77L);
        for (final int total : List.of(0, 1, Integer.MAX_VALUE)) {
            final SchoolImportTaskView task = new SchoolImportTaskView(
                    "task",
                    0,
                    "PROCESSING",
                    null,
                    null,
                    null,
                    null,
                    total,
                    Integer.MAX_VALUE,
                    0,
                    0,
                    null,
                    null,
                    List.of(),
                    null);
            when(service.summary(77L, "task")).thenReturn(task);
            assertThat(Objects.requireNonNull(controller.progress("task").data()))
                    .containsEntry("progress", total == 0 ? 0 : 99);
        }
    }

    private static SchoolImportTaskView task(final String status) {
        return SchoolImportControllerTest.task(status, List.of());
    }

    private static SchoolImportTaskView task(final String status, final List<Map<String, @Nullable Object>> rows) {
        return new SchoolImportTaskView(
                "task-1",
                2027L,
                status,
                "/files/works.zip",
                "works.zip",
                "/files/entry.xlsx",
                "entry.xlsx",
                3,
                2,
                1,
                1,
                LocalDateTime.of(2026, 10, 16, 12, 0),
                LocalDateTime.of(2026, 10, 16, 12, 5),
                rows,
                null);
    }
}
