package bg.nbu.cscb532.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VerificationToken Domain Logic Tests")
class VerificationTokenTest {

    @Test
    @DisplayName("isValid() should return true when expiry date is in the future")
    void shouldReturnTrueWhenNotExpired() {
        // Arrange: Token expires 1 hour from now
        VerificationToken token = VerificationToken.builder()
                .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        // Act & Assert
        assertThat(token.isValid()).isTrue();
    }

    @Test
    @DisplayName("isValid() should return false when expiry date is in the past")
    void shouldReturnFalseWhenExpired() {
        // Arrange: Token expired 1 hour ago
        VerificationToken token = VerificationToken.builder()
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        // Act & Assert
        assertThat(token.isValid()).isFalse();
    }
}
