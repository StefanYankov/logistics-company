package bg.nbu.cscb532.shipment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link Shipment} entities.
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    /**
     * Finds a specific shipment by its unique public tracking number.
     */
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    /**
     * Retrieves all shipments sent by a specific client.
     */
    Page<Shipment> findBySender_Id(UUID senderId, Pageable pageable);

    /**
     * Retrieves all shipments received by a specific client.
     */
    Page<Shipment> findByReceiver_Id(UUID receiverId, Pageable pageable);

    /**
     * Retrieves all shipments registered by a specific employee.
     */
    Page<Shipment> findByRegisteredBy_Id(UUID employeeId, Pageable pageable);

    /**
     * Retrieves all shipments that have NOT reached a specific status (e.g., DELIVERED).
     */
    Page<Shipment> findByStatusNot(ShipmentStatus status, Pageable pageable);
}
