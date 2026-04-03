package bg.nbu.cscb532.office;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

/**
 * Represents a geographical city.
 * Extracted into a separate entity to normalize addresses and allow filtering by city.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cities", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_city_name_postcode",
                columnNames = {"name", "postcode"}
        )
})
public class City extends BaseEntity {

    @Column(nullable = false, length = Constants.Validation.MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false, length = Constants.Validation.MAX_POSTAL_CODE_LENGTH)
    private String postcode;
}
