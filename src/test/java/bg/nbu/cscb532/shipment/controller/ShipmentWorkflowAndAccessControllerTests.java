package bg.nbu.cscb532.shipment.controller;

import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shipment.ShipmentStatus;
import bg.nbu.cscb532.shipment.dto.ShipmentStatusUpdateDto;
import bg.nbu.cscb532.shipment.dto.StaffShipmentViewDto;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Controller Framework: Operational Workflows & Access Control")
public class ShipmentWorkflowAndAccessControllerTests extends AbstractShipmentControllerTestBase {

    @Test
    @DisplayName("Happy Path: Should retrieve single shipment and pass auth context to service")
    void shouldRetrieveShipmentById() throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);
        UUID shipmentId = UUID.randomUUID();
        StaffShipmentViewDto responseDto = createValidStaffShipmentViewDto(shipmentId, "TRK-TEST");

        given(shipmentService.getShipmentById(shipmentId, authUser.getId(), authUser.getApplicationRole()))
                .willReturn(responseDto);

        mockMvc.perform(get(BASE_URL + "/{id}", shipmentId)
                        .with(user(authUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(shipmentId.toString()));
    }

    @Test
    @DisplayName("Error Case: Should return 404 when service denies access (preventing enumeration)")
    void shouldReturn404WhenAccessDenied() throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);
        UUID shipmentId = UUID.randomUUID();

        given(shipmentService.getShipmentById(shipmentId, authUser.getId(), authUser.getApplicationRole()))
                .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/{id}", shipmentId)
                        .with(user(authUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Happy Path: Staff should retrieve full shipment details by ID")
    void staffShouldRetrieveFullShipmentDetails() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
        StaffShipmentViewDto responseDto = createValidStaffShipmentViewDto(shipmentId, "TRK-FULL");

        given(shipmentService.getStaffShipmentDetails(shipmentId, authUser.getId(), authUser.getApplicationRole()))
                .willReturn(responseDto);

        mockMvc.perform(get(BASE_URL + "/details/{id}", shipmentId)
                        .with(user(authUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(shipmentId.toString()))
                .andExpect(jsonPath("$.senderName").value("Sender"))
                .andExpect(jsonPath("$.totalPrice").value(15.00));

        verify(shipmentService).getStaffShipmentDetails(shipmentId, authUser.getId(), authUser.getApplicationRole());
    }

    @Test
    @DisplayName("Security: Client should be forbidden from accessing staff details endpoint")
    void clientShouldBeForbiddenFromStaffDetails() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

        mockMvc.perform(get(BASE_URL + "/details/{id}", shipmentId)
                        .with(user(authUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    @DisplayName("Security: Anonymous should be forbidden from accessing staff details endpoint")
    void anonymousShouldBeForbiddenFromStaffDetails() throws Exception {
        UUID shipmentId = UUID.randomUUID();

        mockMvc.perform(get(BASE_URL + "/details/{id}", shipmentId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    @DisplayName("Happy Path: Should successfully update status and return 200 OK")
    void shouldUpdateStatusSuccessfully() throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.COURIER);
        UUID id = UUID.randomUUID();
        ShipmentStatusUpdateDto request = new ShipmentStatusUpdateDto(ShipmentStatus.DELIVERED, 5L, "lorem ipsum");
        StaffShipmentViewDto response = createValidStaffShipmentViewDto(id, "TRK-TEST");

        given(shipmentService.updateShipmentStatus(eq(id), any(), any())).willReturn(response);

        mockMvc.perform(patch(BASE_URL + "/{id}/status", id)
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()));

        verify(shipmentService).updateShipmentStatus(eq(id), any(), any());
    }

    @Test
    @DisplayName("Security: Should return 403 Forbidden when Client attempts to PATCH status")
    void shouldReturn403WhenClientAttemptsToPatch() throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);
        UUID shipmentId = UUID.randomUUID();
        ShipmentStatusUpdateDto validDto = new ShipmentStatusUpdateDto(ShipmentStatus.DELIVERED, 10L, "Lorem ipsum");

        mockMvc.perform(patch(BASE_URL + "/{id}/status", shipmentId)
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    @DisplayName("Verification: Should stop processing after Resource Not Found")
    void shouldStopProcessingOnException() throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
        UUID id = UUID.randomUUID();

        given(shipmentService.getShipmentById(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/{id}", id).with(user(authUser)))
                .andExpect(status().isNotFound());

        verify(shipmentService).getShipmentById(eq(id), any(), any());
        verifyNoMoreInteractions(shipmentService);
    }

    @Nested
    @DisplayName("PATCH /api/shipments/{id}/assign-pickup/{courierId}")
    class AssignPickupTests {

        @Test
        @DisplayName("Happy Path: Should successfully assign pickup and return 200 OK for CLERK")
        void shouldAssignPickupSuccessfullyForClerk() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();
            StaffShipmentViewDto response = createValidStaffShipmentViewDto(shipmentId, "TRK-TEST");

            given(shipmentService.assignPickup(eq(shipmentId), eq(courierId), any())).willReturn(response);

            // Act and Assert
            mockMvc.perform(patch(BASE_URL + "/{id}/assign-pickup/{courierId}", shipmentId, courierId)
                            .with(user(authUser)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(shipmentId.toString()));

            verify(shipmentService).assignPickup(eq(shipmentId), eq(courierId), any());
        }

        @Test
        @DisplayName("Happy Path: Should successfully assign pickup and return 200 OK for ADMIN")
        void shouldAssignPickupSuccessfullyForAdmin() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();
            StaffShipmentViewDto response = createValidStaffShipmentViewDto(shipmentId, "TRK-TEST");

            given(shipmentService.assignPickup(eq(shipmentId), eq(courierId), any())).willReturn(response);

            // Act and Assert
            mockMvc.perform(patch(BASE_URL + "/{id}/assign-pickup/{courierId}", shipmentId, courierId)
                            .with(user(authUser)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(shipmentId.toString()));

            verify(shipmentService).assignPickup(eq(shipmentId), eq(courierId), any());
        }

        @Test
        @DisplayName("Security: Should return 403 Forbidden when Courier attempts to assign pickup")
        void shouldReturn403WhenCourierAttemptsToAssignPickup() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.COURIER);
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();

            // Act and Assert
            mockMvc.perform(patch(BASE_URL + "/{id}/assign-pickup/{courierId}", shipmentId, courierId)
                            .with(user(authUser)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(shipmentService);
        }
        
        @Test
        @DisplayName("Security: Should return 403 Forbidden when Client attempts to assign pickup")
        void shouldReturn403WhenClientAttemptsToAssignPickup() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();

            // Act and Assert
            mockMvc.perform(patch(BASE_URL + "/{id}/assign-pickup/{courierId}", shipmentId, courierId)
                            .with(user(authUser)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(shipmentService);
        }

        @Test
        @DisplayName("Error Case: Should return 400 Bad Request if service throws VALIDATION_FAILED")
        void shouldReturn400WhenServiceThrowsValidationFailed() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();

            given(shipmentService.assignPickup(eq(shipmentId), eq(courierId), any()))
                    .willThrow(new BusinessException(ErrorCode.VALIDATION_FAILED));

            // Act and Assert
            mockMvc.perform(patch(BASE_URL + "/{id}/assign-pickup/{courierId}", shipmentId, courierId)
                            .with(user(authUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.getCode()));
        }

        @Test
        @DisplayName("Error Case: Should return 404 Not Found if service throws RESOURCE_NOT_FOUND")
        void shouldReturn404WhenServiceThrowsResourceNotFound() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();

            given(shipmentService.assignPickup(eq(shipmentId), eq(courierId), any()))
                    .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

            // Act and Assert
            mockMvc.perform(patch(BASE_URL + "/{id}/assign-pickup/{courierId}", shipmentId, courierId)
                            .with(user(authUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
        }
    }
}
