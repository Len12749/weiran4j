package com.wulicode.weiran.automation.adapter.autoconfigure;

import com.wulicode.weiran.automation.adapter.web.AutomationTaskController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/** weiran-automation HTTP 适配层自动配置。 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(AutomationTaskController.class)
public class AutomationAdapterAutoConfiguration {}
