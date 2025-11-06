package ai.content.auto.controller;

import ai.content.auto.service.EnhancedLoggingExampleService;
import ai.content.auto.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test controller to demonstrate enhanced logging
 * Use this to test the logging configuration
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/logging-test")
@RequiredArgsConstructor
public class LoggingTestController {

    private final EnhancedLoggingExampleService loggingExampleService;

    /**
     * Test endpoint to demonstrate class-based logging
     */
    @GetMapping("/test/{userId}")
    public ResponseEntity<String> testLogging(@PathVariable Long userId,
            @RequestParam(defaultValue = "TEST_OPERATION") String operation) {

        log.info("LoggingTestController.testLogging called with userId: {} and operation: {}", userId, operation);

        try {
            String result = loggingExampleService.processUserData(userId, operation);

            log.info("LoggingTestController.testLogging completed successfully");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("LoggingTestController.testLogging failed for userId: {}", userId, e);
            return ResponseEntity.internalServerError().body("Processing failed: " + e.getMessage());
        }
    }

    /**
     * Test endpoint to demonstrate different log levels
     */
    @PostMapping("/log-levels")
    public ResponseEntity<String> testLogLevels(@RequestBody String testData) {

        log.info("LoggingTestController.testLogLevels called");

        loggingExampleService.demonstrateLogLevels(testData);

        return ResponseEntity.ok("Log levels demonstrated - check logs");
    }

    /**
     * Test endpoint to demonstrate error logging
     */
    @GetMapping("/error/{userId}")
    public ResponseEntity<String> testErrorLogging(@PathVariable Long userId) {

        log.info("LoggingTestController.testErrorLogging called with userId: {}", userId);

        try {
            loggingExampleService.handleError(userId, "ERROR_TEST");
            return ResponseEntity.ok("This should not be reached");
        } catch (Exception e) {
            log.error("LoggingTestController.testErrorLogging caught exception", e);
            return ResponseEntity.internalServerError().body("Expected error occurred");
        }
    }

    /**
     * Test endpoint to demonstrate sensitive data logging
     */
    @PostMapping("/sensitive-data")
    public ResponseEntity<String> testSensitiveDataLogging(@RequestParam String username,
            @RequestParam String password,
            @RequestParam String token) {

        log.info("LoggingTestController.testSensitiveDataLogging called for user: {}", username);

        loggingExampleService.handleSensitiveData(username, password, token);

        return ResponseEntity.ok("Sensitive data processed - check logs for proper sanitization");
    }

    /**
     * Test endpoint to demonstrate correlation ID tracking
     */
    @GetMapping("/correlation-test")
    public ResponseEntity<String> testCorrelationId() {

        log.info("LoggingTestController.testCorrelationId - Step 1");

        // Simulate calling another service
        simulateServiceCall();

        log.info("LoggingTestController.testCorrelationId - Step 3 (final)");

        return ResponseEntity.ok("Correlation ID test completed - check logs for consistent correlation ID");
    }

    private void simulateServiceCall() {
        log.info("LoggingTestController.simulateServiceCall - Step 2 (nested call)");

        // This log should have the same correlation ID as the parent call
        LoggingUtil.logBusinessOperation("SIMULATE_CALL", "Test", "test-id", 999L);
    }
}