package com.weiran.cqt.adapter.review;

import com.weiran.cqt.application.review.ReviewApplicationService;
import com.weiran.framework.auth.RequiresPermission;
import com.weiran.framework.error.BizException;
import com.weiran.framework.log.OperationLog;
import com.weiran.framework.page.PageQuery;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台评审与评分入口。输入敏感文字由专用输入DTO阻止操作日志序列化。 */
@RestController
@RequestMapping("/api/cqt/review")
public class ReviewAdminController {
    private final ReviewApplicationService service;

    public ReviewAdminController(final ReviewApplicationService service) {
        this.service = service;
    }

    private static <T> T body(final @Nullable T value) {
        if (value == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        return value;
    }

    @RequiresPermission({
        "cqt:entry-review:list",
        "cqt:review:list",
        "cqt:reviewer:list",
        "cqt:assignment:list",
        "cqt:assignment:create",
        "cqt:assignment:auto",
        "cqt:review-task:list",
        "cqt:score:list",
        "cqt:award-rule:list",
        "cqt:award-batch:list"
    })
    @GetMapping("/options")
    public Object options() {
        return this.service.options();
    }

    @RequiresPermission("cqt:entry-review:list")
    @GetMapping("/entries")
    public Object entries(
            @RequestParam(value = "competitionId", defaultValue = "0") final long competition,
            @RequestParam(required = false) final @Nullable String keyword,
            @RequestParam(required = false) final @Nullable String source,
            @RequestParam(required = false) final @Nullable String status,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize) {
        return this.service.entries(competition, keyword, source, status, new PageQuery(page, pageSize));
    }

    @RequiresPermission("cqt:entry-review:list")
    @GetMapping("/entries/{id}")
    public Object entry(@PathVariable("id") final long id) {
        return this.service.entry(id);
    }

    @RequiresPermission("cqt:entry-review:decide")
    @PostMapping("/entries/{id}/decision")
    @OperationLog(module = "常青藤评审", description = "decide")
    public Object decide(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Decision command) {
        return this.service.decideEntry(id, body(command).command());
    }

    @RequiresPermission("cqt:entry-review:prepare")
    @PostMapping("/targets/prepare-preview")
    @OperationLog(module = "常青藤评审", description = "preparePreview")
    public Object preparePreview(@RequestBody(required = false) final ReviewRequests.@Nullable Prepare command) {
        return this.service.preparePreview(body(command).command());
    }

    @RequiresPermission("cqt:entry-review:prepare")
    @PostMapping("/targets/prepare")
    @OperationLog(module = "常青藤评审", description = "prepare")
    public Object prepare(@RequestBody(required = false) final ReviewRequests.@Nullable Confirm command) {
        return this.service.prepare(body(command).command());
    }

    @RequiresPermission("cqt:review:list")
    @GetMapping("/phases")
    public Object phases(@RequestParam(value = "competitionId", defaultValue = "0") final long competition) {
        return this.service.phases(competition);
    }

    @RequiresPermission("cqt:review:update")
    @PostMapping("/phases")
    @OperationLog(module = "常青藤评审", description = "createPhases")
    public Object createPhases(@RequestBody(required = false) final ReviewRequests.@Nullable PhaseCreate command) {
        return this.service.createPhases(body(command).command());
    }

    @RequiresPermission("cqt:review:update")
    @PutMapping("/phases/{id}")
    @OperationLog(module = "常青藤评审", description = "rule")
    public Object rule(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Rule command) {
        return this.service.rule(id, body(command).command());
    }

    @RequiresPermission("cqt:review:list")
    @GetMapping("/phases/{id}/dimensions")
    public Object dimensions(@PathVariable("id") final long id) {
        return this.service.dimensions(id);
    }

    @RequiresPermission("cqt:review:dimension")
    @PutMapping("/phases/{id}/dimensions")
    @OperationLog(module = "常青藤评审", description = "saveDimensions")
    public Object saveDimensions(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Dimensions command) {
        return this.service.dimensions(id, body(command).command());
    }

    @RequiresPermission("cqt:review:open")
    @PostMapping("/phases/{id}/state-preview")
    @OperationLog(module = "常青藤评审", description = "statePreview")
    public Object statePreview(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable State command) {
        return this.service.statePreview(id, body(command).command());
    }

    @RequiresPermission("cqt:review:open")
    @PostMapping("/phases/{id}/state")
    @OperationLog(module = "常青藤评审", description = "state")
    public Object state(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Confirm command) {
        return this.service.state(id, body(command).command());
    }

    @RequiresPermission("cqt:reviewer:list")
    @GetMapping("/reviewers")
    public Object reviewers(
            @RequestParam(value = "competitionId", defaultValue = "0") final long competition,
            @RequestParam(required = false) final @Nullable String keyword,
            @RequestParam(value = "phaseId", defaultValue = "0") final long phase,
            @RequestParam(value = "userId", required = false) final @Nullable Long user,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize) {
        return this.service.scopes(competition, phase, user, keyword, new PageQuery(page, pageSize));
    }

    @RequiresPermission("cqt:reviewer:create")
    @PostMapping("/reviewers")
    @OperationLog(module = "常青藤评审", description = "createReviewer")
    public Object createReviewer(@RequestBody(required = false) final ReviewRequests.@Nullable Scope command) {
        return this.service.createScope(body(command).command());
    }

    @RequiresPermission("cqt:reviewer:update")
    @PostMapping("/reviewers/{id}/state-preview")
    @OperationLog(module = "常青藤评审", description = "reviewerPreview")
    public Object reviewerPreview(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Scope command) {
        return this.service.scopePreview(id, body(command).command());
    }

    @RequiresPermission("cqt:reviewer:update")
    @PutMapping("/reviewers/{id}")
    @OperationLog(module = "常青藤评审", description = "reviewer")
    public Object reviewer(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Scope command) {
        return this.service.updateScope(id, body(command).command());
    }

    @RequiresPermission("cqt:assignment:list")
    @GetMapping("/assignments")
    public Object assignments(
            @RequestParam(value = "competitionId", defaultValue = "0") final long competition,
            @RequestParam(value = "phaseId", defaultValue = "0") final long phase,
            @RequestParam(value = "userId", required = false) final @Nullable Long user,
            @RequestParam(required = false) final @Nullable String state,
            @RequestParam(required = false) final @Nullable String keyword,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize) {
        return this.service.assignments(competition, phase, user, state, keyword, new PageQuery(page, pageSize));
    }

    @RequiresPermission({"cqt:assignment:create", "cqt:assignment:auto"})
    @GetMapping("/assignment-candidates")
    public Object candidates(
            @RequestParam(value = "competitionId", defaultValue = "0") final long competition,
            @RequestParam(value = "phaseId", defaultValue = "0") final long phase,
            @RequestParam(required = false) final @Nullable String source,
            @RequestParam(required = false) final @Nullable String targetType,
            @RequestParam(value = "categoryId", required = false) final @Nullable Long category,
            @RequestParam(required = false) final @Nullable String keyword,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize) {
        return this.service.candidates(
                competition, phase, source, targetType, category, keyword, new PageQuery(page, pageSize));
    }

    @RequiresPermission({"cqt:assignment:create", "cqt:assignment:auto"})
    @PostMapping("/assignments/preview")
    @OperationLog(module = "常青藤评审", description = "assignmentPreview")
    public Object assignmentPreview(
            @RequestBody(required = false) final ReviewRequests.@Nullable AssignmentPreview command) {
        return this.service.assignmentPreview(body(command).command());
    }

    @RequiresPermission({"cqt:assignment:create", "cqt:assignment:auto"})
    @PostMapping("/assignments/confirm")
    @OperationLog(module = "常青藤评审", description = "assign")
    public Object assign(@RequestBody(required = false) final ReviewRequests.@Nullable Confirm command) {
        return this.service.assign(body(command).command());
    }

    @RequiresPermission({"cqt:assignment:cancel", "cqt:assignment:restore"})
    @PostMapping("/assignments/{id}/state-preview")
    @OperationLog(module = "常青藤评审", description = "assignmentStatePreview")
    public Object assignmentStatePreview(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable AssignmentState command) {
        return this.service.assignmentStatePreview(id, body(command).command());
    }

    @RequiresPermission({"cqt:assignment:cancel", "cqt:assignment:restore"})
    @PostMapping("/assignments/{id}/state")
    @OperationLog(module = "常青藤评审", description = "assignmentState")
    public Object assignmentState(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable AssignmentState command) {
        return this.service.assignmentState(id, body(command).command());
    }

    @RequiresPermission("cqt:review-task:list")
    @GetMapping("/my-tasks")
    public Object myTasks(
            @RequestParam(required = false) final @Nullable String status,
            @RequestParam(value = "competitionId", defaultValue = "0") final long competition,
            @RequestParam(value = "phaseId", defaultValue = "0") final long phase,
            @RequestParam(required = false) final @Nullable String state,
            @RequestParam(required = false) final @Nullable String keyword,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize) {
        return this.service.myTasks(
                competition, phase, status == null ? state : status, keyword, new PageQuery(page, pageSize));
    }

    @RequiresPermission("cqt:review-task:list")
    @GetMapping("/my-tasks/{id}")
    public Object task(@PathVariable("id") final long id) {
        return this.service.task(id);
    }

    @RequiresPermission("cqt:review-task:score")
    @PutMapping("/my-tasks/{id}/score")
    @OperationLog(module = "常青藤评审", description = "score")
    public Object score(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Score command) {
        return this.service.score(id, body(command).command());
    }

    @RequiresPermission("cqt:score:list")
    @GetMapping("/scores")
    public Object scores(
            @RequestParam(value = "competitionId", defaultValue = "0") final long competition,
            @RequestParam(value = "phaseId", defaultValue = "0") final long phase,
            @RequestParam(required = false) final @Nullable String state,
            @RequestParam(required = false) final @Nullable String keyword,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize) {
        return this.service.scores(competition, phase, state, keyword, new PageQuery(page, pageSize));
    }

    @RequiresPermission("cqt:score:calculate")
    @PostMapping("/phases/{id}/calculate-preview")
    @OperationLog(module = "常青藤评审", description = "calculatePreview")
    public Object calculatePreview(@PathVariable("id") final long id) {
        return this.service.calculatePreview(id);
    }

    @RequiresPermission("cqt:score:calculate")
    @PostMapping("/phases/{id}/calculate")
    @OperationLog(module = "常青藤评审", description = "calculate")
    public Object calculate(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Confirm command) {
        return this.service.calculate(id, body(command).command());
    }

    @RequiresPermission("cqt:score:correct")
    @PostMapping("/scores/{id}/correct")
    @OperationLog(module = "常青藤评审", description = "correct")
    public Object correct(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Correction command) {
        return this.service.correct(id, body(command).command());
    }

    @RequiresPermission("cqt:score:history")
    @GetMapping("/scores/{id}/history")
    public Object history(@PathVariable("id") final long id) {
        return this.service.history(id);
    }

    @RequiresPermission("cqt:score:advance")
    @PostMapping("/phases/{id}/advance-preview")
    @OperationLog(module = "常青藤评审", description = "advancePreview")
    public Object advancePreview(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Advance command) {
        return this.service.advancePreview(id, body(command).command());
    }

    @RequiresPermission("cqt:score:advance")
    @PostMapping("/phases/{id}/advance")
    @OperationLog(module = "常青藤评审", description = "advance")
    public Object advance(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Confirm command) {
        return this.service.advance(id, body(command).command());
    }

    @RequiresPermission("cqt:score:promote")
    @PostMapping("/phases/{id}/promote")
    @OperationLog(module = "常青藤评审", description = "promote")
    public Object promote(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Promote command) {
        return this.service.promote(id, body(command).command());
    }

    @RequiresPermission("cqt:score:list")
    @GetMapping("/qualifications")
    public Object qualifications(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(value = "competitionId", defaultValue = "0") final long competition) {
        return this.service.qualifications(competition, new PageQuery(page, pageSize));
    }

    @RequiresPermission("cqt:score:promote")
    @PostMapping("/qualifications/{id}/attach-preview")
    @OperationLog(module = "常青藤评审", description = "attachPreview")
    public Object attachPreview(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Attach command) {
        return this.service.attachPreview(id, body(command).command());
    }

    @RequiresPermission("cqt:score:promote")
    @PostMapping("/qualifications/{id}/attach")
    @OperationLog(module = "常青藤评审", description = "attach")
    public Object attach(
            @PathVariable("id") final long id,
            @RequestBody(required = false) final ReviewRequests.@Nullable Attach command) {
        return this.service.attach(id, body(command).command());
    }

    @RequiresPermission("cqt:score:promote")
    @GetMapping("/qualifications/{id}/candidates")
    public Object qualificationCandidates(@PathVariable("id") final long id) {
        return this.service.qualificationCandidates(id);
    }
}
