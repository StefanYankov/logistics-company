package bg.nbu.cscb532.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for initiating a password reset flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequestDto {

    @NotBlank(message = "{validation.user.email.notblank}")
    @Email(message = "{validation.user.email.invalid}")
    private String email;
}
