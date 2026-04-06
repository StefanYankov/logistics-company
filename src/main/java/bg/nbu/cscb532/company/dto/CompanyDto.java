package bg.nbu.cscb532.company.dto;

import bg.nbu.cscb532.shared.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CompanyDto(

        @NotBlank(message = "{validation.company.name.notblank}")
        @Size(max = Constants.Validation.MAX_NAME_LENGTH, message = "{validation.company.name.toolong}")
        String name,

        @NotBlank(message = "{validation.company.registration.notblank}")
        @Size(max = Constants.Validation.MAX_REGISTRATION_NUMBER_LENGTH, message = "{validation.company.registration.toolong}")
        String registrationNumber

) {}
