package bg.nbu.cscb532.company.dto;

import lombok.Builder;

/**
 * Data Transfer Object representing the public view of a Company.
 */
@Builder
public record CompanyViewDto(
        Long id,
        String name,
        String registrationNumber
) {
}
