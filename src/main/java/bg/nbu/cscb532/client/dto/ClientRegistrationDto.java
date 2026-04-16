package bg.nbu.cscb532.client.dto;

import bg.nbu.cscb532.shared.Constants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * DTO for the public client registration endpoint.
 */
@Builder
public record ClientRegistrationDto(
        @NotBlank(message = "{validation.user.username.notblank}")
        @Size(max = 255, message = "{validation.user.username.toolong}")
        String username,

        @NotBlank(message = "{validation.user.email.notblank}")
        @Email(message = "{validation.user.email.invalid}")
        @Size(max = 255, message = "{validation.user.email.toolong}")
        String email,

        @NotBlank(message = "{validation.user.password.notblank}")
        @Size(min = 8, max = 255, message = "{validation.user.password.size}")
        String password,

        @NotBlank(message = "{validation.user.firstname.notblank}")
        @Size(max = 255, message = "{validation.user.firstname.toolong}")
        String firstName,

        @NotBlank(message = "{validation.user.lastname.notblank}")
        @Size(max = 255, message = "{validation.user.lastname.toolong}")
        String lastName,

        @NotBlank(message = "{validation.client.phone.notblank}")
        @Size(max = Constants.Validation.MAX_PHONE_NUMBER_LENGTH, message = "{validation.client.phone.toolong}")
        @Pattern(regexp = "^\\+?[0-9\\-\\s]+$", message = "{validation.client.phone.invalid}")
        String phoneNumber
) {
}
