package com.weiran.system.api.sms;

/** 供业务模块调用的 system 短信发送服务。 */
public interface SmsService {

    /** 发送一条短信模板消息。 */
    SmsSendResult send(SmsSendCommand command);
}
