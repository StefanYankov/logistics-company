package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.client.ClientRepository;
import bg.nbu.cscb532.employee.Courier;
import bg.nbu.cscb532.employee.EmployeeRepository;
import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.office.CityRepository;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.office.OfficeRepository;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import bg.nbu.cscb532.shipment.dto.RevenueReportDto;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import bg.nbu.cscb532.shipment.dto.ShipmentStatusUpdateDto;
import bg.nbu.cscb532.shipment.dto.ShipmentViewDto;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Shipment Service Unit Tests")
class ShipmentServiceUnitTests {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentStatusHistoryRepository historyRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    @Captor
    private ArgumentCaptor<Shipment> shipmentCaptor;

    @Captor
    private ArgumentCaptor<ShipmentStatusHistory> historyCaptor;

    // --- TEST DATA FACTORY ---

    private Client createMockClient(UUID id, String firstName, String lastName) {
        Client client = new Client();
        client.setId(id);
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setPhoneNumber("0888123456");
        return client;
    }

    private Courier createMockEmployee(UUID id, String firstName, String lastName) {
        Courier courier = new Courier();
        courier.setId(id);
        courier.setFirstName(firstName);
        courier.setLastName(lastName);
        return courier;
    }

    private City createMockCity(Long id, String name, String postcode) {
        City city = new City();
        city.setId(id);
        city.setName(name);
        city.setPostcode(postcode);
        return city;
    }

    private Office createMockOffice(Long id, City city) {
        Office office = new Office();
        office.setId(id);
        AddressDetails address = new AddressDetails();
        address.setCity(city);
        address.setStreet("Office Street 1");
        office.setAddressDetails(address);
        return office;
    }

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

    private ShipmentCreationDto.ShipmentCreationDtoBuilder baseCreationDtoBuilder(UUID senderId, UUID receiverId) {
        return ShipmentCreationDto.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(ShipmentType.PARCEL)
                .weight(BigDecimal.valueOf(2.5))
                .originOfficeId(5L) 
                .paidBy(PaidBy.SENDER);
    }

    private Shipment createValidShipment() {
        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setTrackingNumber("TRK-TEST");
        shipment.setStatus(ShipmentStatus.REGISTERED);
        shipment.setSender(createMockClient(UUID.randomUUID(), "A", "A"));
        shipment.setReceiver(createMockClient(UUID.randomUUID(), "B", "B"));
        shipment.setRegisteredBy(createMockEmployee(UUID.randomUUID(), "C", "C"));

        PackageDetails details = PackageDetails.builder()
                .type(ShipmentType.PARCEL)
                .weight(BigDecimal.valueOf(2.5))
                .build();

        ShipmentFinancials financials = ShipmentFinancials.builder()
                .totalPrice(BigDecimal.valueOf(15.00))
                .paidBy(PaidBy.SENDER)
                .isPaid(false)
                .build();

        shipment.setPackageDetails(details);
        shipment.setFinancials(financials);

        return shipment;
    }

    @Nested
    @DisplayName("updateShipmentStatus(UUID, ShipmentStatusUpdateDto, CustomUserDetails) Tests")
    class UpdateShipmentStatusTests {

        @Test
        @DisplayName("Happy Path: Should transition from REGISTERED to IN_TRANSIT and log history")
        void shouldTransitionRegisteredToInTransit() {
            // Arrange
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

            // Act
            ShipmentViewDto result = shipmentService.updateShipmentStatus(shipmentId, dto, authUser);

            // Assert
            assertThat(result.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);

            verify(shipmentRepository).save(shipmentCaptor.capture());
            assertThat(shipmentCaptor.getValue().getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);

            verify(historyRepository).save(historyCaptor.capture());
            ShipmentStatusHistory history = historyCaptor.getValue();
            assertThat(history.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
            assertThat(history.getNotes()).isEqualTo("Departed Sofia Hub");
            assertThat(history.getLocation()).isNull();
        }

        @Test
        @DisplayName("Happy Path: Should transition to AT_DELIVERY_OFFICE and record office location")
        void shouldTransitionToDeliveryOfficeAndRecordLocation() {
            // Arrange
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

            // Act
            shipmentService.updateShipmentStatus(shipmentId, dto, authUser);

            // Assert
            verify(historyRepository).save(historyCaptor.capture());
            ShipmentStatusHistory history = historyCaptor.getValue();
            assertThat(history.getLocation().getId()).isEqualTo(officeId);
            
            verify(shipmentRepository).save(shipmentCaptor.capture());
            assertThat(shipmentCaptor.getValue().getCurrentOffice().getId()).isEqualTo(officeId);
            assertThat(shipmentCaptor.getValue().getCurrentCourier()).isNull();
        }

        @Test
        @DisplayName("Happy Path: Courier should be assigned as deliveredBy when marking as DELIVERED")
        void courierShouldBeAssignedWhenDelivered() {
            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();
            CustomUserDetails authUser = createMockAuthUser(courierId, ApplicationRole.COURIER);
            
            ShipmentStatusUpdateDto dto = ShipmentStatusUpdateDto.builder()
                    .newStatus(ShipmentStatus.DELIVERED)
                    .build();

            Shipment existingShipment = createValidShipment();
            existingShipment.setId(shipmentId);
            existingShipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);

            Courier courierEntity = createMockEmployee(courierId, "John", "Courier");

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(existingShipment));
            given(employeeRepository.findById(courierId)).willReturn(Optional.of(courierEntity));
            given(shipmentRepository.save(any(Shipment.class))).willReturn(existingShipment);

