package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.config.SecurityConfig;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import bg.nbu.cscb532.shipment.dto.ShipmentStatusUpdateDto;
import bg.nbu.cscb532.shipment.dto.ShipmentViewDto;
import bg.nbu.cscb532.shipment.dto.RevenueReportDto;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import bg.nbu.cscb532.user.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ShipmentController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class ShipmentControllerTest {

    private static final String BASE_URL = "/api/shipments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShipmentService shipmentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // --- TEST DATA FACTORY ---
    
    private CustomUserDetails createMockAuthUser(UUID id, ApplicationRole role) {
        return new CustomUserDetails(
                id,
                "testUser",
                "password",
                role,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    private ShipmentCreationDto createValidCreationDto() {
        return ShipmentCreationDto.builder()
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .type(ShipmentType.PARCEL)
                .weight(BigDecimal.valueOf(2.5))
                .deliveryOfficeId(10L)
                .build();
    }

    private ShipmentCreationDto createCreationDtoWithWeight(BigDecimal weight) {
        return ShipmentCreationDto.builder()
                .senderId(UUID.randomUUID())
                .receiverId(UUID.randomUUID())
                .type(ShipmentType.PARCEL)
                .weight(weight)
                .deliveryOfficeId(10L)
                .build();
    }

    private ShipmentViewDto createValidViewDto(UUID id, String trackingNumber) {
        return ShipmentViewDto.builder()
                .id(id)
                .trackingNumber(trackingNumber)
                .type(ShipmentType.PARCEL)
                .status(ShipmentStatus.REGISTERED)
                .weight(BigDecimal.valueOf(2.5))
                .totalPrice(BigDecimal.valueOf(15.00))
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
                .build();
    }

    private RevenueReportDto createValidRevenueReport(LocalDate start, LocalDate end, BigDecimal revenue) {
        return RevenueReportDto.builder()
                .startDate(start)
                .endDate(end)
                .totalRevenue(revenue)
                .build();
    }

    @Nested
    @DisplayName("Authorization Constraints")
    class AuthorizationTests {

        @Test
        @DisplayName("Security: Should return 403 Forbidden when Client attempts to POST (Register)")
        void shouldReturn403WhenClientAttemptsToRegister() throws Exception {
            ShipmentCreationDto dto = createValidCreationDto();
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(post(BASE_URL)
                            .with(user(authUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
                    
            verifyNoInteractions(shipmentService);
        }
        
        @Test
        @DisplayName("Security: Should return 403 Forbidden when Client attempts to GET pending shipments")
        void shouldReturn403WhenClientAttemptsToReadPending() throws Exception {
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(get(BASE_URL + "/pending")
                            .with(user(authUser)))
                    .andExpect(status().isForbidden());
                    
            verifyNoInteractions(shipmentService);
        }
        
        @Test
        @DisplayName("Security: Should return 403 Forbidden when Client attempts to GET all shipments")
        void shouldReturn403WhenClientAttemptsToReadAll() throws Exception {
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(get(BASE_URL)
                            .with(user(authUser)))
                    .andExpect(status().isForbidden());
                    
            verifyNoInteractions(shipmentService);
        }

        @Test
        @DisplayName("Security: Should return 403 Forbidden when Client attempts to GET shipments by sender")
        void shouldReturn403WhenClientAttemptsToReadBySender() throws Exception {
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(get(BASE_URL + "/sender/" + UUID.randomUUID())
                            .with(user(authUser)))
                    .andExpect(status().isForbidden());
                    
            verifyNoInteractions(shipmentService);
        }

        @Test
        @DisplayName("Security: Should return 403 Forbidden when Client attempts to GET shipments by receiver")
        void shouldReturn403WhenClientAttemptsToReadByReceiver() throws Exception {
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(get(BASE_URL + "/receiver/" + UUID.randomUUID())
                            .with(user(authUser)))
                    .andExpect(status().isForbidden());
                    
            verifyNoInteractions(shipmentService);
        }
        
        @Test
        @DisplayName("Security: Should return 403 Forbidden when Client attempts to GET shipments registered by employee")
        void shouldReturn403WhenClientAttemptsToReadByEmployee() throws Exception {
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(get(BASE_URL + "/registered-by/" + UUID.randomUUID())
                            .with(user(authUser)))
                    .andExpect(status().isForbidden());
                    
            verifyNoInteractions(shipmentService);
        }
    }

    @Nested
    @DisplayName("POST /api/shipments")
    class RegisterShipmentTests {

        @Test
        @DisplayName("Happy Path: Should successfully create and return 201 Created")
        void shouldRegisterSuccessfully() throws Exception {

            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            ShipmentCreationDto requestDto = createValidCreationDto();
            UUID newId = UUID.randomUUID();
            ShipmentViewDto responseDto = createValidViewDto(newId, "TRK-TEST");

            given(shipmentService.registerShipment(any(ShipmentCreationDto.class), eq(authUser.getId())))
                    .willReturn(responseDto);

            // Act and Assert
            mockMvc.perform(post(BASE_URL)
                            .with(user(authUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/shipments/" + newId))
                    .andExpect(jsonPath("$.id").value(newId.toString()))
                    .andExpect(jsonPath("$.trackingNumber").value("TRK-TEST"));

            verify(shipmentService).registerShipment(any(ShipmentCreationDto.class), eq(authUser.getId()));
        }

        @Test
        @DisplayName("Validation Error: Should return 400 Bad Request when weight is negative")
        void shouldReturn400WhenWeightNegative() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            ShipmentCreationDto invalidDto = ShipmentCreationDto.builder()
                    .senderId(UUID.randomUUID())
                    .receiverId(UUID.randomUUID())
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(-5.0))
                    .deliveryOfficeId(10L)
                    .build();

            // Act and Assert
            mockMvc.perform(post(BASE_URL)
                            .with(user(authUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.weight").exists());

            verifyNoInteractions(shipmentService);
        }
        
        @Test
        @DisplayName("Error Case: Should return 404 Not Found if BusinessException thrown for missing entities")
        void shouldReturn404WhenEntitiesMissing() throws Exception {
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            ShipmentCreationDto requestDto = createValidCreationDto();

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

    @Nested
    @DisplayName("GET /api/shipments/{id}")
    class GetShipmentByIdTests {

        @Test
        @DisplayName("Happy Path: Should retrieve single shipment and pass auth context to service")
        void shouldRetrieveShipmentById() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);
            UUID shipmentId = UUID.randomUUID();
            ShipmentViewDto responseDto = createValidViewDto(shipmentId, "TRK-TEST");

            given(shipmentService.getShipmentById(shipmentId, authUser.getId(), authUser.getApplicationRole()))
                    .willReturn(responseDto);

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/{id}", shipmentId)
                            .with(user(authUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(shipmentId.toString()));
        }
        
        @Test
        @DisplayName("Error Case: Should return 404 when service denies access (preventing enumeration)")
        void shouldReturn404WhenAccessDenied() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);
            UUID shipmentId = UUID.randomUUID();

            given(shipmentService.getShipmentById(shipmentId, authUser.getId(), authUser.getApplicationRole()))
                    .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/{id}", shipmentId)
                            .with(user(authUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("Reporting Queries (Staff Only)")
    class ReportingQueriesTests {

        @Test
        @DisplayName("Happy Path: Should retrieve paginated list of all shipments")
        void shouldGetAll() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            ShipmentViewDto dto = createValidViewDto(UUID.randomUUID(), "TRK-TEST");
            Page<ShipmentViewDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

            given(shipmentService.getAllShipments(any(Pageable.class))).willReturn(page);

            // Act and Assert
            mockMvc.perform(get(BASE_URL)
                            .with(user(authUser))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Happy Path: Should retrieve paginated list by sender ID")
        void shouldGetBySender() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            UUID senderId = UUID.randomUUID();
            ShipmentViewDto dto = createValidViewDto(UUID.randomUUID(), "TRK-TEST");
            Page<ShipmentViewDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

            given(shipmentService.getShipmentsBySender(eq(senderId), any(Pageable.class))).willReturn(page);

            // Act and assert
            mockMvc.perform(get(BASE_URL + "/sender/{id}", senderId)
                            .with(user(authUser))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Happy Path: Should retrieve paginated list by receiver ID")
        void shouldGetByReceiver() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.COURIER);
            UUID receiverId = UUID.randomUUID();
            ShipmentViewDto dto = createValidViewDto(UUID.randomUUID(), "TRK-TEST");
            Page<ShipmentViewDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

            given(shipmentService.getShipmentsByReceiver(eq(receiverId), any(Pageable.class))).willReturn(page);

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/receiver/{id}", receiverId)
                            .with(user(authUser))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Happy Path: Should retrieve paginated list by registered employee ID")
        void shouldGetByEmployee() throws Exception {
            //Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            UUID employeeId = UUID.randomUUID();
            ShipmentViewDto dto = createValidViewDto(UUID.randomUUID(), "TRK-TEST");
            Page<ShipmentViewDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

            given(shipmentService.getShipmentsRegisteredByEmployee(eq(employeeId), any(Pageable.class))).willReturn(page);

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/registered-by/{id}", employeeId)
                            .with(user(authUser))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Happy Path: Should retrieve paginated list of pending shipments")
        void shouldGetPending() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.COURIER);
            ShipmentViewDto dto = createValidViewDto(UUID.randomUUID(), "TRK-TEST");
            Page<ShipmentViewDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

            given(shipmentService.getPendingShipments(any(Pageable.class))).willReturn(page);

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/pending")
                            .with(user(authUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("PATCH /api/shipments/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("Happy Path: Should successfully update status and return 200 OK")
        void shouldUpdateStatusSuccessfully() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.COURIER);
            UUID id = UUID.randomUUID();
            ShipmentStatusUpdateDto request = new ShipmentStatusUpdateDto(ShipmentStatus.DELIVERED, 5L, "lorem ipsum");
            ShipmentViewDto response = createValidViewDto(id, "TRK-TEST");

            given(shipmentService.updateShipmentStatus(eq(id), any(), any())).willReturn(response);

            // Act and Assert
            mockMvc.perform(patch(BASE_URL + "/{id}/status", id)
                            .with(user(authUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()));

            verify(shipmentService).updateShipmentStatus(eq(id), any(), any());
        }

        @Test
        @DisplayName("Security: Should return 403 Forbidden when Client attempts to PATCH status")
        void shouldReturn403WhenClientAttemptsToPatch() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);
            UUID shipmentId = UUID.randomUUID();

            ShipmentStatusUpdateDto validDto = new ShipmentStatusUpdateDto(ShipmentStatus.DELIVERED, 10L, "Lorem ipsum");

            // Act and Assert
            mockMvc.perform(patch(BASE_URL + "/{id}/status", shipmentId)
                            .with(user(authUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validDto)))
                    .andExpect(status().isForbidden());

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

        @ParameterizedTest
        @DisplayName("Boundary Testing: Invalid weights should return 400")
        @CsvSource({
                "-0.01",
                "-100.0",
                "0.0" // Assuming weight must be > 0
        })
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
    }

    @Nested
    @DisplayName("GET /api/shipments/revenue")
    class RevenueReportTests {

        @Test
        @DisplayName("Happy Path: Should return 200 OK and revenue data when requested by ADMIN")
        void shouldReturnRevenueForAdmin() throws Exception {
            // Arrange
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 1, 31);
            RevenueReportDto expectedReport = createValidRevenueReport(start, end, BigDecimal.valueOf(5000.00));

            given(shipmentService.getCompanyRevenue(start, end)).willReturn(expectedReport);

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/revenue")
                            .with(user(adminUser))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRevenue").value(5000.00))
                    .andExpect(jsonPath("$.startDate").value("2026-01-01"))
                    .andExpect(jsonPath("$.endDate").value("2026-01-31"));

            verify(shipmentService).getCompanyRevenue(start, end);
        }

        @Test
        @DisplayName("Security: Should return 403 Forbidden when CLERK attempts to access revenue")
        void shouldReturn403WhenClerkAccessesRevenue() throws Exception {
            // Arrange
            CustomUserDetails clerkUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/revenue")
                            .with(user(clerkUser))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(shipmentService);
        }

        @Test
        @DisplayName("Defense in Depth: Should return 400 Bad Request when required date parameters are missing")
        void shouldReturn400WhenParametersMissing() throws Exception {
            // Arrange
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/revenue")
                            .with(user(adminUser)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(shipmentService);
        }

        @ParameterizedTest
        @DisplayName("Boundary Testing: Should return 400 for malformed date strings")
        @CsvSource({
                "2026-13-01, 2026-01-31", // Invalid month
                "2026-01-01, not-a-date",  // Malformed string
                "01-01-2026, 31-01-2026"  // Non-ISO format (DD-MM-YYYY)
        })
        void shouldReturn400ForInvalidDateFormats(String startStr, String endStr) throws Exception {
            // Arrange
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/revenue")
                            .with(user(adminUser))
                            .param("startDate", startStr)
                            .param("endDate", endStr))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(shipmentService);
        }

        @Test
        @DisplayName("Error Case: Should handle BusinessException when startDate is after endDate")
        void shouldHandleChronologicalValidationError() throws Exception {
            // Arrange
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            LocalDate start = LocalDate.of(2026, 12, 31);
            LocalDate end = LocalDate.of(2026, 1, 1);

            given(shipmentService.getCompanyRevenue(start, end))
                    .willThrow(new BusinessException(ErrorCode.VALIDATION_FAILED));

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/revenue")
                            .with(user(adminUser))
                            .param("startDate", "2026-12-31")
                            .param("endDate", "2026-01-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.getCode()));

            verify(shipmentService).getCompanyRevenue(start, end);
            verifyNoMoreInteractions(shipmentService);
        }
    }

    @Nested
    @DisplayName("GET /api/shipments/track/{trackingNumber}")
    class GetShipmentByTrackingNumberTests {

        @Test
        @DisplayName("Happy Path: Should retrieve shipment by tracking number and pass auth context to service")
        void shouldRetrieveShipmentByTrackingNumber() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);
            String trackingNumber = "TRK-12345";
            ShipmentViewDto responseDto = createValidViewDto(UUID.randomUUID(), trackingNumber);

            given(shipmentService.getShipmentByTrackingNumber(trackingNumber, authUser.getId(), authUser.getApplicationRole()))
                    .willReturn(responseDto);

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/track/{trackingNumber}", trackingNumber)
                            .with(user(authUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trackingNumber").value(trackingNumber));

            verify(shipmentService).getShipmentByTrackingNumber(trackingNumber, authUser.getId(), authUser.getApplicationRole());
        }

        @Test
        @DisplayName("Error Case: Should return 404 when service denies access (preventing enumeration)")
        void shouldReturn404WhenAccessDenied() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);
            String trackingNumber = "TRK-INVALID";

            given(shipmentService.getShipmentByTrackingNumber(trackingNumber, authUser.getId(), authUser.getApplicationRole()))
                    .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/track/{trackingNumber}", trackingNumber)
                            .with(user(authUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("Validation Error: Should return 400 Bad Request when tracking number is blank")
        void shouldReturn400WhenTrackingNumberIsBlank() throws Exception {
            // Arrange
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            String blankTrackingNumber = "   ";

            given(shipmentService.getShipmentByTrackingNumber(blankTrackingNumber, authUser.getId(), authUser.getApplicationRole()))
                    .willThrow(new BusinessException(ErrorCode.VALIDATION_FAILED));

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/track/{trackingNumber}", blankTrackingNumber)
                            .with(user(authUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.getCode()));
        }
    }

}