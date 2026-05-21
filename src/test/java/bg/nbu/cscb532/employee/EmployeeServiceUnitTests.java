package bg.nbu.cscb532.employee;

import bg.nbu.cscb532.employee.dto.AdminPasswordResetDto;
import bg.nbu.cscb532.employee.dto.EmployeeCreationDto;
import bg.nbu.cscb532.employee.dto.EmployeeUpdateDto;
import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.office.OfficeRepository;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.User;
import bg.nbu.cscb532.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService Unit Tests")
class EmployeeServiceUnitTests {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OfficeClerkRepository officeClerkRepository;

    @Mock
    private CourierRepository courierRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Captor
    private ArgumentCaptor<Employee> employeeCaptor;

    // --- TEST DATA FACTORY ---
    private EmployeeCreationDto.EmployeeCreationDtoBuilder createBaseDtoBuilder() {
        return EmployeeCreationDto.builder()
                .username("newemployee")
                .email("emp@example.com")
                .password("rawPassword123")
                .firstName("John")
                .lastName("Doe")
                .employeeNumber("EMP-123")
                .hireDate(LocalDate.now())
                .salary(BigDecimal.valueOf(2000.00));
    }

    @Nested
    @DisplayName("create(EmployeeCreationDto) Tests")
    class CreateTests {

