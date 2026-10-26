package com.weiran.system.application.autoconfigure;

import com.weiran.system.application.auth.AuthApplicationService;
import com.weiran.system.application.auth.AuthSnapshotCache;
import com.weiran.system.application.auth.AuthorizationResolver;
import com.weiran.system.application.auth.DispatchingTokenAuthenticator;
import com.weiran.system.application.auth.ExternalLoginApplicationService;
import com.weiran.system.application.auth.IdentityProvisioner;
import com.weiran.system.application.auth.LocalIdentityResolver;
import com.weiran.system.application.auth.LocalRbacPermissionSource;
import com.weiran.system.application.auth.PhoneLoginApplicationService;
import com.weiran.system.application.auth.PhoneLoginSettings;
import com.weiran.system.application.batch.BatchJobApplicationService;
import com.weiran.system.application.batch.BatchJobWorker;
import com.weiran.system.application.loginlog.LoginLogApplicationService;
import com.weiran.system.application.media.AvatarMediaReferenceChecker;
import com.weiran.system.application.media.ConfigMediaReferenceChecker;
import com.weiran.system.application.media.MediaApplicationService;
import com.weiran.system.application.media.MediaAssetDeletionService;
import com.weiran.system.application.media.MediaReferenceApplicationService;
import com.weiran.system.application.menu.MenuApplicationService;
import com.weiran.system.application.role.RoleApplicationService;
import com.weiran.system.application.sequence.SequenceApplicationService;
import com.weiran.system.application.sms.SmsApplicationService;
import com.weiran.system.application.sms.SystemSmsSettings;
import com.weiran.system.application.user.UserApplicationService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * weiran-system 应用层自动配置：各用例服务与 {@code TokenAuthenticator} 实现（三段式认证，D-014）。
 *
 * <p>服务类都只有一个构造器，{@code @Import} 后由容器按构造器注入；事务由 Spring Boot 默认开启的
 * 注解事务驱动，服务类因此不能是 final（CGLIB 代理）。
 */
@AutoConfiguration
@EnableScheduling
@Import({
    BatchJobApplicationService.class,
    BatchJobWorker.class,
    AuthSnapshotCache.class,
    AuthorizationResolver.class,
    LocalIdentityResolver.class,
    LocalRbacPermissionSource.class,
    DispatchingTokenAuthenticator.class,
    AuthApplicationService.class,
    PhoneLoginSettings.class,
    SystemSmsSettings.class,
    PhoneLoginApplicationService.class,
    IdentityProvisioner.class,
    ExternalLoginApplicationService.class,
    UserApplicationService.class,
    RoleApplicationService.class,
    SequenceApplicationService.class,
    MenuApplicationService.class,
    LoginLogApplicationService.class,
    MediaApplicationService.class,
    MediaReferenceApplicationService.class,
    ConfigMediaReferenceChecker.class,
    AvatarMediaReferenceChecker.class,
    MediaAssetDeletionService.class,
    SmsApplicationService.class
})
public class SystemApplicationAutoConfiguration {}
