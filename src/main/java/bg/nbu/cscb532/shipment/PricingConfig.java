package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing the dynamic pricing configuration for shipments.
 * Designed to maintain a history of price changes for auditability.
 * Only one record should have a null 'activeTo' at any given time.
 */
@Entity
@Table(name = "pricing_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingConfig extends BaseEntity {

    @NotNull(message = "{validation.pricing.base.notnull}")
    @PositiveOrZero(message = "{validation.pricing.base.positive}")
    @Column(name = "base_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;

    @NotNull(message = "{validation.pricing.perkg.notnull}")
    @PositiveOrZero(message = "{validation.pricing.perkg.positive}")
    @Column(name = "price_per_kg", nullable = false, precision = 19, scale = 2)
    private BigDecimal pricePerKg;

    @NotNull(message = "{validation.pricing.address.notnull}")
    @PositiveOrZero(message = "{validation.pricing.address.positive}")
    @Column(name = "address_surcharge", nullable = false, precision = 19, scale = 2)
    private BigDecimal addressSurcharge;

    @NotNull(message = "{validation.pricing.activefrom.notnull}")
    @Column(name = "active_from", nullable = false)
    private LocalDateTime activeFrom;

    /**
     * If null, this is the currently active pricing configuration.
     * If populated, this configuration is historical and no longer active.
     */
    @Column(name = "active_to")
    private LocalDateTime activeTo;
}
