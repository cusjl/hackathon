package org.hackathon.data.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ResultCode {
    TOKEN_UNREADABLE(1001, HttpStatus.UNAUTHORIZED, "token格式错误"),
    TOKEN_EXPIRED(1002, HttpStatus.UNAUTHORIZED, "token过期"),
    ALREADY_REGISTERED(1003, HttpStatus.CONFLICT, "学生已注册"),
    PHONE_CONFLICT(1004, HttpStatus.CONFLICT, "手机号已被占用"),
    EMAIL_CONFLICT(1005, HttpStatus.CONFLICT, "邮箱已被占用"),
    USER_NOT_EXIST(1006, HttpStatus.UNAUTHORIZED, "用户不存在"),
    PASSWORD_INCORRECT(1007, HttpStatus.UNAUTHORIZED, "密码错误"),
    NOT_REGISTERED(1008, HttpStatus.UNAUTHORIZED, "学生尚未注册"),
    TOKEN_IS_BLANK(1009, HttpStatus.UNAUTHORIZED, "未携带token"),

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
