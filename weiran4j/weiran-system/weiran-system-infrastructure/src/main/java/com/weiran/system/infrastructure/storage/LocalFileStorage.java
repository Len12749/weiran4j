package com.weiran.system.infrastructure.storage;

import com.weiran.system.domain.storage.FileStorage;
import com.weiran.system.domain.storage.FileStorage.StoredFile;
import com.weiran.system.domain.storage.FileStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/** 本地文件存储，仅供开发与测试环境使用。 */
public final class LocalFileStorage implements FileStorage {

    private final Path root;
    private final String urlPrefix;

    public LocalFileStorage(final Path root, final String urlPrefix) {
        this.root = root.toAbsolutePath().normalize();
        this.urlPrefix = LocalFileStorage.trimSlash(urlPrefix);
    }

    @Override
    public StoredFile store(final String originalName, final InputStream input, final long size) {
        final String objectName = LocalFileStorage.newObjectName(originalName);
        final Path target = this.resolve(objectName);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(input, target);
        } catch (final IOException ex) {
            throw new FileStorageException("图片上传失败，请稍后重试", ex);
        }
        return new StoredFile(this.urlPrefix + "/" + objectName, objectName, size);
    }

    @Override
    public void delete(final String objectName) {
        try {
            Files.deleteIfExists(this.resolve(objectName));
        } catch (final IOException ex) {
            throw new FileStorageException("图片删除失败，请稍后重试", ex);
        }
    }

    private Path resolve(final String objectName) {
        if (!objectName.matches("media/[0-9]{6}/[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|gif)")) {
            throw new IllegalArgumentException("图片对象名无效");
        }
        final Path path = this.root.resolve(objectName).normalize();
        if (!path.startsWith(this.root)) {
            throw new IllegalArgumentException("图片对象名越出存储目录");
        }
        return path;
    }

    static String newObjectName(final String originalName) {
        final int dot = originalName.lastIndexOf('.');
        final String extension = (dot < 0 ? "" : originalName.substring(dot + 1)).toLowerCase(Locale.ROOT);
        if (!extension.matches("jpg|jpeg|png|gif")) {
            throw new IllegalArgumentException("图片扩展名无效");
        }
        return "media/" + YearMonth.now(Clock.systemUTC()).format(DateTimeFormatter.ofPattern("yyyyMM", Locale.ROOT))
                + "/"
                + UUID.randomUUID() + "." + extension;
    }

    private static String trimSlash(final String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
