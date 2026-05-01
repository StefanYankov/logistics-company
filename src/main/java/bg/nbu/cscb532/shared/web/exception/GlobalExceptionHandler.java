package bg.nbu.cscb532.shared.web.exception;

import bg.nbu.cscb532.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Intercepts all exceptions thrown by REST Controllers and normalizes them into RFC 9457 ProblemDetail JSON.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles our custom domain business rules (e.g., Duplicates, Not Found).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getErrorCode().getHttpStatus(), ex.getMessage());
        problem.setTitle("Business Rule Violation");
        problem.setType(URI.create("urn:logistics:business-error"));
        problem.setProperty("errorCode", ex.getErrorCode().getCode());

        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(problem);
    }

    /**
     * Handles Spring Security Bad Credentials during login.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Unauthorized");
        problem.setProperty("errorCode", "E3005");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Handles Spring Security authorization failures (@PreAuthorize).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access Denied: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "You do not have the required permissions to perform this action.");
        problem.setTitle("Forbidden");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * Catch-all for unhandled server crashes (Developer Bugs).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Unexpected internal server error: ", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("urn:logistics:internal-error"));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    /**
     * Override to inject our custom Angular-friendly field errors map.
     */
    @Override
    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        log.warn("DTO Validation failed: {}", ex.getMessage());

        ProblemDetail problem = ex.getBody();
        problem.setDetail("Input validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("urn:logistics:validation-error"));

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, _) -> existing
                ));

        problem.setProperty("errors", fieldErrors);

        return ResponseEntity.status(status).body(problem);
    }

    /**
     * Override to customizes the title and detail string
     */
    @Override
    @Nullable
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            @NonNull MissingServletRequestParameterException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        log.warn("Missing required parameter: {}", ex.getParameterName());

        ProblemDetail problem = ex.getBody();
        problem.setDetail("Missing required request parameter: " + ex.getParameterName());
        problem.setTitle("Bad Request");

        return ResponseEntity.status(status).body(problem);
    }
}