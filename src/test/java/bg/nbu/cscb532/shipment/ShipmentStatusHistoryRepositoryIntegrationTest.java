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
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ShipmentStatusHistoryRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private ShipmentStatusHistoryRepository historyRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CityRepository cityRepository;

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

    private Client createAndSaveClient(String username, String email) {
        Client client = new Client();
        client.setUsername(username);
        client.setEmail(email);
        client.setPassword("hashed");
        client.setFirstName("First");
        client.setLastName("Last");
        client.setPhoneNumber(UUID.randomUUID().toString().substring(0, 10));
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

    private Shipment createAndSaveShipment(String trackingNumber, Client sender, Client receiver, Courier employee, ShipmentStatus status, City deliveryCity) {
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
        return shipmentRepository.saveAndFlush(shipment);
    }

    private void createAndSaveHistory(Shipment shipment, ShipmentStatus status, String notes) {
        ShipmentStatusHistory history = ShipmentStatusHistory.builder()
                .shipment(shipment)
                .status(status)
                .notes(notes)
                .build();
        historyRepository.saveAndFlush(history);
    }

    @Nested
    @DisplayName("findByShipment_IdOrderByCreatedAtDesc(UUID) Tests")
    class FindByShipmentIdTests {

        @Test
        @DisplayName("Happy Path: Should retrieve complete chronological history for a specific shipment")
        void shouldRetrieveChronologicalHistory() throws InterruptedException {
            // Arrange
            City city = getOrCreateCity("Sofia");
            Client sender = createAndSaveClient("sender1", "sender1@test.com");
            Client receiver = createAndSaveClient("receiver1", "receiver1@test.com");
            Courier employee = createAndSaveCourier("emp1", "emp1@test.com", "EMP-001");

            Shipment targetShipment = createAndSaveShipment("TRACK-123", sender, receiver, employee, ShipmentStatus.REGISTERED, city);
            Shipment otherShipment = createAndSaveShipment("TRACK-456", sender, receiver, employee, ShipmentStatus.REGISTERED, city);

            createAndSaveHistory(targetShipment, ShipmentStatus.REGISTERED, "Package accepted at office.");
            Thread.sleep(10);
            createAndSaveHistory(targetShipment, ShipmentStatus.IN_TRANSIT, "Package loaded onto truck.");
            Thread.sleep(10);
            createAndSaveHistory(targetShipment, ShipmentStatus.DELIVERED, "Package delivered to recipient.");
            createAndSaveHistory(otherShipment, ShipmentStatus.REGISTERED, "Other package accepted.");

            // Act
            List<ShipmentStatusHistory> results = historyRepository.findByShipment_IdOrderByCreatedAtDesc(targetShipment.getId());

            // Assert
            assertThat(results).isNotNull();
            assertThat(results).hasSize(3);
            assertThat(results.get(0).getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
            assertThat(results.get(0).getNotes()).isEqualTo("Package delivered to recipient.");
            assertThat(results.get(1).getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
            assertThat(results.get(2).getStatus()).isEqualTo(ShipmentStatus.REGISTERED);

            assertThat(results).noneMatch(h -> h.getShipment().getTrackingNumber().equals("TRACK-456"));
        }

        @Test
        @DisplayName("Edge Case: Should return empty list when shipment has no history or does not exist")
        void shouldReturnEmptyListWhenNoHistory() {
            // Arrange
            City city = getOrCreateCity("Plovdiv");
            Client sender = createAndSaveClient("sender2", "sender2@test.com");
            Client receiver = createAndSaveClient("receiver2", "receiver2@test.com");
            Courier employee = createAndSaveCourier("emp2", "emp2@test.com", "EMP-002");

            Shipment shipment = createAndSaveShipment("TRACK-789", sender, receiver, employee, ShipmentStatus.REGISTERED, city);

            // Act
            List<ShipmentStatusHistory> results = historyRepository.findByShipment_IdOrderByCreatedAtDesc(shipment.getId());

            // Assert
            assertThat(results).isNotNull().isEmpty();
        }
    }
}
