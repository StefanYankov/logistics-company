package bg.nbu.cscb532.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    /**
     * Looks up a verification token by its SHA-256 hash.
     */
    Optional<VerificationToken> findByTokenHash(String tokenHash);
    
    /**
     * Finds the most recently issued token for a specific user and type.
     * This is useful if a user requested multiple password resets and we only want to honor the newest one.
     */
    Optional<VerificationToken> findFirstByUser_IdAndTokenTypeOrderByCreatedAtDesc(UUID userId, TokenType tokenType);
}
