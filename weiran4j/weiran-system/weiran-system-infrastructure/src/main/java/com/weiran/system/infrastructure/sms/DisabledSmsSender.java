package com.weiran.system.infrastructure.sms;

import com.weiran.system.domain.sms.SmsSendOutcome;
import com.weiran.system.domain.sms.SmsSender;
import java.util.Map;

/** 默认关闭的短信 sender，不访问外部服务。 */
public final class DisabledSmsSender implements SmsSender {

    @Override
    public SmsSendOutcome send(final String phone, final String templateCode, final Map<String, String> parameters) {
        return SmsSendOutcome.NOT_CONFIGURED;
    }
}
