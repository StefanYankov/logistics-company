package bg.nbu.cscb532.company.dto;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CompanyUpdateDto(
        @NotBlank(message = "{validation.company.name.notblank}")
        @Size(max = 255, message = "{validation.company.name.toolong}")
        String name,

        @NotBlank(message = "{validation.company.registration.notblank}")
        @Size(max = Constants.Validation.MAX_REGISTRATION_NUMBER_LENGTH, message = "{validation.company.registration.toolong}")
        String registrationNumber,

        @NotNull(message = "{validation.office.address.notnull}")
        AddressDetailsDto addressDetails
) {
}
