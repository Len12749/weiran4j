package com.weiran.cqt.application.entry;

import com.weiran.cqt.domain.file.FileStorage;
import com.weiran.cqt.domain.file.UploadedFiles;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jspecify.annotations.Nullable;

/** 安全扫描附件 ZIP 并按文件名索引条目；不解压到文件系统。 */
final class SchoolImportArchiveIndex {

    private static final int MAX_ENTRIES = 10_000;

    private static final long MAX_ENTRY_BYTES = 256L * 1024 * 1024;

    private static final long MAX_TOTAL_BYTES = 512L * 1024 * 1024;

    private final Map<String, Integer> fileNameCounts;

    private SchoolImportArchiveIndex(final Map<String, Integer> fileNameCounts) {
        this.fileNameCounts = Map.copyOf(fileNameCounts);
    }

    static SchoolImportArchiveIndex scan(final InputStream input) throws IOException {
        final Map<String, Integer> names = new HashMap<>();
        long totalBytes = 0;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries++;
                if (entries > SchoolImportArchiveIndex.MAX_ENTRIES) {
                    throw new IllegalArgumentException("ZIP 文件条目超过限制");
                }
                final String path = SchoolImportArchiveIndex.normalizedPath(entry.getName());
                if (!entry.isDirectory()) {
                    final String baseName = path.substring(path.lastIndexOf('/') + 1);
                    names.merge(SchoolImportArchiveIndex.key(baseName), 1, Integer::sum);
                    long entryBytes = 0;
                    final byte[] buffer = new byte[8192];
                    int read;
                    while ((read = zip.read(buffer)) >= 0) {
                        entryBytes += read;
                        totalBytes += read;
                        if (entryBytes > SchoolImportArchiveIndex.MAX_ENTRY_BYTES
                                || totalBytes > SchoolImportArchiveIndex.MAX_TOTAL_BYTES) {
                            throw new IllegalArgumentException("ZIP 文件展开大小超过限制");
                        }
                    }
                }
                zip.closeEntry();
            }
        }
        return new SchoolImportArchiveIndex(names);
    }

    /** 按文件名唯一匹配，不允许路径或重复 basename 造成歧义。 */
    @Nullable
    String matchError(final String requestedName) {
        final String value = requestedName == null ? "" : requestedName.strip().replace('\\', '/');
        if (value.isEmpty() || value.startsWith("/") || value.contains("../")) {
            return "附件文件名为空或路径不安全";
        }
        final String baseName = value.substring(value.lastIndexOf('/') + 1);
        final int count = this.fileNameCounts.getOrDefault(SchoolImportArchiveIndex.key(baseName), 0);
        if (count == 0) {
            return "ZIP 中未找到附件文件“" + baseName + "”";
        }
        return count == 1 ? null : "ZIP 中存在多个同名附件“" + baseName + "”";
    }

    /** 将本任务使用的附件逐个写入账号自己的文件区，返回 basename 到实际对象的映射。 */
    static Map<String, ImportedAttachment> extractRequested(
            final InputStream input,
            final Set<String> requestedNames,
            final FileStorage storage,
            final long accountId,
            final String keyPrefix,
            final Clock clock)
            throws IOException {
        final Set<String> requested = requestedNames.stream()
                .map(SchoolImportArchiveIndex::fileKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        final Map<String, ImportedAttachment> extracted = new HashMap<>();
        final Set<String> seen = new HashSet<>();
        long totalBytes = 0;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries++;
                if (entries > SchoolImportArchiveIndex.MAX_ENTRIES) {
                    throw new IllegalArgumentException("ZIP 文件条目超过限制");
                }
                final String path = SchoolImportArchiveIndex.normalizedPath(entry.getName());
                if (entry.isDirectory()) {
                    zip.closeEntry();
                    continue;
                }
                final String baseName = path.substring(path.lastIndexOf('/') + 1);
                final String normalizedName = SchoolImportArchiveIndex.key(baseName);
                final boolean shouldStore = requested.contains(normalizedName);
                if (shouldStore && !seen.add(normalizedName)) {
                    throw new IllegalArgumentException("ZIP 中存在多个同名附件“" + baseName + "”");
                }
                final Path temporaryFile = shouldStore ? Files.createTempFile("cqt-school-import-", ".part") : null;
                long entryBytes = 0;
                try {
                    final OutputStream output = temporaryFile == null
                            ? OutputStream.nullOutputStream()
                            : Files.newOutputStream(temporaryFile);
                    try (output) {
                        final byte[] buffer = new byte[8192];
                        int read;
                        while ((read = zip.read(buffer)) >= 0) {
                            entryBytes += read;
                            totalBytes += read;
                            if (entryBytes > SchoolImportArchiveIndex.MAX_ENTRY_BYTES
                                    || totalBytes > SchoolImportArchiveIndex.MAX_TOTAL_BYTES) {
                                throw new IllegalArgumentException("ZIP 文件展开大小超过限制");
                            }
                            output.write(buffer, 0, read);
                        }
                    }
                    if (shouldStore) {
                        SchoolImportArchiveIndex.storeExtracted(
                                Objects.requireNonNull(temporaryFile),
                                baseName,
                                entryBytes,
                                storage,
                                accountId,
                                keyPrefix,
                                clock,
                                extracted);
                    }
                } finally {
                    if (temporaryFile != null) {
                        Files.deleteIfExists(temporaryFile);
                    }
                }
                zip.closeEntry();
            }
        }
        return Map.copyOf(extracted);
    }

    private static void storeExtracted(
            final Path temporaryFile,
            final String fileName,
            final long size,
            final FileStorage storage,
            final long accountId,
            final String keyPrefix,
            final Clock clock,
            final Map<String, ImportedAttachment> extracted)
            throws IOException {
        final String extension = UploadedFiles.extensionOf(fileName)
                .orElseThrow(() -> new IllegalArgumentException("ZIP 中的附件文件类型不受支持：" + fileName));
        if (size == 0) {
            throw new IllegalArgumentException("ZIP 中的附件文件为空：" + fileName);
        }
        final String objectName =
                UploadedFiles.objectName(keyPrefix, accountId, YearMonth.now(clock), UUID.randomUUID(), extension);
        final String contentType = java.net.URLConnection.guessContentTypeFromName(fileName);
        try (InputStream content = Files.newInputStream(temporaryFile)) {
            final String url = storage.store(objectName, content, size, contentType);
            extracted.put(SchoolImportArchiveIndex.key(fileName), new ImportedAttachment(url, fileName));
        }
    }

    private static String normalizedPath(final String rawName) {
        if (rawName.isBlank() || rawName.startsWith("/") || rawName.startsWith("\\") || rawName.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("ZIP 中存在不安全路径");
        }
        final String[] segments = rawName.split("/", -1);
        for (final String segment : segments) {
            if (segment.equals("..") || segment.equals(".")) {
                throw new IllegalArgumentException("ZIP 中存在越界路径");
            }
        }
        return rawName;
    }

    static String key(final String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    static String fileKey(final String path) {
        final String normalized = path.strip().replace('\\', '/');
        return SchoolImportArchiveIndex.key(normalized.substring(normalized.lastIndexOf('/') + 1));
    }

    record ImportedAttachment(String url, String name) {}
}
