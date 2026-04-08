package bg.nbu.cscb532.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a string conforms to the company's supported postal code formats.
 * Currently enforces Bulgarian format (exactly 4 digits).
 */
@Documented
@Constraint(validatedBy = PostalCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PostalCode {

    String message() default "{validation.postalcode.invalid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}