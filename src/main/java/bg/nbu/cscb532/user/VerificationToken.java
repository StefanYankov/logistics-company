package bg.nbu.cscb532.user;

import bg.nbu.cscb532.shared.base.BaseUUIDEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity representing a secure, time-bound token used for critical user lifecycle events
 * such as email verification and password resets.
 * Storing only the SHA-256 hash of the token ensures that database leaks do not compromise active tokens.
 */
@Entity
@Table(
        name = "verification_tokens",
        indexes = {
                // Index significantly speeds up cleanup queries when searching by user_id
                @Index(name = "idx_verification_tokens_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken extends BaseUUIDEntity {

    /**
     * Stores the SHA-256 hash of the token, never the raw UUID/String.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TokenType tokenType;

    @Column(nullable = false)
    private Instant expiryDate;

    // We use ManyToOne instead of OneToOne because a user might request multiple
    // password resets over time, resulting in multiple rows (though typically only one is valid).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Business logic check to determine if the token is still mathematically valid.
     * @return true if the current exact UTC moment is before the expiry threshold.
     */
    public boolean isValid() {
        return Instant.now().isBefore(expiryDate);
    }
}
