package com.weiran.system.infrastructure.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.weiran.system.domain.media.MediaFileTypes;
import com.weiran.system.domain.storage.FileStorage;
import com.weiran.system.domain.storage.FileStorage.StoredFile;
import com.weiran.system.domain.storage.FileStorageException;
import java.io.InputStream;
import java.util.Objects;

/** 阿里云 OSS 文件存储。 */
public final class OssFileStorage implements FileStorage, AutoCloseable {

    private final SystemStorageProperties.Aliyun properties;
    private final OSS client;
    private final String publicBaseUrl;

    public OssFileStorage(final SystemStorageProperties.Aliyun properties) {
        this.properties = properties;
        this.publicBaseUrl = OssFileStorage.trimSlash(properties.publicBaseUrl());
        this.client = new OSSClientBuilder()
                .build(properties.endpoint(), properties.accessKeyId(), properties.accessKeySecret());
    }

    @Override
    public StoredFile store(final String originalName, final InputStream input, final long size) {
        final String objectName = LocalFileStorage.newObjectName(originalName);
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        final String extension = MediaFileTypes.extension(originalName);
        metadata.setContentType(MediaFileTypes.contentType(extension));
        if (!MediaFileTypes.image(extension) && !extension.equals("pdf") && !extension.equals("txt")) {
            metadata.setContentDisposition("attachment");
        }
        try {
            this.client.putObject(this.properties.bucket(), objectName, input, metadata);
        } catch (final RuntimeException ex) {
            throw new FileStorageException("文件上传失败，请稍后重试", ex);
        }
        return new StoredFile(this.publicBaseUrl + "/" + objectName, objectName, size);
    }

    @Override
    public void delete(final String objectName) {
        if (!Objects.requireNonNull(objectName).matches("media/[0-9]{6}/[0-9a-fA-F-]{36}\\.[a-z0-9]{2,5}")
                || !MediaFileTypes.allowed(MediaFileTypes.extension(objectName))) {
            throw new IllegalArgumentException("文件对象名无效");
        }
        try {
            this.client.deleteObject(this.properties.bucket(), objectName);
        } catch (final RuntimeException ex) {
            throw new FileStorageException("文件删除失败，请稍后重试", ex);
        }
    }

    @Override
    public void close() {
        this.client.shutdown();
    }

    private static String trimSlash(final String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
