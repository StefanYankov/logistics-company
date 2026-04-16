package bg.nbu.cscb532.client;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.base.BaseEntity;
import bg.nbu.cscb532.shared.location.AddressDetails;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Address saved in a client's personal address book for reuse.
 */
@Entity
@Table(name = "client_addresses")
@Getter
@Setter
public class ClientAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(length = Constants.Validation.MAX_NAME_LENGTH)
    private String alias;

    @Embedded
    private AddressDetails addressDetails;
}
