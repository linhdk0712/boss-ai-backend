# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Java Version**: 17
- **Build Tool**: Gradle with Gradle Wrapper
- **Database**: PostgreSQL with HikariCP connection pooling
- **Caching**: Redis with Lettuce client
- **Security**: Spring Security with JWT (JJWT library)
- **AI Integration**: Spring AI with OpenAI (currently disabled)
- **Key Libraries**: Lombok, MapStruct, Spring Data JPA

## Common Commands

### Build and Run
```bash
# Run application in development mode
./gradlew bootRun

# Build the application (creates JAR in build/libs/)
./gradlew build

# Clean and rebuild
./gradlew clean build

# Run tests
./gradlew test

# Run tests for a specific class
./gradlew test --tests "ai.content.auto.BossAiBackendApplicationTests"

# Run tests matching a pattern
./gradlew test --tests "*ServiceTest"

# Skip tests during build
./gradlew build -x test
```

### Code Quality
```bash
# Compile Java classes
./gradlew compileJava

# Check for dependency updates
./gradlew dependencyUpdates
```

## Architecture Overview

### Authentication & Security Architecture

The application uses **JWT-based stateless authentication** with the following components:

1. **JwtService** (`service/JwtService.java`): Handles token generation and validation
   - Generates access tokens (1 hour expiration) and refresh tokens (30 days)
   - Uses JJWT library with HS256 signing
   - Supports token type distinction (access vs refresh)

2. **JwtAuthenticationFilter** (`security/JwtAuthenticationFilter.java`): Intercepts requests
   - Extracts JWT from Authorization header (Bearer token)
   - Validates token and loads user details
   - Sets SecurityContext for authenticated requests

3. **SecurityConfig** (`config/SecurityConfig.java`): Central security configuration
   - Configures stateless session management
   - Defines public endpoints (auth APIs, actuator health, API docs)
   - Role-based access control (USER, ADMIN)
   - CORS configuration with environment-specific origins
   - BCrypt password encoding with strength 12

4. **AuthService** (`service/AuthService.java`): Business logic for authentication
   - Login with credentials validation
   - User registration with email verification
   - Account activation via email tokens
   - Token refresh with validation
   - Failed login tracking and account locking

5. **UserService** implements Spring Security's `UserDetailsService`
   - Loads users by username for authentication
   - Dynamically loads user roles from database
   - Integrates User entity with Spring Security

### Database Architecture

**Connection Pooling**: HikariCP with production-ready configuration
- Pool size: 20 max connections, 5 minimum idle
- Connection leak detection enabled (60s threshold)
- Prepared statement caching for performance
- Connection lifecycle management with timeouts

**JPA Configuration**:
- Schema validation mode (`validate`) - requires existing database schema
- Batch processing enabled (batch size: 25)
- Hibernate optimizations for inserts/updates ordering
- PostgreSQL dialect with SQL formatting and comments

**Entity Relationships**:
- `User` entity: Core user information with security fields
- `UserRole` entity: Many-to-many relationship via composite key (`UserRoleId`)
- Roles stored in database, loaded dynamically (fallback to "USER")

**Important**: This project uses `hibernate.hbm2ddl.auto=validate`, meaning the database schema must exist before running the application. The application will not auto-create tables.

### Transaction Management Pattern

The codebase follows a **separation of concerns** pattern for transaction management:

1. **Read-only operations** should use `@Transactional(readOnly = true)` to optimize performance
2. **Authentication operations** (login) avoid transactions on the main flow:
   - User lookup: no transaction (quick read)
   - Authentication: no transaction (Spring Security handles this)
   - Success/failure updates: delegated to separate transactional service (`UserUpdateService`)
3. **User updates** are isolated in `UserUpdateService` with separate transactions to avoid:
   - Blocking the main authentication flow
   - Long-running transactions during authentication
   - Transaction rollback affecting the authentication response

Example from `AuthService.login()`:
```java
// No @Transactional on login method
public AuthResponse login(LoginRequest request) {
  // ... authentication logic ...

  // Delegate to separate transaction
  updateSuccessfulLogin(user.getId());
}

// Calls separate service with its own transaction
protected void updateSuccessfulLogin(Long userId) {
  userUpdateService.resetFailedLoginAttempts(userId); // @Transactional
}
```

### Global Exception Handling

**GlobalExceptionHandler** (`exception/GlobalExceptionHandler.java`) provides centralized error handling:
- Validation errors with field-level details
- Authentication/authorization errors with BaseResponse format
- Business exceptions with custom error codes
- Spring Security exceptions (BadCredentials, Locked, Disabled)
- Database integrity violations
- HTTP protocol errors (method not allowed, media type, etc.)

Returns different response formats:
- `BaseResponse<Object>` for authentication/business errors (consistent with API responses)
- `ErrorResponse` for validation/technical errors (detailed error information)

### Configuration Classes

1. **DatabaseConfig**: HikariCP + JPA configuration with production optimizations
2. **RedisConfig**: Redis caching with Jackson JSON serialization, connection pooling
3. **SecurityConfig**: Spring Security with JWT filter chain, CORS, role-based access
4. **DataInitializer**: Seeds initial data (roles, default admin user) on application startup

