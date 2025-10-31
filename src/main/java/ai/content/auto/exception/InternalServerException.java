package ai.content.auto.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown for internal server errors
 */
@Getter
public class InternalServerException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public InternalServerException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.error = "Internal Server Error";
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.error = "Internal Server Error";
    }
}