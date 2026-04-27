package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.config.SecurityConfig;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import bg.nbu.cscb532.shipment.dto.ShipmentStatusUpdateDto;
import bg.nbu.cscb532.shipment.dto.ShipmentViewDto;
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

    private ShipmentViewDto createValidViewDto(UUID id) {
        return ShipmentViewDto.builder()
                .id(id)
                .trackingNumber("TRK-TEST")
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
            ShipmentViewDto responseDto = createValidViewDto(newId);

            given(shipmentService.registerShipment(any(ShipmentCreationDto.class), eq(authUser.getId())))
                    .willReturn(responseDto);

            // Act & Assert
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

            // Act & Assert
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
            ShipmentViewDto responseDto = createValidViewDto(shipmentId);

            given(shipmentService.getShipmentById(shipmentId, authUser.getId(), authUser.getApplicationRole()))
                    .willReturn(responseDto);

            // Act & Assert
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
            ShipmentViewDto dto = createValidViewDto(UUID.randomUUID());
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
            ShipmentViewDto dto = createValidViewDto(UUID.randomUUID());
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
            ShipmentViewDto dto = createValidViewDto(UUID.randomUUID());
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
            ShipmentViewDto dto = createValidViewDto(UUID.randomUUID());
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
            ShipmentViewDto dto = createValidViewDto(UUID.randomUUID());
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
            ShipmentViewDto response = createValidViewDto(id);

            given(shipmentService.updateShipmentStatus(eq(id), any(), any())).willReturn(response);

            // Act & Assert
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

            // Act & Assert
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
}