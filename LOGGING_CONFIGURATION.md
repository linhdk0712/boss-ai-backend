# Enhanced Logging Configuration

## Overview
This document explains the enhanced logging configuration that includes class information, method names, line numbers, and contextual information for better traceability.

## What's New

### 1. Enhanced Log Format
**Before:**
```
2024-01-01 10:30:45 - User login successful
```

**After:**
```
2024-01-01 10:30:45.123 [http-nio-8080-exec-1] INFO  ai.content.auto.service.AuthService.login:85 [abc12345][123][john.doe][LOGIN] - User login successful
```

### 2. Log Format Breakdown
```
[Timestamp] [Thread] [Level] [Class.Method:Line] [CorrelationId][UserId][Username][Operation] - Message
```

- **Timestamp**: Precise timestamp with milliseconds
- **Thread**: Thread name executing the code
- **Level**: Log level (INFO, WARN, ERROR, DEBUG, TRACE)
- **Class.Method:Line**: Exact class, method, and line number
- **CorrelationId**: Unique ID to track requests across services
- **UserId**: Current user ID (if available)
- **Username**: Current username (if available)
- **Operation**: Current operation being performed
- **Message**: The actual log message

## Configuration Files

### 1. application.yml
Enhanced logging patterns in Spring Boot configuration:
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36}.%M:%L - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50}.%M:%L - %msg%n"
```

### 2. logback-spring.xml
Advanced Logback configuration with:
- **Colored console output** for better readability
- **Separate error log file** for error-only logs
- **Async appenders** for better performance
- **MDC context** for correlation IDs and user information
- **Profile-specific configurations** for dev/prod environments

## Log Files Structure

### Production Log Files
```
logs/
├── boss-ai-backend.log              # Main application log
├── boss-ai-backend-error.log        # Error-only log
├── boss-ai-backend.2024-01-01.1.log.gz  # Archived logs
└── boss-ai-backend-error.2024-01-01.1.log.gz
```

### Log Rotation
- **File Size**: 10MB per file
- **History**: 30 days retention
- **Total Size**: 1GB cap for main logs, 500MB for error logs
- **Compression**: Automatic gzip compression for archived files

## Usage Examples

### 1. Basic Service Logging
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with username: {}", request.getUsername());
        
        try {
            // Business logic
            User user = userRepository.save(buildUser(request));
            
            log.info("Successfully created user with ID: {}", user.getId());
            return mapToResponse(user);
            
        } catch (Exception e) {
            log.error("Failed to create user: {}", request.getUsername(), e);
            throw new BusinessException("User creation failed");
        }
    }
}
```

**Output:**
```
2024-01-01 10:30:45.123 [http-nio-8080-exec-1] INFO  ai.content.auto.service.UserService.createUser:25 [abc12345][123][admin][CREATE_USER] - Creating user with username: john.doe
2024-01-01 10:30:45.456 [http-nio-8080-exec-1] INFO  ai.content.auto.service.UserService.createUser:32 [abc12345][123][admin][CREATE_USER] - Successfully created user with ID: 456
```

### 2. Using LoggingUtil for Enhanced Context
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    public OrderResponse processOrder(Long userId, CreateOrderRequest request) {
        // Set user context
        LoggingUtil.setUserContext(userId, "user-" + userId);
        LoggingUtil.setOperationContext("PROCESS_ORDER");
        
        try {
            log.info("Processing order for {} items", request.getItems().size());
            
            // Log business operation
            LoggingUtil.logBusinessOperation("CREATE_ORDER", "Order", null, userId);
            
            // Business logic...
            
            return orderResponse;
        } finally {
            LoggingUtil.clearUserContext();
        }
    }
}
```

### 3. Controller Logging
```java
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    @PostMapping
    public ResponseEntity<BaseResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        
        log.info("UserController.createUser called for username: {}", request.getUsername());
        
        UserResponse user = userService.createUser(request);
        
        log.info("UserController.createUser completed successfully");
        return ResponseEntity.ok(new BaseResponse<>().setData(user));
    }
}
```

## Testing the Configuration

### 1. Test Endpoints
Use the `LoggingTestController` to test different logging scenarios:

```bash
# Test basic logging with class information
GET /api/v1/logging-test/test/123?operation=TEST_OP

