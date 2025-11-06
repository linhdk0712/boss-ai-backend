package ai.content.auto.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for enhanced logging with context information
 * Provides methods to add correlation IDs and user context to logs
 */
@Slf4j
public class LoggingUtil {

    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String USER_ID_KEY = "userId";
    private static final String USERNAME_KEY = "username";
    private static final String OPERATION_KEY = "operation";

    /**
     * Generate and set a correlation ID for request tracking
     * 
     * @return the generated correlation ID
     */
    public static String generateCorrelationId() {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(CORRELATION_ID_KEY, correlationId);
        return correlationId;
    }

    /**
     * Set user context in MDC for logging
     * 
     * @param userId   the user ID
     * @param username the username
     */
    public static void setUserContext(Long userId, String username) {
        if (userId != null) {
            MDC.put(USER_ID_KEY, userId.toString());
        }
        if (username != null) {
            MDC.put(USERNAME_KEY, username);
        }
    }

    /**
     * Set operation context in MDC for logging
     * 
     * @param operation the operation being performed
     */
    public static void setOperationContext(String operation) {
        if (operation != null) {
            MDC.put(OPERATION_KEY, operation);
        }
    }

    /**
     * Clear all MDC context
     */
    public static void clearContext() {
        MDC.clear();
    }

    /**
     * Clear specific MDC keys
     */
    public static void clearUserContext() {
        MDC.remove(USER_ID_KEY);
        MDC.remove(USERNAME_KEY);
    }

    /**
     * Log method entry with parameters
     * 
     * @param methodName the method name
     * @param params     the method parameters
     */
    public static void logMethodEntry(String methodName, Object... params) {
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Entering method: ").append(methodName);
            if (params != null && params.length > 0) {
                sb.append(" with parameters: ");
                for (int i = 0; i < params.length; i++) {
                    if (i > 0)
                        sb.append(", ");
                    sb.append(sanitizeForLogging(params[i]));
                }
            }
            log.debug(sb.toString());
        }
    }

    /**
     * Log method exit with result
     * 
     * @param methodName the method name
     * @param result     the method result
     */
    public static void logMethodExit(String methodName, Object result) {
        if (log.isDebugEnabled()) {
            log.debug("Exiting method: {} with result: {}", methodName, sanitizeForLogging(result));
        }
    }

    /**
     * Log method exit with execution time
     * 
     * @param methodName the method name
     * @param startTime  the start time in milliseconds
     */
    public static void logMethodExitWithTime(String methodName, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        if (executionTime > 1000) {
            log.warn("Method {} took {}ms to execute", methodName, executionTime);
        } else if (log.isDebugEnabled()) {
            log.debug("Method {} completed in {}ms", methodName, executionTime);
        }
    }

    /**
     * Sanitize sensitive data for logging
     * 
     * @param obj the object to sanitize
     * @return sanitized string representation
     */
    private static String sanitizeForLogging(Object obj) {
        if (obj == null) {
            return "null";
        }

        String str = obj.toString();

        // Mask sensitive information
        if (str.toLowerCase().contains("password")) {
            return "[MASKED]";
        }
        if (str.toLowerCase().contains("token")) {
            return "[MASKED]";
        }
        if (str.toLowerCase().contains("secret")) {
            return "[MASKED]";
        }

        // Truncate long strings
        if (str.length() > 100) {
            return str.substring(0, 97) + "...";
        }

        return str;
    }

    /**
     * Log business operation with context
     * 
     * @param operation  the business operation
     * @param entityType the entity type
     * @param entityId   the entity ID
     * @param userId     the user ID performing the operation
     */
    public static void logBusinessOperation(String operation, String entityType, Object entityId, Long userId) {
        log.info("Business operation: {} on {}:{} by user:{}",
                operation, entityType, entityId, userId);
    }

    /**
     * Log security event
     * 
     * @param event    the security event
     * @param username the username involved
     * @param details  additional details
     */
    public static void logSecurityEvent(String event, String username, String details) {
        log.warn("Security event: {} for user: {} - {}", event, username, details);
    }

    /**
     * Log performance warning
     * 
     * @param operation     the operation
     * @param executionTime the execution time in milliseconds
     * @param threshold     the threshold in milliseconds
     */
    public static void logPerformanceWarning(String operation, long executionTime, long threshold) {
        if (executionTime > threshold) {
            log.warn("Performance warning: {} took {}ms (threshold: {}ms)",
                    operation, executionTime, threshold);
        }
    }
}