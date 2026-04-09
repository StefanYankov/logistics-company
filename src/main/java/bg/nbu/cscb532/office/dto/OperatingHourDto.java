package bg.nbu.cscb532.office.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

// TODO (Feature): Add Custom Class-Level Validator (e.g. @ValidOperatingHours)
// To enforce that if isClosed == false, openTime and closeTime must not be null 
// and closeTime must be strictly after openTime. If isClosed == true, both must be null.
public record OperatingHourDto(
        @NotNull(message = "{validation.operatinghour.dayofweek.notnull}")
        DayOfWeek dayOfWeek,

        LocalTime openTime,
        LocalTime closeTime,
        
        boolean isClosed
) {}