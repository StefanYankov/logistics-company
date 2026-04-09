package bg.nbu.cscb532.office.dto;


import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record OfficeDto(
        @NotNull(message = "{validation.office.company.notnull}")
        Long companyId,
        @Valid
        @NotNull(message = "{validation.office.address.notnull}")
        AddressDetailsDto address,
        @NotEmpty(message = "{validation.office.hours.notempty}")
        Set<@Valid OperatingHourDto> operatingHours

) {}
