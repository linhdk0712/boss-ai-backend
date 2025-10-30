package ai.content.auto.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom business exception for application-specific errors
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.error = "Business Error";
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.error = "Business Error";
    }

    public BusinessException(String message, HttpStatus status, String error) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.BAD_REQUEST;
        this.error = "Business Error";
    }

    public BusinessException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
        this.error = "Business Error";
    }
}