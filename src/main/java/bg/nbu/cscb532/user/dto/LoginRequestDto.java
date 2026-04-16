package bg.nbu.cscb532.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Data Transfer Object representing a login request.
 */
@Builder
public record LoginRequestDto(
        @NotBlank(message = "{validation.user.username.notblank}")
        String username,

        @NotBlank(message = "{validation.user.password.notblank}")
        String password
) {
}
