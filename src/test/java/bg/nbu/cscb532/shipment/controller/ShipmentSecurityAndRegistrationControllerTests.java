package bg.nbu.cscb532.shipment.controller;

import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shipment.PaidBy;
import bg.nbu.cscb532.shipment.ShipmentType;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import bg.nbu.cscb532.shipment.dto.StaffShipmentViewDto;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Controller Framework: Security Constraints & Registration Processing")
public class ShipmentSecurityAndRegistrationControllerTests extends AbstractShipmentControllerTestBase {

    @Test
    @DisplayName("Security: Should allow Client to POST (Register) if senderId matches their own ID")
    void shouldAllowClientToRegisterOwnShipment() throws Exception {
        UUID clientId = UUID.randomUUID();
        ShipmentCreationDto dto = createValidCreationDto(clientId);
        CustomUserDetails authUser = createMockAuthUser(clientId, ApplicationRole.CLIENT);
        StaffShipmentViewDto responseDto = createValidStaffShipmentViewDto(UUID.randomUUID(), "TRK-TEST");

        given(shipmentService.registerShipment(any(ShipmentCreationDto.class), eq(authUser.getId())))
                .willReturn(responseDto);

        mockMvc.perform(post(BASE_URL)
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(shipmentService).registerShipment(any(ShipmentCreationDto.class), eq(authUser.getId()));
    }

    @Test
    @DisplayName("Security: Should return 403 Forbidden when Client attempts to POST (Register) for a different senderId")
    void shouldReturn403WhenClientAttemptsToRegisterForOther() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID otherSenderId = UUID.randomUUID();
        ShipmentCreationDto dto = createValidCreationDto(otherSenderId);
        CustomUserDetails authUser = createMockAuthUser(clientId, ApplicationRole.CLIENT);

        mockMvc.perform(post(BASE_URL)
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    @DisplayName("Happy Path: Should successfully create and return 201 Created for Staff")
    void shouldRegisterSuccessfullyForStaff() throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
        ShipmentCreationDto requestDto = createValidCreationDto(UUID.randomUUID());
        UUID newId = UUID.randomUUID();
        StaffShipmentViewDto responseDto = createValidStaffShipmentViewDto(newId, "TRK-TEST");

        given(shipmentService.registerShipment(any(ShipmentCreationDto.class), eq(authUser.getId())))
                .willReturn(responseDto);

        mockMvc.perform(post(BASE_URL)
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location", "http://localhost/api/shipments/" + newId))
                .andExpect(jsonPath("$.id").value(newId.toString()))
                .andExpect(jsonPath("$.trackingNumber").value("TRK-TEST"));

        verify(shipmentService).registerShipment(any(ShipmentCreationDto.class), eq(authUser.getId()));
    }

    @Test
    @DisplayName("Validation Error: Should return 400 Bad Request when weight is negative")
    void shouldReturn400WhenWeightNegative() throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
        ShipmentCreationDto invalidDto = ShipmentCreationDto.builder()
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .type(ShipmentType.PARCEL)
                .weight(BigDecimal.valueOf(-5.0))
                .deliveryOfficeId(10L)
                .paidBy(PaidBy.SENDER)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.weight").exists());

        verifyNoInteractions(shipmentService);
    }

    @ParameterizedTest
    @DisplayName("Boundary Testing: Invalid weights should return 400")
    @CsvSource({"-0.01", "-100.0", "0.0"})
    void shouldReturn400ForInvalidWeights(BigDecimal invalidWeight) throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
        ShipmentCreationDto dto = createCreationDtoWithWeight(invalidWeight);

        mockMvc.perform(post(BASE_URL)
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(shipmentService);
    }

    @Test
    @DisplayName("Defense in Depth: Should return 400 when Request Body is missing")
    void shouldFailFastOnMissingBody() throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

        mockMvc.perform(post(BASE_URL)
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(shipmentService);
    }

    @Test
    @DisplayName("Error Case: Should return 404 Not Found if BusinessException thrown for missing entities")
    void shouldReturn404WhenEntitiesMissing() throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
        ShipmentCreationDto requestDto = createValidCreationDto(UUID.randomUUID());

        given(shipmentService.registerShipment(any(), any()))
                .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(post(BASE_URL)
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }
}