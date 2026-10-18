package com.weiran.region.api.region;

import com.weiran.framework.error.ErrorCode;

/** 行政区划模块的业务错误码（本下游分配序号段 40–49）。 */
public enum RegionErrors implements ErrorCode {
    INVALID_HIERARCHY(40040, 400, "父级地区不存在或层级关系无效"),
    DUPLICATE_CODE(40940, 409, "区划代码已存在"),
    HAS_CHILDREN(40941, 409, "该地区下存在子地区，请先删除子地区");

    private final int code;

    private final int httpStatus;

    private final String message;

    RegionErrors(final int code, final int httpStatus, final String message) {
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
