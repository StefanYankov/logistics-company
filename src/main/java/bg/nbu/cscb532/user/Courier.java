package bg.nbu.cscb532.user;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "couriers")
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public class Courier extends Employee {

}
