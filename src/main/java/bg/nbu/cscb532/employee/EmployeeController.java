package bg.nbu.cscb532.employee;

import bg.nbu.cscb532.employee.dto.AdminPasswordResetDto;
import bg.nbu.cscb532.employee.dto.EmployeeCreationDto;
import bg.nbu.cscb532.employee.dto.EmployeeUpdateDto;
import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import bg.nbu.cscb532.shared.web.ApiStandardResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST Controller for managing Employee entities (Couriers and Office Clerks).
 * All endpoints in this controller are strictly restricted to users with the ADMIN role.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/employees", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@ApiStandardResponses
@RequiredArgsConstructor
@Tag(name = "Employee Management API", description = "Admin-only endpoints for managing company staff.")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(
            summary = "Create a new employee",
            description = "Creates a new Courier or Office Clerk based on the provided applicationRole. Returns 201 Created."
    )
    @ApiResponse(responseCode = "201", description = "Employee created successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., missing fields, invalid role, missing officeId for clerk)")
    @ApiResponse(responseCode = "409", description = "Conflict - Username or email is already taken")
    @PostMapping
    public ResponseEntity<EmployeeViewDto> createEmployee(@Valid @RequestBody EmployeeCreationDto request) {
        log.info("API POST request to create new employee with username: {}", request.username());

        EmployeeViewDto createdEmployee = employeeService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdEmployee.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdEmployee);
    }

    @Operation(summary = "Get an employee by ID")
    @ApiResponse(responseCode = "200", description = "Employee found")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeViewDto> getEmployeeById(
            @Parameter(description = "The UUID of the employee") @PathVariable UUID id) {
        log.info("API GET request for employee ID: {}", id);
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @Operation(
            summary = "Get all employees (Paginated)",
            description = "Retrieves a paginated list of all employees (both Couriers and Clerks). Supports sorting and filtering via Pageable parameters."
    )
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping
    public ResponseEntity<Page<EmployeeViewDto>> getAllEmployees(
            @Parameter(description = "Pagination and sorting parameters (e.g., ?page=0&size=10&sort=lastName,asc)") Pageable pageable) {
        log.info("API GET request for all employees. Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(employeeService.getAll(pageable));
    }

    @Operation(
            summary = "Update an employee's basic info",
            description = "Updates safe fields like name, email, salary, and office assignment. Does not update credentials or core identity fields."
    )
    @ApiResponse(responseCode = "200", description = "Employee updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Employee or target Office not found")
    @ApiResponse(responseCode = "409", description = "Conflict - New email is already taken by another user")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeViewDto> updateEmployeeBasicInfo(
            @Parameter(description = "The UUID of the employee to update") @PathVariable UUID id,
            @Valid @RequestBody EmployeeUpdateDto request) {
        log.info("API PUT request to update employee ID: {}", id);
        return ResponseEntity.ok(employeeService.updateBasicInfo(id, request));
    }

    @Operation(
            summary = "Deactivate an employee (Soft Delete)",
            description = "Sets the employee's active status to false, preventing login, but preserves their historical data (e.g., related shipments)."
    )
    @ApiResponse(responseCode = "204", description = "Employee successfully deactivated")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateEmployee(
            @Parameter(description = "The UUID of the employee to deactivate") @PathVariable UUID id) {
        log.info("API DELETE request to deactivate employee ID: {}", id);
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Activate an employee",
            description = "Sets the employee's active status to true, allowing login."
    )
    @ApiResponse(responseCode = "204", description = "Employee successfully activated")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateEmployee(
            @Parameter(description = "The UUID of the employee to activate") @PathVariable UUID id) {
        log.info("API PATCH request to activate employee ID: {}", id);
        employeeService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Force a password reset",
            description = "Allows an administrator to immediately overwrite an employee's password."
    )
    @ApiResponse(responseCode = "204", description = "Password successfully reset")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., password too short)")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> forcePasswordReset(
            @Parameter(description = "The UUID of the employee") @PathVariable UUID id,
            @Valid @RequestBody AdminPasswordResetDto request) {
        log.info("API POST request to force password reset for employee ID: {}", id);
        employeeService.forcePasswordReset(id, request);
        return ResponseEntity.noContent().build();
    }
}
