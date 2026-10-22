package com.weiran.cqt.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weiran.cqt.domain.file.FileStorageException;
import com.weiran.cqt.infrastructure.autoconfigure.CqtProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OssFileStorageTest {

    private static final String SECRET_VALUE = "oss-super-secret";

    private static CqtProperties.Oss config(final String bucket) {
        return new CqtProperties.Oss(
                "AKID",
                OssFileStorageTest.SECRET_VALUE,
                bucket,
                "oss-cn-beijing.aliyuncs.com",
                "https://cdn.example.com/");
    }

    /** 记录调用的假写入器。 */
    private static final class FakeWriter implements OssObjectWriter {

        private final List<String> calls = new ArrayList<>();

        private final boolean fail;

        private boolean closed;

        FakeWriter(final boolean fail) {
            this.fail = fail;
        }

        @Override
        public void put(
                final String bucket,
                final String objectName,
                final InputStream content,
                final long size,
                final @Nullable String contentType) {
            if (this.fail) {
                throw new IllegalStateException("network down");
            }
            this.calls.add(bucket + "|" + objectName + "|" + size + "|" + contentType);
        }

        @Override
        public InputStream get(final String bucket, final String objectName) {
            if (this.fail) {
                throw new IllegalStateException("network down");
            }
            this.calls.add(bucket + "|GET|" + objectName);
            return new ByteArrayInputStream("zip".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public void close() {
            this.closed = true;
        }
    }

    @Test
    @DisplayName("流式写入：Bucket、对象名、长度、类型原样传给 SDK；地址为去尾斜杠的公网前缀 + 对象名；关闭时释放客户端")
    void storesAndBuildsUrl() {
        final FakeWriter writer = new FakeWriter(false);
        final OssFileStorage storage = new OssFileStorage(OssFileStorageTest.config("cqt-bucket"), writer);

        final String url = storage.store(
                "cqt/user_files/42/202610/abc.zip", new ByteArrayInputStream(new byte[12]), 12, "application/zip");

        assertThat(url).isEqualTo("https://cdn.example.com/cqt/user_files/42/202610/abc.zip");
        assertThat(writer.calls).containsExactly("cqt-bucket|cqt/user_files/42/202610/abc.zip|12|application/zip");
        storage.destroy();
        assertThat(writer.closed).isTrue();
    }

    @Test
    @DisplayName("SDK 抛异常 → FileStorageException，信息只含异常类名")
    void wrapsFailures() {
        final OssFileStorage storage =
                new OssFileStorage(OssFileStorageTest.config("cqt-bucket"), new FakeWriter(true));

        assertThatThrownBy(() -> storage.store("a.zip", new ByteArrayInputStream(new byte[1]), 1, null))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("OSS 写入失败: IllegalStateException");
    }

    @Test
    @DisplayName("配置不全启动失败：列出缺少的环境变量名，不含 Secret；配置齐全可构造 SDK 客户端（不发网络请求）")
    void validatesConfig() {
        assertThatThrownBy(() -> OssFileStorage.create(OssFileStorageTest.config(" ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WEIRAN_STORAGE_ALIYUN_BUCKET")
                .hasMessageNotContaining(OssFileStorageTest.SECRET_VALUE)
                .hasMessageNotContaining("WEIRAN_STORAGE_ALIYUN_ENDPOINT");
        assertThatThrownBy(() -> OssFileStorage.requireComplete(new CqtProperties.Oss("", "", "", "", "")))
                .hasMessageContaining("WEIRAN_STORAGE_ALIYUN_ACCESS_KEY_ID")
                .hasMessageContaining("WEIRAN_STORAGE_ALIYUN_ACCESS_KEY_SECRET")
                .hasMessageContaining("WEIRAN_STORAGE_ALIYUN_ENDPOINT")
                .hasMessageContaining("WEIRAN_STORAGE_ALIYUN_PUBLIC_BASE_URL");

        final OssFileStorage storage = OssFileStorage.create(OssFileStorageTest.config("cqt-bucket"));
        assertThat(storage).isNotNull();
        storage.destroy();
    }

    @Test
    @DisplayName("isStoredUrl：只认公网前缀 + / 开头的地址，不被相似域名骗过")
    void recognizesOwnUrls() {
        final OssFileStorage storage =
                new OssFileStorage(OssFileStorageTest.config("cqt-bucket"), new FakeWriter(false));

        assertThat(storage.isStoredUrl("https://cdn.example.com/cqt/user_files/1/202610/a.zip"))
                .isTrue();
        assertThat(storage.isStoredUrl("https://cdn.example.com.evil.com/a.zip"))
                .isFalse();
        assertThat(storage.isStoredUrl("https://other.com/a.zip")).isFalse();
    }

    @Test
    @DisplayName("仅按 URL 前缀和账号对象路径读取 OSS 文件")
    void opensOwnedObject() throws IOException {
        final FakeWriter writer = new FakeWriter(false);
        final OssFileStorage storage = new OssFileStorage(OssFileStorageTest.config("cqt-bucket"), writer);

        try (InputStream input =
                storage.openOwnedObject("https://cdn.example.com/cqt/user_files/42/202610/a.zip", 42)) {
            assertThat(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .isEqualTo("zip");
        }
        assertThat(writer.calls).containsExactly("cqt-bucket|GET|cqt/user_files/42/202610/a.zip");
        assertThatThrownBy(() -> storage.openOwnedObject("https://cdn.example.com/cqt/user_files/42/a.zip", 9))
                .isInstanceOf(FileStorageException.class);
    }
}