# Test different log levels
POST /api/v1/logging-test/log-levels
Content-Type: application/json
Body: "test data"

# Test error logging
GET /api/v1/logging-test/error/123

# Test correlation ID tracking
GET /api/v1/logging-test/correlation-test
```

### 2. Expected Output
```
2024-01-01 10:30:45.123 [http-nio-8080-exec-1] INFO  ai.content.auto.filter.LoggingFilter.doFilter:45 [abc12345][][] - Request started: GET /api/v1/logging-test/test/123
2024-01-01 10:30:45.124 [http-nio-8080-exec-1] INFO  ai.content.auto.controller.LoggingTestController.testLogging:28 [abc12345][][] - LoggingTestController.testLogging called with userId: 123 and operation: TEST_OP
2024-01-01 10:30:45.125 [http-nio-8080-exec-1] INFO  ai.content.auto.service.EnhancedLoggingExampleService.processUserData:32 [abc12345][123][user-123][TEST_OP] - Starting user data processing for user: 123
2024-01-01 10:30:45.126 [http-nio-8080-exec-1] INFO  ai.content.auto.service.EnhancedLoggingExampleService.processUserData:45 [abc12345][123][user-123][TEST_OP] - Successfully processed data for user: 123 with operation: TEST_OP
2024-01-01 10:30:45.127 [http-nio-8080-exec-1] INFO  ai.content.auto.controller.LoggingTestController.testLogging:32 [abc12345][][] - LoggingTestController.testLogging completed successfully
2024-01-01 10:30:45.128 [http-nio-8080-exec-1] INFO  ai.content.auto.filter.LoggingFilter.doFilter:58 [abc12345][][] - Request completed: GET /api/v1/logging-test/test/123 - Status: 200 - Time: 5ms
```

## Benefits

### 1. **Easy Tracing**
- See exactly which class and method generated each log
- Track requests across multiple services with correlation IDs
- Follow user actions with user context

### 2. **Better Debugging**
- Line numbers help locate issues quickly
- Method names provide execution flow context
- Thread information helps with concurrency issues

### 3. **Performance Monitoring**
- Request timing information
- Performance warnings for slow operations
- Thread pool monitoring

### 4. **Security Auditing**
- User context in all operations
- Security event logging
- Sensitive data sanitization

## Best Practices

### 1. **Log Levels**
- **ERROR**: System errors, exceptions that need attention
- **WARN**: Unusual conditions, performance issues
- **INFO**: Business events, request/response logging
- **DEBUG**: Detailed debugging information
- **TRACE**: Very detailed execution flow

### 2. **What to Log**
```java
// ✅ GOOD: Business events with context
log.info("User {} created order {} with {} items", userId, orderId, itemCount);

// ✅ GOOD: Errors with full context
log.error("Failed to process payment for order: {} user: {}", orderId, userId, exception);

// ❌ BAD: Sensitive information
log.info("User password: {}", password);

// ❌ BAD: Too verbose
log.info("Entering method processOrder");
log.info("Validating order");
log.info("Order validation complete");
```

### 3. **Performance Considerations**
- Use async appenders for high-throughput applications
- Set appropriate log levels for production
- Monitor log file sizes and rotation
- Use structured logging for log analysis tools

## Troubleshooting

### 1. **Logs Not Showing Class Information**
- Verify `logback-spring.xml` is in `src/main/resources/`
- Check that `%logger{36}.%M:%L` is in the pattern
- Restart the application after configuration changes

### 2. **Performance Issues**
- Enable async appenders in `logback-spring.xml`
- Reduce log levels in production
- Monitor disk space for log files

### 3. **Missing Context Information**
- Ensure `LoggingFilter` is registered
- Check MDC context is set properly
- Verify correlation ID generation

## Production Recommendations

### 1. **Log Levels**
```yaml
# Production logging levels
logging:
  level:
    ai.content.auto: INFO
    org.springframework: WARN
    org.hibernate: WARN
```

### 2. **File Management**
- Monitor disk space regularly
- Set up log rotation and archival
- Consider centralized logging (ELK stack, Splunk)

### 3. **Security**
- Never log sensitive data (passwords, tokens, PII)
- Use LoggingUtil for automatic sanitization
- Implement log access controls

This enhanced logging configuration provides comprehensive traceability while maintaining good performance and security practices.