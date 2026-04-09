package bg.nbu.cscb532.office.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OperatingHourViewDto(
        DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean isClosed
) {}