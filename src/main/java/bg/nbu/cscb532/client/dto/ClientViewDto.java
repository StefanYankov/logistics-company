package bg.nbu.cscb532.client.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * A secure, public-facing representation of a Client.
 * Excludes sensitive information like the password hash.
 */
@Builder
public record ClientViewDto(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phoneNumber
) {
}
