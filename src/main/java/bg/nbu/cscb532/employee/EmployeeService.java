package bg.nbu.cscb532.employee;

import bg.nbu.cscb532.employee.dto.AdminPasswordResetDto;
import bg.nbu.cscb532.employee.dto.EmployeeCreationDto;
import bg.nbu.cscb532.employee.dto.EmployeeUpdateDto;
import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for managing the Employee aggregate root.
 */
public interface EmployeeService {

    /**
     * Creates a new employee (either an OfficeClerk or a Courier).
     *
     * @param dto The DTO containing the employee's details.
     * @return A view DTO of the newly created employee.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if a business rule is violated (e.g., duplicates).
     */
    EmployeeViewDto create(EmployeeCreationDto dto);

    /**
     * Retrieves a single employee by their unique ID.
     *
     * @param id The UUID of the employee.
     * @return A view DTO of the employee.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the employee is not found.
     */
    EmployeeViewDto getById(UUID id);

    /**
     * Retrieves a paginated list of all employees.
     *
     * @param pageable The pagination information.
     * @return A page of employee view DTOs.
     */
    Page<EmployeeViewDto> getAll(Pageable pageable);

    /**
     * Updates an existing employee's basic information safely.
     *
     * @param id The UUID of the employee to update.
     * @param dto The DTO containing the new values.
     * @return A view DTO of the updated employee.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the employee is not found or validation fails.
     */
    EmployeeViewDto updateBasicInfo(UUID id, EmployeeUpdateDto dto);

    /**
     * Deactivates an employee (soft delete).
     * The employee record remains in the database but they can no longer log in.
     *
     * @param id The UUID of the employee to deactivate.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the employee is not found.
     */
    void deactivate(UUID id);

    /**
     * Activates a previously deactivated employee.
     * The employee will be able to log in again.
     *
     * @param id The UUID of the employee to activate.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the employee is not found.
     */
    void activate(UUID id);

    /**
     * Forces a password reset for an employee.
     * This is intended to be an Admin-only operation.
     *
     * @param id The UUID of the employee.
     * @param dto The DTO containing the new password.
     */
    void forcePasswordReset(UUID id, AdminPasswordResetDto dto);
}
