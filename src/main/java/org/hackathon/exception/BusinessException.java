package org.hackathon.exception;

import lombok.Getter;
import org.hackathon.data.enums.ResultCode;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

}