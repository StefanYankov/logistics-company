package bg.nbu.cscb532.employee;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Employee extends User {

    @NotBlank(message = "{validation.employee.number.notblank}")
    @Size(max = Constants.Validation.MAX_REGISTRATION_NUMBER_LENGTH, message = "{validation.employee.number.toolong}")
    @Column(name = "employee_number", nullable = false, unique = true, length = Constants.Validation.MAX_REGISTRATION_NUMBER_LENGTH)
    private String employeeNumber;

    @NotNull(message = "{validation.employee.hiredate.notnull}")
    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(nullable = false, precision = 19, scale = 2)
    @Positive(message = "{validation.employee.salary.positive}")
    private BigDecimal salary;

}