### Service Layer Standards

Services follow these patterns:
- Use `@Loggable` annotation for method execution logging (via AOP)
- Proper exception handling with context logging
- Configuration injection via `@Value` annotations
- Transaction boundaries defined based on operation type
- Separation between read operations and write operations

### API Response Format

Standard response wrapper: `BaseResponse<T>` with fields:
- `errorCode`: String code indicating result status (e.g., "SUCCESS", "BUSINESS_ERROR")
- `errorMessage`: Human-readable message
- `data`: Generic payload of type T

Authentication responses use `AuthResponse` containing:
- Token information (accessToken, refreshToken, tokenType, expiresIn, refreshExpiresIn)
- User information (id, username, email, firstName, lastName, role, emailVerified, profilePictureUrl)

## Environment Configuration

Required environment variables:
```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT (use a strong secret in production, minimum 256 bits)
JWT_SECRET=your_jwt_secret_key_minimum_256_bits

# Email Service
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM=noreply@yourdomain.com

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://yourdomain.com

# Security
MAX_FAILED_ATTEMPTS=5
LOCK_DURATION_MINUTES=30

# Frontend
FRONTEND_URL=http://localhost:3000
```

All configurations in `application.yml` have default values for local development.

## Package Structure

```
ai.content.auto/
├── annotation/       # Custom annotations (@Loggable for AOP logging)
├── aspect/           # AOP aspects (LoggingAspect for method logging)
├── config/           # Configuration classes (Database, Redis, Security, DataInitializer)
├── controller/       # REST controllers (AuthController, TestController)
├── dtos/             # Data Transfer Objects with nested auth/ package
├── entity/           # JPA entities (User, UserRole, UserRoleId)
├── exception/        # Exception classes and GlobalExceptionHandler
├── mapper/           # MapStruct mappers for entity-DTO conversion
├── repository/       # Spring Data JPA repositories
├── security/         # JWT filter and entry point
├── service/          # Business logic (AuthService, UserService, JwtService, EmailService, UserUpdateService)
└── utils/            # Utility classes
```

## Key Implementation Details

### User Roles System
- Roles stored in `user_roles` table with composite key (user_id, role)
- Users can have multiple roles (e.g., USER, ADMIN)
- Default role "USER" assigned during registration
- Roles loaded dynamically from database in `UserService.loadUserByUsername()`

### Email Verification Flow
1. User registers → `emailVerified=false`, `isActive=false`
2. System generates UUID token, stores in `emailVerificationToken`
3. Token expires in 24 hours (`emailVerificationExpiresAt`)
4. User clicks link → `POST /api/v1/auth/user-active` with token
5. System activates account: `emailVerified=true`, `isActive=true`

### Account Security Features
- Failed login attempts tracked in `failedLoginAttempts` field
- Account locked after 5 failed attempts (configurable)
- Lock duration: 30 minutes (configurable)
- Lock stored in `accountLockedUntil` timestamp
- Successful login resets failed attempts counter

### JWT Token Types
- **Access Token**: Short-lived (1 hour), used for API authentication
- **Refresh Token**: Long-lived (30 days), used to obtain new access tokens
- Tokens distinguished by `type` claim in payload
- Refresh endpoint validates token type before issuing new tokens

## Database Schema Notes

- Schema must be created manually or via migration tools (Flyway/Liquibase)
- Application uses `validate` mode - will not create/update schema
- Tables required: `users`, `user_roles` (at minimum for auth to work)
- For tests: Consider using H2 in-memory database or testcontainers

## Logging Configuration

- Application logs: `ai.content.auto` at DEBUG level
- Security logs: `org.springframework.security` at DEBUG
- SQL logs: `org.hibernate.SQL` at DEBUG with parameter binding
- HikariCP logs: Connection pool monitoring at DEBUG
- Log file: `logs/boss-ai-backend.log` with 10MB max size, 30 days retention

## Monitoring & Actuator

Exposed endpoints:
- `/actuator/health` - Health check (public access)
- `/actuator/info` - Application info (public access)
- `/actuator/metrics` - Metrics (requires ADMIN role)
- `/actuator/prometheus` - Prometheus metrics (requires ADMIN role)

## API Endpoint Patterns

- Public: `/api/v1/auth/**` (login, register, activate, refresh)
- User: `/api/v1/users/**` (requires USER or ADMIN role)
- Admin: `/api/v1/admin/**` (requires ADMIN role)
- Actuator: `/actuator/**` (health/info public, others require ADMIN)

## Development Notes

- **Lombok**: Used extensively - ensure Lombok plugin is installed in IDE
- **MapStruct**: Mapper interfaces for entity-DTO conversion
- **@Loggable**: Custom annotation for automatic method logging via AOP
- **Global Exception Handler**: All exceptions caught and formatted consistently
- **CORS**: Configured for local development (localhost:3000, localhost:5173)
- **Spring AI**: Currently disabled via autoconfigure exclude in application.yml

## Testing Considerations

- Tests require database schema or H2 configuration
- Current test configuration may need adjustment for in-memory database
- Security tests should use Spring Security Test framework
- Use `@SpringBootTest` for integration tests
- Consider `@DataJpaTest` for repository tests with H2
