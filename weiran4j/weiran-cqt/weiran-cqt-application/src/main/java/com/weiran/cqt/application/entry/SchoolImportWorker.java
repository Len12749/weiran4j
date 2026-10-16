package com.weiran.cqt.application.entry;

import org.springframework.scheduling.annotation.Scheduled;

/** 定时领取持久化的学校导入任务；数据库租约允许应用重启后恢复。 */
public class SchoolImportWorker {

    private final SchoolImportApplicationService importService;

    /** 构造 worker。 */
    public SchoolImportWorker(final SchoolImportApplicationService importService) {
        this.importService = importService;
    }

    /** 处理一个待处理或租约已过期的任务。 */
    @Scheduled(fixedDelayString = "${weiran.cqt.import.poll-delay-ms:3000}")
    public void processNext() {
        this.importService.processNext();
    }
}
