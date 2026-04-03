package bg.nbu.cscb532.company;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents the core logistic company operating the network.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(nullable = false, length = Constants.Validation.MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false, unique = true, length = Constants.Validation.MAX_REGISTRATION_NUMBER_LENGTH)
    private String registrationNumber;
}