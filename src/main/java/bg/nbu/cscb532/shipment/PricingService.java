package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;

import java.math.BigDecimal;

/**
 * Service interface defining the strategy for calculating shipment prices.
 */
public interface PricingService {

    /**
     * Calculates the total price of a shipment based on its weight, dimensions,
     * type, and destination (office vs. specific address).
     *
     * @param request The data transfer object containing the shipment details.
     * @return The final calculated price in BGN.
     */
    BigDecimal calculatePrice(ShipmentCreationDto request);
}
