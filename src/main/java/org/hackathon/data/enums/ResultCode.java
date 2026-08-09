package org.hackathon.data.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ResultCode {
    UNAUTHORIZED(1001, HttpStatus.UNAUTHORIZED, "权限校验未通过"),
    TOKEN_EXPIRED(1002, HttpStatus.UNAUTHORIZED, "登录过期"),
    ALREADY_REGISTERED(1003, HttpStatus.CONFLICT, "学生已注册"),

    PARAM_ERROR(4000, HttpStatus.BAD_REQUEST, "参数错误"),

    INTERNAL_ERROR(5000, HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误"),

    RESULT_CODE_NOT_FOUND(9999, HttpStatus.NOT_FOUND, "出现未定义业务码"),
    ;

    private final Integer code;         //唯一业务码
    private final HttpStatus status;
    private final String msg;

    public static ResultCode fromCode(Integer code) {
        for (ResultCode resultCode : ResultCode.values()) {
            if (resultCode.code.equals(code)) {
                return resultCode;
            }
        }
        return RESULT_CODE_NOT_FOUND;
    }
}
