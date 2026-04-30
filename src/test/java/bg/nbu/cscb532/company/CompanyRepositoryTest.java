package bg.nbu.cscb532.company;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.config.JpaConfig;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Company Repository Integration Tests")
class CompanyRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private CompanyRepository companyRepository;

    // --- DATA FACTORY ---

    private Company createCompanyEntity(String name, String reg) {
        return Company.builder()
                .name(name)
                .registrationNumber(reg)
                .build();
    }

    @Nested
    @DisplayName("Persistence & Auditing")
    class PersistenceTests {

        @Test
        @DisplayName("Should save valid company and populate auditing fields")
        void shouldSaveAndAuditCompany() {
            // Arrange
            Company company = createCompanyEntity("Speedy Logistics", "BG123456789");

            // Act
            Company saved = companyRepository.saveAndFlush(company);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Query Methods (findByName & findByRegistrationNumber)")
    class QueryMethodTests {

        @Test
        @DisplayName("Happy Path: Should retrieve company by unique name")
        void shouldFindByName() {
            // Arrange
            companyRepository.saveAndFlush(createCompanyEntity("DHL Express", "REG-1"));

            // Act
            Optional<Company> found = companyRepository.findByName("DHL Express");

            // Assert
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("DHL Express");
        }

        @Test
        @DisplayName("Edge Case: Should be case-sensitive for name lookups")
        void shouldBeCaseSensitiveForName() {
            // Arrange
            companyRepository.saveAndFlush(createCompanyEntity("FedEx", "REG-2"));

            // Act and Assert
            assertThat(companyRepository.findByName("fedex")).isEmpty();
            assertThat(companyRepository.findByName("FedEx")).isPresent();
        }

        @Test
        @DisplayName("Happy Path: Should retrieve company by registration number")
        void shouldFindByRegistrationNumber() {
            // Arrange
            companyRepository.saveAndFlush(createCompanyEntity("Econt", "BG987654321"));

            // Act
            Optional<Company> found = companyRepository.findByRegistrationNumber("BG987654321");

            // Assert
            assertThat(found).isPresent();
            assertThat(found.get().getRegistrationNumber()).isEqualTo("BG987654321");
        }
    }

    @Nested
    @DisplayName("Data Integrity & Constraints")
    class ConstraintTests {

        @Test
        @DisplayName("Constraint: Should throw exception on duplicate registration number")
        void shouldThrowOnDuplicateReg() {
            // Arrange
            companyRepository.saveAndFlush(createCompanyEntity("Company A", "UNIQUE-REG"));
            Company duplicate = createCompanyEntity("Company B", "UNIQUE-REG");

            // Act and Assert
            assertThatThrownBy(() -> companyRepository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }


        @Test
        @DisplayName("Constraint: Should throw exception when name is null")
        void shouldThrowOnNullName() {
            // Arrange
            Company invalid = createCompanyEntity(null, "REG-NULL");

            // Act and Assert
            assertThatThrownBy(() -> companyRepository.saveAndFlush(invalid))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Constraint: Should throw exception when name exceeds max length")
        void shouldThrowOnLongName() {
            // Arrange
            String longName = "A".repeat(Constants.Validation.MAX_NAME_LENGTH + 1);
            Company invalid = createCompanyEntity(longName, "REG-LONG");

            // Act and Assert
            assertThatThrownBy(() -> companyRepository.saveAndFlush(invalid))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}