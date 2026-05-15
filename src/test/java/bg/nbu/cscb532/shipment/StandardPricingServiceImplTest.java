package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Standard Pricing Service Unit Tests")
class StandardPricingServiceImplTest {

    @Mock
    private PricingConfigRepository pricingConfigRepository;

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @InjectMocks
    private StandardPricingServiceImpl pricingService;

    // --- TEST DATA FACTORY ---
    private PricingConfig createActiveConfig() {
        PricingConfig config = new PricingConfig();
        config.setBasePrice(BigDecimal.valueOf(5.00));
        config.setPricePerKg(BigDecimal.valueOf(1.50));
        config.setAddressSurcharge(BigDecimal.valueOf(3.50));
        return config;
    }

    private ShipmentCreationDto.ShipmentCreationDtoBuilder baseCreationDtoBuilder() {
        return ShipmentCreationDto.builder()
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .type(ShipmentType.PARCEL)
                .weight(BigDecimal.valueOf(2.0))
                .originOfficeId(5L)
                .paidBy(PaidBy.SENDER);
    }

    @Nested
    @DisplayName("calculatePrice(ShipmentCreationDto) Core Math Tests")
    class CalculatePriceTests {

        @Test
        @DisplayName("Math: Base price + Weight calculation only (Office Delivery)")
        void shouldCalculateBaseAndWeightOnly() {
            // Arrange
            // Base: 5.00, Weight: 2.0 * 1.50 = 3.00, Total: 8.00
            ShipmentCreationDto request = baseCreationDtoBuilder()
                    .deliveryOfficeId(10L)
                    .build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(createActiveConfig()));

            // Act
            BigDecimal result = pricingService.calculatePrice(request);

