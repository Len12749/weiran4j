package com.weiran.system.adapter.autoconfigure;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 本地开发模式下提供已上传图片的静态访问。 */
@Configuration
@ConditionalOnProperty(prefix = "weiran.system.storage", name = "mode", havingValue = "local")
public class SystemStorageWebConfiguration implements WebMvcConfigurer {

    private final Path localRoot;
    private final String localUrlPrefix;

    public SystemStorageWebConfiguration(
            @Value("${weiran.system.storage.local-root}") final Path localRoot,
            @Value("${weiran.system.storage.local-url-prefix}") final String localUrlPrefix) {
        this.localRoot = localRoot;
        this.localUrlPrefix = localUrlPrefix;
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        final Path root = this.localRoot.toAbsolutePath().normalize();
        final String location = root.toUri().toString();
        registry.addResourceHandler(this.localUrlPrefix + "/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
