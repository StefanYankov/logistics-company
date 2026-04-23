package bg.nbu.cscb532.employee;

import bg.nbu.cscb532.shared.config.JpaConfig;
import bg.nbu.cscb532.user.ApplicationRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EmployeeRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private EmployeeRepository employeeRepository;

    // --- TEST DATA FACTORY ---
    private Employee createEmployeeEntity(String username, String email, String employeeNumber) {
        Courier employee = new Courier();
        employee.setUsername(username);
        employee.setEmail(email);
        employee.setPassword("hashed-password");
        employee.setFirstName("Test");
        employee.setLastName("User");
        employee.setApplicationRole(ApplicationRole.COURIER);
        employee.setEmployeeNumber(employeeNumber);
        employee.setHireDate(LocalDate.now());
        employee.setSalary(BigDecimal.valueOf(2500.00));
        return employee;
    }

    @Nested
    @DisplayName("findByEmployeeNumber(String) Tests")
    class FindByEmployeeNumberTests {

        @Test
        @DisplayName("Happy Path: Should find employee when exact number exists")
        void shouldFindEmployeeWhenNumberExists() {
            // Arrange
            employeeRepository.saveAndFlush(createEmployeeEntity("emp1", "emp1@test.com", "EMP-001"));

            // Act
            Optional<Employee> result = employeeRepository.findByEmployeeNumber("EMP-001");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getEmployeeNumber()).isEqualTo("EMP-001");
        }

        @Test
        @DisplayName("Error Case: Should return empty when employee number does not exist")
        void shouldReturnEmptyWhenNumberNotFound() {
            // Act
            Optional<Employee> result = employeeRepository.findByEmployeeNumber("NON-EXISTENT");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Edge Case: Should return empty when case differs (assuming default case-sensitive collation)")
        void shouldReturnEmptyWhenCaseIsDifferent() {
            // Arrange
            employeeRepository.saveAndFlush(createEmployeeEntity("emp2", "emp2@test.com", "EMP-002"));

            // Act
            Optional<Employee> result = employeeRepository.findByEmployeeNumber("emp-002");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Database Constraint Tests")
    class ConstraintTests {

        @Test
        @DisplayName("Error Case: Should throw DataIntegrityViolationException on duplicate employee number")
        void shouldThrowOnDuplicateEmployeeNumber() {
            // Arrange
            employeeRepository.saveAndFlush(createEmployeeEntity("user1", "test1@test.com", "DUPLICATE-ID"));
            Employee duplicateEmp = createEmployeeEntity("user2", "test2@test.com", "DUPLICATE-ID");

            // Act & Assert
            assertThatThrownBy(() -> employeeRepository.saveAndFlush(duplicateEmp))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("employee_number");
        }
    }
}
