package org.hackathon.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    private Integer code; // 业务状态码
    private Object data;  // 数据
    private String msg;   // 提示信息

    public static ResponseEntity<Result> build(Result result) {
        return ResponseEntity
                .status(result.httpStatus())
                .body(result);
    }

    public static ResponseEntity<Result> success(Object data, String msg) {
        return build(new Result(200, data, msg));
    }

    public static ResponseEntity<Result> ok() {
        return build(new Result(200, null, "成功"));
    }

    public static ResponseEntity<Result> error(Integer code, String msg) {
        return build(new Result(code, null, msg));
    }

    public HttpStatus httpStatus() {
        return switch (this.code) {
            case 400 -> HttpStatus.BAD_REQUEST; // 请求错误
            case 401 -> HttpStatus.UNAUTHORIZED; // 未授权
            case 403 -> HttpStatus.FORBIDDEN;    // 禁止访问
            case 404 -> HttpStatus.NOT_FOUND;    // 资源未找到
            case 409 -> HttpStatus.CONFLICT;     // 资源冲突
            case 500, 554 -> HttpStatus.INTERNAL_SERVER_ERROR; // 服务器错误
            default -> HttpStatus.OK; // 默认返回 200
        };
    }
}