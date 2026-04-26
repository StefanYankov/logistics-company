package bg.nbu.cscb532.shipment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link ShipmentStatusHistory} entities.
 * Essential for generating the tracking log/audit trail of a shipment.
 */
@Repository
public interface ShipmentStatusHistoryRepository extends JpaRepository<ShipmentStatusHistory, Long> {

    /**
     * Retrieves the entire status history for a given shipment, ordered from newest to oldest.
     * This is used when a client or employee requests the tracking history of a package.
     *
     * @param shipmentId The UUID of the shipment.
     * @return A chronological list of status events.
     */
    List<ShipmentStatusHistory> findByShipment_IdOrderByCreatedAtDesc(UUID shipmentId);
}
