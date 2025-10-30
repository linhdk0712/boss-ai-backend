package ai.content.auto.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Logging Aspect for comprehensive application logging
 * Following best practices for production monitoring
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private final ObjectMapper objectMapper;

    /**
     * Pointcut for all controller methods
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {
    }

    /**
     * Pointcut for all service methods
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {
    }

    /**
     * Pointcut for all repository methods
     */
    @Pointcut("within(@org.springframework.stereotype.Repository *)")
    public void repositoryMethods() {
    }

    /**
     * Pointcut for methods annotated with @Loggable
     */
    @Pointcut("@annotation(ai.content.auto.annotation.Loggable)")
    public void loggableMethods() {
    }

    /**
     * Around advice for controller methods
     */
    @Around("controllerMethods()")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        // Get HTTP request details
        HttpServletRequest request = getCurrentHttpRequest();
        String requestId = generateRequestId();

        try {
            // Log request
            logHttpRequest(request, methodName, joinPoint.getArgs(), requestId);

            // Execute method
            Object result = joinPoint.proceed();

            // Log successful response
            long executionTime = System.currentTimeMillis() - startTime;
            logHttpResponse(methodName, result, executionTime, requestId, true);

            return result;

        } catch (Exception ex) {
            // Log error response
            long executionTime = System.currentTimeMillis() - startTime;
            logHttpResponse(methodName, ex, executionTime, requestId, false);
            throw ex;
        }
    }

    /**
     * Around advice for service methods
     */
    @Around("serviceMethods()")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        log.debug("Service method started: {} with args: {}",
                methodName, sanitizeArgs(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.debug("Service method completed: {} in {}ms", methodName, executionTime);
            return result;

        } catch (Exception ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Service method failed: {} in {}ms with error: {}",
                    methodName, executionTime, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Around advice for repository methods
     */
    @Around("repositoryMethods()")
    public Object logRepositoryExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        log.trace("Repository method started: {}", methodName);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.trace("Repository method completed: {} in {}ms", methodName, executionTime);
            return result;

        } catch (Exception ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Repository method failed: {} in {}ms with error: {}",
                    methodName, executionTime, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Around advice for methods annotated with @Loggable
     */
    @Around("loggableMethods()")
    public Object logAnnotatedMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        log.info("Method execution started: {} with args: {}",
                methodName, sanitizeArgs(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("Method execution completed: {} in {}ms", methodName, executionTime);
            return result;

        } catch (Exception ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Method execution failed: {} in {}ms with error: {}",
                    methodName, executionTime, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * After throwing advice for all exceptions
     */
    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        String methodName = joinPoint.getSignature().toShortString();
        log.error("Exception in method: {} - Error: {} - Stack trace: {}",
                methodName, ex.getMessage(), getStackTrace(ex));
    }

    /**
     * Log HTTP request details
     */
    private void logHttpRequest(HttpServletRequest request, String methodName,
            Object[] args, String requestId) {
        if (request == null)
            return;

        Map<String, Object> requestLog = new HashMap<>();
        requestLog.put("requestId", requestId);
        requestLog.put("timestamp", LocalDateTime.now());
        requestLog.put("method", request.getMethod());
        requestLog.put("uri", request.getRequestURI());
        requestLog.put("queryString", request.getQueryString());
        requestLog.put("remoteAddr", getClientIpAddress(request));
        requestLog.put("userAgent", request.getHeader("User-Agent"));
        requestLog.put("contentType", request.getContentType());
        requestLog.put("methodName", methodName);

        // Add headers (excluding sensitive ones)
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!isSensitiveHeader(headerName)) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        requestLog.put("headers", headers);

        // Add sanitized arguments
        requestLog.put("args", sanitizeArgs(args));

        try {
            log.info("HTTP Request: {}", objectMapper.writeValueAsString(requestLog));
        } catch (Exception e) {
            log.info("HTTP Request: {} (JSON serialization failed)", requestLog);
        }
    }

    /**
     * Log HTTP response details
     */
    private void logHttpResponse(String methodName, Object result, long executionTime,
            String requestId, boolean success) {
        Map<String, Object> responseLog = new HashMap<>();
        responseLog.put("requestId", requestId);
        responseLog.put("timestamp", LocalDateTime.now());
        responseLog.put("methodName", methodName);
        responseLog.put("executionTime", executionTime + "ms");
        responseLog.put("success", success);

        if (success) {
            responseLog.put("resultType", result != null ? result.getClass().getSimpleName() : "null");
        } else {
            responseLog.put("error",
                    result instanceof Exception ? ((Exception) result).getMessage() : result.toString());
        }

        try {
            log.info("HTTP Response: {}", objectMapper.writeValueAsString(responseLog));
        } catch (Exception e) {
            log.info("HTTP Response: {} (JSON serialization failed)", responseLog);
        }
    }

    /**
     * Get current HTTP request
     */
    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generate unique request ID
     */
    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" +
                Thread.currentThread().getId();
    }

    /**
     * Get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null || xForwardedForHeader.isEmpty()) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0].trim();
        }
    }

    /**
     * Check if header is sensitive and should not be logged
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerCaseHeaderName = headerName.toLowerCase();
        return lowerCaseHeaderName.contains("authorization") ||
                lowerCaseHeaderName.contains("cookie") ||
                lowerCaseHeaderName.contains("password") ||
                lowerCaseHeaderName.contains("token") ||
                lowerCaseHeaderName.contains("secret");
    }

    /**
     * Sanitize method arguments to remove sensitive data
     */
    private Object[] sanitizeArgs(Object[] args) {
        if (args == null)
            return null;

        return Arrays.stream(args)
                .map(this::sanitizeObject)
                .toArray();
    }

    /**
     * Sanitize individual object to remove sensitive data
     */
    private Object sanitizeObject(Object obj) {
        if (obj == null)
            return null;

        String objString = obj.toString();
        String className = obj.getClass().getSimpleName();

        // Don't log sensitive objects in detail
        if (className.toLowerCase().contains("password") ||
                className.toLowerCase().contains("credential") ||
                objString.toLowerCase().contains("password")) {
            return "[SANITIZED-" + className + "]";
        }

        return obj;
    }

    /**
     * Get formatted stack trace
     */
    private String getStackTrace(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getClass().getSimpleName()).append(": ").append(ex.getMessage());

        StackTraceElement[] elements = ex.getStackTrace();
        for (int i = 0; i < Math.min(5, elements.length); i++) {
            sb.append("\n\tat ").append(elements[i]);
        }

        if (elements.length > 5) {
            sb.append("\n\t... ").append(elements.length - 5).append(" more");
        }

        return sb.toString();
    }
}