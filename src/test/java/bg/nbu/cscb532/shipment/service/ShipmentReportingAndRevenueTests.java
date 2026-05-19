package bg.nbu.cscb532.shipment.service;

import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shipment.Shipment;
import bg.nbu.cscb532.shipment.ShipmentStatus;
import bg.nbu.cscb532.shipment.dto.RevenueReportDto;
import bg.nbu.cscb532.shipment.dto.StaffShipmentViewDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Shipment Service: Financial Metrics & Operations Reports Tests")
public class  ShipmentReportingAndRevenueTests extends AbstractShipmentUnitTestBase {

    @Test
    @DisplayName("Happy Path: Should retrieve shipments by sender")
    void shouldGetBySender() {
        UUID senderId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shipment> page = new PageImpl<>(List.of(createValidShipment()));

        given(shipmentRepository.findBySender_Id(senderId, pageable)).willReturn(page);

        Page<StaffShipmentViewDto> result = shipmentService.getShipmentsBySender(senderId, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(shipmentRepository).findBySender_Id(senderId, pageable);
    }

    @Test
    @DisplayName("Happy Path: Should retrieve all shipments")
    void shouldGetAllShipments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shipment> page = new PageImpl<>(List.of(createValidShipment()));

        given(shipmentRepository.findAll(pageable)).willReturn(page);

        Page<StaffShipmentViewDto> result = shipmentService.getAllShipments(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Happy Path: Should return correctly formatted RevenueReportDto")
    void shouldReturnRevenueReport() {
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);
        BigDecimal mockRevenue = BigDecimal.valueOf(1250.50);

        given(shipmentRepository.calculateTotalRevenue(any(Instant.class), any(Instant.class))).willReturn(mockRevenue);

        RevenueReportDto result = shipmentService.getCompanyRevenue(startDate, endDate);

        assertThat(result).isNotNull();
        assertThat(result.totalRevenue()).isEqualByComparingTo(mockRevenue);
    }

    @Test
    @DisplayName("Error Case: Should throw VALIDATION_FAILED when start date is after end date")
    void shouldThrowWhenDatesInvalid() {
        LocalDate startDate = LocalDate.of(2026, 4, 30);
        LocalDate endDate = LocalDate.of(2026, 4, 1);

        assertThatThrownBy(() -> shipmentService.getCompanyRevenue(startDate, endDate))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("Happy Path: Should return paginated list of shipments for the specified courier")
    void shouldReturnDeliveriesForCourier() {
        UUID courierId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shipment> page = new PageImpl<>(List.of(createValidShipment()));

        given(shipmentRepository.findByCurrentCourier_IdAndStatus(courierId, ShipmentStatus.OUT_FOR_DELIVERY, pageable))
                .willReturn(page);

        Page<StaffShipmentViewDto> result = shipmentService.getMyDeliveries(courierId, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(shipmentRepository).findByCurrentCourier_IdAndStatus(courierId, ShipmentStatus.OUT_FOR_DELIVERY, pageable);
    }

    @Test
    @DisplayName("Happy Path: Should return paginated list of pickups for the specified courier")
    void shouldReturnPickupsForCourier() {
        UUID courierId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shipment> page = new PageImpl<>(List.of(createValidShipment()));

        given(shipmentRepository.findByCurrentCourier_IdAndStatusAndOriginOfficeIsNull(courierId, ShipmentStatus.REGISTERED, pageable))
                .willReturn(page);

        Page<StaffShipmentViewDto> result = shipmentService.getMyPickups(courierId, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(shipmentRepository).findByCurrentCourier_IdAndStatusAndOriginOfficeIsNull(courierId, ShipmentStatus.REGISTERED, pageable);
    }
}
