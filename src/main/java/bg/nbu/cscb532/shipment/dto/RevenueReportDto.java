package bg.nbu.cscb532.shipment.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object representing a summarized revenue report for a specific period.
 */
@Builder
public record RevenueReportDto(
        BigDecimal totalRevenue,
        LocalDate startDate,
        LocalDate endDate
) {
}
