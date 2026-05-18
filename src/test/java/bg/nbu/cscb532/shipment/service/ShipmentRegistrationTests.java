package bg.nbu.cscb532.shipment.service;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.employee.Courier;
import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import bg.nbu.cscb532.shipment.*;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import bg.nbu.cscb532.shipment.dto.StaffShipmentViewDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Shipment Service: Registration Logic Tests")
public class  ShipmentRegistrationTests extends AbstractShipmentUnitTestBase {

    @Test
    @DisplayName("Happy Path: Should successfully register a shipment destined for an Office (Registered Receiver)")
    void shouldRegisterShipmentToOffice() {
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

        StaffShipmentViewDto result = shipmentService.registerShipment(dto, employeeId);

        assertThat(result).isNotNull();
        assertThat(result.trackingNumber()).isEqualTo("TRK-TEST");
        assertThat(result.deliveryOfficeName()).contains("Sofia", "Office Street 1");
        assertThat(result.deliveryAddressString()).isNull();

        verify(shipmentRepository).save(shipmentCaptor.capture());
        Shipment capturedShipment = shipmentCaptor.getValue();
        assertThat(capturedShipment.getTrackingNumber()).startsWith("TRK-");
        assertThat(capturedShipment.getStatus()).isEqualTo(ShipmentStatus.REGISTERED);

        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(ShipmentStatus.REGISTERED);
    }

    @Test
    @DisplayName("Happy Path: Should auto-match guest receiver by phone number if client exists")
    void shouldAutoMatchReceiverByPhoneNumber() {
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

        shipmentService.registerShipment(dto, employeeId);

        verify(shipmentRepository).save(shipmentCaptor.capture());
        assertThat(shipmentCaptor.getValue().getReceiver().getId()).isEqualTo(matchedReceiverId);
    }

    @Test
    @DisplayName("Happy Path: Should successfully register a shipment for a Guest Receiver from an Address")
    void shouldRegisterShipmentGuestReceiverFromAddress() {
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

        StaffShipmentViewDto result = shipmentService.registerShipment(dto, employeeId);

        assertThat(result).isNotNull();
        assertThat(result.receiverName()).isEqualTo("Guest Mom");
        verify(shipmentRepository).save(shipmentCaptor.capture());
        assertThat(shipmentCaptor.getValue().getOriginAddressSnapshot().getStreet()).isEqualTo("Origin St");
    }

    @Test
    @DisplayName("Happy Path: Should successfully register a shipment destined for a specific Address")
    void shouldRegisterShipmentToAddress() {
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

        shipmentService.registerShipment(dto, employeeId);

        verify(shipmentRepository).save(shipmentCaptor.capture());
        assertThat(shipmentCaptor.getValue().getDeliveryAddressSnapshot().getStreet()).isEqualTo("Home St");
    }

    @Test
    @DisplayName("Happy Path: Should correctly process and save shipment addons")
    void shouldProcessShipmentAddons() {
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

        shipmentService.registerShipment(dto, employeeId);

        verify(shipmentAddonRepository, times(2)).save(addonCaptor.capture());
        assertThat(addonCaptor.getAllValues()).extracting(addon -> addon.getServiceCatalog().getName())
                .containsExactlyInAnyOrder("Fragile", "SMS");
    }

    @Test
    @DisplayName("Error Case: Should throw RESOURCE_NOT_FOUND if an invalid service ID is provided")
    void shouldThrowIfInvalidServiceIdProvided() {
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

        given(serviceCatalogRepository.findAllById(dto.selectedServiceIds())).willReturn(List.of(fragileService));

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