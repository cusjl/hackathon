package org.hackathon.exception;

import lombok.Getter;
import org.hackathon.data.enums.ResultCode;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String msg;
    private final HttpStatus status;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
        this.status = resultCode.getStatus();
    }

    public BusinessException(ResultCode resultCode, String msg) {
        super(msg);
        this.code = resultCode.getCode();
        this.msg = msg;
        this.status = resultCode.getStatus();
    }

}