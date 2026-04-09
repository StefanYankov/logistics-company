package bg.nbu.cscb532.office.dto;

import java.util.Set;

public record OfficeViewDto(
        Long id,
        Long companyId,
        String cityName,
        String cityPostcode,
        String fullAddress,
        Set<OperatingHourViewDto> operatingHours
) {}