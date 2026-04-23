package bg.nbu.cscb532.employee.dto;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.user.ApplicationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

        @NotBlank(message = "{validation.employee.number.notblank}")
        @Size(max = Constants.Validation.MAX_REGISTRATION_NUMBER_LENGTH, message = "{validation.employee.number.toolong}")
        String employeeNumber,

        @NotNull(message = "{validation.employee.hiredate.notnull}")
        LocalDate hireDate,

        @NotNull(message = "{validation.employee.salary.positive}")
        @Positive(message = "{validation.employee.salary.positive}")
        BigDecimal salary,

        @NotNull(message = "Application role must be specified")
        ApplicationRole applicationRole,

        Long officeId
) {
}