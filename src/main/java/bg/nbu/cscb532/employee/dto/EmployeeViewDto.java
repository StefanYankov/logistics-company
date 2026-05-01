package bg.nbu.cscb532.employee.dto;

import bg.nbu.cscb532.user.ApplicationRole;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record EmployeeViewDto(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String employeeNumber,
        LocalDate hireDate,
        BigDecimal salary,
        ApplicationRole applicationRole,
        boolean isActive,
        boolean isEmailVerified,
        Long officeId // Will be null for Couriers, populated for Clerks
) {
}