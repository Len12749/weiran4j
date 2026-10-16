package com.weiran.cqt.application.entry;

import com.weiran.cqt.domain.entry.SchoolImportEntryType;
import com.weiran.cqt.domain.entry.SchoolImportRow;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;

/** 按报名示例工作簿表头读取导入行；省赛/国赛奖项列不进入解析模型。 */
final class SchoolImportSpreadsheetParser {

    private static final String SHEET_NAME = "报名表";

    private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

    private static final List<String> TEMPLATE_HEADERS = List.of(
            "序号",
            "*团体/个人",
            "*团体人数",
            "*姓名",
            "*身份证号/护照号",
            "*所在省/市/自治区",
            "*所在学校",
            "联系方式",
            "*组别",
            "*赛项",
            "*专业",
            "*作品名称",
            "*作品简介",
            "*作品形式",
            "*附件名称/百度网盘链接",
            "指导教师1",
            "指导教师1身份证号",
            "指导教师2",
            "指导教师2身份证号",
            "渠道标识");

    static byte[] blankTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final var sheet = workbook.createSheet(SchoolImportSpreadsheetParser.SHEET_NAME);
            final var title = sheet.createRow(0);
            title.createCell(0).setCellValue("常青藤作品报名表");
            final var header = sheet.createRow(1);
            for (int index = 0; index < SchoolImportSpreadsheetParser.TEMPLATE_HEADERS.size(); index++) {
                header.createCell(index).setCellValue(SchoolImportSpreadsheetParser.TEMPLATE_HEADERS.get(index));
                sheet.setColumnWidth(index, index == 14 ? 48 * 256 : 20 * 256);
            }
            sheet.createFreezePane(0, 2);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    List<SchoolImportRow> parse(final InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            final var sheet = workbook.getSheet(SchoolImportSpreadsheetParser.SHEET_NAME);
            if (sheet == null || sheet.getRow(1) == null) {
                throw new IllegalArgumentException("报名文件缺少“报名表”工作表或第二行表头");
            }
            final FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            final Map<String, Integer> headers =
                    SchoolImportSpreadsheetParser.headers(sheet.getRow(1), evaluator, this.formatter);
            final List<SchoolImportRow> result = new ArrayList<>();
            for (int index = 2; index <= sheet.getLastRowNum(); index++) {
                final Row row = sheet.getRow(index);
                if (row == null || SchoolImportSpreadsheetParser.empty(row, evaluator, this.formatter)) {
                    continue;
                }
                result.add(SchoolImportSpreadsheetParser.parseRow(index + 1, row, headers, evaluator, this.formatter));
            }
            return List.copyOf(result);
        }
    }

    private static Map<String, Integer> headers(
            final Row row, final FormulaEvaluator evaluator, final DataFormatter formatter) {
        final Map<String, Integer> headers = new HashMap<>();
        for (int index = 0; index < row.getLastCellNum(); index++) {
            final String name = SchoolImportSpreadsheetParser.value(row.getCell(index), evaluator, formatter);
            if (!name.isBlank()) {
                headers.put(SchoolImportSpreadsheetParser.header(name), index);
            }
        }
        return headers;
    }

    private static SchoolImportRow parseRow(
            final int sourceRow,
            final Row row,
            final Map<String, Integer> headers,
            final FormulaEvaluator evaluator,
            final DataFormatter formatter) {
        String error = null;
        SchoolImportEntryType type = SchoolImportEntryType.INDIVIDUAL;
        int groupSize = 0;
        try {
            final String rawType = SchoolImportSpreadsheetParser.required(row, headers, "团体/个人", evaluator, formatter);
            type = switch (rawType.strip()) {
                case "个人" -> SchoolImportEntryType.INDIVIDUAL;
                case "团体" -> SchoolImportEntryType.TEAM;
                default -> throw new IllegalArgumentException("第" + sourceRow + "行的团体/个人类型不正确");
            };
            final String rawSize = SchoolImportSpreadsheetParser.required(row, headers, "团体人数", evaluator, formatter);
            groupSize = Integer.parseInt(rawSize.strip().replaceAll("\\.0+$", ""));
            if (groupSize < 1 || groupSize > 100) {
                throw new IllegalArgumentException("第" + sourceRow + "行的申报人数必须在1至100之间");
            }
        } catch (final IllegalArgumentException ex) {
            error = ex.getMessage();
        }
        final String credential = SchoolImportSpreadsheetParser.value(row, headers, "身份证号/护照号", evaluator, formatter);
        final String teacher1 = SchoolImportSpreadsheetParser.value(row, headers, "指导教师1", evaluator, formatter);
        final String teacher1Credential =
                SchoolImportSpreadsheetParser.value(row, headers, "指导教师1身份证号", evaluator, formatter);
        final String teacher2 = SchoolImportSpreadsheetParser.value(row, headers, "指导教师2", evaluator, formatter);
        final String teacher2Credential =
                SchoolImportSpreadsheetParser.value(row, headers, "指导教师2身份证号", evaluator, formatter);
        return new SchoolImportRow(
                sourceRow,
                type,
                groupSize,
                SchoolImportSpreadsheetParser.value(row, headers, "姓名", evaluator, formatter),
                credential.matches("(?i)\\d{17}[\\dX]") ? "身份证号" : "其他",
                credential,
                SchoolImportSpreadsheetParser.value(row, headers, "联系方式", evaluator, formatter),
                SchoolImportSpreadsheetParser.value(row, headers, "所在省/市/自治区", evaluator, formatter),
                SchoolImportSpreadsheetParser.value(row, headers, "所在学校", evaluator, formatter),
                SchoolImportSpreadsheetParser.value(row, headers, "组别", evaluator, formatter),
                SchoolImportSpreadsheetParser.value(row, headers, "赛项", evaluator, formatter),
                SchoolImportSpreadsheetParser.value(row, headers, "专业", evaluator, formatter),
                SchoolImportSpreadsheetParser.value(row, headers, "作品名称", evaluator, formatter),
                SchoolImportSpreadsheetParser.nullable(row, headers, "作品简介", evaluator, formatter),
                SchoolImportSpreadsheetParser.value(row, headers, "作品形式", evaluator, formatter),
                SchoolImportSpreadsheetParser.value(row, headers, "附件名称/百度网盘链接", evaluator, formatter),
                SchoolImportSpreadsheetParser.nullableText(teacher1),
                SchoolImportSpreadsheetParser.nullableText(teacher1Credential),
                SchoolImportSpreadsheetParser.nullableText(teacher2),
                SchoolImportSpreadsheetParser.nullableText(teacher2Credential),
                SchoolImportSpreadsheetParser.nullable(row, headers, "渠道标识", evaluator, formatter),
                error);
    }

    private static boolean empty(final Row row, final FormulaEvaluator evaluator, final DataFormatter formatter) {
        for (int index = row.getFirstCellNum(); index < row.getLastCellNum(); index++) {
            if (!SchoolImportSpreadsheetParser.value(row.getCell(index), evaluator, formatter)
                    .isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String required(
            final Row row,
            final Map<String, Integer> headers,
            final String header,
            final FormulaEvaluator evaluator,
            final DataFormatter formatter) {
        final Integer index = headers.get(SchoolImportSpreadsheetParser.header(header));
        if (index == null) {
            throw new IllegalArgumentException("报名文件缺少“" + header + "”列");
        }
        final String value = SchoolImportSpreadsheetParser.value(row.getCell(index), evaluator, formatter);
        if (value.isBlank()) {
            throw new IllegalArgumentException("第" + (row.getRowNum() + 1) + "行“" + header + "”不能为空");
        }
        return value;
    }

    private static String value(
            final Row row,
            final Map<String, Integer> headers,
            final String header,
            final FormulaEvaluator evaluator,
            final DataFormatter formatter) {
        final Integer index = headers.get(SchoolImportSpreadsheetParser.header(header));
        return index == null ? "" : SchoolImportSpreadsheetParser.value(row.getCell(index), evaluator, formatter);
    }

    private static @Nullable String nullable(
            final Row row,
            final Map<String, Integer> headers,
            final String header,
            final FormulaEvaluator evaluator,
            final DataFormatter formatter) {
        return SchoolImportSpreadsheetParser.nullableText(
                SchoolImportSpreadsheetParser.value(row, headers, header, evaluator, formatter));
    }

    private static @Nullable String nullableText(final String value) {
        return value.isBlank() ? null : value.strip();
    }

    private static String value(
            final @Nullable Cell cell, final FormulaEvaluator evaluator, final DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell, evaluator).strip();
    }

    private static String header(final String value) {
        return value.replace("*", "").replaceAll("\\s+", "").strip().toLowerCase(Locale.ROOT);
    }
}
