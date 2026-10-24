package com.weiran.demo.batch;

import com.weiran.system.api.batch.BatchFileException;
import com.weiran.system.api.batch.BatchProcessor;
import com.weiran.system.api.batch.BatchProgress;
import com.weiran.system.api.batch.BatchSource;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/** XLSX 首张工作表演示：两次流式读取，表头 name/value，逐行写入演示业务表。 */
public class DemoExcelBatchProcessor implements BatchProcessor {

    private static final long MAX_ROWS = 10_000;
    private final DemoBatchItemStore items;

    public DemoExcelBatchProcessor(final DemoBatchItemStore items) {
        this.items = items;
    }

    @Override
    public String kind() {
        return "demo_excel";
    }

    @Override
    public String extension() {
        return "xlsx";
    }

    @Override
    public void authorize(final long userId) {
        // local/test 演示，仅需入口已有的 system:batch-job:create 权限。
    }

    @Override
    public long maxBytes() {
        return 5L * 1024 * 1024;
    }

    @Override
    public void process(final BatchSource source, final BatchProgress progress) throws Exception {
        final long[] count = {0};
        this.read(source, new RowConsumer() {
            @Override
            public void row(final long number, final String name, final String value) {
                if (++count[0] > DemoExcelBatchProcessor.MAX_ROWS) {
                    progress.precheckFailure(number, name, "Excel 超过 10000 行");
                    throw new BatchFileException("Excel 超过 10000 行");
                }
                if (progress.cancelled()) {
                    return;
                }
                if (name.isBlank() || value.isBlank()) {
                    progress.precheckFailure(number, name, "name 和 value 都不能为空");
                } else if (name.length() > 255 || value.length() > 255) {
                    progress.precheckFailure(number, name, "name 或 value 超过 255 个字符");
                }
            }
        });
        if (progress.cancelled()) {
            return;
        }
        progress.total(count[0]);
        this.read(source, (number, name, value) -> {
            if (progress.cancelled()) {
                return;
            }
            if (name.isBlank() || value.isBlank()) {
                progress.failure(number, name, "name 和 value 都不能为空");
                return;
            }
            if (name.length() > 255 || value.length() > 255) {
                progress.failure(number, name, "name 或 value 超过 255 个字符");
                return;
            }
            try {
                this.items.save(progress.jobId(), number, name, value);
            } catch (final RuntimeException ex) {
                progress.failure(number, name, "写入演示数据失败");
                return;
            }
            progress.success(number, name);
        });
    }

    private void read(final BatchSource source, final RowConsumer consumer) throws Exception {
        try (InputStream input = source.open();
                OPCPackage packageFile = OPCPackage.open(input)) {
            final XSSFReader reader = new XSSFReader(packageFile);
            final Iterator<InputStream> sheets = reader.getSheetsData();
            if (!sheets.hasNext()) {
                throw new BatchFileException("Excel 没有工作表");
            }
            final StylesTable styles = reader.getStylesTable();
            final SharedStrings strings = reader.getSharedStringsTable();
            try (InputStream sheet = sheets.next()) {
                final XMLReader parser = XMLHelper.newXMLReader();
                parser.setContentHandler(new XSSFSheetXMLHandler(
                        styles,
                        null,
                        strings,
                        new XSSFSheetXMLHandler.SheetContentsHandler() {
                            private String name = "";
                            private String value = "";

                            @Override
                            public void startRow(final int rowNum) {
                                this.name = "";
                                this.value = "";
                            }

                            @Override
                            public void endRow(final int rowNum) {
                                if (rowNum == 0) {
                                    if (!"name".equals(this.name.toLowerCase(Locale.ROOT))
                                            || !"value".equals(this.value.toLowerCase(Locale.ROOT))) {
                                        throw new BatchFileException("Excel 表头必须是 name、value");
                                    }
                                } else {
                                    consumer.row(rowNum + 1L, this.name.trim(), this.value.trim());
                                }
                            }

                            @Override
                            public void cell(
                                    final String cellReference, final String formattedValue, final Comment comment) {
                                if (cellReference.matches("A[0-9]+")) {
                                    this.name = formattedValue;
                                } else if (cellReference.matches("B[0-9]+")) {
                                    this.value = formattedValue;
                                }
                            }
                        },
                        new DataFormatter(),
                        false));
                parser.parse(new InputSource(sheet));
            }
        }
    }

    @FunctionalInterface
    private interface RowConsumer {
        void row(long number, String name, String value);
    }
}
