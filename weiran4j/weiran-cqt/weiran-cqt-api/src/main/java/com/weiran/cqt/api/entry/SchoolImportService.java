package com.weiran.cqt.api.entry;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** 学校账号批量创建作品与查询导入任务。 */
public interface SchoolImportService {

    /** 生成无示例数据的报名模板工作簿。 */
    byte[] templateWorkbook();

    /** 查询当前学校在指定赛事是否已有待处理导入。 */
    boolean checkSignup(long accountId, @Nullable Long competitionId);

    /** 当前学校已导入作品列表。 */
    List<Map<String, @Nullable Object>> schoolProducts(long accountId, @Nullable Long competitionId);

    /** 登记已上传 ZIP，返回持久上传会话 ID。 */
    String registerZip(long accountId, String url, @Nullable String name);

    /** 登记 Excel 并关联已登记 ZIP 会话，返回可供旧页面使用的 Excel ID。 */
    String registerExcel(long accountId, String sessionId, String url, @Nullable String name);

    /** 确认并排队处理，返回任务 ID。 */
    String confirm(
            long accountId,
            long competitionId,
            String sessionId,
            String excelId,
            @Nullable String importType,
            @Nullable String zipName,
            @Nullable String excelName);

    /** 当前学校指定赛事的任务列表。 */
    List<SchoolImportTaskView> tasks(long accountId, long competitionId);

    /** 按当前学校和任务ID读取轻量摘要，不加载结果行。 */
    SchoolImportTaskView summary(long accountId, String taskId);

    /** 按当前学校和任务ID读取逐行结果，无需赛事参数。 */
    SchoolImportTaskView detailById(long accountId, String taskId);

    /** 仅当前学校可读任务详情。 */
    SchoolImportTaskView detail(long accountId, long competitionId, String taskId);
}
