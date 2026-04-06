package bg.nbu.cscb532.shared.exception;

import lombok.Getter;

/**
 * Base unchecked exception for all intentional business logic violations.
 * Triggers a standardized JSON error response via @ControllerAdvice.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
}
