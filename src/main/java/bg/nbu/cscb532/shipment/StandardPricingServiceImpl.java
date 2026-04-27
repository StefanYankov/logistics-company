package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Implementation of the Pricing Strategy using dynamic database configuration.
 * Fulfills the requirement: "Deliveries to an office are cheaper than deliveries to an address."
 */
@Service
@RequiredArgsConstructor
public class StandardPricingServiceImpl implements PricingService {

    private final PricingConfigRepository pricingConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculatePrice(ShipmentCreationDto request) {
        if (request == null || request.weight() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        // Protect against negative math bugs
        if (request.weight().compareTo(BigDecimal.ZERO) < 0) {
             throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.length() != null && request.length().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.width() != null && request.width().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.height() != null && request.height().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        // Fetch the active pricing configuration from the database dynamically
        PricingConfig activeConfig = pricingConfigRepository.findByActiveToIsNull()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));

        BigDecimal finalPrice = activeConfig.getBasePrice();

        BigDecimal weightComponent = request.weight().multiply(activeConfig.getPricePerKg());
        finalPrice = finalPrice.add(weightComponent);

        if (request.deliveryOfficeId() != null && request.deliveryAddress() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        if (request.deliveryAddress() != null) {
            finalPrice = finalPrice.add(activeConfig.getAddressSurcharge());
        } else if (request.deliveryOfficeId() == null) {
             throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        return finalPrice;
    }
}
