package bg.nbu.cscb532.client;

import bg.nbu.cscb532.shared.config.JpaConfig;
import bg.nbu.cscb532.user.ApplicationRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClientRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private ClientRepository clientRepository;

    private void createAndSaveClient(String username, String email, String firstName, String lastName, String phone) {
        Client client = new Client();
        client.setUsername(username);
        client.setEmail(email);
        client.setPassword("hashed");
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setPhoneNumber(phone);
        client.setApplicationRole(ApplicationRole.CLIENT);
        client.setActive(true);
        clientRepository.saveAndFlush(client);
    }

    @Nested
    @DisplayName("findByPhoneNumber(String) Tests")
    class FindByPhoneNumberTests {
        @Test
        @DisplayName("Happy Path: Should find client by exact phone number")
        void shouldFindClientByExactPhoneNumber() {
            // Arrange
            String targetPhone = "0888123456";
            createAndSaveClient("user1", "u1@test.com", "John", "Doe", targetPhone);
            createAndSaveClient("user2", "u2@test.com", "Jane", "Smith", "0888999888");

            // Act
            Optional<Client> result = clientRepository.findByPhoneNumber(targetPhone);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getPhoneNumber()).isEqualTo(targetPhone);
            assertThat(result.get().getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("Error Case: Should return empty when phone number does not exist")
        void shouldReturnEmptyWhenPhoneNumberNotFound() {
            // Arrange
            createAndSaveClient("user1", "u1@test.com", "John", "Doe", "0888111222");

            // Act
            Optional<Client> result = clientRepository.findByPhoneNumber("0999999999");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchClients(String) Tests")
    class SearchClientsTests {
        @Test
        @DisplayName("Happy Path: Should find clients by partial first name, case-insensitive")
        void shouldSearchByPartialFirstName() {
            createAndSaveClient("user1", "u1@test.com", "Alexander", "Smith", "0888111222");
            createAndSaveClient("user2", "u2@test.com", "Alexandra", "Jones", "0888333444");
            createAndSaveClient("user3", "u3@test.com", "Bob", "Smith", "0888555666");

            Page<Client> result = clientRepository.searchClients("alex", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(Client::getFirstName).containsExactlyInAnyOrder("Alexander", "Alexandra");
        }

        @Test
        @DisplayName("Happy Path: Should find clients by partial last name, case-insensitive")
        void shouldSearchByPartialLastName() {
            createAndSaveClient("user1", "u1@test.com", "John", "MacDonald", "0888111222");
            createAndSaveClient("user2", "u2@test.com", "Jane", "MacArthur", "0888333444");
            createAndSaveClient("user3", "u3@test.com", "Bob", "Smith", "0888555666");

            Page<Client> result = clientRepository.searchClients("mac", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(Client::getLastName).containsExactlyInAnyOrder("MacDonald", "MacArthur");
        }

        @Test
        @DisplayName("Happy Path: Should find clients by partial phone number")
        void shouldSearchByPartialPhone() {
            createAndSaveClient("user1", "u1@test.com", "A", "B", "0888123456");
            createAndSaveClient("user2", "u2@test.com", "C", "D", "0888987654");
            createAndSaveClient("user3", "u3@test.com", "E", "F", "0899123999");

            Page<Client> result = clientRepository.searchClients("123", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(Client::getPhoneNumber).containsExactlyInAnyOrder("0888123456", "0899123999");
        }

        @Test
        @DisplayName("Edge Case: Should return empty page when no match is found")
        void shouldReturnEmptyWhenNoMatch() {
            createAndSaveClient("user1", "u1@test.com", "John", "Doe", "0888111222");

            Page<Client> result = clientRepository.searchClients("Zebra", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }
}
