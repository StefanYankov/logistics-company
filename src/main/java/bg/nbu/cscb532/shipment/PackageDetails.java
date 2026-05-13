package bg.nbu.cscb532.shipment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Value object encapsulating the physical properties of a shipment.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageDetails {

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_type", nullable = false)
    private ShipmentType type;

    @Column(nullable = false, precision = 8, scale = 3)
    private BigDecimal weight;

    @Column(precision = 8, scale = 2)
    private BigDecimal length;

    @Column(precision = 8, scale = 2)
    private BigDecimal width;

    @Column(precision = 8, scale = 2)
    private BigDecimal height;
}
