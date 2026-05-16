package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.base.BaseUUIDEntity;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.employee.Courier;
import bg.nbu.cscb532.employee.Employee;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

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
    @JoinColumn(name = "receiver_id")
    private Client receiver;

    // Guest receiver details (used if 'receiver' is null)
    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone", length = Constants.Validation.MAX_PHONE_LENGTH)
    private String receiverPhone;

    @Column(name = "receiver_email")
    private String receiverEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_id", nullable = false)
    private Employee registeredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivered_by_id")
    private Courier deliveredBy;

    // --- 2. Origin & Destinations ---

    // Origin (Where the package started)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_office_id")
    private Office originOffice;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "origin_street", length = Constants.Validation.MAX_STREET_LENGTH)),
            @AttributeOverride(name = "district", column = @Column(name = "origin_district", length = Constants.Validation.MAX_DISTRICT_LENGTH)),
            @AttributeOverride(name = "building", column = @Column(name = "origin_building", length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)),
            @AttributeOverride(name = "entrance", column = @Column(name = "origin_entrance", length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)),
            @AttributeOverride(name = "floor", column = @Column(name = "origin_floor", length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)),
            @AttributeOverride(name = "apartment", column = @Column(name = "origin_apartment", length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)),
            @AttributeOverride(name = "latitude", column = @Column(name = "origin_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "origin_longitude"))
    })
    @AssociationOverrides({
            @AssociationOverride(name = "city", joinColumns = @JoinColumn(name = "origin_city_id", columnDefinition = "bigint"))
    })
    private AddressDetails originAddressSnapshot;

    // Destination (Where the package is going)
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
            @AttributeOverride(name = "apartment", column = @Column(name = "delivery_apartment", length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)),
            @AttributeOverride(name = "latitude", column = @Column(name = "delivery_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "delivery_longitude"))
    })
    @AssociationOverrides({
            @AssociationOverride(name = "city", joinColumns = @JoinColumn(name = "delivery_city_id", columnDefinition = "bigint"))
    })
    private AddressDetails deliveryAddressSnapshot;

    // Current Physical Location (For fast querying)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_office_id")
    private Office currentOffice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_courier_id")
    private Courier currentCourier;

    // --- 3. Package Details ---

    @Column(nullable = false, unique = true, length = 50)
    private String trackingNumber;

    @Embedded
    private PackageDetails packageDetails;

    // --- 4. Financials ---

    @Embedded
    private ShipmentFinancials financials;

    // --- 5. Status ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    // --- 6. Addons ---

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ShipmentAddon> addons = new HashSet<>();
}
