package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shipment.dto.PricingConfigViewDto;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the Pricing Strategy using dynamic database configuration.
 * Fulfills the requirement: "Deliveries to an office are cheaper than deliveries to an address."
 */
@Service
@RequiredArgsConstructor
public class StandardPricingServiceImpl implements PricingService {

    private final PricingConfigRepository pricingConfigRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculatePrice(ShipmentCreationDto request) {
        if (request == null || request.weight() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
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

        if (request.selectedServiceIds() != null && !request.selectedServiceIds().isEmpty()) {
            Set<ServiceCatalog> selectedServices = new HashSet<>(serviceCatalogRepository.findAllById(request.selectedServiceIds()));

            if (selectedServices.size() != request.selectedServiceIds().size()) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }

            for (ServiceCatalog service : selectedServices) {
                if (service.getPricingType() == PricingType.FIXED_AMOUNT) {
                    finalPrice = finalPrice.add(service.getPricingValue());
                } else if (service.getPricingType() == PricingType.PERCENTAGE_OF_BASE) {
                    BigDecimal surcharge = finalPrice.multiply(service.getPricingValue());
                    finalPrice = finalPrice.add(surcharge);
                }
            }
        }

        return finalPrice;
    }

    @Override
    @Transactional(readOnly = true)
    public PricingConfigViewDto getActiveConfig() {
        PricingConfig activeConfig = pricingConfigRepository.findByActiveToIsNull()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));

        return PricingConfigViewDto.builder()
                .basePrice(activeConfig.getBasePrice())
                .pricePerKg(activeConfig.getPricePerKg())
                .addressSurcharge(activeConfig.getAddressSurcharge())
                .build();
    }
}
