package bg.nbu.cscb532.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for completing a password reset flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDto {

    @NotBlank(message = "{validation.token.notblank}")
    private String token;

    @NotBlank(message = "{validation.user.password.notblank}")
    private String newPassword;
}
