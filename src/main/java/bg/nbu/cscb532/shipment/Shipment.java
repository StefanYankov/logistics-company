package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.base.BaseUUIDEntity;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.user.Client;
import bg.nbu.cscb532.user.Courier;
import bg.nbu.cscb532.user.Employee;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Core domain entity representing a physical package being transported from a sender to a receiver.
 */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment extends BaseUUIDEntity {

    // --- 1. Participants ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Client sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Client receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_id", nullable = false)
    private Employee registeredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivered_by_id")
    private Courier deliveredBy;

    // --- 2. Destinations ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_office_id")
    private Office deliveryOffice;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "delivery_street", length = Constants.Validation.MAX_STREET_LENGTH)),
            @AttributeOverride(name = "district", column = @Column(name = "delivery_district", length = Constants.Validation.MAX_DISTRICT_LENGTH)),
            @AttributeOverride(name = "building", column = @Column(name = "delivery_building", length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)),
            @AttributeOverride(name = "entrance", column = @Column(name = "delivery_entrance", length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)),
            @AttributeOverride(name = "floor", column = @Column(name = "delivery_floor", length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)),
            @AttributeOverride(name = "apartment", column = @Column(name = "delivery_apartment", length = Constants.Validation.MAX_BUILDING_INFO_LENGTH))
    })
    private AddressDetails deliveryAddressSnapshot;

    // --- 3. Package Details ---

    @Column(nullable = false, unique = true, length = 50)
    private String trackingNumber;

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

    // --- 4. Financials ---

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    // --- 5. Status ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    // --- 6. Addons ---

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ShipmentAddon> addons = new HashSet<>();
}