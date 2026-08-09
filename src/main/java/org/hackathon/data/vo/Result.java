package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.ResultCode;
import org.springframework.http.ResponseEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code; // 业务状态码
    private T data;  // 数据
    private String msg;   // 提示信息

    public static <T> ResponseEntity<Result<T>> build(Result<T> result) {
        return ResponseEntity
                .status(ResultCode.fromCode(result.getCode()).getStatus())
                .body(result);
    }

    public static <T> ResponseEntity<Result<T>> success(T data, String msg) {
        return build(new Result<>(200, data, msg));
    }

    public static ResponseEntity<Result<Void>> ok() {
        return build(new Result<>(200, null, "成功"));
    }

    public static ResponseEntity<Result<Void>> error(ResultCode resultCode) {
        return build(new Result<>(resultCode.getCode(), null, resultCode.getMsg()));
    }

    public static ResponseEntity<Result<Void>> error(ResultCode resultCode, String msg) {
        return build(new Result<>(resultCode.getCode(), null, msg));
    }

}