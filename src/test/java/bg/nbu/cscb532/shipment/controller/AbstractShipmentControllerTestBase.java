package bg.nbu.cscb532.shipment.controller;

import bg.nbu.cscb532.shared.config.SecurityConfig;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.shipment.*;
import bg.nbu.cscb532.shipment.dto.*;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import bg.nbu.cscb532.user.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@WebMvcTest(controllers = {ShipmentController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public abstract class AbstractShipmentControllerTestBase {

    protected static final String BASE_URL = "/api/shipments";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected ShipmentService shipmentService;

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected UserDetailsService userDetailsService;

    // --- TEST DATA FACTORIES ---

    protected CustomUserDetails createMockAuthUser(UUID id, ApplicationRole role) {
        return new CustomUserDetails(
                id,
                "testUser",
                "password",
                role,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    protected ShipmentCreationDto createValidCreationDto(UUID senderId) {
        return ShipmentCreationDto.builder()
                .senderId(senderId)
                .receiverId(UUID.randomUUID())
                .type(ShipmentType.PARCEL)
                .weight(BigDecimal.valueOf(2.5))
                .deliveryOfficeId(10L)
                .paidBy(PaidBy.SENDER)
                .build();
    }

    protected ShipmentCreationDto createCreationDtoWithWeight(BigDecimal weight) {
        return ShipmentCreationDto.builder()
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .type(ShipmentType.PARCEL)
                .weight(weight)
                .deliveryOfficeId(10L)
                .paidBy(PaidBy.SENDER)
                .build();
    }

    protected StaffShipmentViewDto createValidStaffShipmentViewDto(UUID id, String trackingNumber) {
        return StaffShipmentViewDto.builder()
                .id(id)
                .trackingNumber(trackingNumber)
                .type(ShipmentType.PARCEL)
                .status(ShipmentStatus.REGISTERED)
                .weight(BigDecimal.valueOf(2.5))
                .totalPrice(BigDecimal.valueOf(15.00))
                .paidBy(PaidBy.SENDER)
                .isPaid(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .senderId(UUID.randomUUID())
                .senderName("Sender")
                .receiverId(UUID.randomUUID())
                .receiverName("Receiver")
                .deliveryOfficeId(10L)
                .deliveryOfficeName("Office 1")
                .registeredById(UUID.randomUUID())
                .registeredByName("Employee 1")
                .appliedAddons(List.of("Fragile"))
                .build();
    }

    protected PublicShipmentViewDto createValidPublicShipmentViewDto(String trackingNumber) {
        return PublicShipmentViewDto.builder()
                .trackingNumber(trackingNumber)
                .type(ShipmentType.PARCEL)
                .status(ShipmentStatus.REGISTERED)
                .weight(BigDecimal.valueOf(2.5))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .originCityName("Sofia")
                .destinationCityName("Plovdiv")
                .appliedAddons(List.of("Fragile"))
                .build();
    }

    protected RevenueReportDto createValidRevenueReport(LocalDate start, LocalDate end, BigDecimal revenue) {
        return RevenueReportDto.builder()
                .startDate(start)
                .endDate(end)
                .totalRevenue(revenue)
                .build();
    }
}