package com.weiran.cqt.application.entry;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weiran.cqt.domain.file.FileStorage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SchoolImportArchiveIndexTest {

    @Test
    @DisplayName("附件按不区分大小写的唯一文件名匹配")
    void matchesUniqueBaseName() throws Exception {
        final SchoolImportArchiveIndex index = SchoolImportArchiveIndex.scan(
                new ByteArrayInputStream(SchoolImportArchiveIndexTest.archive("folder/Video.MP4")));

        assertThat(index.matchError("video.mp4")).isNull();
        assertThat(index.matchError("missing.mp4")).contains("未找到");
    }

    @Test
    @DisplayName("拒绝 ZIP 路径穿越并拒绝重复文件名歧义")
    void rejectsUnsafeAndAmbiguousNames() throws Exception {
        assertThatThrownBy(() -> SchoolImportArchiveIndex.scan(
                        new ByteArrayInputStream(SchoolImportArchiveIndexTest.archive("../secret.txt"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");

        final SchoolImportArchiveIndex index = SchoolImportArchiveIndex.scan(
                new ByteArrayInputStream(SchoolImportArchiveIndexTest.archive("a/file.pdf", "b/file.pdf")));
        assertThat(index.matchError("file.pdf")).contains("多个同名");
    }

    @Test
    @DisplayName("从 ZIP 提取匹配附件并写成学校账号独立对象")
    void extractsRequestedAttachmentToOwnedStorage() throws Exception {
        final FileStorage storage = mock(FileStorage.class);
        final AtomicReference<byte[]> storedBytes = new AtomicReference<>();
        when(storage.store(anyString(), any(InputStream.class), anyLong(), any()))
                .thenAnswer(invocation -> {
                    storedBytes.set(invocation.<InputStream>getArgument(1).readAllBytes());
                    return "/uploads/" + invocation.<String>getArgument(0);
                });

        final var attachments = SchoolImportArchiveIndex.extractRequested(
                new ByteArrayInputStream(SchoolImportArchiveIndexTest.archive("folder/Video.MP4")),
                Set.of("folder/video.mp4"),
                storage,
                42,
                "cqt/",
                Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"), ZoneOffset.UTC));

        assertThat(attachments).containsKey("video.mp4");
        final var attachment = requireNonNull(attachments.get("video.mp4"));
        assertThat(attachment.url()).startsWith("/uploads/cqt/user_files/42/202610/");
        assertThat(attachment.name()).isEqualTo("Video.MP4");
        assertThat(storedBytes.get()).containsExactly((byte) 1);
    }

    private static byte[] archive(final String... names) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output)) {
            for (final String name : names) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(1);
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        }
    }
}
