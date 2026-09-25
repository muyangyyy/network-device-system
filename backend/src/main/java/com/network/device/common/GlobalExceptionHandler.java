package com.network.device.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.format.DateTimeParseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("Validation exception: {}", message);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数绑定失败");
        log.warn("Bind exception: {}", message);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Bad credentials: {}", e.getMessage());
        return Result.error(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return Result.error(ResultCode.FORBIDDEN.getCode(), "没有访问权限");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return Result.error(405, "不支持的请求方法: " + e.getMethod());
    }

    /**
     * 请求体无法解析（JSON 格式错误、字段类型不匹配、必填的 body 缺失等）。
     *
     * <p>没有这个处理器时，这类错误会落到兜底的 {@code Exception} 分支返回 500，
     * 把「客户端传错了」误报成「服务器故障」，前端也拿不到可读的原因。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Message not readable: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求体格式错误，请检查提交的数据");
    }

    /**
     * 上传文件超过大小限制（spring.servlet.multipart.max-file-size）。
     *
     * <p>不处理会返回 500「服务器内部错误」，用户无法得知是自己传的文件太大。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("Upload size exceeded: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "上传文件过大，请压缩后重试");
    }

    /**
     * 唯一键冲突。
     *
     * <p>典型来源：设备编码 / 工单号取号并发冲突、用户名或字典类型重复。
     * 兜底返回 500 会把「数据重复」误报成「服务器故障」，
     * 且原始异常信息里含有表名与索引名，属于不必要的信息外泄。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<?> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("Duplicate key: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "数据已存在或编号冲突，请重试");
    }

    /**
     * 日期/时间参数格式错误。
     *
     * <p>典型来源：列表页与统计页的筛选条件。日期选择器给的是 {@code 2026-09-01}，
     * 而 {@code LocalDateTime.parse("2026-09-01")} 会直接抛 {@code DateTimeParseException}；
     * 手输或外部调用传了 {@code 2026/09/01} 之类的串同样会抛。
     *
     * <p>没有这个处理器时，这类「客户端传错了」会落到兜底的 {@code Exception} 分支返回 500，
     * 把参数问题误报成服务器故障，前端也拿不到可读原因。
     */
    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleDateTimeParseException(DateTimeParseException e) {
        log.warn("Date time parse failed: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(),
                "日期时间格式错误，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        // 原始异常信息（含 SQL 片段、表名/列名、类名、文件路径）只写日志，不返回给客户端。
        // 直接拼进响应体会造成不必要的信息外泄。
        log.error("Unexpected exception: ", e);
        return Result.error(ResultCode.INTERNAL_ERROR.getCode(), "服务器内部错误，请稍后重试或联系管理员");
    }
}
