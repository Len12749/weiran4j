package com.weiran.cqt.application.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiran.cqt.api.review.ReviewCommands;
import com.weiran.cqt.domain.review.ReviewWorkflowRepository;
import com.weiran.cqt.domain.review.WorkflowPhase;
import com.weiran.cqt.domain.review.WorkflowPreview;
import com.weiran.cqt.domain.review.WorkflowPreviewStore;
import com.weiran.cqt.domain.review.WorkflowReceipt;
import com.weiran.framework.error.BizException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class ReviewWorkflowSupportTest {
    private static final String TOKEN = "44cfef76-b91e-4bd7-a38e-25bed88444aa";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneOffset.UTC);
    private final WorkflowPreviewStore store = mock(WorkflowPreviewStore.class);
    private final ReviewWorkflowRepository repository = mock(ReviewWorkflowRepository.class);
    private final ReviewWorkflowSupport support = spy(new ReviewWorkflowSupport(
            this.repository,
            this.store,
            mock(ReviewUserValidationService.class),
            new ObjectMapper(),
            ReviewWorkflowSupportTest.CLOCK));

    @Test
    @DisplayName("成功回执独立于已清理预览，重试同体直接重放")
    void replaysReceiptWithoutDependingOnExpiredPreview() {
        final ReviewCommands.Confirm command = ReviewWorkflowSupportTest.command();
        final WorkflowReceipt receipt = this.receipt(command);
        when(this.store.receipt(7, "CALCULATE", "request")).thenReturn(Optional.of(receipt));
        assertThat(this.support.receipt(7, "CALCULATE", "request", command, Long.class))
                .contains(42L);
        verify(this.store, never()).find(any());
        verify(this.store, never()).consume(any());
    }

    @Test
    @DisplayName("同一请求键更换正文包括敏感评语仍返回冲突")
    void rejectsReceiptPayloadChangesIncludingComment() {
        final ReviewCommands.Score original =
                new ReviewCommands.Score("DRAFT", null, java.util.List.of(), "原评语", 0, "request");
        final ReviewCommands.Score changed =
                new ReviewCommands.Score("DRAFT", null, java.util.List.of(), "新评语", 0, "request");
        final WorkflowReceipt receipt = this.receipt(original);
        when(this.store.receipt(7, "SCORE", "request")).thenReturn(Optional.of(receipt));
        assertThatThrownBy(() -> this.support.receipt(7, "SCORE", "request", changed, Long.class))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("相同请求键");
        assertThat(this.support.hash(original)).isNotEqualTo(this.support.hash(changed));
    }

    @Test
    @DisplayName("并发首查未命中，阶段锁后再次读取已提交回执且不消费预览")
    void rechecksReceiptAfterSerializingOnPhase() {
        final ReviewCommands.Confirm command = ReviewWorkflowSupportTest.command();
        final WorkflowReceipt receipt = this.receipt(command);
        when(this.store.receipt(7, "CALCULATE", "request")).thenReturn(Optional.empty(), Optional.of(receipt));
        when(this.store.find(ReviewWorkflowSupportTest.TOKEN))
                .thenReturn(Optional.of(this.preview(7, "CALCULATE", 3, 30)));
        doReturn(ReviewWorkflowSupportTest.phase(3)).when(this.support).lockAndValidate(4);
        assertThat(this.support.receipt(7, "CALCULATE", "request", command, Long.class))
                .contains(42L);
        verify(this.support).lockAndValidate(4);
        verify(this.store, never()).consume(any());
    }

    @Test
    @DisplayName("他人预览不可读取且不获取业务锁")
    void rejectsAnotherOperatorsPreview() {
        when(this.store.find(ReviewWorkflowSupportTest.TOKEN))
                .thenReturn(Optional.of(this.preview(8, "CALCULATE", 3, 30)));
        assertThatThrownBy(() -> this.support.requirePreview(ReviewWorkflowSupportTest.TOKEN, "CALCULATE", 7))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
        verify(this.support, never()).lockAndValidate(4);
    }

    @Test
    @DisplayName("错误用途与30分钟到期均拒绝确认")
    void rejectsWrongPurposeAndExpiredPreview() {
        when(this.store.find(ReviewWorkflowSupportTest.TOKEN))
                .thenReturn(Optional.of(this.preview(7, "CALCULATE", 3, 30)));
        assertThatThrownBy(() -> this.support.requirePreview(ReviewWorkflowSupportTest.TOKEN, "ASSIGN", 7))
                .isInstanceOf(BizException.class);
        when(this.store.find(ReviewWorkflowSupportTest.TOKEN))
                .thenReturn(Optional.of(this.preview(7, "CALCULATE", 3, 0)));
        assertThatThrownBy(() -> this.support.requirePreview(ReviewWorkflowSupportTest.TOKEN, "CALCULATE", 7))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("过期");
    }

    @Test
    @DisplayName("输入版本漂移使旧预览冲突且不消费")
    void rejectsInputVersionDrift() {
        when(this.store.find(ReviewWorkflowSupportTest.TOKEN))
                .thenReturn(Optional.of(this.preview(7, "CALCULATE", 3, 30)));
        doReturn(ReviewWorkflowSupportTest.phase(4)).when(this.support).lockAndValidate(4);
        assertThatThrownBy(() -> this.support.requirePreview(ReviewWorkflowSupportTest.TOKEN, "CALCULATE", 7))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("输入已变化");
        verify(this.store, never()).consume(any());
    }

    @Test
    @DisplayName("校验预览不提前消费，确认用例才显式消费")
    void leavesConsumptionToConfirmedBusinessTransaction() {
        final WorkflowPreview preview = this.preview(7, "CALCULATE", 3, 30);
        when(this.store.find(ReviewWorkflowSupportTest.TOKEN)).thenReturn(Optional.of(preview));
        doReturn(ReviewWorkflowSupportTest.phase(3)).when(this.support).lockAndValidate(4);
        assertThat(this.support.requirePreview(ReviewWorkflowSupportTest.TOKEN, "CALCULATE", 7))
                .isEqualTo(preview);
        verify(this.store, never()).consume(any());
        this.support.consumePreview(ReviewWorkflowSupportTest.TOKEN);
        verify(this.store).consume(ReviewWorkflowSupportTest.TOKEN);
    }

    @Test
    @DisplayName("跨阶段请求键并发唯一冲突转为业务409")
    void mapsReceiptUniqueConflictToBusinessConflict() {
        doThrow(new DuplicateKeyException("duplicate")).when(this.store).saveReceipt(any());
        assertThatThrownBy(() ->
                        this.support.complete(7, "CALCULATE", "request", ReviewWorkflowSupportTest.command(), 42L))
                .isInstanceOfSatisfying(
                        BizException.class,
                        error -> assertThat(error.getErrorCode().httpStatus()).isEqualTo(409));
    }

    @Test
    @DisplayName("回执隔离操作者和操作种类且拒绝无效请求键")
    void isolatesActorAndOperationAndValidatesKeys() {
        final WorkflowReceipt receipt = this.receipt("payload");
        when(this.store.receipt(7, "CALCULATE", "request")).thenReturn(Optional.of(receipt));
        assertThat(this.support.receipt(8, "CALCULATE", "request", "payload", Long.class))
                .isEmpty();
        assertThat(this.support.receipt(7, "ASSIGN", "request", "payload", Long.class))
                .isEmpty();
        assertThatThrownBy(() -> this.support.receipt(7, "CALCULATE", " ", "payload", Long.class))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> this.support.receipt(7, "CALCULATE", "x".repeat(101), "payload", Long.class))
                .isInstanceOf(BizException.class);
    }

    private WorkflowReceipt receipt(final Object command) {
        return new WorkflowReceipt(7, "CALCULATE", "request", this.support.hash(command), "42");
    }

    private WorkflowPreview preview(final long owner, final String purpose, final long version, final int minutes) {
        return new WorkflowPreview(
                ReviewWorkflowSupportTest.TOKEN,
                purpose,
                owner,
                4,
                version,
                "hash",
                "{}",
                LocalDateTime.ofInstant(ReviewWorkflowSupportTest.CLOCK.instant(), ZoneOffset.UTC)
                        .plusMinutes(minutes));
    }

    private static ReviewCommands.Confirm command() {
        return new ReviewCommands.Confirm(ReviewWorkflowSupportTest.TOKEN, "request", true);
    }

    private static WorkflowPhase phase(final long version) {
        return new WorkflowPhase(4, 5, "PROVINCIAL", "CLOSED", 6, version, 0);
    }
}
