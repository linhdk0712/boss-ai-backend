# Boss AI Backend

Spring Boot REST API backend for the Boss AI content automation platform.

## Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Java Version**: 17
- **Build Tool**: Gradle
- **Database**: PostgreSQL with Spring Data JPA
- **Caching**: Redis with Spring Data Redis
- **Security**: Spring Security with JWT authentication
- **AI Integration**: Spring AI with OpenAI
- **Monitoring**: Micrometer with Prometheus
- **Email**: Spring Boot Mail Starter

## Project Structure

```
src/main/java/ai/content/auto/
├── annotation/      # Custom annotations
│   └── Loggable.java
├── config/          # Configuration classes
│   ├── DatabaseConfig.java
│   ├── RedisConfig.java
│   └── SecurityConfig.java
├── controller/      # REST API controllers
├── dtos/           # Data Transfer Objects
│   └── auth/       # Authentication DTOs
├── entity/         # JPA entities
│   ├── User.java
│   ├── UserRole.java
│   └── UserRoleId.java
├── exception/      # Custom exceptions
├── mapper/         # Entity-DTO mappers
├── repository/     # Data access layer
│   ├── UserRepository.java
│   └── UserRoleRepository.java
├── security/       # Security components
│   ├── JwtAuthenticationEntryPoint.java
│   └── JwtAuthenticationFilter.java
├── service/        # Business logic services
│   ├── AuthService.java
│   ├── EmailService.java
│   ├── JwtService.java
│   └── UserService.java
└── utils/          # Utility classes
```

## Configuration

### Security Configuration
The `SecurityConfig` class handles:
- JWT authentication setup with stateless sessions
- JWT token validation and processing
- API endpoint security rules with role-based access
- CORS configuration with environment-specific origins
- Security headers (HSTS, frame options, content type protection)

### Database Configuration
The `DatabaseConfig` class provides comprehensive database setup with:
- **HikariCP Connection Pool**: High-performance JDBC connection pooling
- **JPA/Hibernate Configuration**: Optimized for PostgreSQL with performance tuning
- **Connection Pool Settings**: Configurable pool size, timeouts, and leak detection
- **Performance Optimizations**: Prepared statement caching, batch processing, and connection reuse
- **Production-Ready**: Includes monitoring and connection lifecycle management

#### Database Features
- **Connection Pooling**: HikariCP with configurable pool sizes (default: 20 max, 5 min idle)
- **Performance Tuning**: Prepared statement caching, batch operations, and connection reuse
- **Monitoring**: Connection leak detection and pool statistics
- **JPA Auditing**: Automatic creation and modification timestamps
- **Transaction Management**: Optimized transaction handling with proper isolation
- **Hibernate Optimizations**: Batch processing, SQL formatting, and query optimization

### Redis Configuration
The `RedisConfig` class handles caching and session management:
- **Connection Pooling**: Lettuce connection factory with Apache Commons Pool2
- **Flexible Timeout Configuration**: Support for multiple timeout formats (ms, s, m)
- **Serialization**: Jackson2 JSON serialization for complex objects
- **Cache Manager**: Redis-based caching with configurable TTL
- **Performance**: Connection pooling and optimized serialization

### Redis Configuration
- Redis connection setup
- Caching strategies
- Session management

## Development

### Prerequisites
- Java 17+
- PostgreSQL 12+
- Redis 6+

### Running the Application

```bash
# Development mode
./gradlew bootRun

# Build application
./gradlew build

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

### Environment Variables

Configure the following environment variables for different environments:

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/boss-ai
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# HikariCP Connection Pool (Optional - defaults provided)
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5
SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=300000
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=20000
SPRING_DATASOURCE_HIKARI_LEAK_DETECTION_THRESHOLD=60000
SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=1800000

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_TIMEOUT=2000ms  # Supports formats: 2000ms, 2s, 2000 (default: ms)

# OpenAI
OPENAI_API_KEY=your_openai_api_key

# JWT Security
JWT_SECRET=your_jwt_secret_key_minimum_256_bits

# Email Service
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM=noreply@yourdomain.com

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://yourdomain.com,http://localhost:3000
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS,PATCH
CORS_ALLOW_CREDENTIALS=true

# Account Security
MAX_FAILED_ATTEMPTS=5
LOCK_DURATION_MINUTES=30

# Frontend Integration
FRONTEND_URL=https://yourdomain.com
```

## API Documentation

The application exposes RESTful APIs for:
- User authentication and authorization
- AI content generation
- Data management operations
- Administrative functions

API documentation is available via Spring Boot Actuator endpoints when running in development mode.

## Monitoring

The application includes:
- Prometheus metrics endpoint (`/actuator/prometheus`)
- Health check endpoint (`/actuator/health`)
- Application info endpoint (`/actuator/info`)

## Security

The application implements comprehensive security features:

### JWT Authentication
- **JWT Service**: Token generation and validation using JJWT library
- **JWT Authentication Filter**: Processes JWT tokens from Authorization header
- **JWT Authentication Entry Point**: Handles unauthorized access attempts
- **Stateless Authentication**: No server-side session storage

