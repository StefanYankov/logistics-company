package bg.nbu.cscb532.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for {@link OfficeClerk} entities.
 * Provides standard CRUD operations and custom query methods for office clerks.
 */
@Repository
public interface OfficeClerkRepository extends JpaRepository<OfficeClerk, UUID> {

    /**
     * Retrieves a paginated list of office clerks associated with a specific office.
     *
     * @param officeId the unique identifier of the office
     * @param pageable the pagination information
     * @return a page of office clerks belonging to the specified office
     */
    Page<OfficeClerk> findOfficeClerksByOfficeId(Long officeId, Pageable pageable);
}
