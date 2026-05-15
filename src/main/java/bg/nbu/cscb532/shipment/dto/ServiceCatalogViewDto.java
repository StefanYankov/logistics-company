package bg.nbu.cscb532.shipment.dto;

import bg.nbu.cscb532.shipment.PricingType;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO for displaying a service catalog entry to the client.
 */
@Builder
public record ServiceCatalogViewDto(
        Long id,
        String name,
        PricingType pricingType,
        BigDecimal pricingValue
) {
}
