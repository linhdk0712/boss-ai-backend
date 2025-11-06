package ai.content.auto.service;

import ai.content.auto.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Example service demonstrating enhanced logging practices
 * Shows how to use class information, method names, and context in logs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedLoggingExampleService {

    /**
     * Example method showing comprehensive logging
     */
    public String processUserData(Long userId, String operation) {
        long startTime = System.currentTimeMillis();

        // Set user context for all subsequent logs
        LoggingUtil.setUserContext(userId, "user-" + userId);
        LoggingUtil.setOperationContext(operation);

        try {
            // Log method entry
            LoggingUtil.logMethodEntry("processUserData", userId, operation);
            log.info("Starting user data processing for user: {}", userId);

            // Simulate some business logic
            if (userId == null) {
                log.error("Invalid input: userId cannot be null");
                throw new IllegalArgumentException("User ID is required");
            }

            // Log business operation
            LoggingUtil.logBusinessOperation("PROCESS_DATA", "User", userId, userId);

            // Simulate processing time
            Thread.sleep(100);

            String result = "Processed data for user " + userId;

            // Log successful completion
            log.info("Successfully processed data for user: {} with operation: {}", userId, operation);
            LoggingUtil.logMethodExit("processUserData", result);

            return result;

        } catch (InterruptedException e) {
            log.error("Processing interrupted for user: {}", userId, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        } catch (Exception e) {
            log.error("Failed to process data for user: {} with operation: {}", userId, operation, e);
            throw e;
        } finally {
            // Log execution time
            LoggingUtil.logMethodExitWithTime("processUserData", startTime);

            // Clear user context (correlation ID will be cleared by filter)
            LoggingUtil.clearUserContext();
        }
    }

    /**
     * Example method showing different log levels and contexts
     */
    public void demonstrateLogLevels(String testData) {
        log.trace("TRACE: Very detailed information - testData length: {}", testData.length());
        log.debug("DEBUG: Detailed information for debugging - processing: {}", testData);
        log.info("INFO: General information - operation started");
        log.warn("WARN: Warning message - unusual condition detected");
        log.error("ERROR: Error message - something went wrong");

        // Example of logging with different contexts
        LoggingUtil.logSecurityEvent("DATA_ACCESS", "system", "Accessed sensitive data");
        LoggingUtil.logPerformanceWarning("data-processing", 1500, 1000);
    }

    /**
     * Example showing how to log sensitive data safely
     */
    public void handleSensitiveData(String username, String password, String token) {
        // ✅ GOOD: Log username (not sensitive)
        log.info("Processing authentication for user: {}", username);

        // ✅ GOOD: Don't log sensitive data directly
        log.debug("Authentication data received - password length: {}, token present: {}",
                password != null ? password.length() : 0, token != null);

        // ❌ BAD: Never log sensitive data
        // log.info("User {} with password {} and token {}", username, password, token);

        // ✅ GOOD: Use LoggingUtil for automatic sanitization
        LoggingUtil.logSecurityEvent("LOGIN_ATTEMPT", username, "Authentication data processed");
    }

    /**
     * Example showing error logging with context
     */
    public void handleError(Long userId, String operation) {
        try {
            // Simulate an error
            throw new RuntimeException("Simulated error");
        } catch (Exception e) {
            // ✅ GOOD: Log with full context
            log.error("Operation failed - User: {}, Operation: {}, Error: {}",
                    userId, operation, e.getMessage(), e);

            // ✅ GOOD: Log business impact
            LoggingUtil.logBusinessOperation("OPERATION_FAILED", "User", userId, userId);

            // Re-throw or handle as appropriate
            throw new RuntimeException("Failed to process operation: " + operation, e);
        }
    }
}