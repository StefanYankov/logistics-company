package bg.nbu.cscb532.shipment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing ShipmentAddon entities.
 */
@Repository
public interface ShipmentAddonRepository extends JpaRepository<ShipmentAddon, UUID> {
}
