package com.network.device.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.device.annotation.OperationLog;
import com.network.device.mapper.OperationLogMapper;
import com.network.device.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(OperationLogMapper operationLogMapper, ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        com.network.device.entity.OperationLog logEntity = new com.network.device.entity.OperationLog();
        logEntity.setModule(operationLog.module());
        logEntity.setOperation(operationLog.operation());

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        logEntity.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logEntity.setRequestUrl(request.getRequestURI());
                logEntity.setRequestMethod(request.getMethod());
                logEntity.setOperatorIp(request.getRemoteAddr());
            }
        } catch (Exception ignored) {
        }

        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                logEntity.setRequestParams(objectMapper.writeValueAsString(args));
            }
        } catch (Exception ignored) {
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            logEntity.setOperatorId(loginUser.getId());
            logEntity.setOperatorName(loginUser.getUsername());
        }

        logEntity.setOperateTime(LocalDateTime.now());

        Object result = null;
        try {
            result = joinPoint.proceed();
            logEntity.setStatus(1);
            try {
                logEntity.setResponseResult(objectMapper.writeValueAsString(result));
            } catch (Exception ignored) {
            }
        } catch (Throwable e) {
            logEntity.setStatus(0);
            logEntity.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            try {
                operationLogMapper.insert(logEntity);
            } catch (Exception e) {
                log.error("Failed to save operation log", e);
            }
        }

        return result;
    }
}
