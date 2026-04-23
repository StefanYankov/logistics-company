package bg.nbu.cscb532.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO for updating an existing Employee's safe, modifiable fields.
 * Explicitly excludes fields that should never be updated via a generic form
 * (e.g., employeeNumber, applicationRole, hireDate).
 */
@Builder
public record EmployeeUpdateDto(
        @NotBlank(message = "{validation.user.firstname.notblank}")
        @Size(max = 255, message = "{validation.user.firstname.toolong}")
        String firstName,

        @NotBlank(message = "{validation.user.lastname.notblank}")
        @Size(max = 255, message = "{validation.user.lastname.toolong}")
        String lastName,

        @NotBlank(message = "{validation.user.email.notblank}")
        @Email(message = "{validation.user.email.invalid}")
        @Size(max = 255, message = "{validation.user.email.toolong}")
        String email,

        @NotNull(message = "{validation.employee.salary.positive}")
        @Positive(message = "{validation.employee.salary.positive}")
        BigDecimal salary,

        // Optional: If provided and the employee is a Clerk, reassign them.
        // If the employee is a Courier, this field will be ignored by the service logic.
        Long officeId
) {
}
