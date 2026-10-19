package com.weiran.system.domain.sms;

import java.util.Map;

/** 短信发送设施端口。 */
public interface SmsSender {

    SmsSendOutcome send(String phone, String templateCode, Map<String, String> templateParameters);
}
