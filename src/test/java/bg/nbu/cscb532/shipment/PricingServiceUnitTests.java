package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dynamic Pricing Strategy Tests")
class PricingServiceUnitTests {

    @Mock
    private PricingConfigRepository pricingConfigRepository;

    @InjectMocks
    private StandardPricingServiceImpl pricingService;

    private PricingConfig mockConfig;

    @BeforeEach
    void setUp() {
        // Create a mock active configuration with standard prices
        mockConfig = PricingConfig.builder()
                .basePrice(BigDecimal.valueOf(5.00))
                .pricePerKg(BigDecimal.valueOf(1.50))
                .addressSurcharge(BigDecimal.valueOf(3.00))
                .activeFrom(LocalDateTime.now())
                .activeTo(null)
                .build();
    }

    private AddressDetailsDto createDummyAddress() {
        return new AddressDetailsDto(1L, "Street", "Dist", "1", "A", "1", "1", 0.0, 0.0);
    }

    private ShipmentCreationDto.ShipmentCreationDtoBuilder baseDto() {
        return ShipmentCreationDto.builder()
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .type(ShipmentType.PARCEL);
    }

    @Nested
    @DisplayName("calculatePrice(ShipmentCreationDto) Mathematical Tests")
    class CalculationTests {

        @ParameterizedTest(name = "Weight: {0} kg -> Expected Price: {1} BGN")
        @CsvSource({
                "1.0, 6.50",   // 5.00 + (1.0 * 1.50)
                "2.5, 8.75",   // 5.00 + (2.5 * 1.50)
                "10.0, 20.00"  // 5.00 + (10.0 * 1.50)
        })
        @DisplayName("Happy Path: Delivery to Office (Cheaper)")
        void shouldCalculatePriceForOfficeDelivery(BigDecimal weight, BigDecimal expectedPrice) {
            ShipmentCreationDto dto = baseDto()
                    .weight(weight)
                    .deliveryOfficeId(10L) // Valid office ID
                    .build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(mockConfig));

            BigDecimal actualPrice = pricingService.calculatePrice(dto);

            // Assert exact value without scale issues (e.g., 6.5 vs 6.50)
            assertThat(actualPrice).isEqualByComparingTo(expectedPrice);
        }

        @ParameterizedTest(name = "Weight: {0} kg -> Expected Price: {1} BGN (with 3.00 BGN Surcharge)")
        @CsvSource({
                "1.0, 9.50",   // 5.00 + (1.0 * 1.50) + 3.00 Surcharge
                "2.5, 11.75",  // 5.00 + (2.5 * 1.50) + 3.00 Surcharge
                "10.0, 23.00"  // 5.00 + (10.0 * 1.50) + 3.00 Surcharge
        })
        @DisplayName("Happy Path: Delivery to Address (More Expensive)")
        void shouldCalculatePriceForAddressDelivery(BigDecimal weight, BigDecimal expectedPrice) {
            ShipmentCreationDto dto = baseDto()
                    .weight(weight)
                    .deliveryAddress(createDummyAddress()) // Valid Address
                    .build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(mockConfig));

            BigDecimal actualPrice = pricingService.calculatePrice(dto);

            assertThat(actualPrice).isEqualByComparingTo(expectedPrice);
        }
    }

    @Nested
    @DisplayName("Validation and Constraint Tests")
    class ValidationTests {

        @Test
        @DisplayName("Error Case: Should throw Exception when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThatThrownBy(() -> pricingService.calculatePrice(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Error Case: Should throw Exception when weight is missing")
        void shouldThrowExceptionWhenWeightIsMissing() {
            ShipmentCreationDto dto = baseDto().deliveryOfficeId(1L).build();

            assertThatThrownBy(() -> pricingService.calculatePrice(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Error Case: Should throw Exception when weight is negative")
        void shouldThrowExceptionWhenWeightIsNegative() {
            ShipmentCreationDto dto = baseDto().weight(BigDecimal.valueOf(-1.0)).deliveryOfficeId(1L).build();

            assertThatThrownBy(() -> pricingService.calculatePrice(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }
        
        @Test
        @DisplayName("Error Case: Should throw Exception when length is negative")
        void shouldThrowExceptionWhenLengthIsNegative() {
            ShipmentCreationDto dto = baseDto().weight(BigDecimal.ONE).length(BigDecimal.valueOf(-1.0)).deliveryOfficeId(1L).build();

            assertThatThrownBy(() -> pricingService.calculatePrice(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }
        
        @Test
        @DisplayName("Error Case: Should throw Exception when width is negative")
        void shouldThrowExceptionWhenWidthIsNegative() {
            ShipmentCreationDto dto = baseDto().weight(BigDecimal.ONE).width(BigDecimal.valueOf(-1.0)).deliveryOfficeId(1L).build();

            assertThatThrownBy(() -> pricingService.calculatePrice(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }
        
        @Test
        @DisplayName("Error Case: Should throw Exception when height is negative")
        void shouldThrowExceptionWhenHeightIsNegative() {
            ShipmentCreationDto dto = baseDto().weight(BigDecimal.ONE).height(BigDecimal.valueOf(-1.0)).deliveryOfficeId(1L).build();

            assertThatThrownBy(() -> pricingService.calculatePrice(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Error Case: Should throw Exception when BOTH Office and Address are provided (Mutually Exclusive)")
        void shouldThrowExceptionWhenBothDestinationsProvided() {
            ShipmentCreationDto dto = baseDto()
                    .weight(BigDecimal.ONE)
                    .deliveryOfficeId(1L)
                    .deliveryAddress(createDummyAddress())
                    .build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(mockConfig));

            assertThatThrownBy(() -> pricingService.calculatePrice(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Error Case: Should throw Exception when NEITHER Office nor Address are provided")
        void shouldThrowExceptionWhenNoDestinationProvided() {
            ShipmentCreationDto dto = baseDto()
                    .weight(BigDecimal.ONE)
                    .build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.of(mockConfig));

            assertThatThrownBy(() -> pricingService.calculatePrice(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Error Case: Should throw Internal Server Error if DB is missing active pricing config")
        void shouldThrowExceptionWhenConfigMissing() {
            ShipmentCreationDto dto = baseDto().weight(BigDecimal.ONE).deliveryOfficeId(1L).build();

            given(pricingConfigRepository.findByActiveToIsNull()).willReturn(Optional.empty());

            assertThatThrownBy(() -> pricingService.calculatePrice(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}