package bg.nbu.cscb532.office;

import bg.nbu.cscb532.shared.Constants;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represented as an @Embeddable value object rather than an @Entity.
 * This flattens the database schema, avoiding redundant JOINs and N+1 query risks
 * since an Address typically does not have a lifecycle independent of its parent (e.g., Office).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Address {

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