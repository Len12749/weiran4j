package com.weiran.system.infrastructure.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;

/** 阿里云官方 SDK 适配器。发送接口不幂等，因此关闭 SDK 自动重试。 */
final class SdkAliyunSmsGateway implements AliyunSmsGateway {

    private static final String ENDPOINT = "dysmsapi.aliyuncs.com";

    private final Client client;
    private final RuntimeOptions runtimeOptions;

    SdkAliyunSmsGateway(final SystemSmsProperties properties) {
        final int connectTimeoutMillis =
                SdkAliyunSmsGateway.toMillis(properties.connectTimeout(), "WEIRAN_SYSTEM_SMS_CONNECT_TIMEOUT");
        final int readTimeoutMillis =
                SdkAliyunSmsGateway.toMillis(properties.readTimeout(), "WEIRAN_SYSTEM_SMS_READ_TIMEOUT");
        try {
            this.client = new Client(new Config()
                    .setAccessKeyId(properties.accessKeyId())
                    .setAccessKeySecret(properties.accessKeySecret())
                    .setEndpoint(SdkAliyunSmsGateway.ENDPOINT)
                    .setConnectTimeout(connectTimeoutMillis)
                    .setReadTimeout(readTimeoutMillis));
        } catch (final Exception exception) {
            throw new IllegalStateException(
                    "阿里云短信客户端初始化失败: " + exception.getClass().getSimpleName());
        }
        this.runtimeOptions = SdkAliyunSmsGateway.runtimeOptions(connectTimeoutMillis, readTimeoutMillis);
    }

    SdkAliyunSmsGateway(final Client client, final RuntimeOptions runtimeOptions) {
        this.client = client;
        this.runtimeOptions = runtimeOptions;
    }

    static RuntimeOptions runtimeOptions(final int connectTimeoutMillis, final int readTimeoutMillis) {
        return new RuntimeOptions()
                .setAutoretry(false)
                .setMaxAttempts(1)
                .setConnectTimeout(connectTimeoutMillis)
                .setReadTimeout(readTimeoutMillis);
    }

    @Override
    public Reply send(final String phone, final String signName, final String templateCode, final String templateParam)
            throws Exception {
        final SendSmsResponse response = this.client.sendSmsWithOptions(
                new SendSmsRequest()
                        .setPhoneNumbers(phone)
                        .setSignName(signName)
                        .setTemplateCode(templateCode)
                        .setTemplateParam(templateParam),
                this.runtimeOptions);
        final SendSmsResponseBody body = response == null ? null : response.getBody();
        return body == null ? new Reply(null, null) : new Reply(body.getCode(), body.getRequestId());
    }

    static int toMillis(final String timeoutSeconds, final String variableName) {
        try {
            final int seconds = Integer.parseInt(timeoutSeconds);
            if (seconds < 1) {
                throw new IllegalStateException("短信超时配置无效: " + variableName);
            }
            return Math.multiplyExact(seconds, 1000);
        } catch (final ArithmeticException | NumberFormatException exception) {
            throw new IllegalStateException("短信超时配置无效: " + variableName);
        }
    }
}
