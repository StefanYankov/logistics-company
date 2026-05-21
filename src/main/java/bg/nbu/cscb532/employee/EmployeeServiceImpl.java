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
import bg.nbu.cscb532.shared.infrastructure.email.EmailService;
import bg.nbu.cscb532.shared.security.SecurityUtils;
import bg.nbu.cscb532.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    private final OfficeClerkRepository officeClerkRepository;
    private final CourierRepository courierRepository;

    @Override
    @Transactional
    public EmployeeViewDto create(EmployeeCreationDto dto) {
        log.debug("Attempting to create new employee");

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

        // TODO: (Refactor) This generation logic should be moved to a dedicated, configurable EmployeeNumberGenerator service.
        // The service would use a database sequence for the numeric part and a configurable prefix (e.g., from application.yaml)
        // to ensure uniqueness and flexibility. For now, a UUID-based approach is a safe, temporary solution.
        String employeeNumber = "E-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Common Fields Setup
        boolean isEmailVerified = true;
        newEmployee.setUsername(dto.username().trim());
        newEmployee.setEmail(dto.email().trim().toLowerCase());
        newEmployee.setPassword(passwordEncoder.encode(dto.password()));
        newEmployee.setFirstName(dto.firstName().trim());
        newEmployee.setLastName(dto.lastName().trim());
        newEmployee.setEmployeeNumber(employeeNumber);
        newEmployee.setHireDate(dto.hireDate());
        newEmployee.setSalary(dto.salary());
        newEmployee.setApplicationRole(dto.applicationRole());
        newEmployee.setActive(true);
        newEmployee.setEmailVerified(isEmailVerified);

        Employee savedEmployee = employeeRepository.save(newEmployee);

        // current logic handles upon admin creation users are automatically veirified
        if (!isEmailVerified) {
            // Generate Verification Token
            String rawToken = UUID.randomUUID().toString();
            VerificationToken token = VerificationToken.builder()
                    .tokenHash(SecurityUtils.hashSha256(rawToken))
                    .tokenType(TokenType.EMAIL_VERIFICATION)
                    .expiryDate(Instant.now().plus(Duration.ofHours(48)))
                    .user(savedEmployee)
                    .build();

            verificationTokenRepository.save(token);
            emailService.sendVerificationEmail(savedEmployee.getEmail(), rawToken);
        }

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
        
        Page<OfficeClerk> clerks = officeClerkRepository.findAll(pageable);
        Page<Courier> couriers = courierRepository.findAll(pageable);

        List<Employee> allEmployees = new ArrayList<>();
        allEmployees.addAll(clerks.getContent());
        allEmployees.addAll(couriers.getContent());

        List<EmployeeViewDto> dtoList = allEmployees.stream()
                .map(this::mapToViewDto)
                .toList();

        return new PageImpl<>(dtoList, pageable, clerks.getTotalElements() + couriers.getTotalElements());
    }

    @Override
    @Transactional
    public EmployeeViewDto updateBasicInfo(UUID id, EmployeeUpdateDto dto) {
        log.debug("Attempting to update employee with ID: {}", id);
        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));

        String normalizedEmail = dto.email().trim().toLowerCase();

        if (!employee.getEmail().equals(normalizedEmail) && userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        employee.setFirstName(dto.firstName().trim());
        employee.setLastName(dto.lastName().trim());
        employee.setEmail(normalizedEmail);
        employee.setSalary(dto.salary());

        if (employee instanceof OfficeClerk clerk && dto.officeId() != null) {
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

        if (employee.isActive()) {
            employee.setActive(false);
            employeeRepository.save(employee);
            log.info("Successfully deactivated employee with ID: {}", id);
        }
    }

    @Override
    @Transactional
    public void activate(UUID id) {
        log.debug("Attempting to activate employee with ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));

        if (!employee.isActive()) {
            employee.setActive(true);
            employeeRepository.save(employee);
            log.info("Successfully activated employee with ID: {}", id);
        }
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

        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }
    }

    private OfficeClerk createOfficeClerk(EmployeeCreationDto dto) {
        if (dto.officeId() == null) {
            log.warn("Attempted to create an OfficeClerk without specifying an Office ID.");
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        Office office = officeRepository.findById(dto.officeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OFFICE_NOT_FOUND));

        OfficeClerk clerk = new OfficeClerk();
        clerk.setOffice(office);
        return clerk;
    }

    // future-proof if we expand courier
    private Courier createCourier(@SuppressWarnings("unused") EmployeeCreationDto dto) {
        return new Courier();
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
                .isEmailVerified(employee.isEmailVerified())
                .officeId(officeId)
                .build();
    }
}
