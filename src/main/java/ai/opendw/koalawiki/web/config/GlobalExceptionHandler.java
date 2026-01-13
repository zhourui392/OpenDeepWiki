package ai.opendw.koalawiki.web.config;

import ai.opendw.koalawiki.web.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理参数验证异常,返回标准的Result格式
 *
 * @author zhourui(V33215020)
 * @since 2025/11/15
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理@Valid验证失败异常
     * 当请求体参数验证失败时触发
     *
     * @param e 方法参数验证异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(
            MethodArgumentNotValidException e) {

        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("参数验证失败: {}", message);
        return ResponseEntity.ok(Result.error("参数验证失败: " + message));
    }

    /**
     * 处理@Validated验证失败异常
     * 当方法参数验证失败时触发
     *
     * @param e 约束违反异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(
            ConstraintViolationException e) {

        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        log.warn("参数约束验证失败: {}", message);
        return ResponseEntity.ok(Result.error("参数验证失败: " + message));
    }

    /**
     * 处理绑定异常
     * 当参数绑定失败时触发
     *
     * @param e 绑定异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("参数绑定失败: {}", message);
        return ResponseEntity.ok(Result.error("参数绑定失败: " + message));
    }

    /**
     * 处理IllegalArgumentException
     * 当方法参数不合法时触发
     *
     * @param e 非法参数异常
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(
            IllegalArgumentException e) {

        log.warn("非法参数: {}", e.getMessage());
        return ResponseEntity.ok(Result.error("参数错误: " + e.getMessage()));
    }

    /**
     * 处理所有未捕获的异常
     * 作为兜底处理,避免500错误暴露给前端
     *
     * @param e 异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.ok(Result.error("系统异常,请联系管理员"));
    }
}
