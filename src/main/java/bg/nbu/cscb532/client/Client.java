package bg.nbu.cscb532.client;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a customer of the logistics company who sends or receives shipments.
 */
@Entity
@Table(name = "clients")
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public class Client extends User {

    @Column(name = "phone_number", nullable = false, length = Constants.Validation.MAX_PHONE_NUMBER_LENGTH)
    private String phoneNumber;

}
