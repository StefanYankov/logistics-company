package bg.nbu.cscb532.user;
import bg.nbu.cscb532.office.Office;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "office_clerks")
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public class OfficeClerk extends Employee {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    private Office office;

}
