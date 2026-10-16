package com.weiran.cqt.infrastructure.file;

import com.weiran.cqt.domain.file.FileStorageException;
import java.net.URI;

/** 校验本服务文件 URL 并提取其对象名。 */
final class StoredObjectName {

    private StoredObjectName() {}

    static String requireOwned(final String baseUrl, final String url, final long accountId) {
        if (!url.startsWith(baseUrl + "/")) {
            throw new FileStorageException("文件不是本服务存储对象", new IllegalArgumentException("stored object URL"));
        }
        final URI uri;
        try {
            uri = URI.create(url);
        } catch (final IllegalArgumentException ex) {
            throw new FileStorageException("文件地址非法", ex);
        }
        final String basePath = URI.create(baseUrl).getPath();
        final String path = uri.getPath();
        if (path == null || basePath == null || !path.startsWith(basePath + "/")) {
            throw new FileStorageException("文件地址非法", new IllegalArgumentException("stored object path"));
        }
        final String objectName = path.substring(basePath.length() + 1);
        final String ownerSegment = "/user_files/" + accountId + "/";
        if (!(objectName.startsWith("user_files/" + accountId + "/") || objectName.contains(ownerSegment))
                || objectName.contains("../")
                || objectName.equals("..")
                || objectName.indexOf('\\') >= 0
                || objectName.startsWith("/")) {
            throw new FileStorageException("文件不属于当前账号", new IllegalArgumentException("stored object owner"));
        }
        return objectName;
    }
}
