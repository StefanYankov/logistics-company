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
import java.time.LocalDate;
import java.util.Optional;

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

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .sender(sender)
                .receiver(receiver)
                .registeredBy(employee)
                .type(ShipmentType.PARCEL)
                .weight(BigDecimal.valueOf(2.5))
                .totalPrice(BigDecimal.valueOf(10.50))
                .status(status)
                .deliveryAddressSnapshot(deliveryAddress)
                .build();
        shipmentRepository.saveAndFlush(shipment);
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
        @DisplayName("Should return paginated shipments sent by a specific client")
        void shouldReturnShipmentsBySender() {
            City city = createAndSaveCity("Plovdiv", "4000");
            Client targetSender = createAndSaveClient("senderA", "sendA@test.com");
            Client otherSender = createAndSaveClient("senderB", "sendB@test.com");
            Client receiver = createAndSaveClient("receiverA", "recvA@test.com");
            Courier employee = createAndSaveCourier("employeeA", "employeeA@test.com", "EMP-A");

            createAndSaveShipment("TR-01", targetSender, receiver, employee, ShipmentStatus.DELIVERED, city);
            createAndSaveShipment("TR-02", targetSender, receiver, employee, ShipmentStatus.IN_TRANSIT, city);
            createAndSaveShipment("TR-03", otherSender, receiver, employee, ShipmentStatus.REGISTERED, city);

            Page<Shipment> resultPage = shipmentRepository.findBySender_Id(targetSender.getId(), PageRequest.of(0, 10));

            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            assertThat(resultPage.getContent()).extracting(Shipment::getTrackingNumber).containsExactlyInAnyOrder("TR-01", "TR-02");
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

            Shipment duplicateShipment = Shipment.builder()
                    .trackingNumber("DUPLICATE-TRACK")
                    .sender(sender)
                    .receiver(receiver)
                    .registeredBy(employee)
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(5.0))
                    .totalPrice(BigDecimal.valueOf(20.00))
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
            Client receiver = createAndSaveClient("receiverSZ", "recvSZ@test.com");
            Courier employee = createAndSaveCourier("empSZ", "empSZ@test.com", "EMP-SZ");

            AddressDetails deliveryAddress = new AddressDetails();
            deliveryAddress.setCity(city);
            deliveryAddress.setStreet("Delivery Street 1");

            Shipment invalidShipment = Shipment.builder()
                    .trackingNumber("TRACK-NO-SENDER")
                    .sender(null)
                    .receiver(receiver)
                    .registeredBy(employee)
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(5.0))
                    .totalPrice(BigDecimal.valueOf(20.00))
                    .status(ShipmentStatus.REGISTERED)
                    .deliveryAddressSnapshot(deliveryAddress)
                    .build();

            assertThatThrownBy(() -> shipmentRepository.saveAndFlush(invalidShipment))
                    .isInstanceOf(Exception.class);
        }
        
        @Test
        @DisplayName("Error Case: Should throw Exception when missing tracking number")
        void shouldThrowOnMissingTrackingNumber() {
            City city = createAndSaveCity("Veliko Tarnovo", "7000");
            Client sender = createAndSaveClient("senderVT", "senderVT@test.com");
            Client receiver = createAndSaveClient("receiverVT", "recvVT@test.com");
            Courier employee = createAndSaveCourier("empVT", "empVT@test.com", "EMP-VT");

            AddressDetails deliveryAddress = new AddressDetails();
            deliveryAddress.setCity(city);
            deliveryAddress.setStreet("Delivery Street 1");

            Shipment invalidShipment = Shipment.builder()
                    .trackingNumber(null)
                    .sender(sender)
                    .receiver(receiver)
                    .registeredBy(employee)
                    .type(ShipmentType.PARCEL)
                    .weight(BigDecimal.valueOf(5.0))
                    .totalPrice(BigDecimal.valueOf(20.00))
                    .status(ShipmentStatus.REGISTERED)
                    .deliveryAddressSnapshot(deliveryAddress)
                    .build();

            assertThatThrownBy(() -> shipmentRepository.saveAndFlush(invalidShipment))
                    .isInstanceOf(Exception.class);
        }
    }
}
