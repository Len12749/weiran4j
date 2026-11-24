package com.wulicode.weiran.automation.api.task;

import com.wulicode.weiran.framework.page.PageQuery;
import com.wulicode.weiran.framework.page.PageResult;
import java.util.List;

/** 自动化任务查询与启停用例。 */
public interface AutomationTaskService {

    /** 全部自动化任务，按 ID 升序。 */
    List<AutomationTaskView> list();

    /** 某个任务的执行记录，按开始时间倒序分页。 */
    PageResult<AutomationRunView> listRuns(long taskId, PageQuery query);

    /** 一次执行的明细与全部评审结果。 */
    AutomationRunDetailView getRun(long taskId, long runId);

    /** 管理员启停任务。 */
    AutomationTaskView setEnabled(long id, boolean enabled);

    /** 管理员立即执行一次；这次执行时间同时成为下一次自动检查的基线。 */
    AutomationTaskView trigger(long id);

    /** 删除一次执行记录及其评审结果。 */
    void deleteRun(long taskId, long runId);
}
