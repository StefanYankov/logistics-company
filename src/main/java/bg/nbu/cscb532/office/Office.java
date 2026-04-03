package bg.nbu.cscb532.office;

import bg.nbu.cscb532.shared.base.BaseEntity;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.company.Company;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a physical branch or location of the logistics company.
 */
@Entity
@Table(name = "offices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Office extends BaseEntity {

    @Embedded
    private AddressDetails addressDetails;

    @ElementCollection
    @CollectionTable(
            name = "office_operating_hours",
            joinColumns = @JoinColumn(name = "office_id")
    )
    @Builder.Default
    private Set<OperatingHour> operatingHours = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}