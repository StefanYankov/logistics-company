package bg.nbu.cscb532.employee;

import bg.nbu.cscb532.employee.dto.AdminPasswordResetDto;
import bg.nbu.cscb532.employee.dto.EmployeeCreationDto;
import bg.nbu.cscb532.employee.dto.EmployeeUpdateDto;
import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.office.OfficeRepository;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of the Employee Service.
 * Manages both Courier and OfficeClerk entities using a Simple Factory approach.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final OfficeRepository officeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public EmployeeViewDto create(EmployeeCreationDto dto) {
        log.debug("Attempting to create new employee with number: {}", dto.employeeNumber());

        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        validateDuplicates(dto);

        Employee newEmployee;

        // Simple Factory Pattern: Instantiate the correct subclass based on the role
        if (dto.applicationRole() == ApplicationRole.CLERK) {
            newEmployee = createOfficeClerk(dto);
        } else if (dto.applicationRole() == ApplicationRole.COURIER) {
            newEmployee = createCourier(dto);
        } else {
            log.error("Invalid role attempted for employee creation: {}", dto.applicationRole());
            throw new BusinessException(ErrorCode.INVALID_EMPLOYEE_ROLE);
        }

        // Common Fields Setup
        newEmployee.setUsername(dto.username().trim());
        newEmployee.setEmail(dto.email().trim().toLowerCase());
        newEmployee.setPassword(passwordEncoder.encode(dto.password()));
        newEmployee.setFirstName(dto.firstName().trim());
        newEmployee.setLastName(dto.lastName().trim());
        newEmployee.setEmployeeNumber(dto.employeeNumber().trim());
        newEmployee.setHireDate(dto.hireDate());
        newEmployee.setSalary(dto.salary());
        newEmployee.setApplicationRole(dto.applicationRole());
        newEmployee.setActive(true);

        Employee savedEmployee = employeeRepository.save(newEmployee);
        log.info("Successfully created new {} with ID: {}", dto.applicationRole(), savedEmployee.getId());

        return mapToViewDto(savedEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeViewDto getById(UUID id) {
        log.debug("Fetching employee by ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        return mapToViewDto(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeViewDto> getAll(Pageable pageable) {
        log.debug("Fetching all employees with pagination");
        return employeeRepository.findAll(pageable)
                .map(this::mapToViewDto);
    }

    @Override
    @Transactional
    public EmployeeViewDto updateBasicInfo(UUID id, EmployeeUpdateDto dto) {
        log.debug("Attempting to update employee with ID: {}", id);
        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));

        String normalizedEmail = dto.email().trim().toLowerCase();

        // Check for email collision ONLY if the email is actually changing
        if (!employee.getEmail().equals(normalizedEmail) && userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        employee.setFirstName(dto.firstName().trim());
        employee.setLastName(dto.lastName().trim());
        employee.setEmail(normalizedEmail);
        employee.setSalary(dto.salary());

        // Handle Office Reassignment if the employee is a Clerk
        if (employee instanceof OfficeClerk clerk && dto.officeId() != null) {
            // Only fetch and assign if the office ID is actually different
            if (clerk.getOffice() == null || !clerk.getOffice().getId().equals(dto.officeId())) {
                Office newOffice = officeRepository.findById(dto.officeId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.OFFICE_NOT_FOUND));
                clerk.setOffice(newOffice);
                log.info("Reassigned clerk {} to office {}", id, dto.officeId());
            }
        }

        Employee updatedEmployee = employeeRepository.save(employee);
        log.info("Successfully updated employee with ID: {}", updatedEmployee.getId());

        return mapToViewDto(updatedEmployee);
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        log.debug("Attempting to deactivate employee with ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));

        employee.setActive(false);
        employeeRepository.save(employee);

        log.info("Successfully deactivated employee with ID: {}", id);
    }

    @Override
    @Transactional
    public void forcePasswordReset(UUID id, AdminPasswordResetDto dto) {
        log.debug("Admin attempting to force reset password for employee ID: {}", id);
        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));

        String hashedPassword = passwordEncoder.encode(dto.newPassword());
        employee.setPassword(hashedPassword);
        employeeRepository.save(employee);

        log.info("Admin successfully forced a password reset for employee ID: {}", id);
    }

    // --- Private Helper Methods ---

    private void validateDuplicates(EmployeeCreationDto dto) {
        String username = dto.username().trim();
        String email = dto.email().trim().toLowerCase();
        String empNumber = dto.employeeNumber().trim();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }
        if (employeeRepository.findByEmployeeNumber(empNumber).isPresent()) {
            throw new BusinessException(ErrorCode.EMPLOYEE_NUMBER_DUPLICATE);
        }
    }

    private OfficeClerk createOfficeClerk(EmployeeCreationDto dto) {
        if (dto.officeId() == null) {
            log.warn("Attempted to create an OfficeClerk without specifying an Office ID.");
            // We reuse VALIDATION_FAILED here, but it could be a specific business rule
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        Office office = officeRepository.findById(dto.officeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICE_NOT_FOUND));

        OfficeClerk clerk = new OfficeClerk();
        clerk.setOffice(office);
        return clerk;
    }

    private Courier createCourier(EmployeeCreationDto dto) {
        return new Courier(); // Couriers do not require an office mapping
    }

    private EmployeeViewDto mapToViewDto(Employee employee) {
        if (employee == null) {
            return null;
        }

        Long officeId = null;
        if (employee instanceof OfficeClerk clerk && clerk.getOffice() != null) {
            officeId = clerk.getOffice().getId();
        }

        return EmployeeViewDto.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .email(employee.getEmail())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .employeeNumber(employee.getEmployeeNumber())
                .hireDate(employee.getHireDate())
                .salary(employee.getSalary())
                .applicationRole(employee.getApplicationRole())
                .isActive(employee.isActive())
                .officeId(officeId)
                .build();
    }
}
