package bg.nbu.cscb532.shipment.controller;

import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shipment.dto.RevenueReportDto;
import bg.nbu.cscb532.shipment.dto.StaffShipmentViewDto;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Controller Framework: Queries, Lists & Revenue Reporting")
public class ShipmentReportingAndRevenueControllerTests extends AbstractShipmentControllerTestBase {

    @Test
    @DisplayName("Happy Path: Should retrieve paginated list of all shipments")
    void shouldGetAll() throws Exception {
        CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
        StaffShipmentViewDto dto = createValidStaffShipmentViewDto(UUID.randomUUID(), "TRK-TEST");
        Page<StaffShipmentViewDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        given(shipmentService.getAllShipments(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .with(user(authUser))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Security: Should allow Client to GET shipments by sender if senderId matches their own ID")
    void shouldAllowClientToReadOwnSentShipments() throws Exception {
        UUID clientId = UUID.randomUUID();
        CustomUserDetails authUser = createMockAuthUser(clientId, ApplicationRole.CLIENT);
        Page<StaffShipmentViewDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(shipmentService.getShipmentsBySender(eq(clientId), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/sender/" + clientId)
                        .with(user(authUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Security: Should return 403 Forbidden when Client attempts to GET shipments by sender for a different ID")
    void shouldReturn403WhenClientAttemptsToReadBySenderForOther() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID otherSenderId = UUID.randomUUID();
        CustomUserDetails authUser = createMockAuthUser(clientId, ApplicationRole.CLIENT);

        mockMvc.perform(get(BASE_URL + "/sender/" + otherSenderId)
                        .with(user(authUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    @DisplayName("Happy Path: Should retrieve courier deliveries list")
    void shouldGetCourierDeliveries() throws Exception {
        UUID courierId = UUID.randomUUID();
        CustomUserDetails authUser = createMockAuthUser(courierId, ApplicationRole.COURIER);
        Page<StaffShipmentViewDto> page = new PageImpl<>(List.of());

        given(shipmentService.getMyDeliveries(eq(courierId), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/my-deliveries").with(user(authUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Happy Path: Should retrieve courier pickups list")
    void shouldGetCourierPickups() throws Exception {
        UUID courierId = UUID.randomUUID();
        CustomUserDetails authUser = createMockAuthUser(courierId, ApplicationRole.COURIER);
        Page<StaffShipmentViewDto> page = new PageImpl<>(List.of());

        given(shipmentService.getMyPickups(eq(courierId), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/my-pickups").with(user(authUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Happy Path: Should return 200 OK and revenue data when requested by ADMIN")
    void shouldReturnRevenueForAdmin() throws Exception {
        CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        RevenueReportDto expectedReport = createValidRevenueReport(start, end, BigDecimal.valueOf(5000.00));

        given(shipmentService.getCompanyRevenue(start, end)).willReturn(expectedReport);

        mockMvc.perform(get(BASE_URL + "/revenue")
                        .with(user(adminUser))
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalRevenue").value(5000.00));
    }

    @Test
    @DisplayName("Security: Should return 403 Forbidden when CLERK attempts to access revenue")
    void shouldReturn403WhenClerkAccessesRevenue() throws Exception {
        CustomUserDetails clerkUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

        mockMvc.perform(get(BASE_URL + "/revenue")
                        .with(user(clerkUser))
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @ParameterizedTest
    @DisplayName("Boundary Testing: Should return 400 for malformed date strings")
    @CsvSource({
            "2026-13-01, 2026-01-31",
            "2026-01-01, not-a-date"
    })
    void shouldReturn400ForInvalidDateFormats(String startStr, String endStr) throws Exception {
        CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);

        mockMvc.perform(get(BASE_URL + "/revenue")
                        .with(user(adminUser))
                        .param("startDate", startStr)
                        .param("endDate", endStr))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Error Case: Should handle BusinessException when startDate is after endDate")
    void shouldHandleChronologicalValidationError() throws Exception {
        CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
        LocalDate start = LocalDate.of(2026, 12, 31);
        LocalDate end = LocalDate.of(2026, 1, 1);

        given(shipmentService.getCompanyRevenue(start, end))
                .willThrow(new BusinessException(ErrorCode.VALIDATION_FAILED));

        mockMvc.perform(get(BASE_URL + "/revenue")
                        .with(user(adminUser))
                        .param("startDate", "2026-12-31")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.getCode()));
    }
}