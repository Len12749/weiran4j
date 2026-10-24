package com.weiran.demo.batch;

import com.weiran.system.api.batch.BatchFileException;
import com.weiran.system.api.batch.BatchProcessor;
import com.weiran.system.api.batch.BatchProgress;
import com.weiran.system.api.batch.BatchSource;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** ZIP 演示：逐文件计算 SHA-256 并写入演示业务表，不将条目解压到文件系统。 */
public class DemoZipBatchProcessor implements BatchProcessor {

    private static final long MAX_ENTRY_BYTES = 10L * 1024 * 1024;
    private static final long MAX_EXPANDED_BYTES = 200L * 1024 * 1024;
    private static final int MAX_ENTRIES = 1_000;
    private static final Set<String> ALLOWED =
            Set.of("txt", "csv", "pdf", "png", "jpg", "jpeg", "doc", "docx", "xls", "xlsx", "ppt", "pptx");
    private final DemoBatchItemStore items;

    public DemoZipBatchProcessor(final DemoBatchItemStore items) {
        this.items = items;
    }

    @Override
    public String kind() {
        return "demo_zip";
    }

    @Override
    public String extension() {
        return "zip";
    }

    @Override
    public void authorize(final long userId) {
        // local/test 演示，仅需入口已有的 system:batch-job:create 权限。
    }

    @Override
    public void process(final BatchSource source, final BatchProgress progress) throws Exception {
        final long total = this.scan(source, progress, true);
        if (progress.cancelled()) {
            return;
        }
        progress.total(total);
        this.scan(source, progress, false);
    }

    private long scan(final BatchSource source, final BatchProgress progress, final boolean validating)
            throws Exception {
        long count = 0;
        long expanded = 0;
        try (InputStream input = source.open();
                ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            final byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zip.closeEntry();
                    continue;
                }
                final String name = entry.getName().replace('\\', '/');
                if (++count > DemoZipBatchProcessor.MAX_ENTRIES) {
                    if (validating) {
                        progress.precheckFailure(count, name, "ZIP 文件数量超过限制");
                    }
                    throw new BatchFileException("ZIP 文件数量超过限制");
                }
                if (progress.cancelled()) {
                    return count - 1;
                }
                if (name.startsWith("/")
                        || name.matches("^[A-Za-z]:.*")
                        || name.equals("..")
                        || name.endsWith("/..")
                        || name.contains("../")
                        || name.contains("\u0000")) {
                    if (validating) {
                        progress.precheckFailure(count, name, "ZIP 包含无效路径");
                    }
                    throw new BatchFileException("ZIP 包含无效路径");
                }
                final MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long bytes = 0;
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    bytes += read;
                    expanded += read;
                    if (bytes > DemoZipBatchProcessor.MAX_ENTRY_BYTES
                            || expanded > DemoZipBatchProcessor.MAX_EXPANDED_BYTES) {
                        if (validating) {
                            progress.precheckFailure(count, name, "ZIP 解压后大小超过限制");
                        }
                        throw new BatchFileException("ZIP 解压后大小超过限制");
                    }
                    digest.update(buffer, 0, read);
                }
                if (entry.getCompressedSize() > 0 && bytes / entry.getCompressedSize() > 100) {
                    if (validating) {
                        progress.precheckFailure(count, name, "ZIP 压缩比超过限制");
                    }
                    throw new BatchFileException("ZIP 压缩比超过限制");
                }
                zip.closeEntry();
                final String suffix = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                if (!DemoZipBatchProcessor.ALLOWED.contains(suffix)) {
                    if (validating) {
                        progress.precheckFailure(count, name, "不支持的 ZIP 内文件类型");
                    } else {
                        progress.failure(count, name, "不支持的 ZIP 内文件类型");
                    }
                } else if (name.length() > 255) {
                    if (validating) {
                        progress.precheckFailure(count, name, "文件路径超过 255 个字符");
                    } else {
                        progress.failure(count, name, "文件路径超过 255 个字符");
                    }
                } else if (!validating) {
                    try {
                        this.items.save(
                                progress.jobId(), count, name, HexFormat.of().formatHex(digest.digest()));
                    } catch (final RuntimeException ex) {
                        progress.failure(count, name, "写入演示数据失败");
                        continue;
                    }
                    progress.success(count, name);
                }
            }
        }
        if (count == 0) {
            throw new BatchFileException("ZIP 没有文件");
        }
        return count;
    }
}
