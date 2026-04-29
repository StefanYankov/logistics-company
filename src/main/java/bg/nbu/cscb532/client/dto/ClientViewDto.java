package bg.nbu.cscb532.client.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Data Transfer Object representing the public view of a Client entity.
 * Strips away sensitive information like passwords and flattens the user graph.
 */
@Builder
public record ClientViewDto(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        boolean isActive
) {
}
