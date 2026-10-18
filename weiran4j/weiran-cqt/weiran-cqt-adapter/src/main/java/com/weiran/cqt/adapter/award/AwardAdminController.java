package com.weiran.cqt.adapter.award;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weiran.cqt.api.award.AwardBatchView;
import com.weiran.cqt.api.award.AwardPreviewCommand;
import com.weiran.cqt.api.award.AwardPreviewView;
import com.weiran.cqt.api.award.AwardRuleCommand;
import com.weiran.cqt.api.award.AwardRuleView;
import com.weiran.cqt.api.award.AwardService;
import com.weiran.framework.auth.CurrentUser;
import com.weiran.framework.auth.RequiresPermission;
import com.weiran.framework.log.OperationLog;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端独立评奖：预览与确认严格分离。 */
@RestController
@RequestMapping("/api/cqt/award")
public class AwardAdminController {
    private final AwardService service;

    public AwardAdminController(final AwardService service) {
        this.service = service;
    }

    @GetMapping("/rules")
    @RequiresPermission("cqt:award-rule:list")
    public PageResult<AwardRuleView> rules(
            @RequestParam(required = false) final @Nullable Long competitionId,
            @RequestParam(required = false) final @Nullable Long phaseId,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize) {
        return this.service.rules(competitionId, phaseId, new PageQuery(page, pageSize));
    }

    @PostMapping("/rules")
    @RequiresPermission("cqt:award-rule:create")
    @OperationLog(module = "奖项规则", description = "创建规则")
    public Map<String, Long> create(@RequestBody final AwardRuleCommand body) {
        return Map.of("id", this.service.createRule(CurrentUser.require().id(), body), "version", 1L);
    }

    @PutMapping("/rules/{id}")
    @RequiresPermission("cqt:award-rule:update")
    @OperationLog(module = "奖项规则", description = "修订规则")
    public Map<String, Long> update(@PathVariable final long id, @RequestBody final AwardRuleCommand body) {
        return Map.of(
                "id",
                id,
                "version",
                this.service.updateRule(CurrentUser.require().id(), id, body));
    }

    @PostMapping("/preview")
    @RequiresPermission("cqt:award-batch:preview")
    @OperationLog(module = "评奖批次", description = "生成评奖预览")
    public AwardPreviewView preview(@RequestBody final AwardPreviewCommand body) {
        return this.service.preview(CurrentUser.require().id(), body);
    }

    public record ConfirmRequest(
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            String previewToken,

            String requestKey,
            @Nullable Long supersedesBatchId) {}

    @PostMapping("/batches")
    @RequiresPermission("cqt:award-batch:confirm")
    @OperationLog(module = "评奖批次", description = "确认评奖批次")
    public Map<String, Long> confirm(@RequestBody final ConfirmRequest body) {
        return Map.of(
                "id",
                this.service.confirm(
                        CurrentUser.require().id(), body.previewToken(), body.requestKey(), body.supersedesBatchId()),
                "version",
                1L);
    }

    @GetMapping("/batches")
    @RequiresPermission("cqt:award-batch:list")
    public PageResult<AwardBatchView> batches(
            @RequestParam(required = false) final @Nullable Long competitionId,
            @RequestParam(required = false) final @Nullable Long phaseId,
            @RequestParam(required = false) final @Nullable String status,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize) {
        return this.service.batches(competitionId, phaseId, status, new PageQuery(page, pageSize));
    }

    @GetMapping("/batches/{id}")
    @RequiresPermission("cqt:award-batch:detail")
    public AwardBatchView detail(@PathVariable final long id) {
        return this.service.detail(id);
    }

    public record RevokeRequest(
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            String reason,

            long expectedVersion,
            String requestKey) {}

    @PostMapping("/batches/{id}/revoke")
    @RequiresPermission("cqt:award-batch:revoke")
    @OperationLog(module = "评奖批次", description = "撤销评奖批次")
    public Map<String, Long> revoke(@PathVariable final long id, @RequestBody final RevokeRequest body) {
        this.service.revoke(CurrentUser.require().id(), id, body.reason(), body.expectedVersion(), body.requestKey());
        return Map.of("id", id, "version", body.expectedVersion() + 1);
    }
}
