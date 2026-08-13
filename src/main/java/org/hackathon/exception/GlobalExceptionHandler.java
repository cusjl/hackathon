package org.hackathon.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.vo.Result;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 1. 业务异常 ====================
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(new Result<>(e.getCode(), null, e.getMessage()));
    }

    // ==================== 2. 参数校验异常（@Valid） ====================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    // ==================== 3. 参数绑定异常（@RequestParam 绑定失败） ====================
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数绑定失败: {}", message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    // ==================== 4. 缺少请求参数 ====================
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParams(MissingServletRequestParameterException e) {
        String message = "缺少必要参数: " + e.getParameterName();
        log.warn(message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    // ==================== 5. 路径请求错误 ====================
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handlePathVariable(NoResourceFoundException e) {
        String message = "请求路径无资源: /" + e.getResourcePath();
        log.warn(message);
        return Result.error(ResultCode.PATH_NOT_FOUND, message);
    }

    // ==================== 6. 参数类型不匹配 ====================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数 '%s' 类型不匹配，期望类型: %s",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        log.warn(message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    // ==================== 7. JSON 格式错误 ====================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR, "JSON请求体格式错误");
    }

    // ==================== 8. 约束校验异常（如 @Pattern 在 DTO 外） ====================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("；"));
        log.warn("约束校验失败: {}", message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    // ==================== 9. 资源重复 =====================
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("数据插入/更新失败，违反唯一约束，详细信息：{}", e.getMessage());
        return Result.error(ResultCode.RESOURCE_CONFLICT);
    }

    // =================== 10. 所有未捕获的异常（兜底） ====================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(ResultCode.INTERNAL_ERROR);
    }
}