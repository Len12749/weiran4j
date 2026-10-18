package com.weiran.cqt.application.review;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiran.cqt.domain.review.ReviewWorkflowRepository;
import com.weiran.cqt.domain.review.WorkflowPhase;
import com.weiran.cqt.domain.review.WorkflowPreview;
import com.weiran.cqt.domain.review.WorkflowPreviewStore;
import com.weiran.cqt.domain.review.WorkflowReceipt;
import com.weiran.framework.error.BizException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 共享阶段锁、不可变预览与事务幂等回执。 */
public class ReviewWorkflowSupport {
    private final ReviewWorkflowRepository workflow;
    private final WorkflowPreviewStore previews;
    private final ReviewUserValidationService validation;
    private final ObjectMapper mapper;
    private final Clock clock;

    public ReviewWorkflowSupport(
            final ReviewWorkflowRepository workflow,
            final WorkflowPreviewStore previews,
            final ReviewUserValidationService validation,
            final ObjectMapper mapper,
            final Clock clock) {
        this.workflow = workflow;
        this.previews = previews;
        this.validation = validation;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void lockCompetitionAndValidate(final long competitionId) {
        @SuppressWarnings("unchecked")
        final Set<Long> existing = (Set<Long>) TransactionSynchronizationManager.getResource(this);
        final Set<Long> validated = existing == null ? new HashSet<>() : existing;
        if (existing == null) {
            TransactionSynchronizationManager.bindResource(this, validated);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(final int status) {
                    TransactionSynchronizationManager.unbindResourceIfPossible(ReviewWorkflowSupport.this);
                }
            });
        }
        if (validated.add(competitionId)) {
            this.validation.validate(competitionId);
        }
        this.workflow.lockCompetition(competitionId);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public WorkflowPhase lockAndValidate(final long phaseId) {
        final WorkflowPhase p = this.workflow.phase(phaseId).orElseThrow(() -> BizException.notFound("阶段不存在"));
        this.lockCompetitionAndValidate(p.competitionId());
        return this.workflow.lockPhase(phaseId);
    }

    public String json(final Object value) {
        try {
            return this.mapper.writeValueAsString(value);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("业务快照序列化失败", ex);
        }
    }

    public <T> T read(final String value, final Class<T> type) {
        try {
            return Objects.requireNonNull(this.mapper.readValue(value, type));
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("业务快照格式无效", ex);
        }
    }

    public String hash(final Object value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(this.json(value).getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public WorkflowPreview preview(
            final String purpose, final long operatorId, final long phaseId, final Object snapshot) {
        final WorkflowPhase phase = this.workflow.phase(phaseId).orElseThrow(() -> BizException.notFound("阶段不存在"));
        final WorkflowPreview value = new WorkflowPreview(
                UUID.randomUUID().toString(),
                purpose,
                operatorId,
                phaseId,
                phase.inputVersion(),
                this.hash(snapshot),
                this.json(snapshot),
                LocalDateTime.now(this.clock).plusMinutes(30));
        this.previews.save(value);
        return value;
    }

    public WorkflowPreview requirePreview(final String token, final String purpose, final long operatorId) {
        if (token == null || token.isBlank()) {
            throw BizException.badRequest("请先生成操作预览");
        }
        final WorkflowPreview p = this.previews.find(token).orElseThrow(() -> BizException.conflict("预览不存在，请重新预览"));
        if (p.operatorId() != operatorId) {
            throw BizException.notFound("预览不存在");
        }
        if (!p.purpose().equals(purpose) || !p.expiresAt().isAfter(LocalDateTime.now(this.clock))) {
            throw BizException.conflict("预览用途错误或已过期");
        }
        final WorkflowPhase phase = this.lockAndValidate(p.phaseId());
        if (phase.inputVersion() != p.inputVersion()) {
            throw BizException.conflict("输入已变化，请重新预览");
        }
        return p;
    }

    public WorkflowPreview lockPreview(final String token, final long operatorId) {
        if (token == null || token.isBlank()) {
            throw BizException.badRequest("请先生成操作预览");
        }
        final WorkflowPreview preview = this.previews.find(token).orElseThrow(() -> BizException.conflict("预览不存在"));
        if (preview.operatorId() != operatorId) {
            throw BizException.notFound("预览不存在");
        }
        this.lockAndValidate(preview.phaseId());
        return preview;
    }

    public void consumePreview(final String token) {
        this.previews.consume(token);
    }

    public <T> Optional<T> receipt(
            final long operator, final String operation, final String key, final Object payload, final Class<T> type) {
        this.validateKey(key);
        final Optional<T> existing = this.readReceipt(operator, operation, key, payload, type);
        if (existing.isPresent()) {
            return existing;
        }
        final String token;
        if (payload instanceof com.weiran.cqt.api.review.ReviewCommands.Confirm command) {
            token = command.previewToken();
        } else if (payload instanceof com.weiran.cqt.api.review.ReviewCommands.AssignmentState command) {
            token = command.previewToken();
        } else if (payload instanceof com.weiran.cqt.api.review.ReviewCommands.Attach command) {
            token = command.previewToken();
        } else {
            token = null;
        }
        if (token != null && !token.isBlank()) {
            this.lockPreview(token, operator);
        }
        return this.readReceipt(operator, operation, key, payload, type);
    }

    private <T> Optional<T> readReceipt(
            final long operator, final String operation, final String key, final Object payload, final Class<T> type) {
        return this.previews.receipt(operator, operation, key).map(r -> {
            if (!r.payloadHash().equals(this.hash(payload))) {
                throw BizException.conflict("相同请求键不能提交不同内容");
            }
            return this.read(r.result(), type);
        });
    }

    public void complete(
            final long operator, final String operation, final String key, final Object payload, final Object result) {
        this.validateKey(key);
        try {
            this.previews.saveReceipt(
                    new WorkflowReceipt(operator, operation, key, this.hash(payload), this.json(result)));
        } catch (final org.springframework.dao.DuplicateKeyException ex) {
            throw BizException.conflict("请求键已被并发使用，请重试原请求或更换请求键");
        }
    }

    private void validateKey(final String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw BizException.badRequest("请求幂等键不能为空且最多100字");
        }
    }
}