            // Assert
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(8.00));
        }

        @Test
        @DisplayName("Math: Base price + Weight + Address Surcharge (Address Delivery)")
        void shouldCalculateBaseWeightAndAddressSurcharge() {
            // Arrange
            // Base: 5.00, Weight: 2.0 * 1.50 = 3.00, Address: 3.50, Total: 11.50
            AddressDetailsDto address = new AddressDetailsDto(1L, "Street", null, null, null, null, null, null, null);
            ShipmentCreationDto request = baseCreationDtoBuilder()
                    .deliveryAddress(address)
                    .build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(createActiveConfig()));

            // Act
            BigDecimal result = pricingService.calculatePrice(request);

            // Assert
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(11.50));
        }
        
        @Test
        @DisplayName("Math: Should accurately process ZERO weight (if business rules later allow documents as 0kg)")
        void shouldHandleZeroWeight() {
            // Arrange
            ShipmentCreationDto request = baseCreationDtoBuilder()
                    .weight(BigDecimal.ZERO)
                    .deliveryOfficeId(10L)
                    .build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(createActiveConfig()));

            // Act
            BigDecimal result = pricingService.calculatePrice(request);

            // Assert
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        }
    }
    
    @Nested
    @DisplayName("calculatePrice(ShipmentCreationDto) Addon Tests")
    class AddonPricingTests {
        
        @Test
        @DisplayName("Math: Base price + Weight + Fixed Addon")
        void shouldCalculateFixedAddon() {
            // Arrange
            // Base: 5.00, Weight: 2.0 * 1.50 = 3.00, Addon: 2.50, Total: 10.50
            ShipmentCreationDto request = baseCreationDtoBuilder()
                    .deliveryOfficeId(10L)
                    .selectedServiceIds(Set.of(1L))
                    .build();

            ServiceCatalog fragile = new ServiceCatalog();
            fragile.setId(1L);
            fragile.setPricingType(PricingType.FIXED_AMOUNT);
            fragile.setPricingValue(BigDecimal.valueOf(2.50));

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(createActiveConfig()));
            given(serviceCatalogRepository.findAllById(request.selectedServiceIds())).willReturn(List.of(fragile));

            // Act
            BigDecimal result = pricingService.calculatePrice(request);

            // Assert
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(10.50));
        }
        
        @Test
        @DisplayName("Math: Base price + Weight + Percentage Addon")
        void shouldCalculatePercentageAddon() {
            // Arrange
            // Subtotal: Base(5.00) + Weight(3.00) = 8.00
            // Addon: 8.00 * 0.25 (25%) = 2.00
            // Total: 10.00
            ShipmentCreationDto request = baseCreationDtoBuilder()
                    .deliveryOfficeId(10L)
                    .selectedServiceIds(Set.of(2L))
                    .build();

            ServiceCatalog heavy = new ServiceCatalog();
            heavy.setId(2L);
            heavy.setPricingType(PricingType.PERCENTAGE_OF_BASE);
            heavy.setPricingValue(BigDecimal.valueOf(0.25));

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(createActiveConfig()));
            given(serviceCatalogRepository.findAllById(request.selectedServiceIds())).willReturn(List.of(heavy));

            // Act
            BigDecimal result = pricingService.calculatePrice(request);

            // Assert
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        }
        
        @Test
        @DisplayName("Math: Complex order of operations (Base + Weight + Address + Multiple Addons)")
        void shouldCalculateComplexCombinations() {
            // Arrange
            // Subtotal: Base(5.00) + Weight(3.00) + Address(3.50) = 11.50
            // Addon 1 (Fixed): 11.50 + 2.50 = 14.00
            // Addon 2 (Percentage): 14.00 + (14.00 * 0.50) = 21.00
            // Total: 21.00
            AddressDetailsDto address = new AddressDetailsDto(1L, "Street", null, null, null, null, null, null, null);
            ShipmentCreationDto request = baseCreationDtoBuilder()
                    .deliveryAddress(address)
                    .selectedServiceIds(Set.of(1L, 2L))
                    .build();

            ServiceCatalog fixed = new ServiceCatalog();
            fixed.setId(1L);
            fixed.setPricingType(PricingType.FIXED_AMOUNT);
            fixed.setPricingValue(BigDecimal.valueOf(2.50));
            
            ServiceCatalog percentage = new ServiceCatalog();
            percentage.setId(2L);
            percentage.setPricingType(PricingType.PERCENTAGE_OF_BASE);
            percentage.setPricingValue(BigDecimal.valueOf(0.50));

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(createActiveConfig()));
            given(serviceCatalogRepository.findAllById(request.selectedServiceIds())).willReturn(List.of(fixed, percentage));

            // Act
            BigDecimal result = pricingService.calculatePrice(request);

            // Assert
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(21.00));
        }

        @Test
        @DisplayName("Error Case: Should throw RESOURCE_NOT_FOUND if an invalid service ID is provided")
        void shouldThrowIfInvalidServiceId() {
            // Arrange
            ShipmentCreationDto request = baseCreationDtoBuilder()
                    .deliveryOfficeId(10L)
                    .selectedServiceIds(Set.of(1L, 999L)) // 999 doesn't exist
                    .build();

            ServiceCatalog validService = new ServiceCatalog();
            validService.setId(1L);

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(createActiveConfig()));
            given(serviceCatalogRepository.findAllById(request.selectedServiceIds())).willReturn(List.of(validService));

            // Act & Assert
            assertThatThrownBy(() -> pricingService.calculatePrice(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("calculatePrice(ShipmentCreationDto) Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Validation Error: Null request")
        void shouldThrowOnNullRequest() {
            assertThatThrownBy(() -> pricingService.calculatePrice(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verifyNoInteractions(pricingConfigRepository);
        }

        @Test
        @DisplayName("Validation Error: Null weight")
        void shouldThrowOnNullWeight() {
            ShipmentCreationDto request = baseCreationDtoBuilder().weight(null).build();

            assertThatThrownBy(() -> pricingService.calculatePrice(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verifyNoInteractions(pricingConfigRepository);
        }

        @Test
        @DisplayName("Validation Error: Negative weight")
        void shouldThrowOnNegativeWeight() {
            ShipmentCreationDto request = baseCreationDtoBuilder().weight(BigDecimal.valueOf(-1.0)).build();

            assertThatThrownBy(() -> pricingService.calculatePrice(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verifyNoInteractions(pricingConfigRepository);
        }

        @Test
        @DisplayName("Validation Error: Both delivery options provided")
        void shouldThrowOnBothDeliveryOptions() {
            AddressDetailsDto address = new AddressDetailsDto(1L, "Street", null, null, null, null, null, null, null);
            ShipmentCreationDto request = baseCreationDtoBuilder()
                    .deliveryOfficeId(10L)
                    .deliveryAddress(address)
                    .build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(createActiveConfig()));

            assertThatThrownBy(() -> pricingService.calculatePrice(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Validation Error: Neither delivery option provided")
        void shouldThrowOnNeitherDeliveryOption() {
            ShipmentCreationDto request = baseCreationDtoBuilder()
                    .deliveryOfficeId(null)
                    .deliveryAddress(null)
                    .build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(createActiveConfig()));

            assertThatThrownBy(() -> pricingService.calculatePrice(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Error Case: Missing active pricing configuration")
        void shouldThrowWhenNoActiveConfig() {
            ShipmentCreationDto request = baseCreationDtoBuilder().deliveryOfficeId(10L).build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.empty());

            assertThatThrownBy(() -> pricingService.calculatePrice(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
