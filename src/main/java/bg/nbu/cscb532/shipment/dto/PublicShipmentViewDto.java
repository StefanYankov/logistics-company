package bg.nbu.cscb532.shipment.dto;

import bg.nbu.cscb532.shipment.ShipmentStatus;
import bg.nbu.cscb532.shipment.ShipmentType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A restricted, public-facing view of a shipment, containing no PII or financial data.
 */
@Builder
public record PublicShipmentViewDto(
        String trackingNumber,
        ShipmentType type,
        ShipmentStatus status,
        BigDecimal weight,
        BigDecimal length,
        BigDecimal width,
        BigDecimal height,
        Instant createdAt,
        Instant updatedAt,

        // Sanitized Location Data
        String originCityName,
        String destinationCityName,
        String currentOfficeName,

        // Sanitized Addons (if we want to show them publicly)
        List<String> appliedAddons
) {
}
