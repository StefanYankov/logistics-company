package bg.nbu.cscb532.shipment.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class PricingConfigViewDto {
    BigDecimal basePrice;
    BigDecimal pricePerKg;
    BigDecimal addressSurcharge;
}
