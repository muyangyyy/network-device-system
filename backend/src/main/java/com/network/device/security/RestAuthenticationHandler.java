package com.network.device.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.device.common.Result;
import com.network.device.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 过滤器链内的「认证失败 / 权限不足」统一响应。
 *
 * <p><b>为什么必须有这个类</b>：过滤器链里抛出的异常<b>不会</b>进入
 * {@code @RestControllerAdvice}（那只处理 Controller 层抛出的异常）。
 * 若不配置 entryPoint，Spring Security 会回落到默认的 {@code Http403ForbiddenEntryPoint}，
 * 表现为 <b>token 过期或缺失时返回 403 且响应体为空</b>。前端只能识别到「没有权限」，
 * 既不会清理 token 也不会跳登录页，用户会卡在当前页面出不来。
 *
 * <p>这里统一改写成与业务接口一致的 {@code Result} JSON：
 * 未认证 -&gt; 401，已认证但无权限 -&gt; 403，前端据此分别执行「跳登录」与「提示无权限」。
 */
@Component
public class RestAuthenticationHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationHandler.class);

    private final ObjectMapper objectMapper;

    public RestAuthenticationHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 未认证：token 缺失、过期或非法 */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("未认证请求: {} {}", request.getMethod(), request.getRequestURI());
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                ResultCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
    }

    /** 已认证但权限不足 */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("权限不足: {} {}", request.getMethod(), request.getRequestURI());
        write(response, HttpServletResponse.SC_FORBIDDEN,
                ResultCode.FORBIDDEN.getCode(), "没有访问权限");
    }

    private void write(HttpServletResponse response, int httpStatus, Integer code, String message)
            throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}
