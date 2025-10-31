package ai.content.auto.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found
 */
@Getter
public class NotFoundException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public NotFoundException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;
        this.error = "Resource Not Found";
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.NOT_FOUND;
        this.error = "Resource Not Found";
    }
}