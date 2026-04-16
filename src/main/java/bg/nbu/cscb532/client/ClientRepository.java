package bg.nbu.cscb532.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing {@link Client} entities.
 * <p>
 * While most user-based lookups (like findByUsername) are handled by the generic {@link bg.nbu.cscb532.user.UserRepository},
 * this repository can be used for client-specific queries in the future (e.g., finding clients by phone number).
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
}
