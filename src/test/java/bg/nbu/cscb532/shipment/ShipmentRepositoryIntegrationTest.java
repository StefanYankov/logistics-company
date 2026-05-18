package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.client.ClientRepository;
import bg.nbu.cscb532.company.Company;
import bg.nbu.cscb532.company.CompanyRepository;
import bg.nbu.cscb532.employee.Courier;
import bg.nbu.cscb532.employee.EmployeeRepository;
import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.office.CityRepository;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.office.OfficeRepository;
import bg.nbu.cscb532.shared.config.JpaConfig;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.user.ApplicationRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ShipmentRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EntityManager entityManager;

    // --- TEST DATA FACTORY ---
    
    private City getOrCreateCity(String name) {
        String uniquePostcode = UUID.randomUUID().toString().substring(0, 5);
        return cityRepository.findByPostcode(uniquePostcode).orElseGet(() -> {
            City city = new City();
            city.setName(name);
            city.setPostcode(uniquePostcode);
            return cityRepository.saveAndFlush(city);
        });
    }

    private Company getOrCreateCompany() {
        return companyRepository.findAll().stream().findFirst().orElseGet(() -> {
            Company company = new Company();
            company.setName("Logistics Corp");
            company.setRegistrationNumber("BG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            return companyRepository.saveAndFlush(company);
        });
    }

    private Office createAndSaveOffice(City city, String street) {
        Company company = getOrCreateCompany();

        Office office = new Office();
        AddressDetails address = new AddressDetails();
        address.setCity(city);
        address.setStreet(street);
        office.setAddressDetails(address);
        office.setCompany(company);
        return officeRepository.saveAndFlush(office);
    }

    private Client createAndSaveClient(String username, String email) {
        Client client = new Client();
        client.setUsername(username);
        client.setEmail(email);
        client.setPassword("hashed");
        client.setFirstName("First");
        client.setLastName("Last");
        client.setPhoneNumber(UUID.randomUUID().toString().substring(0, 10)); // Unique phone
        client.setApplicationRole(ApplicationRole.CLIENT);
        client.setActive(true);
        return clientRepository.saveAndFlush(client);
    }

    private Courier createAndSaveCourier(String username, String email, String empNumber) {
        Courier courier = new Courier();
        courier.setUsername(username);
        courier.setEmail(email);
        courier.setPassword("hashed");
        courier.setFirstName("First");
        courier.setLastName("Last");
        courier.setEmployeeNumber(empNumber);
        courier.setHireDate(LocalDate.now());
        courier.setSalary(BigDecimal.valueOf(2000));
        courier.setApplicationRole(ApplicationRole.COURIER);
        courier.setActive(true);
        return employeeRepository.saveAndFlush(courier);
    }

    private void createAndSaveShipment(String trackingNumber, Client sender, Client receiver, Courier employee, ShipmentStatus status, City deliveryCity) {
        AddressDetails deliveryAddress = new AddressDetails();
        deliveryAddress.setCity(deliveryCity);
        deliveryAddress.setStreet("Delivery Street 1");

        PackageDetails packageDetails = PackageDetails.builder()
                .type(ShipmentType.PARCEL)
                .weight(BigDecimal.valueOf(2.5))
                .build();

        ShipmentFinancials financials = ShipmentFinancials.builder()
                .totalPrice(BigDecimal.valueOf(10.50))
                .paidBy(PaidBy.SENDER)
                .isPaid(false)
                .build();

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .sender(sender)
                .receiver(receiver)
                .registeredBy(employee)
                .packageDetails(packageDetails)
                .financials(financials)
                .status(status)
                .deliveryAddressSnapshot(deliveryAddress)
                .build();
        shipmentRepository.saveAndFlush(shipment);
    }

    private void createAndSaveGuestShipment(String trackingNumber, Client sender, Courier employee, ShipmentStatus status, City deliveryCity) {
        AddressDetails deliveryAddress = new AddressDetails();
        deliveryAddress.setCity(deliveryCity);
        deliveryAddress.setStreet("Delivery Street 1");

        PackageDetails packageDetails = PackageDetails.builder()
                .type(ShipmentType.PARCEL)
                .weight(BigDecimal.valueOf(2.5))
                .build();

        ShipmentFinancials financials = ShipmentFinancials.builder()
                .totalPrice(BigDecimal.valueOf(10.50))
                .paidBy(PaidBy.SENDER)
                .isPaid(false)
                .build();

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .sender(sender)
                .receiverName("Guest Mom")
                .receiverPhone("0888999999")
                .registeredBy(employee)
                .packageDetails(packageDetails)
                .financials(financials)
                .status(status)
                .deliveryAddressSnapshot(deliveryAddress)
                .build();
        shipmentRepository.saveAndFlush(shipment);
    }

    private void createAndSaveShipmentWithDate(String trackingNumber, Client sender, Client receiver, Courier employee, BigDecimal price, Instant createdAt, City city) {
        UUID newId = UUID.randomUUID();
        @SuppressWarnings("SqlResolve")
        String nativeQuery = """
            INSERT INTO shipments (
                id, version, created_at, updated_at,
                sender_id, receiver_id, registered_by_id,
                tracking_number, shipment_type, weight, total_price, status,
                delivery_city_id, delivery_street, paid_by, is_paid
            ) VALUES (
                :id, 0, :createdAt, :updatedAt,
                :senderId, :receiverId, :employeeId,
                :trackingNumber, 'PARCEL', 2.500, :price, 'REGISTERED',
                :cityId, 'Delivery Street', 'SENDER', false
            )
        """;

        entityManager.createNativeQuery(nativeQuery)
                .setParameter("id", newId)
                .setParameter("createdAt", createdAt)
                .setParameter("updatedAt", createdAt)
                .setParameter("senderId", sender.getId())
                .setParameter("receiverId", receiver.getId())
                .setParameter("employeeId", employee.getId())
                .setParameter("trackingNumber", trackingNumber)
                .setParameter("price", price)
                .setParameter("cityId", city.getId())
                .executeUpdate();
    }

    @Nested
    @DisplayName("findByTrackingNumber(String) Tests")
    class FindByTrackingNumberTests {

        @Test
        @DisplayName("Happy Path: Should find shipment when exact tracking number exists")
        void shouldFindShipmentWhenNumberExists() {
            City city = getOrCreateCity("Sofia");
            Client sender = createAndSaveClient("sender1", "sender1@test.com");
            Client receiver = createAndSaveClient("receiver1", "receiver1@test.com");
            Courier employee = createAndSaveCourier("emp1", "emp1@test.com", "EMP-001");
            createAndSaveShipment("TRACK-123", sender, receiver, employee, ShipmentStatus.REGISTERED, city);

            Optional<Shipment> result = shipmentRepository.findByTrackingNumber("TRACK-123");

            assertThat(result).isPresent();
            assertThat(result.get().getTrackingNumber()).isEqualTo("TRACK-123");
        }

        @Test
        @DisplayName("Error Case: Should return empty when tracking number does not exist")
        void shouldReturnEmptyWhenNumberNotFound() {
            Optional<Shipment> result = shipmentRepository.findByTrackingNumber("NON-EXISTENT");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Edge Case: Should return empty when case differs")
        void shouldReturnEmptyWhenCaseIsDifferent() {
            City city = getOrCreateCity("Sofia");
            Client sender = createAndSaveClient("sender2", "sender2@test.com");
            Client receiver = createAndSaveClient("receiver2", "receiver2@test.com");
            Courier employee = createAndSaveCourier("emp2", "emp2@test.com", "EMP-002");
            createAndSaveShipment("TRACK-456", sender, receiver, employee, ShipmentStatus.REGISTERED, city);

            Optional<Shipment> result = shipmentRepository.findByTrackingNumber("track-456");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Complex Reporting Queries Tests")
    class ReportingQueriesTests {

        @Test
        @DisplayName("Should return paginated shipments sent by a specific client (including guest receivers)")
        void shouldReturnShipmentsBySender() {
            City city = getOrCreateCity("Plovdiv");
            Client targetSender = createAndSaveClient("senderA", "sendA@test.com");
            Client otherSender = createAndSaveClient("senderB", "sendB@test.com");
            Client receiver = createAndSaveClient("receiverA", "recvA@test.com");
            Courier employee = createAndSaveCourier("employeeA", "employeeA@test.com", "EMP-A");

            createAndSaveShipment("TR-01", targetSender, receiver, employee, ShipmentStatus.DELIVERED, city);
            createAndSaveShipment("TR-02", targetSender, receiver, employee, ShipmentStatus.IN_TRANSIT, city);
            createAndSaveShipment("TR-03", otherSender, receiver, employee, ShipmentStatus.REGISTERED, city);
            createAndSaveGuestShipment("TR-04", targetSender, employee, ShipmentStatus.REGISTERED, city);

            Page<Shipment> resultPage = shipmentRepository.findBySender_Id(targetSender.getId(), PageRequest.of(0, 10));

            assertThat(resultPage.getTotalElements()).isEqualTo(3);
            assertThat(resultPage.getContent()).extracting(Shipment::getTrackingNumber).containsExactlyInAnyOrder("TR-01", "TR-02", "TR-04");
        }

        @Test
        @DisplayName("Should return paginated shipments received by a specific client")
        void shouldReturnShipmentsByReceiver() {
            City city = getOrCreateCity("Varna");
            Client sender = createAndSaveClient("senderX", "sendX@test.com");
            Client targetReceiver = createAndSaveClient("receiverX", "recvX@test.com");
            Client otherReceiver = createAndSaveClient("receiverY", "recvY@test.com");
            Courier employee = createAndSaveCourier("employeeX", "employeeX@test.com", "EMP-X");

            createAndSaveShipment("TR-11", sender, targetReceiver, employee, ShipmentStatus.DELIVERED, city);
            createAndSaveShipment("TR-12", sender, otherReceiver, employee, ShipmentStatus.IN_TRANSIT, city);

            Page<Shipment> resultPage = shipmentRepository.findByReceiver_Id(targetReceiver.getId(), PageRequest.of(0, 10));

            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            assertThat(resultPage.getContent().getFirst().getTrackingNumber()).isEqualTo("TR-11");
        }

        @Test
        @DisplayName("Should return paginated shipments registered by a specific employee")
        void shouldReturnShipmentsByEmployee() {
            City city = getOrCreateCity("Burgas");
            Client sender = createAndSaveClient("senderZ", "sendZ@test.com");
            Client receiver = createAndSaveClient("receiverZ", "recvZ@test.com");
            Courier targetEmployee = createAndSaveCourier("employeeZ", "employeeZ@test.com", "EMP-Z");
            Courier otherEmployee = createAndSaveCourier("employeeW", "employeeW@test.com", "EMP-W");

            createAndSaveShipment("TR-21", sender, receiver, targetEmployee, ShipmentStatus.REGISTERED, city);
            createAndSaveShipment("TR-22", sender, receiver, otherEmployee, ShipmentStatus.REGISTERED, city);

            Page<Shipment> resultPage = shipmentRepository.findByRegisteredBy_Id(targetEmployee.getId(), PageRequest.of(0, 10));

            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            assertThat(resultPage.getContent().getFirst().getTrackingNumber()).isEqualTo("TR-21");
        }

        @Test
        @DisplayName("Should return shipments that are NOT in the specified status (e.g., Not Delivered)")
        void shouldReturnShipmentsNotDelivered() {
            City city = getOrCreateCity("Ruse");
            Client sender = createAndSaveClient("senderL", "sendL@test.com");
            Client receiver = createAndSaveClient("receiverL", "recvL@test.com");
            Courier employee = createAndSaveCourier("employeeL", "employeeL@test.com", "EMP-L");

            createAndSaveShipment("TR-31", sender, receiver, employee, ShipmentStatus.REGISTERED, city);
            createAndSaveShipment("TR-32", sender, receiver, employee, ShipmentStatus.IN_TRANSIT, city);
            createAndSaveShipment("TR-33", sender, receiver, employee, ShipmentStatus.DELIVERED, city);

            Page<Shipment> resultPage = shipmentRepository.findByStatusNot(ShipmentStatus.DELIVERED, PageRequest.of(0, 10));

            assertThat(resultPage.getContent()).extracting(Shipment::getTrackingNumber).contains("TR-31", "TR-32");
            assertThat(resultPage.getContent()).extracting(Shipment::getTrackingNumber).doesNotContain("TR-33");
        }

        @Test
        @DisplayName("calculateTotalRevenue: Should sum total prices within the strict time boundaries")
        void shouldCalculateRevenueInTimeWindow() {
            shipmentRepository.deleteAll();
            
            City city = getOrCreateCity("Vidin");
            Client sender = createAndSaveClient("senderRev", "sendRev@test.com");
            Client receiver = createAndSaveClient("receiverRev", "recvRev@test.com");
            Courier employee = createAndSaveCourier("employeeRev", "employeeRev@test.com", "EMP-REV");

            Instant now = Instant.now();
            Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
            Instant fourDaysAgo = now.minus(4, ChronoUnit.DAYS);

            createAndSaveShipmentWithDate("REV-1", sender, receiver, employee, BigDecimal.valueOf(10.00), now, city);
            createAndSaveShipmentWithDate("REV-2", sender, receiver, employee, BigDecimal.valueOf(15.50), twoDaysAgo, city);

            createAndSaveShipmentWithDate("REV-3", sender, receiver, employee, BigDecimal.valueOf(100.00), fourDaysAgo, city);

            Instant startDate = now.minus(3, ChronoUnit.DAYS);
            Instant endDate = now.plus(1, ChronoUnit.MINUTES);

            BigDecimal revenue = shipmentRepository.calculateTotalRevenue(startDate, endDate);

            // Assert
            // Expecting 10.00 + 15.50 = 25.50
            assertThat(revenue).isEqualByComparingTo(BigDecimal.valueOf(25.50));
        }

        @Test
        @DisplayName("calculateTotalRevenue: Edge Case: Should be fully inclusive of the exact boundary seconds")
        void shouldBeInclusiveOfBoundaryDates() {
            shipmentRepository.deleteAll();

            City city = getOrCreateCity("Silistra");
            Client sender = createAndSaveClient("senderB", "sendB@test.com");
            Client receiver = createAndSaveClient("receiverB", "recvB@test.com");
            Courier employee = createAndSaveCourier("employeeB", "employeeB@test.com", "EMP-B");

            Instant startDate = Instant.parse("2026-01-01T00:00:00Z");
            Instant endDate = Instant.parse("2026-01-31T23:59:59Z");

            createAndSaveShipmentWithDate("BOUND-1", sender, receiver, employee, BigDecimal.valueOf(50.00), startDate, city);
            createAndSaveShipmentWithDate("BOUND-2", sender, receiver, employee, BigDecimal.valueOf(20.00), endDate, city);

            BigDecimal revenue = shipmentRepository.calculateTotalRevenue(startDate, endDate);

            // Assert
            assertThat(revenue).isEqualByComparingTo(BigDecimal.valueOf(70.00));
        }

        @Test
        @DisplayName("calculateTotalRevenue: Edge Case: Should return null when no shipments found in window")
        void shouldReturnNullWhenNoRevenueInWindow() {
            // Arrange
            Instant startDate = Instant.now().minus(10, ChronoUnit.DAYS);
            Instant endDate = Instant.now().minus(9, ChronoUnit.DAYS);

            // Act
            BigDecimal revenue = shipmentRepository.calculateTotalRevenue(startDate, endDate);

            // Assert
            // The SQL SUM() function natively returns NULL
            assertThat(revenue).isNull();
        }
        
        @Test
        @DisplayName("calculateTotalRevenue: Error Case: Should return null when startDate is after endDate")
        void shouldHandleImpossibleDateRanges() {
            City city = getOrCreateCity("Shumen");
            Client sender = createAndSaveClient("senderImp", "sendImp@test.com");
            Client receiver = createAndSaveClient("receiverImp", "recvImp@test.com");
            Courier employee = createAndSaveCourier("employeeImp", "employeeImp@test.com", "EMP-IMP");

            Instant now = Instant.now();
            createAndSaveShipmentWithDate("IMP-1", sender, receiver, employee, BigDecimal.valueOf(10.00), now, city);

            Instant startDate = now.plus(1, ChronoUnit.DAYS);
            Instant endDate = now.minus(1, ChronoUnit.DAYS);

            BigDecimal revenue = shipmentRepository.calculateTotalRevenue(startDate, endDate);

            assertThat(revenue).isNull();
        }
    }

    @Nested
    @DisplayName("Database Constraint Tests")
    class ConstraintTests {

        @Test
        @DisplayName("Error Case: Should throw DataIntegrityViolationException on duplicate tracking number")
        void shouldThrowOnDuplicateTrackingNumber() {
            City city = getOrCreateCity("Pleven");
            Client sender = createAndSaveClient("senderD", "sendD@test.com");
            Client receiver = createAndSaveClient("receiverD", "recvD@test.com");
            Courier employee = createAndSaveCourier("employeeD", "employeeD@test.com", "EMP-D");

            createAndSaveShipment("DUPLICATE-TRACK", sender, receiver, employee, ShipmentStatus.REGISTERED, city);

            AddressDetails deliveryAddress = new AddressDetails();
            deliveryAddress.setCity(city);
            deliveryAddress.setStreet("Delivery Street 1");

            PackageDetails packageDetails = PackageDetails.builder()
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(5.0))
                    .build();

            ShipmentFinancials financials = ShipmentFinancials.builder()
                    .totalPrice(BigDecimal.valueOf(20.00))
                    .paidBy(PaidBy.SENDER)
                    .isPaid(false)
                    .build();

            Shipment duplicateShipment = Shipment.builder()
                    .trackingNumber("DUPLICATE-TRACK")
                    .sender(sender)
                    .receiver(receiver)
                    .registeredBy(employee)
                    .packageDetails(packageDetails)
                    .financials(financials)
                    .status(ShipmentStatus.REGISTERED)
                    .deliveryAddressSnapshot(deliveryAddress)
                    .build();

            assertThatThrownBy(() -> shipmentRepository.saveAndFlush(duplicateShipment))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("tracking_number");
        }

        @Test
        @DisplayName("Error Case: Should throw Exception when sender is missing")
        void shouldThrowOnMissingSender() {
            City city = getOrCreateCity("Stara Zagora");
            Client receiver = createAndSaveClient("receiver SZ", "recvSZ@test.com");
            Courier employee = createAndSaveCourier("employee SZ", "empSZ@test.com", "EMP-SZ");

            AddressDetails deliveryAddress = new AddressDetails();
            deliveryAddress.setCity(city);
            deliveryAddress.setStreet("Delivery Street 1");

            PackageDetails packageDetails = PackageDetails.builder()
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(5.0))
                    .build();

            ShipmentFinancials financials = ShipmentFinancials.builder()
                    .totalPrice(BigDecimal.valueOf(20.00))
                    .paidBy(PaidBy.SENDER)
                    .isPaid(false)
                    .build();

            Shipment invalidShipment = Shipment.builder()
                    .trackingNumber("TRACK-NO-SENDER")
                    .sender(null)
                    .receiver(receiver)
                    .registeredBy(employee)
                    .packageDetails(packageDetails)
                    .financials(financials)
                    .status(ShipmentStatus.REGISTERED)
                    .deliveryAddressSnapshot(deliveryAddress)
                    .build();

            assertThatThrownBy(() -> shipmentRepository.saveAndFlush(invalidShipment))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Happy Path: Should successfully save a shipment when receiver is missing (Guest Receiver)")
        void shouldSaveOnMissingReceiver() {
            City city = getOrCreateCity("Pernik");
            Client sender = createAndSaveClient("sender Guest", "senderGuest@test.com");
            Courier employee = createAndSaveCourier("employee Guest", "empGuest@test.com", "EMP-GST");

            AddressDetails deliveryAddress = new AddressDetails();
            deliveryAddress.setCity(city);
            deliveryAddress.setStreet("Delivery Street 1");

            PackageDetails packageDetails = PackageDetails.builder()
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(5.0))
                    .build();

            ShipmentFinancials financials = ShipmentFinancials.builder()
                    .totalPrice(BigDecimal.valueOf(20.00))
                    .paidBy(PaidBy.SENDER)
                    .isPaid(false)
                    .build();

            Shipment guestShipment = Shipment.builder()
                    .trackingNumber("TRACK-GUEST")
                    .sender(sender)
                    .receiver(null)
                    .receiverName("Guest Name")
                    .receiverPhone("0888111222")
                    .registeredBy(employee)
                    .packageDetails(packageDetails)
                    .financials(financials)
                    .status(ShipmentStatus.REGISTERED)
                    .deliveryAddressSnapshot(deliveryAddress)
                    .build();

            Shipment saved = shipmentRepository.saveAndFlush(guestShipment);
            
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getReceiver()).isNull();
            assertThat(saved.getReceiverName()).isEqualTo("Guest Name");
        }

        @Test
        @DisplayName("Error Case: Should throw Exception when missing tracking number")
        void shouldThrowOnMissingTrackingNumber() {
            City city = getOrCreateCity("Veliko Tarnovo");
            Client sender = createAndSaveClient("sender VT", "senderVT@test.com");
            Client receiver = createAndSaveClient("receiver VT", "recvVT@test.com");
            Courier employee = createAndSaveCourier("employee VT", "empVT@test.com", "EMP-VT");

            AddressDetails deliveryAddress = new AddressDetails();
            deliveryAddress.setCity(city);
            deliveryAddress.setStreet("Delivery Street 1");

            PackageDetails packageDetails = PackageDetails.builder()
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(5.0))
                    .build();

            ShipmentFinancials financials = ShipmentFinancials.builder()
                    .totalPrice(BigDecimal.valueOf(20.00))
                    .paidBy(PaidBy.SENDER)
                    .isPaid(false)
                    .build();

            Shipment invalidShipment = Shipment.builder()
                    .trackingNumber(null)
                    .sender(sender)
                    .receiver(receiver)
                    .registeredBy(employee)
                    .packageDetails(packageDetails)
                    .financials(financials)
                    .status(ShipmentStatus.REGISTERED)
                    .deliveryAddressSnapshot(deliveryAddress)
                    .build();

            assertThatThrownBy(() -> shipmentRepository.saveAndFlush(invalidShipment))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("findByCurrentCourier_IdAndStatus Tests")
    class FindByCurrentCourierAndStatusTests {

        @Test
        @DisplayName("Should return only shipments assigned to the specific courier and with the correct status")
        void shouldReturnShipmentsForCourierAndStatus() {
            // Arrange
            City city = getOrCreateCity("Test City");
            Client sender = createAndSaveClient("sender-courier-test", "sender-ct@test.com");
            Client receiver = createAndSaveClient("receiver-courier-test", "receiver-ct@test.com");
            Courier courier1 = createAndSaveCourier("courier1", "courier1@test.com", "C-001");
            Courier courier2 = createAndSaveCourier("courier2", "courier2@test.com", "C-002");

            createAndSaveShipment("COURIER1-DELIVERY", sender, receiver, courier1, ShipmentStatus.OUT_FOR_DELIVERY, city);
            createAndSaveShipment("COURIER1-REGISTERED", sender, receiver, courier1, ShipmentStatus.REGISTERED, city);
            createAndSaveShipment("COURIER2-DELIVERY", sender, receiver, courier2, ShipmentStatus.OUT_FOR_DELIVERY, city);

            shipmentRepository.findByTrackingNumber("COURIER1-DELIVERY").ifPresent(s -> {
                s.setCurrentCourier(courier1);
                shipmentRepository.saveAndFlush(s);
            });
            shipmentRepository.findByTrackingNumber("COURIER1-REGISTERED").ifPresent(s -> {
                s.setCurrentCourier(courier1);
                shipmentRepository.saveAndFlush(s);
            });
            shipmentRepository.findByTrackingNumber("COURIER2-DELIVERY").ifPresent(s -> {
                s.setCurrentCourier(courier2);
                shipmentRepository.saveAndFlush(s);
            });


            // Act
            Page<Shipment> resultPage = shipmentRepository.findByCurrentCourier_IdAndStatus(courier1.getId(), ShipmentStatus.OUT_FOR_DELIVERY, PageRequest.of(0, 10));

            // Assert
            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            assertThat(resultPage.getContent().getFirst().getTrackingNumber()).isEqualTo("COURIER1-DELIVERY");
        }

        @Test
        @DisplayName("Verification: Should locate address pickups when origin office is null")
        void shouldLocateAddressPickupsWhenOfficeNull() {
            // Arrange
            City city = getOrCreateCity("Dobrich");
            Client sender = createAndSaveClient("sender-pickup", "pickup@test.com");
            Client receiver = createAndSaveClient("receiver-pickup", "recv-p@test.com");
            Courier courier = createAndSaveCourier("courier-p", "courier-p@test.com", "C-777");

            createAndSaveShipment("OFFICE-DROP", sender, receiver, courier, ShipmentStatus.REGISTERED, city);
            Office originOffice = createAndSaveOffice(city, "Main Office St");
            shipmentRepository.findByTrackingNumber("OFFICE-DROP").ifPresent(s -> {
                s.setCurrentCourier(courier);
                s.setOriginOffice(originOffice);
                shipmentRepository.saveAndFlush(s);
            });

            createAndSaveShipment("ADDRESS-PICKUP", sender, receiver, courier, ShipmentStatus.REGISTERED, city);
            shipmentRepository.findByTrackingNumber("ADDRESS-PICKUP").ifPresent(s -> {
                s.setCurrentCourier(courier);
                s.setOriginOffice(null);
                shipmentRepository.saveAndFlush(s);
            });

            // Act
            Page<Shipment> results = shipmentRepository.findByCurrentCourier_IdAndStatusAndOriginOfficeIsNull(
                    courier.getId(), ShipmentStatus.REGISTERED, PageRequest.of(0, 10)
            );

            // Assert
            assertThat(results.getTotalElements()).isEqualTo(1);
            assertThat(results.getContent().getFirst().getTrackingNumber()).isEqualTo("ADDRESS-PICKUP");
        }

        @Test
        @DisplayName("Should return empty page when no shipments match courier and status")
        void shouldReturnEmptyPageForNonMatchingCourierAndStatus() {
            // Arrange
            City city = getOrCreateCity("Another City");
            Client sender = createAndSaveClient("sender-no-match", "sender-nm@test.com");
            Client receiver = createAndSaveClient("receiver-no-match", "receiver-nm@test.com");
            Courier courier1 = createAndSaveCourier("courier-match", "courier-match@test.com", "C-003");
            Courier courier2 = createAndSaveCourier("courier-no-match", "courier-no-match@test.com", "C-004");

            createAndSaveShipment("NO-MATCH-DELIVERY", sender, receiver, courier1, ShipmentStatus.OUT_FOR_DELIVERY, city);
            shipmentRepository.findByTrackingNumber("NO-MATCH-DELIVERY").ifPresent(s -> {
                s.setCurrentCourier(courier1);
                shipmentRepository.saveAndFlush(s);
            });

            // Act
            Page<Shipment> resultPage = shipmentRepository.findByCurrentCourier_IdAndStatus(courier2.getId(), ShipmentStatus.OUT_FOR_DELIVERY, PageRequest.of(0, 10));

            // Assert
            assertThat(resultPage).isEmpty();
            assertThat(resultPage.getTotalElements()).isEqualTo(0);
        }
    }
}