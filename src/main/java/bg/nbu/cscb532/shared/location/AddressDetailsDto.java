package bg.nbu.cscb532.shared.location;

import bg.nbu.cscb532.shared.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddressDetailsDto(
        @NotNull(message = "{validation.address.city.notnull}")
        Long cityId,

        @NotBlank(message = "{validation.address.street.notblank}")
        @Size(max = Constants.Validation.MAX_STREET_LENGTH, message = "{validation.address.street.maxlength}")
        String street,

        @Size(max = Constants.Validation.MAX_DISTRICT_LENGTH, message = "{validation.address.district.maxlength}")
        String district,

        @Size(max = Constants.Validation.MAX_BUILDING_INFO_LENGTH, message = "{validation.address.building.maxlength}")
        String building,

        @Size(max = Constants.Validation.MAX_BUILDING_INFO_LENGTH, message = "{validation.address.entrance.maxlength}")
        String entrance,

        @Size(max = Constants.Validation.MAX_BUILDING_INFO_LENGTH, message = "{validation.address.floor.maxlength}")
        String floor,

        @Size(max = Constants.Validation.MAX_BUILDING_INFO_LENGTH, message = "{validation.address.apartment.maxlength}")
        String apartment,

        Double latitude,

        Double longitude
) {}