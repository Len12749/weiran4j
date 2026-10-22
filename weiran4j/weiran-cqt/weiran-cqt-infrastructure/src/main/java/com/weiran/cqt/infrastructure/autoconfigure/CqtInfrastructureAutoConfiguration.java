package com.weiran.cqt.infrastructure.autoconfigure;

import com.weiran.cqt.domain.account.PasswordHasher;
import com.weiran.cqt.domain.file.FileStorage;
import com.weiran.cqt.domain.portal.PortalTokenCodec;
import com.weiran.cqt.domain.sms.SmsCodeStore;
import com.weiran.cqt.infrastructure.file.LocalFileStorage;
import com.weiran.cqt.infrastructure.file.OssFileStorage;
import com.weiran.cqt.infrastructure.persistence.JdbcRegionRepository;
import com.weiran.cqt.infrastructure.persistence.MybatisAccountRepository;
import com.weiran.cqt.infrastructure.persistence.MybatisCompetitionRepository;
import com.weiran.cqt.infrastructure.persistence.MybatisEntryRepository;
import com.weiran.cqt.infrastructure.persistence.MybatisExpertRepository;
import com.weiran.cqt.infrastructure.persistence.MybatisNewsRepository;
import com.weiran.cqt.infrastructure.persistence.MybatisResourceRepository;
import com.weiran.cqt.infrastructure.persistence.MybatisSchoolImportTaskRepository;
import com.weiran.cqt.infrastructure.persistence.MybatisSequenceGenerator;
import com.weiran.cqt.infrastructure.persistence.MybatisShowcaseRepository;
import com.weiran.cqt.infrastructure.security.BCryptPasswordHasher;
import com.weiran.cqt.infrastructure.security.JjwtPortalTokenCodec;
import com.weiran.cqt.infrastructure.sms.CaffeineSmsCodeStore;
import java.nio.file.Path;
import java.time.Clock;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * weiran-cqt 基础设施自动配置：Mapper 扫描、仓储实现、前台令牌、BCrypt 与短信验证码。
 *
 * <p>Flyway 脚本在 {@code classpath:db/migration/cqt/} 下，由应用统一的 locations 递归发现。
 */
@AutoConfiguration
@MapperScan("com.weiran.cqt.infrastructure.persistence.mapper")
@EnableConfigurationProperties(CqtProperties.class)
@Import({
    com.weiran.cqt.infrastructure.competition.admin.JdbcCompetitionAdminRepository.class,
    com.weiran.cqt.infrastructure.entryexport.JdbcEntryExportRepository.class,
    com.weiran.cqt.infrastructure.review.JdbcReviewRepository.class,
    com.weiran.cqt.infrastructure.award.JdbcAwardRepository.class,
    MybatisResourceRepository.class,
    MybatisShowcaseRepository.class,
    MybatisExpertRepository.class,
    MybatisAccountRepository.class,
    JdbcRegionRepository.class,
    MybatisCompetitionRepository.class,
    MybatisEntryRepository.class,
    MybatisSequenceGenerator.class,
    MybatisSchoolImportTaskRepository.class,
    MybatisNewsRepository.class
})
public class CqtInfrastructureAutoConfiguration {

    /** 前台令牌编解码；密钥不合法时启动失败。 */
    @Bean
    @ConditionalOnMissingBean
    public PortalTokenCodec portalTokenCodec(final CqtProperties properties, final Clock clock) {
        return new JjwtPortalTokenCodec(
                properties.jwt().secret(), properties.jwt().ttl(), clock);
    }

    /** 前台账号密码哈希。 */
    @Bean
    @ConditionalOnMissingBean
    public PasswordHasher cqtPasswordHasher(final CqtProperties properties) {
        return new BCryptPasswordHasher(properties.bcryptStrength());
    }

    /** 验证码存储（进程内）。 */
    @Bean
    @ConditionalOnMissingBean
    public SmsCodeStore smsCodeStore() {
        return new CaffeineSmsCodeStore();
    }

    /** 阿里云 OSS 文件存储，直接使用 system 的存储模式和凭据。 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "weiran.storage", name = "mode", havingValue = "aliyun")
    public FileStorage ossFileStorage(
            @Value("${weiran.storage.aliyun.access-key-id:}") final String accessKeyId,
            @Value("${weiran.storage.aliyun.access-key-secret:}") final String accessKeySecret,
            @Value("${weiran.storage.aliyun.bucket:}") final String bucket,
            @Value("${weiran.storage.aliyun.endpoint:}") final String endpoint,
            @Value("${weiran.storage.aliyun.public-base-url:}") final String publicBaseUrl) {
        return OssFileStorage.create(
                new CqtProperties.Oss(accessKeyId, accessKeySecret, bucket, endpoint, publicBaseUrl));
    }

    /** 本地目录文件存储（仅开发 / 测试），使用 system 的目录与访问前缀。 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "weiran.storage", name = "mode", havingValue = "local")
    public FileStorage localFileStorage(
            @Value("${weiran.storage.local.root:uploads}") final String root,
            @Value("${weiran.storage.local.url-prefix:/system-uploads}") final String urlPrefix) {
        return new LocalFileStorage(Path.of(root), urlPrefix);
    }
}
