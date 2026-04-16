package bg.nbu.cscb532.user.dto;

import lombok.Builder;

/**
 * Data Transfer Object representing a successful login response.
 */
@Builder
public record LoginResponseDto(
        String token
) {
}
