package com.weiran.cqt.application.entryexport;

import com.weiran.cqt.domain.entryexport.EntryExportRow;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class EntryExportWorkbookWriter {
    private static final List<String> HEADERS = List.of(
            "赛事编号", "赛事", "阶段编号", "阶段代码", "阶段", "作品唯一ID", "报名号", "作品名称", "数据来源", "评分对象编号", "评分对象类型", "参赛人编号", "参赛人",
            "赛区", "赛项", "专业", "组别", "初审状态", "对象状态", "成绩状态", "最终成绩", "成绩版本", "奖项");

    public byte[] write(
            final long competitionId,
            final long phaseId,
            final String phaseCode,
            final List<EntryExportRow> rows,
            final Clock clock) {
        try (final XSSFWorkbook workbook = new XSSFWorkbook();
                final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final Sheet sheet = workbook.createSheet("作品清单");
            final Row header = sheet.createRow(0);
            final CellStyle text = workbook.createCellStyle();
            text.setDataFormat(workbook.createDataFormat().getFormat("@"));
            for (int i = 0; i < HEADERS.size(); i++) {
                header.createCell(i).setCellValue(HEADERS.get(i));
                sheet.setColumnWidth(i, (i == 7 ? 36 : i == 18 ? 28 : 18) * 256);
            }
            int index = 1;
            for (final EntryExportRow item : rows) {
                final Row row = sheet.createRow(index++);
                final String[] values = {
                    Long.toString(item.competitionId()),
                    item.competition(),
                    Long.toString(item.phaseId()),
                    item.phaseCode(),
                    item.phaseName(),
                    Long.toString(item.entryId()),
                    item.entryNo(),
                    item.title(),
                    item.source(),
                    item.targetId() == null ? "" : item.targetId().toString(),
                    item.targetType(),
                    item.participantId() == null ? "" : item.participantId().toString(),
                    item.participantName(),
                    item.region(),
                    item.category(),
                    item.major(),
                    item.groupName(),
                    Integer.toString(item.reviewStatus()),
                    item.targetStatus(),
                    item.scoreStatus(),
                    "VALID".equals(item.scoreStatus()) && item.score() != null
                            ? item.score().toPlainString()
                            : "",
                    item.scoreVersion() == null ? "" : item.scoreVersion().toString(),
                    ""
                };
                for (int column = 0; column < values.length; column++) {
                    final var cell = row.createCell(column);
                    cell.setCellValue(values[column]);
                    if (column == 0 || column == 2 || column == 5 || column == 6 || column == 9 || column == 11) {
                        cell.setCellStyle(text);
                    }
                }
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, rows.size()), 0, 22));
            final Sheet instructions = workbook.createSheet("填写说明");
            instructions.setColumnWidth(0, 110 * 256);
            final List<String> notes = List.of(
                    "模板版本：CQT_AWARD_IMPORT_V1",
                    "赛事编号：" + competitionId,
                    "阶段编号：" + phaseId + "；阶段代码：" + phaseCode,
                    "导出时间：" + LocalDateTime.now(clock),
                    "只填写奖项，不改编号、来源、对象类型、阶段及版本。",
                    "未准备或冲突对象请先在后台处理并重新导出；不得凭作品名称匹配。",
                    "空白成绩不代表0分，非VALID成绩不能作为有效成绩。",
                    "现阶段下载不代表存在奖项导入接口。");
            for (int i = 0; i < notes.size(); i++) {
                instructions.createRow(i).createCell(0).setCellValue(notes.get(i));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (final IOException ex) {
            throw new IllegalStateException("作品清单生成失败", ex);
        }
    }
}
