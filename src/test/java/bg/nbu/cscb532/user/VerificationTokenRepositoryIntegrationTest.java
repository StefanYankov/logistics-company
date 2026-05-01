package bg.nbu.cscb532.user;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.client.ClientRepository;
import bg.nbu.cscb532.shared.config.JpaConfig;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VerificationTokenRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private ClientRepository clientRepository;

    // --- TEST DATA FACTORY ---

    private User createAndSaveUser(String username) {
        Client client = new Client();
        client.setUsername(username);
        client.setEmail(username + "@example.com");
        client.setPassword("hashed-password");
        client.setFirstName("Test");
        client.setLastName("User");
        client.setPhoneNumber("0888123456");
        client.setApplicationRole(ApplicationRole.CLIENT);
        client.setActive(true);
        client.setEmailVerified(false);
        return clientRepository.saveAndFlush(client);
    }

    private void createAndSaveToken(User user, String hash, TokenType type, Instant expiry) {
        VerificationToken token = VerificationToken.builder()
                .tokenHash(hash)
                .tokenType(type)
                .expiryDate(expiry)
                .user(user)
                .build();
        verificationTokenRepository.saveAndFlush(token);
    }

    @Nested
    @DisplayName("findByTokenHash(String) Tests")
    class FindByTokenHashTests {

        @Test
        @DisplayName("Happy Path: Should find token when it exists")
        void shouldFindTokenWhenExists() {
            // Arrange
            User user = createAndSaveUser("tokenuser1");
            String hash = "a".repeat(64); // Simulate SHA-256 hash
            createAndSaveToken(user, hash, TokenType.EMAIL_VERIFICATION, Instant.now().plus(1, ChronoUnit.HOURS));

            // Act
            Optional<VerificationToken> result = verificationTokenRepository.findByTokenHash(hash);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getTokenHash()).isEqualTo(hash);
            assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("Error Case: Should return empty when token does not exist")
        void shouldReturnEmptyWhenTokenDoesNotExist() {
            // Act
            Optional<VerificationToken> result = verificationTokenRepository.findByTokenHash("nonexistent");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findFirstByUser_IdAndTokenTypeOrderByCreatedAtDesc(UUID, TokenType) Tests")
    class FindFirstByUserAndTypeTests {

        @Test
        @DisplayName("Happy Path: Should return the most recently created token for a user and type")
        void shouldReturnMostRecentToken() throws InterruptedException {
            // Arrange
            User user = createAndSaveUser("multi-token-user");
            String oldHash = "a".repeat(64);
            String newHash = "b".repeat(64);

            // Save old token
            createAndSaveToken(user, oldHash, TokenType.PASSWORD_RESET, Instant.now().plus(1, ChronoUnit.HOURS));
            
            // Sleep briefly to ensure the created_at timestamps are distinct (since JPA Auditing uses Instant.now())
            Thread.sleep(10);
            
            // Save new token
            createAndSaveToken(user, newHash, TokenType.PASSWORD_RESET, Instant.now().plus(1, ChronoUnit.HOURS));

            // Act
            Optional<VerificationToken> result = verificationTokenRepository.findFirstByUser_IdAndTokenTypeOrderByCreatedAtDesc(user.getId(), TokenType.PASSWORD_RESET);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getTokenHash()).isEqualTo(newHash); // Should fetch the newest one
        }

        @Test
        @DisplayName("Edge Case: Should only return tokens matching the specific TokenType")
        void shouldOnlyReturnMatchingType() {
            // Arrange
            User user = createAndSaveUser("mixed-token-user");
            String emailHash = "c".repeat(64);
            String resetHash = "d".repeat(64);

            // User has one of each type
            createAndSaveToken(user, emailHash, TokenType.EMAIL_VERIFICATION, Instant.now().plus(1, ChronoUnit.HOURS));
            createAndSaveToken(user, resetHash, TokenType.PASSWORD_RESET, Instant.now().plus(1, ChronoUnit.HOURS));

            // Act
            Optional<VerificationToken> result = verificationTokenRepository.findFirstByUser_IdAndTokenTypeOrderByCreatedAtDesc(user.getId(), TokenType.PASSWORD_RESET);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getTokenHash()).isEqualTo(resetHash);
            assertThat(result.get().getTokenType()).isEqualTo(TokenType.PASSWORD_RESET);
        }

        @Test
        @DisplayName("Error Case: Should return empty when user has no tokens of requested type")
        void shouldReturnEmptyWhenNoTokensOfType() {
            // Arrange
            User user = createAndSaveUser("no-token-user");

            // Act
            Optional<VerificationToken> result = verificationTokenRepository.findFirstByUser_IdAndTokenTypeOrderByCreatedAtDesc(user.getId(), TokenType.PASSWORD_RESET);

            // Assert
            assertThat(result).isEmpty();
        }
    }
}
