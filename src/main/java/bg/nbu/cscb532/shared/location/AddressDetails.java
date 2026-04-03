package bg.nbu.cscb532.shared.location;

import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.shared.Constants;
import jakarta.persistence.*;
import lombok.*;

/**
 * Reusable embedded value object representing the physical components of an address.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AddressDetails {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false, length = Constants.Validation.MAX_STREET_LENGTH)
    private String street;

    @Column(length = Constants.Validation.MAX_DISTRICT_LENGTH)
    private String district;

    @Column(length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)
    private String building;

    @Column(length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)
    private String entrance;

    @Column(length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)
    private String floor;

    @Column(length = Constants.Validation.MAX_BUILDING_INFO_LENGTH)
    private String apartment;
}
