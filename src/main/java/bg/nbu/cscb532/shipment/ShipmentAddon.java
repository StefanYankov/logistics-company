package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Represents an applied service (line-item) for a specific shipment.
 * Stores a historical snapshot of the applied cost.
 */
@Entity
@Table(name = "shipment_addons")
@Getter
@Setter
public class ShipmentAddon extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_catalog_id", nullable = false)
    private ServiceCatalog serviceCatalog;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal appliedCost;

}