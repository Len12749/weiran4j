package com.weiran.cqt.infrastructure.autoconfigure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code weiran.cqt.*}：常青藤业务配置（默认值见 {@code application-biz.yml}；密钥只走环境变量）。
 *
 * @param jwt 前台令牌
 * @param bcryptStrength 前台账号密码的 BCrypt 强度，默认 10；测试可调低以加速
 */
@ConfigurationProperties("weiran.cqt")
public record CqtProperties(
        @DefaultValue Jwt jwt, @DefaultValue("10") int bcryptStrength) {

    /**
     * 前台令牌。
     *
     * @param secret HS256 密钥，至少 32 字节；生产由环境变量 {@code WEIRAN_CQT_JWT_SECRET} 注入
     * @param ttl 有效期，默认 7 天
     */
    public record Jwt(
            @DefaultValue("") String secret,
            @DefaultValue("7d") Duration ttl) {}

    /**
     * 由 {@code weiran.storage.aliyun.*} 构造的阿里云 OSS 参数；AccessKey 不得进日志。
     */
    public record Oss(
            @DefaultValue("") String accessKeyId,
            @DefaultValue("") String accessKeySecret,
            @DefaultValue("") String bucket,
            @DefaultValue("") String endpoint,
            @DefaultValue("") String publicBaseUrl) {}
}
