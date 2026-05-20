package bg.nbu.cscb532.shipment.service;

import bg.nbu.cscb532.employee.Courier;
import bg.nbu.cscb532.employee.OfficeClerk;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shipment.Shipment;
import bg.nbu.cscb532.shipment.ShipmentStatus;
import bg.nbu.cscb532.shipment.dto.ShipmentStatusUpdateDto;
import bg.nbu.cscb532.shipment.dto.StaffShipmentViewDto;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Shipment Service: State Machine Lifecycle Tests")
public class ShipmentLifecycleWorkflowTests extends AbstractShipmentUnitTestBase {

    @Test
    @DisplayName("Happy Path: Should transition from REGISTERED to IN_TRANSIT and log history")
    void shouldTransitionRegisteredToInTransit() {
        UUID shipmentId = UUID.randomUUID();
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

        ShipmentStatusUpdateDto dto = ShipmentStatusUpdateDto.builder()
                .newStatus(ShipmentStatus.IN_TRANSIT)
                .notes("Departed Sofia Hub")
                .build();

        Shipment existingShipment = createValidShipment();
        existingShipment.setId(shipmentId);

        given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(existingShipment));
        given(shipmentRepository.save(any(Shipment.class))).willReturn(existingShipment);

        StaffShipmentViewDto result = shipmentService.updateShipmentStatus(shipmentId, dto, authUser);

        assertThat(result.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }

    @Test
    @DisplayName("Happy Path: Should transition to AT_DELIVERY_OFFICE and record office location")
    void shouldTransitionToDeliveryOfficeAndRecordLocation() {
        UUID shipmentId = UUID.randomUUID();
        Long officeId = 99L;
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

        ShipmentStatusUpdateDto dto = ShipmentStatusUpdateDto.builder()
                .newStatus(ShipmentStatus.AT_DELIVERY_OFFICE)
                .locationOfficeId(officeId)
                .build();

        Shipment existingShipment = createValidShipment();
        existingShipment.setId(shipmentId);
        existingShipment.setStatus(ShipmentStatus.IN_TRANSIT);

        Office locationOffice = createMockOffice(officeId, createMockCity(1L, "Varna", "9000"));

        given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(existingShipment));
        given(officeRepository.findById(officeId)).willReturn(Optional.of(locationOffice));
        given(shipmentRepository.save(any(Shipment.class))).willReturn(existingShipment);

        shipmentService.updateShipmentStatus(shipmentId, dto, authUser);

        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getLocation().getId()).isEqualTo(officeId);
    }

    @ParameterizedTest(name = "Current Status: {0} -> Target Status: {1}")
    @CsvSource({
            "REGISTERED, AT_DELIVERY_OFFICE",
            "REGISTERED, DELIVERED",
            "IN_TRANSIT, DELIVERED",
            "DELIVERED, IN_TRANSIT"
    })
    @DisplayName("Business Rule: Should reject invalid state machine transitions")
    void shouldRejectInvalidTransitions(ShipmentStatus currentStatus, ShipmentStatus newStatus) {
        UUID shipmentId = UUID.randomUUID();
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);

        ShipmentStatusUpdateDto dto = ShipmentStatusUpdateDto.builder().newStatus(newStatus).build();

        Shipment existingShipment = createValidShipment();
        existingShipment.setId(shipmentId);
        existingShipment.setStatus(currentStatus);

        given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(existingShipment));

        assertThatThrownBy(() -> shipmentService.updateShipmentStatus(shipmentId, dto, authUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Nested
    @DisplayName("assignPickup(UUID, UUID, CustomUserDetails) Tests")
    class AssignPickupTests {

        @Test
        @DisplayName("Happy Path: Should successfully assign a courier to a REGISTERED address pickup shipment")
        void shouldAssignCourierToRegisteredAddressPickup() {
            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

            Shipment shipment = createValidShipment();
            shipment.setId(shipmentId);
            shipment.setStatus(ShipmentStatus.REGISTERED);
            shipment.setOriginOffice(null); // Ensure it's an address pickup
            shipment.setOriginAddressSnapshot(createMockAddressDetails()); // Set address details

            Courier courier = createMockCourier(courierId, "John", "Doe");

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));
            given(employeeRepository.findById(courierId)).willReturn(Optional.of(courier));
            given(shipmentRepository.save(any(Shipment.class))).willReturn(shipment); // Return the same shipment

            // Act
            StaffShipmentViewDto result = shipmentService.assignPickup(shipmentId, courierId, authUser);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.currentCourierId()).isEqualTo(courierId);
            assertThat(result.currentCourierName()).isEqualTo("John Doe");
            verify(shipmentRepository).save(shipmentCaptor.capture());
            assertThat(shipmentCaptor.getValue().getCurrentCourier().getId()).isEqualTo(courierId);
            verify(historyRepository).save(any()); // Verify history is logged
        }

        @Test
        @DisplayName("Error Case: Should throw RESOURCE_NOT_FOUND if shipment does not exist")
        void shouldThrowIfShipmentNotFound() {
            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.empty());

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.assignPickup(shipmentId, courierId, authUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("Error Case: Should throw RESOURCE_NOT_FOUND if courier does not exist or is not a Courier type")
        void shouldThrowIfCourierNotFoundOrNotCourier() {
            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

            Shipment shipment = createValidShipment();
            shipment.setId(shipmentId);
            shipment.setStatus(ShipmentStatus.REGISTERED);
            shipment.setOriginOffice(null);

            OfficeClerk nonCourier = createMockOfficeClerk(courierId, "Not", "Courier");

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));
            given(employeeRepository.findById(courierId)).willReturn(Optional.of(nonCourier)); // Return a non-Courier instance

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.assignPickup(shipmentId, courierId, authUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("Error Case: Should throw VALIDATION_FAILED if shipment is not in REGISTERED status")
        void shouldThrowIfShipmentNotRegistered() {
            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

            Shipment shipment = createValidShipment();
            shipment.setId(shipmentId);
            shipment.setStatus(ShipmentStatus.IN_TRANSIT); // Not REGISTERED
            shipment.setOriginOffice(null);

            Courier courier = createMockCourier(courierId, "John", "Doe");

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));
            given(employeeRepository.findById(courierId)).willReturn(Optional.of(courier));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.assignPickup(shipmentId, courierId, authUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Error Case: Should throw VALIDATION_FAILED if shipment originates from an office")
        void shouldThrowIfShipmentOriginatesFromOffice() {
            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

            Shipment shipment = createValidShipment();
            shipment.setId(shipmentId);
            shipment.setStatus(ShipmentStatus.REGISTERED);
            shipment.setOriginOffice(createMockOffice(1L, createMockCity(1L, "Sofia", "1000")));

            Courier courier = createMockCourier(courierId, "John", "Doe");

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));
            given(employeeRepository.findById(courierId)).willReturn(Optional.of(courier));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.assignPickup(shipmentId, courierId, authUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }
    }
}