package com.wulicode.weiran.automation.domain.task;

import java.util.List;

/**
 * 外部依赖就绪检查端口。
 *
 * <p>返回缺失配置项的可读说明；为空表示可以执行。用于在真正发起网络调用前给出明确的执行结果，
 * 而不是让每一分钟都产生一条失败记录。
 */
public interface AutomationReadiness {

    /** 当前缺失的配置说明；为空表示配置完整。 */
    List<String> missingItems();
}
