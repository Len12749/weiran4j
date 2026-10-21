package com.weiran.system.domain.sms;

/** 从短信配置读取当前发送模式的签名与验证码模板。 */
public interface SmsSettings {

    String signName();

    String verificationTemplateCode();
}
