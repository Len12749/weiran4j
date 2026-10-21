package com.weiran.cqt.api.error;

import com.weiran.framework.error.ErrorCode;

/**
 * weiran-cqt 业务错误码：服务 13、资源 01，末两位区分错误（宪法 CP-14）。
 *
 * <p>前台 {@code /api-web} 使用 {@link #httpStatus()}，所以只有前端需要区分处理、或通用码里没有对应状态时才在这里加码。
 */
public enum CqtErrors implements ErrorCode {

    /** 登录失败：账号不存在与密码错误同一个码、同一句话（宪法 CP-10）。 */
    LOGIN_FAILED(130101, 400, "手机号或密码错误"),

    /** 短信验证码不存在、过期、不匹配或失败次数用尽。 */
    SMS_CODE_INVALID(130102, 401, "验证码错误"),

    /** 注册时手机号已被使用。 */
    PHONE_REGISTERED(130103, 409, "当前手机号已经注册"),

    /** 个人证件号（同类型）已被使用。 */
    CREDENTIAL_REGISTERED(130104, 409, "该证件号已经注册"),

    /** 同一手机号发送过于频繁。 */
    SMS_TOO_FREQUENT(130105, 429, "发送过于频繁，请稍后再试"),

    /** 未配置短信发送实现。 */
    SMS_NOT_CONFIGURED(130106, 503, "短信服务未配置"),

    /** 短信服务商返回失败、调用异常或超时。 */
    SMS_SEND_FAILED(130107, 503, "短信发送失败，请稍后再试"),

    /** 未配置文件存储。 */
    STORAGE_NOT_CONFIGURED(130108, 503, "文件存储未配置"),

    /** 写入文件存储失败。 */
    UPLOAD_FAILED(130109, 503, "文件上传失败，请稍后再试");

    private final int code;

    private final int httpStatus;

    private final String message;

    CqtErrors(final int code, final int httpStatus, final String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public int code() {
        return this.code;
    }

    @Override
    public int httpStatus() {
        return this.httpStatus;
    }

    @Override
    public String message() {
        return this.message;
    }
}
