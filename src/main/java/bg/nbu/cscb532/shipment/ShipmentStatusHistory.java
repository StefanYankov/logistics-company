package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks the historical lifecycle events of a shipment.
 */
@Entity
@Table(name = "shipment_status_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    /**
     * Where this status change occurred (optional).
     * E.g., The sorting hub or the final delivery office.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_office_id")
    private Office location;

    /**
     * Optional remarks (e.g., "Client not at home, will retry tomorrow").
     */
    @Column(length = 255)
    private String notes;
}
