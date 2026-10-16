package com.weiran.cqt.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weiran.cqt.domain.file.FileStorageException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    @TempDir
    Path root;

    @Test
    @DisplayName("写入 <root>/<对象名>，返回 <前缀>/<对象名>")
    void storesUnderRoot() throws IOException {
        final LocalFileStorage storage = new LocalFileStorage(this.root, "/uploads/");

        final String url = storage.store(
                "cqt/user_files/1/202610/a.pdf",
                new ByteArrayInputStream("pdf".getBytes(StandardCharsets.UTF_8)),
                3,
                "application/pdf");

        assertThat(url).isEqualTo("/uploads/cqt/user_files/1/202610/a.pdf");
        assertThat(Files.readString(this.root.resolve("cqt/user_files/1/202610/a.pdf")))
                .isEqualTo("pdf");
        assertThat(storage.root()).isEqualTo(this.root.toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("对象名越出根目录 → FileStorageException")
    void rejectsTraversal() {
        final LocalFileStorage storage = new LocalFileStorage(this.root, "/uploads");

        assertThatThrownBy(() -> storage.store("../evil.zip", new ByteArrayInputStream(new byte[1]), 1, null))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    @DisplayName("isStoredUrl：只认访问前缀 + / 开头的地址")
    void recognizesOwnUrls() {
        final LocalFileStorage storage = new LocalFileStorage(this.root, "/uploads/");

        assertThat(storage.isStoredUrl("/uploads/cqt/user_files/1/202610/a.zip"))
                .isTrue();
        assertThat(storage.isStoredUrl("/uploads-evil/a.zip")).isFalse();
        assertThat(storage.isStoredUrl("https://other.com/uploads/a.zip")).isFalse();
    }

    @Test
    @DisplayName("只允许当前账号读取其 user_files 对象")
    void opensOwnedObject() throws IOException {
        final LocalFileStorage storage = new LocalFileStorage(this.root, "/uploads");
        final Path file = this.root.resolve("cqt/user_files/42/202610/a.xlsx");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "xlsx");

        try (InputStream input = storage.openOwnedObject("/uploads/cqt/user_files/42/202610/a.xlsx", 42)) {
            assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("xlsx");
        }
        assertThatThrownBy(() -> storage.openOwnedObject("/uploads/cqt/user_files/42/202610/a.xlsx", 24))
                .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.openOwnedObject("https://example.test/a.xlsx", 42))
                .isInstanceOf(FileStorageException.class);
    }
}
