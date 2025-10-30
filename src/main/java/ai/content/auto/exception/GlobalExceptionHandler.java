package ai.content.auto.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Global Exception Handler using @ControllerAdvice
 * Handles all exceptions across the application
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Handle validation errors for request body
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationExceptions(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {

                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Validation Failed")
                                .message("Input validation failed")
                                .path(request.getRequestURI())
                                .validationErrors(errors)
                                .build();

                log.warn("Validation error: {} at {}", errors, request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle constraint violation exceptions
         */
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolationException(
                        ConstraintViolationException ex, HttpServletRequest request) {

                Map<String, String> errors = new HashMap<>();
                Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();

                for (ConstraintViolation<?> violation : violations) {
                        String fieldName = violation.getPropertyPath().toString();
                        String errorMessage = violation.getMessage();
                        errors.put(fieldName, errorMessage);
                }

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Constraint Violation")
                                .message("Constraint validation failed")
                                .path(request.getRequestURI())
                                .validationErrors(errors)
                                .build();

                log.warn("Constraint violation: {} at {}", errors, request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle authentication exceptions
         */
        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ai.content.auto.dtos.BaseResponse<Object>> handleAuthenticationException(
                        AuthenticationException ex, HttpServletRequest request) {

                String message = "Authentication failed";

                if (ex instanceof BadCredentialsException) {
                        message = "Invalid username or password";
                } else if (ex instanceof DisabledException) {
                        message = "Account is disabled";
                } else if (ex instanceof LockedException) {
                        message = "Account is locked";
                }

                ai.content.auto.dtos.BaseResponse<Object> response = new ai.content.auto.dtos.BaseResponse<>()
                                .setErrorCode("AUTHENTICATION_ERROR")
                                .setErrorMessage(message)
                                .setData(null);

                log.warn("Authentication error: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        /**
         * Handle access denied exceptions
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(
                        AccessDeniedException ex, HttpServletRequest request) {

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.FORBIDDEN.value())
                                .error("Access Denied")
                                .message("You don't have permission to access this resource")
                                .path(request.getRequestURI())
                                .build();

                log.warn("Access denied: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

        /**
         * Handle entity not found exceptions
         */
        @ExceptionHandler(EntityNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
                        EntityNotFoundException ex, HttpServletRequest request) {

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Resource Not Found")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();

                log.warn("Entity not found: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        /**
         * Handle data integrity violation exceptions
         */
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
                        DataIntegrityViolationException ex, HttpServletRequest request) {

                String message = "Data integrity violation";
                if (ex.getMessage().contains("duplicate key")) {
                        message = "Resource already exists";
                } else if (ex.getMessage().contains("foreign key")) {
                        message = "Cannot delete resource due to existing references";
                }

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error("Data Integrity Error")
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                log.error("Data integrity violation: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        /**
         * Handle method not supported exceptions
         */
        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
                        HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                                .error("Method Not Allowed")
                                .message(String.format("Method '%s' is not supported for this endpoint",
                                                ex.getMethod()))
                                .path(request.getRequestURI())
                                .build();

                log.warn("Method not supported: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
        }

        /**
         * Handle media type not supported exceptions
         */
        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleMediaTypeNotSupportedException(
                        HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                                .error("Unsupported Media Type")
                                .message("Content type not supported")
                                .path(request.getRequestURI())
                                .build();

                log.warn("Media type not supported: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        /**
         * Handle missing request parameter exceptions
         */
        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ErrorResponse> handleMissingParameterException(
                        MissingServletRequestParameterException ex, HttpServletRequest request) {

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Missing Parameter")
                                .message(String.format("Required parameter '%s' is missing", ex.getParameterName()))
                                .path(request.getRequestURI())
                                .build();

                log.warn("Missing parameter: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle method argument type mismatch exceptions
         */
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatchException(
                        MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Type Mismatch")
                                .message(String.format("Parameter '%s' should be of type %s",
                                                ex.getName(),
                                                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName()
                                                                : "unknown"))
                                .path(request.getRequestURI())
                                .build();

                log.warn("Type mismatch: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle message not readable exceptions
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleMessageNotReadableException(
                        HttpMessageNotReadableException ex, HttpServletRequest request) {

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Malformed JSON")
                                .message("Request body is not readable or malformed")
                                .path(request.getRequestURI())
                                .build();

                log.warn("Message not readable: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handle file upload size exceeded exceptions
         */
        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
                        MaxUploadSizeExceededException ex, HttpServletRequest request) {

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                                .error("File Too Large")
                                .message("File size exceeds maximum allowed limit")
                                .path(request.getRequestURI())
                                .build();

                log.warn("File size exceeded: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);
        }

        /**
         * Handle no handler found exceptions
         */
        @ExceptionHandler(NoHandlerFoundException.class)
        public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
                        NoHandlerFoundException ex, HttpServletRequest request) {

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Endpoint Not Found")
                                .message(String.format("No handler found for %s %s", ex.getHttpMethod(),
                                                ex.getRequestURL()))
                                .path(request.getRequestURI())
                                .build();

                log.warn("No handler found: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        /**
         * Handle custom business exceptions
         */
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ai.content.auto.dtos.BaseResponse<Object>> handleBusinessException(
                        BusinessException ex, HttpServletRequest request) {

                ai.content.auto.dtos.BaseResponse<Object> response = new ai.content.auto.dtos.BaseResponse<>()
                                .setErrorCode("BUSINESS_ERROR")
                                .setErrorMessage(ex.getMessage())
                                .setData(null);

                log.warn("Business exception: {} at {}", ex.getMessage(), request.getRequestURI());
                return new ResponseEntity<>(response, ex.getStatus());
        }

        /**
         * Handle all other exceptions
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex, HttpServletRequest request) {

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error("Internal Server Error")
                                .message("An unexpected error occurred")
                                .path(request.getRequestURI())
                                .build();

                log.error("Unexpected error: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
                return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}