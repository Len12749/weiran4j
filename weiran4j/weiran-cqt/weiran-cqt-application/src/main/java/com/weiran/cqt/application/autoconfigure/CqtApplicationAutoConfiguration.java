package com.weiran.cqt.application.autoconfigure;

import com.weiran.cqt.application.account.AccountApplicationService;
import com.weiran.cqt.application.competition.CompetitionQueryApplicationService;
import com.weiran.cqt.application.entry.MyEntriesApplicationService;
import com.weiran.cqt.application.entry.SchoolImportApplicationService;
import com.weiran.cqt.application.entry.SchoolImportEntryWriter;
import com.weiran.cqt.application.entry.SchoolImportWorker;
import com.weiran.cqt.application.entry.SignupApplicationService;
import com.weiran.cqt.application.file.FileUploadApplicationService;
import com.weiran.cqt.application.portal.PortalAuthApplicationService;
import com.weiran.cqt.application.region.RegionApplicationService;
import com.weiran.cqt.application.setting.SiteConfigAdminApplicationService;
import com.weiran.cqt.application.setting.SiteConfigApplicationService;
import com.weiran.cqt.application.sms.SmsApplicationService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/** weiran-cqt 应用层自动配置：登记全部应用服务。 */
@AutoConfiguration
@EnableScheduling
@Import({
    SiteConfigApplicationService.class,
    SiteConfigAdminApplicationService.class,
    SmsApplicationService.class,
    AccountApplicationService.class,
    PortalAuthApplicationService.class,
    RegionApplicationService.class,
    FileUploadApplicationService.class,
    CompetitionQueryApplicationService.class,
    SignupApplicationService.class,
    MyEntriesApplicationService.class,
    SchoolImportEntryWriter.class,
    SchoolImportApplicationService.class,
    SchoolImportWorker.class
})
public class CqtApplicationAutoConfiguration {}
