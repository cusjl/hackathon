package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.ResultCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;   // 业务状态码
    private T data;         // 数据
    private String msg;     // 提示信息

    public static <T> ResponseEntity<Result<T>> success(T data, String msg) {
        return ResponseEntity.status(HttpStatus.OK).body(new Result<>(200, data, msg));
    }

    public static ResponseEntity<Result<Void>> ok() {
        return success(null, "成功");
    }

    public static ResponseEntity<Result<Void>> noUpdate() {
        return success(null, "未更新");
    }

    public static ResponseEntity<Result<Void>> error(ResultCode resultCode) {
        return ResponseEntity.status(resultCode.getStatus()).body(
                new Result<>(resultCode.getCode(), null, resultCode.getMsg()));
    }

    public static ResponseEntity<Result<Void>> error(ResultCode resultCode, String msg) {
        return ResponseEntity.status(resultCode.getStatus()).body(
                new Result<>(resultCode.getCode(), null, msg));
    }

}