package com.weiran.cqt.application.entry;

import static org.assertj.core.api.Assertions.assertThat;

import com.weiran.cqt.domain.entry.SchoolImportEntryType;
import com.weiran.cqt.domain.entry.SchoolImportRow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SchoolImportSpreadsheetParserTest {

    private static final List<String> HEADERS = List.of(
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
            "渠道标识",
            "省赛奖项",
            "国赛奖项");

    @Test
    @DisplayName("使用示例表头映射个人行并忽略省赛国赛奖项")
    void parsesExampleHeadersAndIgnoresAwards() throws Exception {
        final byte[] workbook = SchoolImportSpreadsheetParserTest.workbook();

        final List<SchoolImportRow> rows =
                new SchoolImportSpreadsheetParser().parse(new ByteArrayInputStream(workbook));

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.sourceRow()).isEqualTo(3);
            assertThat(row.entryType()).isEqualTo(SchoolImportEntryType.INDIVIDUAL);
            assertThat(row.name()).isEqualTo("测试学生");
            assertThat(row.phone()).isEqualTo("13800000000");
            assertThat(row.region()).isEqualTo("北京市");
            assertThat(row.category()).isEqualTo("校园戏剧");
            assertThat(row.major()).isEqualTo("表演");
            assertThat(row.title()).isEqualTo("测试作品");
            assertThat(row.teacher1CredentialNo()).isEqualTo("110101198001011234");
            assertThat(row.validationError()).isNull();
        });
    }

    @Test
    @DisplayName("解析用户提供的真实报名工作簿并正确识别联系方式与所在省")
    void parsesProvidedWorkbook() throws Exception {
        final InputStream resource = Objects.requireNonNull(
                SchoolImportSpreadsheetParserTest.class.getResourceAsStream("/school-signup-example.xlsx"));
        final List<SchoolImportRow> rows = new SchoolImportSpreadsheetParser().parse(resource);

        assertThat(rows).hasSize(10);
        assertThat(rows.getFirst().sourceRow()).isEqualTo(3);
        assertThat(rows.getFirst().name()).isEqualTo("小甲");
        assertThat(rows.getFirst().phone()).isEqualTo("18500001000");
        assertThat(rows.getFirst().region()).isEqualTo("北京市");
        assertThat(rows.getFirst().school()).isEqualTo("北京市第十八中学");
    }

    @Test
    @DisplayName("下载模板包含报名字段且不含省赛国赛奖项列")
    void templateOmitsAwards() throws Exception {
        try (Workbook workbook =
                new XSSFWorkbook(new ByteArrayInputStream(SchoolImportSpreadsheetParser.blankTemplate()))) {
            final var row = workbook.getSheet("报名表").getRow(1);
            assertThat(row.getLastCellNum()).isEqualTo((short) 20);
            assertThat(row.getCell(19).getStringCellValue()).isEqualTo("渠道标识");
        }
    }

    private static byte[] workbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final var sheet = workbook.createSheet("报名表");
            final var header = sheet.createRow(1);
            for (int index = 0; index < SchoolImportSpreadsheetParserTest.HEADERS.size(); index++) {
                header.createCell(index).setCellValue(SchoolImportSpreadsheetParserTest.HEADERS.get(index));
            }
            final var row = sheet.createRow(2);
            final List<String> values = List.of(
                    "1",
                    "个人",
                    "1",
                    "测试学生",
                    "110101200001011234",
                    "北京市",
                    "测试学校",
                    "13800000000",
                    "高中组",
                    "校园戏剧",
                    "表演",
                    "测试作品",
                    "简介",
                    "文件",
                    "作品.mp4",
                    "指导教师",
                    "110101198001011234",
                    "",
                    "",
                    "渠道",
                    "乱填也忽略",
                    "乱填也忽略");
            for (int index = 0; index < values.size(); index++) {
                row.createCell(index).setCellValue(values.get(index));
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
