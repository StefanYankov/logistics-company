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
    OFFICE_NOT_FOUND("E2003", "Office not found.", HttpStatus.NOT_FOUND),

    // --- User / Client Domain (E3000 - E3999) ---
    USERNAME_DUPLICATE("E3001", "This username is already taken.", HttpStatus.CONFLICT),
    EMAIL_DUPLICATE("E3002", "This email is already registered.", HttpStatus.CONFLICT),
    INVALID_TOKEN("E3003", "The provided token is invalid or has already been used.", HttpStatus.BAD_REQUEST),
    EXPIRED_TOKEN("E3004", "The provided token has expired. Please request a new one.", HttpStatus.BAD_REQUEST),
    PHONE_DUPLICATE("E3005", "This phone number is already registered.", HttpStatus.CONFLICT),
    USER_NOT_FOUND("E3006", "User not found.", HttpStatus.NOT_FOUND), // Added USER_NOT_FOUND

    // --- Employee Domain (E4000 - E4999) ---
    EMPLOYEE_NUMBER_DUPLICATE("E4001", "This employee number is already assigned.", HttpStatus.CONFLICT),
    EMPLOYEE_NOT_FOUND("E4002", "Employee not found.", HttpStatus.NOT_FOUND),
    INVALID_EMPLOYEE_ROLE("E4003", "Invalid role specified for employee creation.", HttpStatus.BAD_REQUEST),

    // --- Shipment Domain (E5000 - E5999) ---
    SHIPMENT_DESTINATION_EXCLUSIVE("E5001", "Shipment must have either a delivery office OR a delivery address.", HttpStatus.BAD_REQUEST),
    SHIPMENT_RECEIVER_EXCLUSIVE("E5002", "Shipment must have either a registered receiver ID OR guest receiver details (name and phone).", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
