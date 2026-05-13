package bg.nbu.cscb532.client.dto;

import bg.nbu.cscb532.shared.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * DTO for a Client updating their own profile details.
 * Note: Username, email, and password changes are generally handled by dedicated security endpoints,
 * so this focuses on mutable profile data.
 */
@Builder
public record ClientUpdateDto(

        @NotBlank(message = "{validation.user.firstname.notblank}")
        @Size(max = Constants.Validation.MAX_NAME_LENGTH, message = "{validation.user.firstname.size}")
        String firstName,

        @NotBlank(message = "{validation.user.lastname.notblank}")
        @Size(max = Constants.Validation.MAX_NAME_LENGTH, message = "{validation.user.lastname.size}")
        String lastName,

        @NotBlank(message = "{validation.user.phone.notblank}")
        @Pattern(regexp = Constants.Validation.PHONE_REGEX, message = "{validation.user.phone.pattern}")
        String phoneNumber
) {
}