            // Act
            shipmentService.updateShipmentStatus(shipmentId, dto, authUser);

            // Assert
            verify(shipmentRepository).save(shipmentCaptor.capture());
            Shipment savedShipment = shipmentCaptor.getValue();
            assertThat(savedShipment.getDeliveredBy()).isNotNull();
            assertThat(savedShipment.getDeliveredBy().getId()).isEqualTo(courierId);
        }
        
        @Test
        @DisplayName("Happy Path: Courier should be assigned as currentCourier when marking as OUT_FOR_DELIVERY")
        void courierShouldBeAssignedWhenOutForDelivery() {
            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();
            CustomUserDetails authUser = createMockAuthUser(courierId, ApplicationRole.COURIER); 
            
            ShipmentStatusUpdateDto dto = ShipmentStatusUpdateDto.builder()
                    .newStatus(ShipmentStatus.OUT_FOR_DELIVERY)
                    .build();

            Shipment existingShipment = createValidShipment();
            existingShipment.setId(shipmentId);
            existingShipment.setStatus(ShipmentStatus.AT_DELIVERY_OFFICE);
            existingShipment.setCurrentOffice(new Office());

            Courier courierEntity = createMockEmployee(courierId, "John", "Courier");

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(existingShipment));
            given(employeeRepository.findById(courierId)).willReturn(Optional.of(courierEntity));
            given(shipmentRepository.save(any(Shipment.class))).willReturn(existingShipment);

            // Act
            shipmentService.updateShipmentStatus(shipmentId, dto, authUser);