        @Test
        @DisplayName("Defense in Depth: Should throw NullPointerException when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThatThrownBy(() -> employeeService.create(null))
                    .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(employeeRepository, userRepository, officeRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Happy Path: Should successfully create an OfficeClerk pre-verified without dispatching emails")
        void shouldCreateOfficeClerkSuccessfully() {
            // Arrange
            EmployeeCreationDto dto = createBaseDtoBuilder()
                    .applicationRole(ApplicationRole.CLERK)
                    .officeId(1L)
                    .build();

            Office mockOffice = new Office();
            mockOffice.setId(1L);

            given(userRepository.findByUsername(dto.username())).willReturn(Optional.empty());
            given(userRepository.findByEmail(dto.email())).willReturn(Optional.empty());
            given(employeeRepository.findByEmployeeNumber(dto.employeeNumber())).willReturn(Optional.empty());
            given(officeRepository.findById(1L)).willReturn(Optional.of(mockOffice));
            given(passwordEncoder.encode(dto.password())).willReturn("hashed-pwd");

            OfficeClerk savedClerk = new OfficeClerk();
            savedClerk.setId(UUID.randomUUID());
            savedClerk.setEmail(dto.email());
            savedClerk.setOffice(mockOffice);
            savedClerk.setApplicationRole(ApplicationRole.CLERK);
            given(employeeRepository.save(any(Employee.class))).willReturn(savedClerk);

            // Act
            EmployeeViewDto result = employeeService.create(dto);

            // Assert
            assertThat(result).isNotNull();

            // Verify Entity Persistence
            verify(employeeRepository).save(employeeCaptor.capture());
            assertThat(employeeCaptor.getValue().isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("Happy Path: Should successfully create a Courier without an office")
        void shouldCreateCourierSuccessfully() {
            // Arrange
            EmployeeCreationDto dto = createBaseDtoBuilder()
                    .applicationRole(ApplicationRole.COURIER)
                    .build();

            given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
            given(employeeRepository.findByEmployeeNumber(anyString())).willReturn(Optional.empty());
            given(passwordEncoder.encode(anyString())).willReturn("hashed");

            Courier savedCourier = new Courier();
            savedCourier.setId(UUID.randomUUID());
            savedCourier.setApplicationRole(ApplicationRole.COURIER);
            savedCourier.setEmployeeNumber(dto.employeeNumber());
            given(employeeRepository.save(any(Employee.class))).willReturn(savedCourier);

            // Act
            EmployeeViewDto result = employeeService.create(dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.applicationRole()).isEqualTo(ApplicationRole.COURIER);
            assertThat(result.officeId()).isNull();

            verify(employeeRepository).save(employeeCaptor.capture());
            Employee capturedEmployee = employeeCaptor.getValue();
            assertThat(capturedEmployee).isInstanceOf(Courier.class);
            verifyNoInteractions(officeRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when creating a Clerk without an Office ID")
        void shouldThrowExceptionWhenClerkHasNoOffice() {
            // Arrange
            EmployeeCreationDto dto = createBaseDtoBuilder()
                    .applicationRole(ApplicationRole.CLERK)
                    .officeId(null)
                    .build();

            given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
            given(employeeRepository.findByEmployeeNumber(anyString())).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> employeeService.create(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException on duplicate email")
        void shouldThrowExceptionOnDuplicateEmail() {
            // Arrange
            EmployeeCreationDto dto = createBaseDtoBuilder()
                    .applicationRole(ApplicationRole.COURIER)
                    .build();

            given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());

            User existingUser = new Courier();
            given(userRepository.findByEmail(dto.email())).willReturn(Optional.of(existingUser));

            // Act & Assert
            assertThatThrownBy(() -> employeeService.create(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATE);

            verifyNoInteractions(passwordEncoder, officeRepository);
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException on duplicate employee number")
        void shouldThrowExceptionOnDuplicateEmployeeNumber() {
            // Arrange
            EmployeeCreationDto dto = createBaseDtoBuilder()
                    .applicationRole(ApplicationRole.COURIER)
                    .build();

            given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
            
            Courier existingCourier = new Courier();
            given(employeeRepository.findByEmployeeNumber(dto.employeeNumber())).willReturn(Optional.of(existingCourier));

            // Act & Assert
            assertThatThrownBy(() -> employeeService.create(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMPLOYEE_NUMBER_DUPLICATE);

            verifyNoInteractions(passwordEncoder, officeRepository);
            verify(employeeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getById(UUID) Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Happy Path: Should successfully return an employee")
        void shouldReturnEmployee() {
            // Arrange
            UUID empId = UUID.randomUUID();
            Courier courier = new Courier();
            courier.setId(empId);
            courier.setUsername("test_courier");

            given(employeeRepository.findById(empId)).willReturn(Optional.of(courier));

            // Act
            EmployeeViewDto result = employeeService.getById(empId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(empId);
            assertThat(result.username()).isEqualTo("test_courier");

            verify(employeeRepository).findById(empId);
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when employee not found")
        void shouldThrowExceptionWhenNotFound() {
            // Arrange
            UUID empId = UUID.randomUUID();
            given(employeeRepository.findById(empId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> employeeService.getById(empId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);

            verify(employeeRepository).findById(empId);
        }
    }

    @Nested
    @DisplayName("getAll(Pageable) Tests")
    class GetAllTests {

        @Test
        @DisplayName("Happy Path: Should return paginated employees")
        void shouldReturnPaginatedEmployees() {
            // Arrange
            PageRequest pageRequest = PageRequest.of(0, 10);
            Courier courier = new Courier();
            courier.setId(UUID.randomUUID());
            
            Page<Courier> courierPage = new PageImpl<>(List.of(courier), pageRequest, 1);
            given(courierRepository.findAll(pageRequest)).willReturn(courierPage);
            given(officeClerkRepository.findAll(pageRequest)).willReturn(Page.empty());

            // Act
            Page<EmployeeViewDto> result = employeeService.getAll(pageRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);

            verify(courierRepository).findAll(pageRequest);
            verify(officeClerkRepository).findAll(pageRequest);
        }
    }

    @Nested
    @DisplayName("updateBasicInfo(UUID, EmployeeUpdateDto) Tests")
    class UpdateBasicInfoTests {

        @Test
        @DisplayName("Defense in Depth: Should throw NullPointerException when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThatThrownBy(() -> employeeService.updateBasicInfo(UUID.randomUUID(), null))
                    .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(employeeRepository, userRepository, officeRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when employee not found")
        void shouldThrowExceptionWhenEmployeeNotFound() {
            // Arrange
            UUID empId = UUID.randomUUID();
            EmployeeUpdateDto updateDto = EmployeeUpdateDto.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .salary(BigDecimal.valueOf(3000.00))
                    .build();

            given(employeeRepository.findById(empId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> employeeService.updateBasicInfo(empId, updateDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);

            verifyNoInteractions(userRepository, officeRepository);
        }

        @Test
        @DisplayName("Happy Path: Should safely update basic info and reassign office for Clerk")
        void shouldUpdateInfoAndReassignOfficeForClerk() {
            // Arrange
            UUID clerkId = UUID.randomUUID();
            EmployeeUpdateDto updateDto = EmployeeUpdateDto.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .salary(BigDecimal.valueOf(3000.00))
                    .officeId(2L) // New office
                    .build();

            Office oldOffice = new Office();
            oldOffice.setId(1L);

            Office newOffice = new Office();
            newOffice.setId(2L);

            OfficeClerk existingClerk = new OfficeClerk();
            existingClerk.setId(clerkId);
            existingClerk.setEmail("old@example.com");
            existingClerk.setOffice(oldOffice);

            given(employeeRepository.findById(clerkId)).willReturn(Optional.of(existingClerk));
            given(userRepository.findByEmail("jane.smith@example.com")).willReturn(Optional.empty()); // No email collision
            given(officeRepository.findById(2L)).willReturn(Optional.of(newOffice));
            given(employeeRepository.save(any(Employee.class))).willReturn(existingClerk);

            // Act
            employeeService.updateBasicInfo(clerkId, updateDto);

            // Assert
            verify(employeeRepository).save(employeeCaptor.capture());
            OfficeClerk savedClerk = (OfficeClerk) employeeCaptor.getValue();
            
            assertThat(savedClerk.getFirstName()).isEqualTo("Jane");
            assertThat(savedClerk.getEmail()).isEqualTo("jane.smith@example.com");
            assertThat(savedClerk.getOffice().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when reassigning Clerk to non-existent Office")
        void shouldThrowExceptionWhenReassigningClerkToInvalidOffice() {
            // Arrange
            UUID clerkId = UUID.randomUUID();
            EmployeeUpdateDto updateDto = EmployeeUpdateDto.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .salary(BigDecimal.valueOf(3000.00))
                    .officeId(999L)
                    .build();

            Office oldOffice = new Office();
            oldOffice.setId(1L);

            OfficeClerk existingClerk = new OfficeClerk();
            existingClerk.setId(clerkId);
            existingClerk.setEmail("jane.smith@example.com");
            existingClerk.setOffice(oldOffice);

            given(employeeRepository.findById(clerkId)).willReturn(Optional.of(existingClerk));
            given(officeRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> employeeService.updateBasicInfo(clerkId, updateDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.OFFICE_NOT_FOUND);

            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException on email collision during update")
        void shouldThrowExceptionOnEmailCollisionDuringUpdate() {
            // Arrange
            UUID empId = UUID.randomUUID();
            EmployeeUpdateDto updateDto = EmployeeUpdateDto.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("taken@example.com")
                    .salary(BigDecimal.valueOf(3000.00))
                    .build();

            Courier existingCourier = new Courier();
            existingCourier.setId(empId);
            existingCourier.setEmail("old@example.com");

            given(employeeRepository.findById(empId)).willReturn(Optional.of(existingCourier));
            
            User otherUser = new Courier();
            given(userRepository.findByEmail("taken@example.com")).willReturn(Optional.of(otherUser));

            // Act & Assert
            assertThatThrownBy(() -> employeeService.updateBasicInfo(empId, updateDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATE);

            verify(employeeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivate(UUID) Tests")
    class DeactivateTests {

        @Test
        @DisplayName("Happy Path: Should successfully soft delete an employee")
        void shouldDeactivateEmployee() {
            // Arrange
            UUID empId = UUID.randomUUID();
            Courier employee = new Courier();
            employee.setId(empId);
            employee.setActive(true);

            given(employeeRepository.findById(empId)).willReturn(Optional.of(employee));

            // Act
            employeeService.deactivate(empId);

            // Assert
            verify(employeeRepository).save(employeeCaptor.capture());
            assertThat(employeeCaptor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when employee not found")
        void shouldThrowExceptionWhenEmployeeNotFound() {
            // Arrange
            UUID empId = UUID.randomUUID();
            given(employeeRepository.findById(empId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> employeeService.deactivate(empId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);

            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Idempotency: Should do nothing when deactivating an already inactive employee")
        void shouldDoNothingWhenDeactivatingInactiveEmployee() {
            // Arrange
            UUID empId = UUID.randomUUID();
            Courier employee = new Courier();
            employee.setId(empId);
            employee.setActive(false);

            given(employeeRepository.findById(empId)).willReturn(Optional.of(employee));

            // Act
            employeeService.deactivate(empId);

            // Assert
            verify(employeeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("forcePasswordReset(UUID, AdminPasswordResetDto) Tests")
    class ForcePasswordResetTests {

        @Test
        @DisplayName("Defense in Depth: Should throw NullPointerException when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThatThrownBy(() -> employeeService.forcePasswordReset(UUID.randomUUID(), null))
                    .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(employeeRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Happy Path: Should securely hash and update password")
        void shouldForcePasswordReset() {
            // Arrange
            UUID empId = UUID.randomUUID();
            AdminPasswordResetDto resetDto = new AdminPasswordResetDto("newSecurePass!123");
            
            Courier employee = new Courier();
            employee.setId(empId);
            employee.setPassword("oldHash");

            given(employeeRepository.findById(empId)).willReturn(Optional.of(employee));
            given(passwordEncoder.encode(resetDto.newPassword())).willReturn("newHash");

            // Act
            employeeService.forcePasswordReset(empId, resetDto);

            // Assert
            verify(employeeRepository).save(employeeCaptor.capture());
            assertThat(employeeCaptor.getValue().getPassword()).isEqualTo("newHash");
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when employee not found")
        void shouldThrowExceptionWhenEmployeeNotFound() {
            // Arrange
            UUID empId = UUID.randomUUID();
            AdminPasswordResetDto resetDto = new AdminPasswordResetDto("newSecurePass!123");

            given(employeeRepository.findById(empId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> employeeService.forcePasswordReset(empId, resetDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);

            verifyNoInteractions(passwordEncoder);
            verify(employeeRepository, never()).save(any());
        }
    }
    
    @Nested
    @DisplayName("activate(UUID) Tests")
    class ActivateTests {

        @Test
        @DisplayName("Happy Path: Should successfully activate a deactivated employee")
        void shouldActivateEmployee() {
            // Arrange
            UUID empId = UUID.randomUUID();
            Courier employee = new Courier();
            employee.setId(empId);
            employee.setActive(false);

            given(employeeRepository.findById(empId)).willReturn(Optional.of(employee));

            // Act
            employeeService.activate(empId);

            // Assert
            verify(employeeRepository).save(employeeCaptor.capture());
            assertThat(employeeCaptor.getValue().isActive()).isTrue();
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when employee not found")
        void shouldThrowExceptionWhenEmployeeNotFound() {
            // Arrange
            UUID empId = UUID.randomUUID();
            given(employeeRepository.findById(empId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> employeeService.activate(empId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);

            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Idempotency: Should do nothing when activating an already active employee")
        void shouldDoNothingWhenActivatingActiveEmployee() {
            // Arrange
            UUID empId = UUID.randomUUID();
            Courier employee = new Courier();
            employee.setId(empId);
            employee.setActive(true);

            given(employeeRepository.findById(empId)).willReturn(Optional.of(employee));

            // Act
            employeeService.activate(empId);

            // Assert
            verify(employeeRepository, never()).save(any());
        }
    }
}