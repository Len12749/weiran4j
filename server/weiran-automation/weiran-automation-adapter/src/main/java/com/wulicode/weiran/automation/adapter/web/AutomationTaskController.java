package com.wulicode.weiran.automation.adapter.web;

import com.wulicode.weiran.automation.adapter.web.request.ToggleAutomationTaskRequest;
import com.wulicode.weiran.automation.api.task.AutomationRunDetailView;
import com.wulicode.weiran.automation.api.task.AutomationRunView;
import com.wulicode.weiran.automation.api.task.AutomationTaskService;
import com.wulicode.weiran.automation.api.task.AutomationTaskView;
import com.wulicode.weiran.framework.auth.RequiresPermission;
import com.wulicode.weiran.framework.log.OperationLog;
import com.wulicode.weiran.framework.page.PageQuery;
import com.wulicode.weiran.framework.page.PageResult;
import com.wulicode.weiran.framework.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自动化任务接口。
 *
 * <p>查询对所有登录用户开放（任务说明与执行结果全员可见）；启停需要 {@code automation:task:toggle}。
 * 任务类型与定时配置由迁移固定，没有新增与修改入口。
 */
@RestController
@RequestMapping("/api-backend/automation/task")
public class AutomationTaskController {

    private static final String MODULE = "自动化任务";

    private final AutomationTaskService service;

    /** 构造控制器。 */
    public AutomationTaskController(final AutomationTaskService service) {
        this.service = service;
    }

    /** 全部自动化任务。 */
    @GetMapping("/list")
    public List<AutomationTaskView> list() {
        return this.service.list();
    }

    /** 执行记录分页。 */
    @GetMapping("/listRuns")
    public PageResult<AutomationRunView> listRuns(
            @RequestParam final long taskId,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize) {
        return this.service.listRuns(taskId, new PageQuery(page, pageSize));
    }

    /** 一次执行的明细与全部评论内容。 */
    @GetMapping("/getRun/{id}")
    public AutomationRunDetailView getRun(@PathVariable final long id, @RequestParam final long taskId) {
        return this.service.getRun(taskId, id);
    }

    /** 管理员立即执行一次；本次执行时间同时成为下一次自动检查的基线。 */
    @PostMapping("/run/{id}")
    @RequiresPermission("automation:task:run")
    @OperationLog(module = AutomationTaskController.MODULE, description = "立即执行自动化任务")
    public AutomationTaskView run(@PathVariable final long id) {
        return this.service.trigger(id);
    }

    /** 删除一次执行记录及其本地评论。 */
    @DeleteMapping("/run/{runId}")
    @RequiresPermission("automation:task:run:delete")
    @OperationLog(module = AutomationTaskController.MODULE, description = "删除自动化任务执行记录")
    public ApiResponse<Void> deleteRun(@PathVariable final long runId, @RequestParam final long taskId) {
        this.service.deleteRun(taskId, runId);
        return ApiResponse.ok();
    }

    /** 管理员启停任务。 */
    @PutMapping("/toggle/{id}")
    @RequiresPermission("automation:task:toggle")
    @OperationLog(module = AutomationTaskController.MODULE, description = "启停自动化任务")
    public AutomationTaskView toggle(
            @PathVariable final long id, @Valid @RequestBody final ToggleAutomationTaskRequest request) {
        return this.service.setEnabled(id, request.requiredEnabled());
    }
}
