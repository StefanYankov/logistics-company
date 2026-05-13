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
 * Value object encapsulating the financial aspects of a shipment.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentFinancials {

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_by", nullable = false)
    private PaidBy paidBy;

    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private boolean isPaid = false;
}