### User Management
- **UserDetailsService**: Custom implementation integrated with User entity
- **Password Encoding**: BCrypt with strength 12 for secure password hashing
- **Account Security**: Failed login attempt tracking and account locking
- **Email Verification**: Account activation via email verification tokens

### Security Configuration
- **Spring Security**: Modern configuration with method-level security
- **CORS**: Configurable cross-origin resource sharing
- **Security Headers**: HSTS, frame options, and content type protection
- **Role-based Access Control**: User roles and permissions system

### API Security
- **Protected Endpoints**: JWT authentication required for most endpoints
- **Public Endpoints**: Authentication, health checks, and API documentation
- **Admin Endpoints**: Restricted to users with ADMIN role

## Authentication API

### Login Response Format
```json
{
  "errorCode": "SUCCESS",
  "errorMessage": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshExpiresIn": 604800,
    "user": {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "USER",
      "emailVerified": true,
      "profilePictureUrl": "https://example.com/avatar.jpg"
    }
  }
}
```

### Authentication Endpoints
- `POST /api/v1/auth/login` - User login with credentials
- `POST /api/v1/auth/register` - User registration with email verification
- `POST /api/v1/auth/user-active` - Account activation via email token
- `POST /api/v1/auth/refresh` - Token refresh with validation

## Recent Updates

### Authentication Features
- ✅ **JWT Authentication**: Complete JWT service with access and refresh token generation
- ✅ **User Registration**: Email verification with activation tokens
- ✅ **Account Security**: Failed login tracking and account locking
- ✅ **Token Refresh**: Full refresh token implementation with validation
- ✅ **AuthResponse DTO**: Complete with refresh token fields (`refreshToken`, `refreshExpiresIn`)
- ✅ **Role-Based Authentication**: Dynamic role loading from database with multiple role support
- ✅ **Profile Picture Support**: Added `profilePictureUrl` field to AuthResponse.UserInfo for avatar functionality

### Security Implementation
- ✅ **Security Filter**: JWT authentication filter for request processing
- ✅ **User Management**: UserDetailsService integration with User entity and UserRole system
- ✅ **Security Configuration**: Modern Spring Security setup with CORS
- ✅ **Authentication Entry Point**: Proper error handling for unauthorized requests
- ✅ **Dynamic Role Loading**: UserService enhanced to load roles from database with fallback mechanism

### Configuration Updates
- ✅ **Redis Configuration**: Updated to use modern Jackson2JsonRedisSerializer constructor
- ✅ **Redis Timeout Configuration**: Added flexible timeout parsing supporting multiple formats (ms, s, m)
- ✅ **Database Configuration**: HikariCP connection pooling with performance optimizations
- ✅ **Security Headers**: Modern Spring Security header configuration
- ✅ **Dependencies**: Added Apache Commons Pool2 for Redis connection pooling

### Code Quality & Error Handling
- ✅ **Compilation**: All Java classes compile successfully
- ✅ **Dependencies**: All required dependencies properly configured
- ✅ **Annotations**: Custom @Loggable annotation for method logging
- ✅ **AuthController Refactoring**: Improved error handling and response consistency
  - Removed redundant try-catch blocks in login method (relies on global exception handling)
  - Fixed BaseResponse instantiation patterns across all endpoints
  - Enhanced code formatting and method consistency
  - Simplified response creation with proper BaseResponse usage
- ✅ **AuthService Code Quality**: Recent formatting improvements for better maintainability
  - Improved code readability with consistent line breaks and indentation
  - Enhanced method chaining format across repository calls
  - Follows Java best practices and service layer standards
- ✅ **Service Layer Standards**: Comprehensive implementation following best practices
  - Proper transaction management with separate concerns
  - Exception handling patterns with context logging
  - Configuration integration with @Value annotations
  - Security considerations and error recovery patterns
- ⚠️ **Tests**: Test configuration needs database schema setup

## Documentation

### Architecture & Implementation Guides
- **Service Layer Standards**: [Service Layer Implementation Guide](.kiro/docs/service-layer-implementation.md)
- **Authentication System**: [Authentication Implementation](.kiro/docs/authentication-implementation.md)
- **API Reference**: [API Endpoints Reference](.kiro/docs/api-endpoints-reference.md)
- **Configuration Guide**: [Application Configuration Reference](.kiro/docs/application-configuration-reference.md)

## Contributing

1. Follow Spring Boot best practices and service layer standards
2. Use Lombok annotations to reduce boilerplate
3. Write unit tests for new features
4. Follow the existing package structure and transaction management patterns
5. Update documentation for significant changes
6. Refer to `.kiro/steering/` for detailed coding standards and best practices

## Known Issues

- **Test Configuration**: Tests require proper database schema setup or migration to use H2 in-memory database
- **Schema Validation**: Current configuration uses `validate` mode which requires existing database schema