package bg.nbu.cscb532.user;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.shared.config.JpaConfig;
import bg.nbu.cscb532.shared.location.AddressDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("deprecation")
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private UserRepository userRepository;

    // --- TEST DATA FACTORY ---
    private Client createClientEntity(String username, String email) {
        Client client = new Client();
        client.setUsername(username);
        client.setEmail(email);
        client.setPassword("hashed-password");
        client.setFirstName("Test");
        client.setLastName("User");
        client.setPhoneNumber("123456789");
        client.setApplicationRole(ApplicationRole.CLIENT);
        
        AddressDetails address = new AddressDetails();
        City city = new City();
        city.setId(1L);
        address.setCity(city);
        address.setStreet("Test Street");

        return client;
    }

    @Nested
    @DisplayName("findByUsername(String) Tests")
    class FindByUsernameTests {

        @Test
        @DisplayName("Happy Path: Should find user when exact username exists")
        void shouldFindUserWhenUsernameExists() {

            // Arrange
            userRepository.saveAndFlush(createClientEntity("testuser", "test@example.com"));

            // Act
            Optional<User> result = userRepository.findByUsername("testuser");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Error Case: Should return empty when username does not exist")
        void shouldReturnEmptyWhenUsernameNotFound() {

            // Act
            Optional<User> result = userRepository.findByUsername("nonexistent");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Edge Case: Should return empty when username case differs")
        void shouldReturnEmptyWhenCaseIsDifferent() {

            // Arrange
            userRepository.saveAndFlush(createClientEntity("testuser", "test@example.com"));

            // Act
            Optional<User> result = userRepository.findByUsername("TestUser");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail(String) Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Happy Path: Should find user when exact email exists")
        void shouldFindUserWhenEmailExists() {

            // Arrange
            userRepository.saveAndFlush(createClientEntity("testuser", "test@example.com"));

            // Act
            Optional<User> result = userRepository.findByEmail("test@example.com");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Error Case: Should return empty when email does not exist")
        void shouldReturnEmptyWhenEmailNotFound() {

            // Act
            Optional<User> result = userRepository.findByEmail("wrong@example.com");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Database Constraint Tests")
    class ConstraintTests {

        @Test
        @DisplayName("Error Case: Should throw DataIntegrityViolationException on duplicate username")
        void shouldThrowOnDuplicateUsername() {

            // Arrange
            userRepository.saveAndFlush(createClientEntity("testuser", "one@example.com"));
            Client duplicateUser = createClientEntity("testuser", "two@example.com");

            // Act & Assert
            assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateUser))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uc_users_username");
        }

        @Test
        @DisplayName("Error Case: Should throw DataIntegrityViolationException on duplicate email")
        void shouldThrowOnDuplicateEmail() {

            // Arrange
            userRepository.saveAndFlush(createClientEntity("userone", "test@example.com"));
            Client duplicateEmail = createClientEntity("usertwo", "test@example.com");

            // Act & Assert
            assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateEmail))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uc_users_email");
        }
    }
}