package com.weiran.system.infrastructure.storage;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 文件存储配置。 */
@ConfigurationProperties("weiran.system.storage")
public record SystemStorageProperties(String mode, Path localRoot, String localUrlPrefix, Oss oss) {

    public record Oss(
            String endpoint, String bucket, String accessKeyId, String accessKeySecret, String publicBaseUrl) {}
}
