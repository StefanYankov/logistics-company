package bg.nbu.cscb532.employee.dto;

import bg.nbu.cscb532.user.ApplicationRole;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record EmployeeCreationDto(
        @NotBlank(message = "{validation.user.username.notblank}")
        @Size(max = 255, message = "{validation.user.username.toolong}")
        String username,

        @NotBlank(message = "{validation.user.email.notblank}")
        @Email(message = "{validation.user.email.invalid}")
        @Size(max = 255, message = "{validation.user.email.toolong}")
        String email,

        @NotBlank(message = "{validation.user.password.notblank}")
        @Size(min = 8, max = 255, message = "{validation.user.password.size}")
        String password,

        @NotBlank(message = "{validation.user.firstname.notblank}")
        @Size(max = 255, message = "{validation.user.firstname.toolong}")
        String firstName,

        @NotBlank(message = "{validation.user.lastname.notblank}")
        @Size(max = 255, message = "{validation.user.lastname.toolong}")
        String lastName,

        @NotNull(message = "{validation.employee.hiredate.notnull}")
        LocalDate hireDate,

        @NotNull(message = "{validation.employee.salary.positive}")
        @Positive(message = "{validation.employee.salary.positive}")
        BigDecimal salary,

        @NotNull(message = "{validation.employee.role.notnull}")
        ApplicationRole applicationRole,

        Long officeId
) {
}
