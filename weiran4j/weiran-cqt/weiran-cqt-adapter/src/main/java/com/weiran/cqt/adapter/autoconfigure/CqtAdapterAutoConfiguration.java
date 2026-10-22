package com.weiran.cqt.adapter.autoconfigure;

import com.weiran.cqt.adapter.portal.AuthController;
import com.weiran.cqt.adapter.portal.CompetCategoryController;
import com.weiran.cqt.adapter.portal.CompetitionController;
import com.weiran.cqt.adapter.portal.ExpertPortalController;
import com.weiran.cqt.adapter.portal.LocalFilesController;
import com.weiran.cqt.adapter.portal.NewsPortalController;
import com.weiran.cqt.adapter.portal.PortalAuthInterceptor;
import com.weiran.cqt.adapter.portal.PortalExceptionAdvice;
import com.weiran.cqt.adapter.portal.ProductController;
import com.weiran.cqt.adapter.portal.ResourcePortalController;
import com.weiran.cqt.adapter.portal.SchoolImportController;
import com.weiran.cqt.adapter.portal.ShowcasePortalController;
import com.weiran.cqt.adapter.web.ExpertAdminController;
import com.weiran.cqt.adapter.web.NewsAdminController;
import com.weiran.cqt.adapter.web.ResourceAdminController;
import com.weiran.cqt.adapter.web.ShowcaseAdminController;
import com.weiran.cqt.api.portal.PortalAuthService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** weiran-cqt 适配层自动配置：登记 CQT 管理与前台 Controller、异常出口及前台认证拦截器。 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
    com.weiran.cqt.adapter.competition.admin.CompetitionAdminController.class,
    com.weiran.cqt.adapter.entryexport.EntryExportAdminController.class,
    com.weiran.cqt.adapter.review.ReviewAdminController.class,
    com.weiran.cqt.adapter.award.AwardAdminController.class,
    ResourcePortalController.class,
    ResourceAdminController.class,
    ShowcasePortalController.class,
    ShowcaseAdminController.class,
    ExpertPortalController.class,
    ExpertAdminController.class,
    PortalExceptionAdvice.class,
    ProductController.class,
    AuthController.class,
    CompetCategoryController.class,
    LocalFilesController.class,
    CompetitionController.class,
    SchoolImportController.class,
    NewsAdminController.class,
    NewsPortalController.class
})
public class CqtAdapterAutoConfiguration {

    /** 前台认证拦截器。 */
    @Bean
    public PortalAuthInterceptor portalAuthInterceptor(final PortalAuthService authService) {
        return new PortalAuthInterceptor(authService);
    }

    /** 只把前台拦截器挂在 {@code /api-web/**}；后台 {@code /api-backend/**} 由框架拦截器负责。 */
    @Bean
    public WebMvcConfigurer cqtPortalWebMvcConfigurer(final PortalAuthInterceptor portalAuthInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(final InterceptorRegistry registry) {
                registry.addInterceptor(portalAuthInterceptor).addPathPatterns("/api-web/**");
            }
        };
    }
}
