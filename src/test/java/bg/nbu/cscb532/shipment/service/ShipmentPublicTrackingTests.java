package bg.nbu.cscb532.shipment.service;

import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shipment.Shipment;
import bg.nbu.cscb532.shipment.dto.PublicShipmentViewDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Shipment Service: Public Data Anonymization Tests")
public class  ShipmentPublicTrackingTests extends AbstractShipmentUnitTestBase {

    @Test
    @DisplayName("Happy Path: Should retrieve a public-restricted DTO by tracking number")
    void shouldRetrieveShipmentByTrackingNumber() {
        String tracking = "TRK-123";
        Shipment shipment = createValidShipment();
        shipment.setTrackingNumber(tracking);

        Office originOffice = createMockOffice(1L, createMockCity(10L, "Sofia", "1000"));
        Office deliveryOffice = createMockOffice(2L, createMockCity(20L, "Plovdiv", "4000"));
        shipment.setOriginOffice(originOffice);
        shipment.setDeliveryOffice(deliveryOffice);

        given(shipmentRepository.findByTrackingNumber(tracking)).willReturn(Optional.of(shipment));

        PublicShipmentViewDto result = shipmentService.getShipmentByTrackingNumber(tracking);

        assertThat(result).isNotNull();
        assertThat(result.trackingNumber()).isEqualTo(tracking);
        assertThat(result.originCityName()).isEqualTo("Sofia");
        assertThat(result.destinationCityName()).isEqualTo("Plovdiv");
    }

    @Test
    @DisplayName("Error Case: Should throw VALIDATION_FAILED when tracking token is blank")
    void shouldThrowIfTrackingNumberBlank() {
        assertThatThrownBy(() -> shipmentService.getShipmentByTrackingNumber("   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);

        verifyNoInteractions(shipmentRepository);
    }

    @Test
    @DisplayName("Error Case: Should throw 404 when tracking number is missing from index")
    void shouldThrowIfShipmentDoesNotExist() {
        String tracking = "INVALID";
        given(shipmentRepository.findByTrackingNumber(tracking)).willReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.getShipmentByTrackingNumber(tracking))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}