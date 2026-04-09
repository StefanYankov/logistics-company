package bg.nbu.cscb532.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PostalCodeValidator implements ConstraintValidator<PostalCode, String> {

    private static final Pattern BULGARIAN_POSTAL_CODE_PATTERN = Pattern.compile("^[0-9]{4}$");
 //   private static final Pattern UK_POSTAL_CODE_PATTERN = Pattern.compile("^([0-9]{4}|[A-Z]{1,2}[0-9][A-Z0-9]? [0-9][ABD-HJLNP-UW-Z]{2})$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        return BULGARIAN_POSTAL_CODE_PATTERN.matcher(value).matches();
    }
}