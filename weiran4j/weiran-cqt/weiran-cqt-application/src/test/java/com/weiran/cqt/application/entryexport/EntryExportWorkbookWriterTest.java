package com.weiran.cqt.application.entryexport;

import static org.assertj.core.api.Assertions.assertThat;

import com.weiran.cqt.domain.entryexport.EntryExportRow;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntryExportWorkbookWriterTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneOffset.UTC);

    private static EntryExportRow row(final String state) {
        return new EntryExportRow(
                1,
                "赛事",
                2,
                "PROVINCIAL",
                "省审",
                9007199254740993L,
                "0000123",
                "=HYPERLINK(\"https://example.com\")",
                "zhongxi",
                3L,
                "ENTRY",
                null,
                "",
                "北京",
                "戏剧",
                "专业",
                "组别",
                3,
                "ACTIVE",
                state,
                new BigDecimal("88.50"),
                5L);
    }

    @Test
    @DisplayName("导出固定23列、文本ID和公式样式文本并保留空奖项")
    void writesStableTextCells() throws Exception {
        final byte[] bytes = new EntryExportWorkbookWriter().write(1, 2, "PROVINCIAL", List.of(row("VALID")), CLOCK);
        try (final XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            final var sheet = book.getSheetAt(0);
            assertThat(sheet.getRow(0).getLastCellNum()).isEqualTo((short) 23);
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo("9007199254740993");
            assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).isEqualTo("0000123");
            assertThat(sheet.getRow(1).getCell(7).getCellType()).isEqualTo(CellType.STRING);
            assertThat(sheet.getRow(1).getCell(20).getStringCellValue()).isEqualTo("88.50");
            assertThat(sheet.getRow(1).getCell(22).getStringCellValue()).isEmpty();
            assertThat(sheet.getRow(1).getCell(11).getStringCellValue()).isEmpty();
            assertThat(book.getSheetAt(1).getRow(0).getCell(0).getStringCellValue())
                    .contains("CQT_AWARD_IMPORT_V1");
        }
    }

    @Test
    @DisplayName("STALE成绩留空但版本保留，空数据仍有表头说明")
    void preservesMissingScoresAndEmptySheet() throws Exception {
        try (final XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(
                new EntryExportWorkbookWriter().write(1, 2, "PROVINCIAL", List.of(row("STALE")), CLOCK)))) {
            assertThat(book.getSheetAt(0).getRow(1).getCell(20).getStringCellValue())
                    .isEmpty();
            assertThat(book.getSheetAt(0).getRow(1).getCell(21).getStringCellValue())
                    .isEqualTo("5");
        }
        try (final XSSFWorkbook book = new XSSFWorkbook(
                new ByteArrayInputStream(new EntryExportWorkbookWriter().write(1, 2, "NATIONAL", List.of(), CLOCK)))) {
            assertThat(book.getNumberOfSheets()).isEqualTo(2);
            assertThat(book.getSheetAt(0).getLastRowNum()).isZero();
        }
    }
}
