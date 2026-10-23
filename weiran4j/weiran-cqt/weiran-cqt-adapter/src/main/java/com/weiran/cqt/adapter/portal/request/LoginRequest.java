package com.weiran.cqt.adapter.portal.request;

import org.jspecify.annotations.Nullable;

/**
 * 登录 / 自动登录（验证码与密码均为可选，至少提交其中一项）。
 *
 * @param phone 手机号
 * @param password 密码
 * @param code 短信验证码
 */
public record LoginRequest(
        @Nullable String phone,
        @Nullable String password,
        @Nullable String code) {}
