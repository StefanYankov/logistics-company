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
import bg.nbu.cscb532.shipment.dto.PublicShipmentViewDto;
import bg.nbu.cscb532.shipment.dto.RevenueReportDto;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

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

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @Mock
    private ShipmentAddonRepository shipmentAddonRepository;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    @Captor
    private ArgumentCaptor<Shipment> shipmentCaptor;

    @Captor
    private ArgumentCaptor<ShipmentStatusHistory> historyCaptor;

    @Captor
    private ArgumentCaptor<ShipmentAddon> addonCaptor;

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

        // FIX: Change emptyList() to emptySet() to align with the Shipment entity definition
        shipment.setAddons(Collections.emptySet());

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
            StaffShipmentViewDto result = shipmentService.registerShipment(dto, employeeId);

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

            AddressDetails originAddressSnapshot = new AddressDetails();
            originAddressSnapshot.setCity(city);
            originAddressSnapshot.setStreet("Origin St");
            savedShipment.setOriginAddressSnapshot(originAddressSnapshot);

            given(shipmentRepository.save(any(Shipment.class))).willReturn(savedShipment);

            // Act
            StaffShipmentViewDto result = shipmentService.registerShipment(dto, employeeId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.receiverName()).isEqualTo("Guest Mom");
            assertThat(result.receiverId()).isNull();

            verify(shipmentRepository).save(shipmentCaptor.capture());
            Shipment capturedShipment = shipmentCaptor.getValue();
            assertThat(capturedShipment.getReceiver()).isNull();
            assertThat(capturedShipment.getReceiverName()).isEqualTo("Guest Mom");
            assertThat(capturedShipment.getOriginAddressSnapshot().getStreet()).isEqualTo("Origin St");
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
            shipmentService.registerShipment(dto, employeeId);

            // Assert
            verify(shipmentRepository).save(shipmentCaptor.capture());
            Shipment capturedShipment = shipmentCaptor.getValue();
            assertThat(capturedShipment.getDeliveryOffice()).isNull();
            assertThat(capturedShipment.getDeliveryAddressSnapshot().getStreet()).isEqualTo("Home St");
        }

        @Test
        @DisplayName("Happy Path: Should correctly process and save shipment addons")
        void shouldProcessShipmentAddons() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();
            Long destOfficeId = 10L;

            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, receiverId)
                    .deliveryOfficeId(destOfficeId)
                    .selectedServiceIds(Set.of(1L, 2L))
                    .build();

            Client sender = createMockClient(senderId, "Sender", "One");
            Client receiver = createMockClient(receiverId, "Receiver", "Two");
            Courier employee = createMockEmployee(employeeId, "Emp", "Three");
            City city = createMockCity(1L, "Sofia", "1000");
            Office destOffice = createMockOffice(destOfficeId, city);
            Office originOffice = createMockOffice(5L, city);

            given(clientRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(clientRepository.findById(receiverId)).willReturn(Optional.of(receiver));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
            given(officeRepository.findById(destOfficeId)).willReturn(Optional.of(destOffice));
            given(officeRepository.findById(5L)).willReturn(Optional.of(originOffice));
            given(pricingService.calculatePrice(dto)).willReturn(BigDecimal.valueOf(25.00));

            ServiceCatalog fragileService = new ServiceCatalog();
            fragileService.setId(1L);
            fragileService.setName("Fragile");
            fragileService.setPricingType(PricingType.FIXED_AMOUNT);
            fragileService.setPricingValue(BigDecimal.valueOf(5.00));

            ServiceCatalog smsService = new ServiceCatalog();
            smsService.setId(2L);
            smsService.setName("SMS");
            smsService.setPricingType(PricingType.FIXED_AMOUNT);
            smsService.setPricingValue(BigDecimal.valueOf(0.50));

            given(serviceCatalogRepository.findAllById(dto.selectedServiceIds()))
                    .willReturn(List.of(fragileService, smsService));

            Shipment savedShipment = createValidShipment();
            given(shipmentRepository.save(any(Shipment.class))).willReturn(savedShipment);

            // Act
            shipmentService.registerShipment(dto, employeeId);

            // Assert
            verify(shipmentAddonRepository, times(2)).save(addonCaptor.capture());
            List<ShipmentAddon> savedAddons = addonCaptor.getAllValues();

            assertThat(savedAddons).hasSize(2);
            assertThat(savedAddons).extracting(addon -> addon.getServiceCatalog().getName())
                    .containsExactlyInAnyOrder("Fragile", "SMS");
        }

        @Test
        @DisplayName("Error Case: Should throw RESOURCE_NOT_FOUND if an invalid service ID is provided")
        void shouldThrowIfInvalidServiceIdProvided() {
            // Arrange
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();
            Long destOfficeId = 10L;

            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, receiverId)
                    .deliveryOfficeId(destOfficeId)
                    .selectedServiceIds(Set.of(1L, 999L))
                    .build();

            Client sender = createMockClient(senderId, "Sender", "One");
            Client receiver = createMockClient(receiverId, "Receiver", "Two");
            Courier employee = createMockEmployee(employeeId, "Emp", "Three");
            City city = createMockCity(1L, "Sofia", "1000");
            Office destOffice = createMockOffice(destOfficeId, city);
            Office originOffice = createMockOffice(5L, city);

            given(clientRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(clientRepository.findById(receiverId)).willReturn(Optional.of(receiver));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
            given(officeRepository.findById(destOfficeId)).willReturn(Optional.of(destOffice));
            given(officeRepository.findById(5L)).willReturn(Optional.of(originOffice));
            given(pricingService.calculatePrice(dto)).willReturn(BigDecimal.valueOf(25.00));

            ServiceCatalog fragileService = new ServiceCatalog();
            fragileService.setId(1L);

            given(serviceCatalogRepository.findAllById(dto.selectedServiceIds()))
                    .willReturn(List.of(fragileService));

            // Act and Assert
            assertThatThrownBy(() -> shipmentService.registerShipment(dto, employeeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

            verifyNoInteractions(shipmentAddonRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw SHIPMENT_RECEIVER_EXCLUSIVE if both receiver ID and guest details provided")
        void shouldThrowIfBothReceiverTypesProvided() {
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

            assertThatThrownBy(() -> shipmentService.registerShipment(dto, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SHIPMENT_RECEIVER_EXCLUSIVE);
        }

        @Test
        @DisplayName("Error Case: Should throw 404 RESOURCE_NOT_FOUND if sender does not exist")
        void shouldThrowIfSenderNotFound() {
            UUID senderId = UUID.randomUUID();
            ShipmentCreationDto dto = baseCreationDtoBuilder(senderId, UUID.randomUUID()).build();

            given(clientRepository.findById(senderId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> shipmentService.registerShipment(dto, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateShipmentStatus(UUID, ShipmentStatusUpdateDto, CustomUserDetails) Tests")
    class UpdateShipmentStatusTests {

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
    }

    @Nested
    @DisplayName("getShipmentByTrackingNumber(String) Public Boundary Tests")
    class GetShipmentByTrackingNumberTests {

        @Test
        @DisplayName("Happy Path: Should retrieve a public-restricted DTO by tracking number")
        void shouldRetrieveShipmentByTrackingNumber() {
            // Arrange
            String tracking = "TRK-123";
            Shipment shipment = createValidShipment();
            shipment.setTrackingNumber(tracking);

            Office originOffice = createMockOffice(1L, createMockCity(10L, "Sofia", "1000"));
            Office deliveryOffice = createMockOffice(2L, createMockCity(20L, "Plovdiv", "4000"));
            shipment.setOriginOffice(originOffice);
            shipment.setDeliveryOffice(deliveryOffice);

            given(shipmentRepository.findByTrackingNumber(tracking)).willReturn(Optional.of(shipment));

            // Act
            PublicShipmentViewDto result = shipmentService.getShipmentByTrackingNumber(tracking);

            // Assert
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

    @Nested
    @DisplayName("Reporting Queries Tests")
    class ReportingQueriesTests {

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
    }

    @Nested
    @DisplayName("Revenue Reporting Tests")
    class RevenueReportingTests {

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
    }
}