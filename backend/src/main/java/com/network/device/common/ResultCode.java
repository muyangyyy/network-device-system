package com.network.device.common;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    PARAM_ERROR(4001, "参数错误"),
    DATA_NOT_EXIST(4002, "数据不存在"),
    DATA_EXIST(4003, "数据已存在"),
    STATUS_ERROR(4004, "状态错误");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