            // Assert
            verify(shipmentRepository).save(shipmentCaptor.capture());
            Shipment savedShipment = shipmentCaptor.getValue();
            assertThat(savedShipment.getCurrentCourier()).isNotNull();
            assertThat(savedShipment.getCurrentCourier().getId()).isEqualTo(courierId);
            assertThat(savedShipment.getCurrentOffice()).isNull();
        }

        @ParameterizedTest(name = "Current Status: {0} -> Target Status: {1}")
        @CsvSource({
                "REGISTERED, AT_DELIVERY_OFFICE", // Skipping IN_TRANSIT
                "REGISTERED, DELIVERED",          // Skipping everything
                "IN_TRANSIT, DELIVERED",          // Must go to AT_OFFICE or OUT_FOR_DELIVERY first
                "DELIVERED, IN_TRANSIT",          // Cannot go backwards
                "AT_DELIVERY_OFFICE, REGISTERED"  // Cannot go backwards
        })
        @DisplayName("Business Rule: Should reject invalid state machine transitions")
        void shouldRejectInvalidTransitions(ShipmentStatus currentStatus, ShipmentStatus newStatus) {
            // Arrange
            UUID shipmentId = UUID.randomUUID();
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            
            ShipmentStatusUpdateDto dto = ShipmentStatusUpdateDto.builder().newStatus(newStatus).build();

            Shipment existingShipment = createValidShipment();
            existingShipment.setId(shipmentId);
            existingShipment.setStatus(currentStatus);

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(existingShipment));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.updateShipmentStatus(shipmentId, dto, authUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verify(shipmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Security: Should throw VALIDATION_FAILED if a non-courier attempts to mark as OUT_FOR_DELIVERY")
        void shouldRejectNonCourierOutForDelivery() {
            // Arrange
            UUID shipmentId = UUID.randomUUID();
            CustomUserDetails clerkUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            
            ShipmentStatusUpdateDto dto = ShipmentStatusUpdateDto.builder().newStatus(ShipmentStatus.OUT_FOR_DELIVERY).build();

            Shipment existingShipment = createValidShipment();
            existingShipment.setId(shipmentId);
            existingShipment.setStatus(ShipmentStatus.AT_DELIVERY_OFFICE);

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(existingShipment));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.updateShipmentStatus(shipmentId, dto, clerkUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verify(shipmentRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Error Case: Should throw OFFICE_NOT_FOUND if invalid locationOfficeId provided")
        void shouldThrowIfInvalidOfficeProvided() {
            UUID shipmentId = UUID.randomUUID();
            Long invalidOfficeId = 999L;
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            
            ShipmentStatusUpdateDto dto = ShipmentStatusUpdateDto.builder()
                    .newStatus(ShipmentStatus.AT_DELIVERY_OFFICE)
                    .locationOfficeId(invalidOfficeId)
                    .build();

            Shipment existingShipment = createValidShipment();
            existingShipment.setId(shipmentId);
            existingShipment.setStatus(ShipmentStatus.IN_TRANSIT);

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(existingShipment));
            given(officeRepository.findById(invalidOfficeId)).willReturn(Optional.empty());

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.updateShipmentStatus(shipmentId, dto, authUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.OFFICE_NOT_FOUND);
                    
            verify(historyRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Error Case: Defense in depth - should throw NullPointerException if DTO is null")
        void shouldFailFastIfDtoNull() {
            CustomUserDetails authUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            
            assertThatThrownBy(() -> shipmentService.updateShipmentStatus(UUID.randomUUID(), null, authUser))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Nested
    @DisplayName("registerShipment(ShipmentCreationDto, UUID) Tests")
    class RegisterShipmentTests {

        @Test
        @DisplayName("Happy Path: Should successfully register a shipment destined for an Office (Registered Receiver)")
        void shouldRegisterShipmentToOffice() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();
            Long originOfficeId = 5L;
            Long destOfficeId = 10L;

            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, receiverId)
                    .deliveryOfficeId(destOfficeId)
                    .build();

            Client sender = createMockClient(senderId, "Sender", "One");
            Client receiver = createMockClient(receiverId, "Receiver", "Two");
            Courier employee = createMockEmployee(employeeId, "Emp", "Three");
            City city = createMockCity(1L, "Sofia", "1000");
            Office destOffice = createMockOffice(destOfficeId, city);
            Office originOffice = createMockOffice(originOfficeId, city);

            given(clientRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(clientRepository.findById(receiverId)).willReturn(Optional.of(receiver));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
            given(officeRepository.findById(destOfficeId)).willReturn(Optional.of(destOffice));
            given(officeRepository.findById(originOfficeId)).willReturn(Optional.of(originOffice));
            given(pricingService.calculatePrice(dto)).willReturn(BigDecimal.valueOf(15.00));

            Shipment savedShipment = createValidShipment();
            savedShipment.setDeliveryOffice(destOffice);
            savedShipment.setOriginOffice(originOffice);

            given(shipmentRepository.save(any(Shipment.class))).willReturn(savedShipment);

            // Act
            ShipmentViewDto result = shipmentService.registerShipment(dto, employeeId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.trackingNumber()).isEqualTo("TRK-TEST");
            assertThat(result.deliveryOfficeName()).contains("Sofia", "Office Street 1");
            assertThat(result.deliveryAddressString()).isNull();

            verify(shipmentRepository).save(shipmentCaptor.capture());
            Shipment capturedShipment = shipmentCaptor.getValue();
            assertThat(capturedShipment.getTrackingNumber()).startsWith("TRK-");
            assertThat(capturedShipment.getStatus()).isEqualTo(ShipmentStatus.REGISTERED);
            assertThat(capturedShipment.getDeliveryOffice().getId()).isEqualTo(destOfficeId);
            assertThat(capturedShipment.getOriginOffice().getId()).isEqualTo(originOfficeId);
            assertThat(capturedShipment.getDeliveryAddressSnapshot()).isNull();
            assertThat(capturedShipment.getOriginAddressSnapshot()).isNull();
            
            assertThat(capturedShipment.getPackageDetails().getWeight()).isEqualByComparingTo(BigDecimal.valueOf(2.5));
            assertThat(capturedShipment.getFinancials().getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(15.0));

            verify(historyRepository).save(historyCaptor.capture());
            ShipmentStatusHistory capturedHistory = historyCaptor.getValue();
            assertThat(capturedHistory.getShipment().getId()).isEqualTo(savedShipment.getId());
            assertThat(capturedHistory.getStatus()).isEqualTo(ShipmentStatus.REGISTERED);
        }

        @Test
        @DisplayName("Happy Path: Should auto-match guest receiver by phone number if client exists")
        void shouldAutoMatchReceiverByPhoneNumber() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();
            Long destOfficeId = 10L;
            String matchingPhone = "0888123456";
            
            ShipmentCreationDto dto = ShipmentCreationDto.builder()
                    .senderId(senderId)
                    .receiverName("Guest Mom")
                    .receiverPhone(matchingPhone)
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(2.5))
                    .paidBy(PaidBy.RECEIVER)
                    .originOfficeId(5L)
                    .deliveryOfficeId(destOfficeId)
                    .build();

            Client sender = createMockClient(senderId, "Sender", "One");
            
            UUID matchedReceiverId = UUID.randomUUID();
            Client matchedReceiver = createMockClient(matchedReceiverId, "Real", "Mom");
            
            Courier employee = createMockEmployee(employeeId, "Emp", "Three");
            Office destOffice = createMockOffice(destOfficeId, createMockCity(1L, "Sofia", "1000"));
            Office originOffice = createMockOffice(5L, createMockCity(1L, "Sofia", "1000"));

            given(clientRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
            given(officeRepository.findById(destOfficeId)).willReturn(Optional.of(destOffice));
            given(officeRepository.findById(5L)).willReturn(Optional.of(originOffice));
            given(pricingService.calculatePrice(dto)).willReturn(BigDecimal.valueOf(15.00));
            given(clientRepository.findByPhoneNumber(matchingPhone)).willReturn(Optional.of(matchedReceiver));

            Shipment savedShipment = createValidShipment();
            savedShipment.setReceiver(matchedReceiver);
            savedShipment.setDeliveryOffice(destOffice);

            given(shipmentRepository.save(any(Shipment.class))).willReturn(savedShipment);

            // Act
            shipmentService.registerShipment(dto, employeeId);

            // Assert
            verify(shipmentRepository).save(shipmentCaptor.capture());
            Shipment capturedShipment = shipmentCaptor.getValue();

            assertThat(capturedShipment.getReceiver()).isNotNull();
            assertThat(capturedShipment.getReceiver().getId()).isEqualTo(matchedReceiverId);
            assertThat(capturedShipment.getReceiverName()).isNull();
        }

        @Test
        @DisplayName("Happy Path: Should successfully register a shipment for a Guest Receiver from an Address")
        void shouldRegisterShipmentGuestReceiverFromAddress() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();
            Long destOfficeId = 10L;
            Long cityId = 20L;
            
            AddressDetailsDto originAddressDto = new AddressDetailsDto(cityId, "Origin St", "Dist", "1", "A", "1", "1", 0.0, 0.0);

            ShipmentCreationDto dto = ShipmentCreationDto.builder()
                    .senderId(senderId)
                    .receiverName("Guest Mom")
                    .receiverPhone("0888123456")
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(2.5))
                    .paidBy(PaidBy.RECEIVER)
                    .originAddress(originAddressDto)
                    .deliveryOfficeId(destOfficeId)
                    .build();

            Client sender = createMockClient(senderId, "Sender", "One");
            Courier employee = createMockEmployee(employeeId, "Emp", "Three");
            City city = createMockCity(cityId, "Sofia", "1000");
            Office destOffice = createMockOffice(destOfficeId, city);

            given(clientRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
            given(officeRepository.findById(destOfficeId)).willReturn(Optional.of(destOffice));
            given(cityRepository.findById(cityId)).willReturn(Optional.of(city));
            given(pricingService.calculatePrice(dto)).willReturn(BigDecimal.valueOf(15.00));
            given(clientRepository.findByPhoneNumber("0888123456")).willReturn(Optional.empty());

            Shipment savedShipment = createValidShipment();
            savedShipment.setReceiver(null);
            savedShipment.setReceiverName("Guest Mom");
            savedShipment.setReceiverPhone("0888123456");
            savedShipment.setDeliveryOffice(destOffice);

            given(shipmentRepository.save(any(Shipment.class))).willReturn(savedShipment);

            // Act
            ShipmentViewDto result = shipmentService.registerShipment(dto, employeeId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.receiverName()).isEqualTo("Guest Mom");
            assertThat(result.receiverId()).isNull();

            verify(shipmentRepository).save(shipmentCaptor.capture());
            Shipment capturedShipment = shipmentCaptor.getValue();
            assertThat(capturedShipment.getReceiver()).isNull();
            assertThat(capturedShipment.getReceiverName()).isEqualTo("Guest Mom");
            assertThat(capturedShipment.getOriginOffice()).isNull();
            assertThat(capturedShipment.getOriginAddressSnapshot().getStreet()).isEqualTo("Origin St");
            assertThat(capturedShipment.getFinancials().getPaidBy()).isEqualTo(PaidBy.RECEIVER);
        }

        @Test
        @DisplayName("Happy Path: Should successfully register a shipment destined for a specific Address")
        void shouldRegisterShipmentToAddress() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();
            Long cityId = 20L;
            Long originOfficeId = 5L;

            AddressDetailsDto addressDto = new AddressDetailsDto(cityId, "Home St", "Dist", "1", "A", "1", "1", 0.0, 0.0);

            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, receiverId)
                    .deliveryAddress(addressDto)
                    .build();

            Client sender = createMockClient(senderId, "Sender", "One");
            Client receiver = createMockClient(receiverId, "Receiver", "Two");
            Courier employee = createMockEmployee(employeeId, "Emp", "Three");
            City city = createMockCity(cityId, "Plovdiv", "4000");
            Office originOffice = createMockOffice(originOfficeId, city);

            given(clientRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(clientRepository.findById(receiverId)).willReturn(Optional.of(receiver));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
            given(cityRepository.findById(cityId)).willReturn(Optional.of(city));
            given(officeRepository.findById(originOfficeId)).willReturn(Optional.of(originOffice));
            given(pricingService.calculatePrice(dto)).willReturn(BigDecimal.valueOf(20.00));

            AddressDetails snapshot = new AddressDetails();
            snapshot.setCity(city);
            snapshot.setStreet("Home St");

            Shipment savedShipment = createValidShipment();
            savedShipment.setOriginOffice(originOffice);
            savedShipment.setDeliveryAddressSnapshot(snapshot);

            given(shipmentRepository.save(any(Shipment.class))).willReturn(savedShipment);

            // Act
            ShipmentViewDto result = shipmentService.registerShipment(dto, employeeId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.deliveryAddressString()).contains("Home St", "Plovdiv");
            assertThat(result.deliveryOfficeId()).isNull();

            verify(shipmentRepository).save(shipmentCaptor.capture());
            Shipment capturedShipment = shipmentCaptor.getValue();
            assertThat(capturedShipment.getDeliveryOffice()).isNull();
            assertThat(capturedShipment.getDeliveryAddressSnapshot().getStreet()).isEqualTo("Home St");

            verify(historyRepository).save(any(ShipmentStatusHistory.class));
        }

        @Test
        @DisplayName("Error Case: Should fail fast (Defense in Depth) if DTO is null")
        void shouldFailFastIfDtoNull() {
            assertThatThrownBy(() -> shipmentService.registerShipment(null, UUID.randomUUID()))
                    .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(clientRepository, shipmentRepository, pricingService);
        }

        @Test
        @DisplayName("Error Case: Should throw SHIPMENT_RECEIVER_EXCLUSIVE if both receiver ID and guest details provided")
        void shouldThrowIfBothReceiverTypesProvided() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();

            ShipmentCreationDto dto = ShipmentCreationDto.builder()
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .receiverName("Guest Name")
                    .receiverPhone("0888123456")
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(2.5))
                    .build();

            given(clientRepository.findById(senderId)).willReturn(Optional.of(new Client()));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SHIPMENT_RECEIVER_EXCLUSIVE);
        }

        @Test
        @DisplayName("Error Case: Should throw SHIPMENT_RECEIVER_EXCLUSIVE if neither receiver ID nor guest details provided")
        void shouldThrowIfNoReceiverDetailsProvided() {
            // Arrange
            UUID senderId = UUID.randomUUID();

            ShipmentCreationDto dto = ShipmentCreationDto.builder()
                    .senderId(senderId)
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(2.5))
                    .build();

            given(clientRepository.findById(senderId)).willReturn(Optional.of(new Client()));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SHIPMENT_RECEIVER_EXCLUSIVE);
        }

        @Test
        @DisplayName("Error Case: Should throw 404 RESOURCE_NOT_FOUND if sender does not exist")
        void shouldThrowIfSenderNotFound() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, UUID.randomUUID()).build();

            given(clientRepository.findById(senderId)).willReturn(Optional.empty());

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

            verifyNoInteractions(pricingService, shipmentRepository, historyRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw 404 RESOURCE_NOT_FOUND if receiver ID does not exist")
        void shouldThrowIfReceiverNotFound() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, receiverId).build();

            given(clientRepository.findById(senderId)).willReturn(Optional.of(new Client()));
            given(clientRepository.findById(receiverId)).willReturn(Optional.empty());

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

            verifyNoInteractions(pricingService, shipmentRepository, historyRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw 404 EMPLOYEE_NOT_FOUND if registering employee does not exist")
        void shouldThrowIfEmployeeNotFound() {

            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();

            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, receiverId)
                    .deliveryOfficeId(10L)
                    .build();

            given(clientRepository.findById(senderId)).willReturn(Optional.of(new Client()));
            given(clientRepository.findById(receiverId)).willReturn(Optional.of(new Client()));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.empty());

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, employeeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);

            verifyNoInteractions(pricingService, shipmentRepository, historyRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw SHIPMENT_DESTINATION_EXCLUSIVE if both destination types are provided")
        void shouldThrowIfBothDestinationsProvided() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();

            AddressDetailsDto addressDto = new AddressDetailsDto(20L, "St", "D", "1", "A", "1", "1", 0.0, 0.0);

            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, receiverId)
                    .deliveryOfficeId(10L)
                    .deliveryAddress(addressDto)
                    .build();

            given(clientRepository.findById(senderId)).willReturn(Optional.of(new Client()));
            given(clientRepository.findById(receiverId)).willReturn(Optional.of(new Client()));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(new Courier()));
            given(officeRepository.findById(5L)).willReturn(Optional.of(new Office()));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, employeeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SHIPMENT_DESTINATION_EXCLUSIVE);

            verifyNoInteractions(pricingService, shipmentRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw SHIPMENT_DESTINATION_EXCLUSIVE if neither destination type is provided")
        void shouldThrowIfNeitherDestinationProvided() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();

            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, receiverId)
                    .deliveryOfficeId(null)
                    .deliveryAddress(null)
                    .build();

            given(clientRepository.findById(senderId)).willReturn(Optional.of(new Client()));
            given(clientRepository.findById(receiverId)).willReturn(Optional.of(new Client()));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(new Courier()));
            given(officeRepository.findById(5L)).willReturn(Optional.of(new Office()));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, employeeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SHIPMENT_DESTINATION_EXCLUSIVE);

            verifyNoInteractions(pricingService, shipmentRepository);
        }
        
        @Test
        @DisplayName("Error Case: Should throw SHIPMENT_DESTINATION_EXCLUSIVE if both origin types are provided")
        void shouldThrowIfBothOriginsProvided() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();

            AddressDetailsDto originDto = new AddressDetailsDto(20L, "St", "D", "1", "A", "1", "1", 0.0, 0.0);

            ShipmentCreationDto dto = ShipmentCreationDto.builder()
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(2.5))
                    .paidBy(PaidBy.SENDER)
                    .originOfficeId(10L)
                    .originAddress(originDto)
                    .deliveryOfficeId(5L)
                    .build();

            given(clientRepository.findById(senderId)).willReturn(Optional.of(new Client()));
            given(clientRepository.findById(receiverId)).willReturn(Optional.of(new Client()));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(new Courier()));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, employeeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SHIPMENT_DESTINATION_EXCLUSIVE);

            verifyNoInteractions(pricingService, shipmentRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw OFFICE_NOT_FOUND if invalid delivery office provided")
        void shouldThrowIfInvalidOfficeProvided() {

            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();
            Long officeId = 999L;

            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, receiverId)
                    .deliveryOfficeId(officeId)
                    .build();

            given(clientRepository.findById(senderId)).willReturn(Optional.of(new Client()));
            given(clientRepository.findById(receiverId)).willReturn(Optional.of(new Client()));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(new Courier()));
            given(officeRepository.findById(5L)).willReturn(Optional.of(new Office()));
            given(officeRepository.findById(officeId)).willReturn(Optional.empty());


            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, employeeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.OFFICE_NOT_FOUND);

            verifyNoInteractions(pricingService, shipmentRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw CITY_NOT_FOUND if invalid city provided in delivery address")
        void shouldThrowIfInvalidCityProvidedInAddress() {

            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();
            Long cityId = 999L;

            AddressDetailsDto addressDto = new AddressDetailsDto(cityId, "St", "D", "1", "A", "1", "1", 0.0, 0.0);

            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, receiverId)
                    .deliveryAddress(addressDto)
                    .build();

            given(clientRepository.findById(senderId)).willReturn(Optional.of(new Client()));
            given(clientRepository.findById(receiverId)).willReturn(Optional.of(new Client()));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(new Courier()));
            given(officeRepository.findById(5L)).willReturn(Optional.of(new Office()));
            given(cityRepository.findById(cityId)).willReturn(Optional.empty());

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, employeeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CITY_NOT_FOUND);

            verifyNoInteractions(pricingService, shipmentRepository);
        }
    }

    @Nested
    @DisplayName("getShipmentById(UUID, UUID, ApplicationRole) Access Control Tests")
    class GetShipmentByIdTests {

        @Test
        @DisplayName("Happy Path: Admin should retrieve any shipment")
        void adminShouldRetrieveAnyShipment() {

            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();
            Shipment shipment = createValidShipment();
            shipment.setId(shipmentId);

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));

            // Act
            ShipmentViewDto result = shipmentService.getShipmentById(shipmentId, adminId, ApplicationRole.ADMIN);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(shipmentId);
        }

        @Test
        @DisplayName("Happy Path: Clerk should retrieve any shipment")
        void clerkShouldRetrieveAnyShipment() {

            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID clerkId = UUID.randomUUID();
            Shipment shipment = createValidShipment();
            shipment.setId(shipmentId);

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));

            // Act
            ShipmentViewDto result = shipmentService.getShipmentById(shipmentId, clerkId, ApplicationRole.CLERK);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(shipmentId);
        }

        @Test
        @DisplayName("Happy Path: Courier should retrieve any shipment")
        void courierShouldRetrieveAnyShipment() {

            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID courierId = UUID.randomUUID();
            Shipment shipment = createValidShipment();
            shipment.setId(shipmentId);

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));

            // Act
            ShipmentViewDto result = shipmentService.getShipmentById(shipmentId, courierId, ApplicationRole.COURIER);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(shipmentId);
        }

        @Test
        @DisplayName("Happy Path: Client should retrieve shipment they sent")
        void senderShouldRetrieveShipment() {
            UUID shipmentId = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();
            Shipment shipment = createValidShipment();
            shipment.setId(shipmentId);
            shipment.setSender(createMockClient(clientId, "S", "S"));

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));

            ShipmentViewDto result = shipmentService.getShipmentById(shipmentId, clientId, ApplicationRole.CLIENT);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Happy Path: Client should retrieve shipment they are receiving")
        void receiverShouldRetrieveShipment() {

            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();
            Shipment shipment = createValidShipment();
            shipment.setId(shipmentId);
            shipment.setReceiver(createMockClient(clientId, "R", "R"));

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));

            // Act
            ShipmentViewDto result = shipmentService.getShipmentById(shipmentId, clientId, ApplicationRole.CLIENT);

            // Assert
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Error Case: Client requesting someone else's shipment should get 404 (Security)")
        void clientShouldNotRetrieveOtherShipment() {

            // Arrange
            UUID shipmentId = UUID.randomUUID();
            UUID maliciousClientId = UUID.randomUUID();

            Shipment shipment = createValidShipment();
            shipment.setId(shipmentId);

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.of(shipment));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.getShipmentById(shipmentId, maliciousClientId, ApplicationRole.CLIENT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("Error Case: Should throw 404 when shipment does not exist")
        void shouldThrowIfShipmentDoesNotExist() {
            //Arrange
            UUID shipmentId = UUID.randomUUID();

            given(shipmentRepository.findById(shipmentId)).willReturn(Optional.empty());
            // Act and Assert
            assertThatThrownBy(() -> shipmentService.getShipmentById(shipmentId, UUID.randomUUID(), ApplicationRole.ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getShipmentByTrackingNumber(String, UUID, ApplicationRole) Access Control Tests")
    class GetShipmentByTrackingNumberTests {

        @Test
        @DisplayName("Happy Path: Admin should retrieve any shipment by tracking number")
        void adminShouldRetrieveAnyShipmentByTracking() {

            // Arrange
            String tracking = "TRK-123";
            UUID adminId = UUID.randomUUID();
            Shipment shipment = createValidShipment();
            shipment.setId(UUID.randomUUID());
            shipment.setTrackingNumber(tracking);

            given(shipmentRepository.findByTrackingNumber(tracking)).willReturn(Optional.of(shipment));

            // Act
            ShipmentViewDto result = shipmentService.getShipmentByTrackingNumber(tracking, adminId, ApplicationRole.ADMIN);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.trackingNumber()).isEqualTo(tracking);
        }

        @Test
        @DisplayName("Happy Path: Client should retrieve shipment they sent by tracking number")
        void senderShouldRetrieveShipmentByTracking() {

            // Arrange
            String tracking = "TRK-456";
            UUID clientId = UUID.randomUUID();
            Shipment shipment = createValidShipment();
            shipment.setId(UUID.randomUUID());
            shipment.setTrackingNumber(tracking);
            shipment.setSender(createMockClient(clientId, "S", "S"));

            given(shipmentRepository.findByTrackingNumber(tracking)).willReturn(Optional.of(shipment));

            // Act
            ShipmentViewDto result = shipmentService.getShipmentByTrackingNumber(tracking, clientId, ApplicationRole.CLIENT);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.trackingNumber()).isEqualTo(tracking);
        }

        @Test
        @DisplayName("Error Case: Client requesting someone else's shipment by tracking number should get 404 (Security)")
        void clientShouldNotRetrieveOtherShipmentByTracking() {

            // Arrange
            String tracking = "TRK-789";
            UUID maliciousClientId = UUID.randomUUID();

            Shipment shipment = createValidShipment();
            shipment.setId(UUID.randomUUID());
            shipment.setTrackingNumber(tracking);

            given(shipmentRepository.findByTrackingNumber(tracking)).willReturn(Optional.of(shipment));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.getShipmentByTrackingNumber(tracking, maliciousClientId, ApplicationRole.CLIENT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("Error Case: Should throw VALIDATION_FAILED when tracking number is null or blank")
        void shouldThrowIfTrackingNumberBlank() {
            assertThatThrownBy(() -> shipmentService.getShipmentByTrackingNumber("   ", UUID.randomUUID(), ApplicationRole.ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verifyNoInteractions(shipmentRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw 404 when tracking number does not exist")
        void shouldThrowIfShipmentDoesNotExist() {
            String tracking = "INVALID";
            given(shipmentRepository.findByTrackingNumber(tracking)).willReturn(Optional.empty());

            assertThatThrownBy(() -> shipmentService.getShipmentByTrackingNumber(tracking, UUID.randomUUID(), ApplicationRole.ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Reporting Queries Tests")
    class ReportingQueriesTests {

        @Test
        @DisplayName("Happy Path: Should retrieve shipments by sender")
        void shouldGetBySender() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            Shipment shipment = createValidShipment();

            Page<Shipment> page = new PageImpl<>(List.of(shipment));

            given(shipmentRepository.findBySender_Id(senderId, pageable)).willReturn(page);

            // Act
            Page<ShipmentViewDto> result = shipmentService.getShipmentsBySender(senderId, pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            verify(shipmentRepository).findBySender_Id(senderId, pageable);
        }

        @Test
        @DisplayName("Edge Case: Should return empty page when sender has no shipments")
        void shouldReturnEmptyPageForSender() {

            // Arrange
            UUID senderId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            Page<Shipment> page = new PageImpl<>(List.of());

            given(shipmentRepository.findBySender_Id(senderId, pageable)).willReturn(page);

            // Act
            Page<ShipmentViewDto> result = shipmentService.getShipmentsBySender(senderId, pageable);

            // Assert
            assertThat(result.getContent()).isEmpty();
            verify(shipmentRepository).findBySender_Id(senderId, pageable);
        }

        @Test
        @DisplayName("Happy Path: Should retrieve shipments by receiver")
        void shouldGetByReceiver() {

            // Arrange
            UUID receiverId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            Shipment shipment = createValidShipment();

            Page<Shipment> page = new PageImpl<>(List.of(shipment));

            given(shipmentRepository.findByReceiver_Id(receiverId, pageable)).willReturn(page);

            // Act
            Page<ShipmentViewDto> result = shipmentService.getShipmentsByReceiver(receiverId, pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            verify(shipmentRepository).findByReceiver_Id(receiverId, pageable);
        }

        @Test
        @DisplayName("Happy Path: Should retrieve shipments registered by employee")
        void shouldGetByEmployee() {

            // Arrange
            UUID employeeId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            Shipment shipment = createValidShipment();

            Page<Shipment> page = new PageImpl<>(List.of(shipment));

            given(shipmentRepository.findByRegisteredBy_Id(employeeId, pageable)).willReturn(page);

            // Act
            Page<ShipmentViewDto> result = shipmentService.getShipmentsRegisteredByEmployee(employeeId, pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            verify(shipmentRepository).findByRegisteredBy_Id(employeeId, pageable);
        }

        @Test
        @DisplayName("Happy Path: Should retrieve pending shipments")
        void shouldGetPendingShipments() {

            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Shipment shipment = createValidShipment();
            shipment.setStatus(ShipmentStatus.IN_TRANSIT);

            Page<Shipment> page = new PageImpl<>(List.of(shipment));

            given(shipmentRepository.findByStatusNot(ShipmentStatus.DELIVERED, pageable)).willReturn(page);

            // Act
            Page<ShipmentViewDto> result = shipmentService.getPendingShipments(pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            verify(shipmentRepository).findByStatusNot(ShipmentStatus.DELIVERED, pageable);
        }

        @Test
        @DisplayName("Happy Path: Should retrieve all shipments")
        void shouldGetAllShipments() {
            Pageable pageable = PageRequest.of(0, 10);

            // Arrange
            Shipment shipment = createValidShipment();

            Page<Shipment> page = new PageImpl<>(List.of(shipment));

            given(shipmentRepository.findAll(pageable)).willReturn(page);

            // Act
            Page<ShipmentViewDto> result = shipmentService.getAllShipments(pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            verify(shipmentRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Revenue Reporting Tests")
    class RevenueReportingTests {

        @Test
        @DisplayName("Happy Path: Should return correctly formatted RevenueReportDto")
        void shouldReturnRevenueReport() {
            // Arrange
            LocalDate startDate = LocalDate.of(2026, 4, 1);
            LocalDate endDate = LocalDate.of(2026, 4, 30);
            BigDecimal mockRevenue = BigDecimal.valueOf(1250.50);

            given(shipmentRepository.calculateTotalRevenue(any(), any())).willReturn(mockRevenue);

            // Act
            RevenueReportDto result = shipmentService.getCompanyRevenue(startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.totalRevenue()).isEqualByComparingTo(mockRevenue);
            assertThat(result.startDate()).isEqualTo(startDate);
            assertThat(result.endDate()).isEqualTo(endDate);

            verify(shipmentRepository).calculateTotalRevenue(any(), any());
        }

        @Test
        @DisplayName("Edge Case: Should map null DB result to BigDecimal.ZERO")
        void shouldMapNullRevenueToZero() {
            // Arrange
            LocalDate startDate = LocalDate.of(2026, 4, 1);
            LocalDate endDate = LocalDate.of(2026, 4, 30);

            given(shipmentRepository.calculateTotalRevenue(any(), any())).willReturn(null);

            // Act
            RevenueReportDto result = shipmentService.getCompanyRevenue(startDate, endDate);

            // Assert
            assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Error Case: Should throw VALIDATION_FAILED when start date is after end date")
        void shouldThrowWhenDatesInvalid() {
            // Arrange
            LocalDate startDate = LocalDate.of(2026, 4, 30);
            LocalDate endDate = LocalDate.of(2026, 4, 1);

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.getCompanyRevenue(startDate, endDate))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verifyNoInteractions(shipmentRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw VALIDATION_FAILED when dates are null")
        void shouldThrowWhenDatesNull() {
            assertThatThrownBy(() -> shipmentService.getCompanyRevenue(null, LocalDate.now()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verifyNoInteractions(shipmentRepository);
        }
    }
}