package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Defines the available additional services (e.g., SMS Notification, Fragile) and their pricing rules.
 */
@Entity
@Table(name = "services_catalog")
@Getter
@Setter
public class ServiceCatalog extends BaseEntity {

    @Column(nullable = false, unique = true, length = Constants.Validation.MAX_NAME_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingType pricingType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal pricingValue;
}