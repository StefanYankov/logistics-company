package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.client.ClientRepository;
import bg.nbu.cscb532.employee.Courier;
import bg.nbu.cscb532.employee.EmployeeRepository;
import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.office.CityRepository;
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
    private EntityManager entityManager;

    // --- TEST DATA FACTORY ---

    private City createAndSaveCity(String name, String postcode) {
        City city = new City();
        city.setName(name);
        city.setPostcode(postcode);
        return cityRepository.saveAndFlush(city);
    }

    private Client createAndSaveClient(String username, String email) {
        Client client = new Client();
        client.setUsername(username);
        client.setEmail(email);
        client.setPassword("hashed");
        client.setFirstName("First");
        client.setLastName("Last");
        client.setPhoneNumber("0888123456");
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
        
        String nativeQuery = """
            INSERT INTO shipments (
                id, version, created_at, updated_at,
                sender_id, receiver_id, registered_by_id,
                tracking_number, shipment_type, weight, total_price, status,
                city_id, delivery_street, paid_by, is_paid
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
            City city = createAndSaveCity("Sofia", "1000");
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
            City city = createAndSaveCity("Sofia", "1000");
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
            City city = createAndSaveCity("Plovdiv", "4000");
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
            City city = createAndSaveCity("Varna", "4000");
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
            City city = createAndSaveCity("Burgas", "9000");
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
            City city = createAndSaveCity("Ruse", "8000");
            Client sender = createAndSaveClient("senderL", "sendL@test.com");
            Client receiver = createAndSaveClient("receiverL", "recvL@test.com");
            Courier employee = createAndSaveCourier("employeeL", "employeeL@test.com", "EMP-L");

            createAndSaveShipment("TR-31", sender, receiver, employee, ShipmentStatus.REGISTERED, city);
            createAndSaveShipment("TR-32", sender, receiver, employee, ShipmentStatus.IN_TRANSIT, city);
            createAndSaveShipment("TR-33", sender, receiver, employee, ShipmentStatus.DELIVERED, city);

            Page<Shipment> resultPage = shipmentRepository.findByStatusNot(ShipmentStatus.DELIVERED, PageRequest.of(0, 10));

            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            assertThat(resultPage.getContent()).extracting(Shipment::getTrackingNumber).containsExactlyInAnyOrder("TR-31", "TR-32");
        }

        @Test
        @DisplayName("calculateTotalRevenue: Should sum total prices within the strict time boundaries")
        void shouldCalculateRevenueInTimeWindow() {
            shipmentRepository.deleteAll();
            
            City city = createAndSaveCity("Vidin", "5000");
            Client sender = createAndSaveClient("senderRev", "sendRev@test.com");
            Client receiver = createAndSaveClient("receiverRev", "recvRev@test.com");
            Courier employee = createAndSaveCourier("employeeRev", "employeeRev@test.com", "EMP-REV");

            Instant now = Instant.now();
            Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
            Instant fourDaysAgo = now.minus(4, ChronoUnit.DAYS);

            // Expected in query window
            createAndSaveShipmentWithDate("REV-1", sender, receiver, employee, BigDecimal.valueOf(10.00), now, city);
            createAndSaveShipmentWithDate("REV-2", sender, receiver, employee, BigDecimal.valueOf(15.50), twoDaysAgo, city);

            // Expected outside query window
            createAndSaveShipmentWithDate("REV-3", sender, receiver, employee, BigDecimal.valueOf(100.00), fourDaysAgo, city);

            // The window is from 3 days ago up to right now
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
            shipmentRepository.deleteAll(); // Isolate test data

            City city = createAndSaveCity("Silistra", "3000");
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
            Instant startDate = Instant.now().minus(10, ChronoUnit.DAYS);
            Instant endDate = Instant.now().minus(9, ChronoUnit.DAYS);

            // DB has shipments, but none in this specific 1-day window
            BigDecimal revenue = shipmentRepository.calculateTotalRevenue(startDate, endDate);

            // The SQL SUM() function natively returns NULL if no rows are found
            assertThat(revenue).isNull();
        }
        
        @Test
        @DisplayName("calculateTotalRevenue: Error Case: Should return null when startDate is after endDate")
        void shouldHandleImpossibleDateRanges() {
            City city = createAndSaveCity("Shumen", "7000");
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
            City city = createAndSaveCity("Pleven", "7000");
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
            City city = createAndSaveCity("Stara Zagora", "5000");
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
            City city = createAndSaveCity("Pernik", "5800");
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
            City city = createAndSaveCity("Veliko Tarnovo", "7000");
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
}