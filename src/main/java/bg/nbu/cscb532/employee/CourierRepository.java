package bg.nbu.cscb532.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for {@link Courier} entities.
 */
@Repository
public interface CourierRepository extends JpaRepository<Courier, UUID> {
}
