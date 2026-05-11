package bg.nbu.cscb532.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link Client} entities.
 * <p>
 * While most user-based lookups (like findByUsername) are handled by the generic {@link bg.nbu.cscb532.user.UserRepository},
 * this repository can be used for client-specific queries in the future (e.g., finding clients by phone number).
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    /**
     * Finds a client by their exact phone number.
     * Used for auto-matching receivers and preventing duplicate registrations.
     */
    Optional<Client> findByPhoneNumber(String phoneNumber);

    /**
     * Searches for clients based on a partial match (case-insensitive) of their first name,
     * last name, or phone number.
     *
     * @param term     The search string provided by the user.
     * @param pageable Pagination and sorting constraints.
     * @return A paginated list of matching Client entities.
     */
    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "c.phoneNumber LIKE CONCAT('%', :term, '%')")
    Page<Client> searchClients(@Param("term") String term, Pageable pageable);
}
