package bg.nbu.cscb532.client.dto;

import bg.nbu.cscb532.shared.Constants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * DTO used by staff (clerks) to rapidly register walk-in customers.
 * It bypasses the strict password requirements of the public self-registration flow.
 */
@Builder
public record ClientQuickRegistrationDto(

        @NotBlank(message = "{validation.user.firstname.notblank}")
        @Size(max = Constants.Validation.MAX_NAME_LENGTH, message = "{validation.user.firstname.size}")
        String firstName,

        @NotBlank(message = "{validation.user.lastname.notblank}")
        @Size(max = Constants.Validation.MAX_NAME_LENGTH, message = "{validation.user.lastname.size}")
        String lastName,

        @NotBlank(message = "{validation.user.phone.notblank}")
        @Pattern(regexp = Constants.Validation.PHONE_REGEX, message = "{validation.user.phone.pattern}")
        String phoneNumber,

        // Optional: If provided, triggers an immediate password reset flow so the user can claim their account.
        @Email(message = "{validation.user.email.format}")
        @Size(max = Constants.Validation.MAX_EMAIL_LENGTH, message = "{validation.user.email.size}")
        String email
) {
}
