package bg.nbu.cscb532.shipment.service;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.client.ClientRepository;
import bg.nbu.cscb532.employee.Courier;
import bg.nbu.cscb532.employee.EmployeeRepository;
import bg.nbu.cscb532.employee.OfficeClerk;
import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.office.CityRepository;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.office.OfficeRepository;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.shipment.*;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

public abstract class AbstractShipmentUnitTestBase {

    @Mock protected ShipmentRepository shipmentRepository;
    @Mock protected ShipmentStatusHistoryRepository historyRepository;
    @Mock protected ClientRepository clientRepository;
    @Mock protected EmployeeRepository employeeRepository;
    @Mock protected OfficeRepository officeRepository;
    @Mock protected CityRepository cityRepository;
    @Mock protected PricingService pricingService;
    @Mock protected ServiceCatalogRepository serviceCatalogRepository;
    @Mock protected ShipmentAddonRepository shipmentAddonRepository;

    @InjectMocks protected ShipmentServiceImpl shipmentService;

    @Captor protected ArgumentCaptor<Shipment> shipmentCaptor;
    @Captor protected ArgumentCaptor<ShipmentStatusHistory> historyCaptor;
    @Captor protected ArgumentCaptor<ShipmentAddon> addonCaptor;

    // --- SHARED DATA FACTORY METHODS ---

    protected Client createMockClient(UUID id, String firstName, String lastName) {
        Client client = new Client();
        client.setId(id);
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setPhoneNumber("0888123456");
        return client;
    }

    protected Courier createMockCourier(UUID id, String firstName, String lastName) {
        Courier courier = new Courier();
        courier.setId(id);
        courier.setFirstName(firstName);
        courier.setLastName(lastName);
        courier.setEmployeeNumber("EMP-" + id.toString().substring(0, 4));
        courier.setHireDate(LocalDate.now());
        courier.setSalary(BigDecimal.valueOf(2000));
        courier.setApplicationRole(ApplicationRole.COURIER);
        return courier;
    }

    protected OfficeClerk createMockOfficeClerk(UUID id, String firstName, String lastName) {
        OfficeClerk clerk = new OfficeClerk();
        clerk.setId(id);
        clerk.setFirstName(firstName);
        clerk.setLastName(lastName);
        clerk.setEmployeeNumber("OC-" + id.toString().substring(0, 4));
        clerk.setHireDate(LocalDate.now());
        clerk.setSalary(BigDecimal.valueOf(1500));
        clerk.setApplicationRole(ApplicationRole.CLERK);
        clerk.setOffice(createMockOffice(1L, createMockCity(1L, "TestCity", "1000")));
        return clerk;
    }

    protected City createMockCity(Long id, String name, String postcode) {
        City city = new City();
        city.setId(id);
        city.setName(name);
        city.setPostcode(postcode);
        return city;
    }

    protected Office createMockOffice(Long id, City city) {
        Office office = new Office();
        office.setId(id);
        AddressDetails address = new AddressDetails();
        address.setCity(city);
        address.setStreet("Office Street 1");
        office.setAddressDetails(address);
        return office;
    }

    protected AddressDetails createMockAddressDetails() {
        AddressDetails address = new AddressDetails();
        address.setCity(createMockCity(1L, "Sofia", "1000"));
        address.setStreet("Some Street 123");
        address.setBuilding("1A");
        return address;
    }

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

    protected ShipmentCreationDto.ShipmentCreationDtoBuilder baseCreationDtoBuilder(UUID senderId, UUID receiverId) {
        return ShipmentCreationDto.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(ShipmentType.PARCEL)
                .weight(BigDecimal.valueOf(2.5))
                .originOfficeId(5L)
                .paidBy(PaidBy.SENDER);
    }

    protected Shipment createValidShipment() {
        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setTrackingNumber("TRK-TEST");
        shipment.setStatus(ShipmentStatus.REGISTERED);
        shipment.setSender(createMockClient(UUID.randomUUID(), "A", "A"));
        shipment.setReceiver(createMockClient(UUID.randomUUID(), "B", "B"));
        shipment.setRegisteredBy(createMockCourier(UUID.randomUUID(), "C", "C"));
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
}
