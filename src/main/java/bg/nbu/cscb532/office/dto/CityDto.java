package bg.nbu.cscb532.office.dto;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.validation.PostalCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CityDto(
        @NotBlank(message = "{validation.city.name.notblank}")
        @Size(
                min = Constants.Validation.MIN_NAME_LENGTH,
                max = Constants.Validation.MAX_NAME_LENGTH,
                message = "{validation.city.name.length}"
        )
        String name,
        @NotBlank(message = "{validation.city.postcode.notblank}")
        @PostalCode
        String postcode
) {

}
