package bg.nbu.cscb532.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Centralized registry of all business error codes across the application.
 * Ensures API responses are strictly typed, predictable, and fully translatable by the frontend.
 */
@Getter
public enum ErrorCode {

    // --- System / General ---
    INTERNAL_SERVER_ERROR("E0000", "An unexpected internal error occurred.", HttpStatus.INTERNAL_SERVER_ERROR),
    RESOURCE_NOT_FOUND("E0001", "The requested resource could not be found.", HttpStatus.NOT_FOUND),
    VALIDATION_FAILED("E0002", "Input validation failed.", HttpStatus.BAD_REQUEST),

    // --- Company Domain (E1000 - E1999) ---
    COMPANY_NOT_FOUND("E1001", "Company not found.", HttpStatus.NOT_FOUND),
    COMPANY_REGISTRATION_DUPLICATE("E1002", "A company with this registration number already exists.", HttpStatus.CONFLICT),
    COMPANY_NAME_DUPLICATE("E1003", "A company with this name already exists.", HttpStatus.CONFLICT),

    // --- Office/City Domain (E2000 - E2999) ---
    CITY_DUPLICATE("E2001", "A city with this postcode already exists.", HttpStatus.CONFLICT),
    CITY_NOT_FOUND("E2002", "City not found.", HttpStatus.NOT_FOUND),
    OFFICE_NOT_FOUND("E2003", "Office not found.", HttpStatus.NOT_FOUND);

    // --- User Domain (E3000 - E3999) ---
    // USER_NOT_FOUND("E3001", ...

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}