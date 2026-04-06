package bg.nbu.cscb532.company;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("deprecation")
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CompanyRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("Should save a valid Company entity and generate ID")
    void shouldSaveCompany() {
        // Arrange
        Company company = Company.builder()
                .name("Speedy Logistics")
                .registrationNumber("BG123456789")
                .build();

        // Act
        Company savedCompany = companyRepository.save(company);

        // Assert
        assertThat(savedCompany.getId()).isNotNull();
        assertThat(savedCompany.getName()).isEqualTo("Speedy Logistics");
        assertThat(savedCompany.getRegistrationNumber()).isEqualTo("BG123456789");
        assertThat(savedCompany.getCreatedAt()).isNotNull();
        assertThat(savedCompany.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should retrieve a company by its exact registration number")
    void shouldFindByRegistrationNumber() {
        // Arrange
        Company company = Company.builder()
                .name("Econt Express")
                .registrationNumber("BG987654321")
                .build();
        companyRepository.save(company);

        // Act
        Optional<Company> foundCompany = companyRepository.findByRegistrationNumber("BG987654321");

        // Assert
        assertThat(foundCompany).isPresent();
        assertThat(foundCompany.get().getName()).isEqualTo("Econt Express");
    }

    @Test
    @DisplayName("Should return empty Optional when registration number does not exist")
    void shouldReturnEmptyWhenRegistrationNumberNotFound() {
        // Act
        Optional<Company> foundCompany = companyRepository.findByRegistrationNumber("UNKNOWN");

        // Assert
        assertThat(foundCompany).isEmpty();
    }

    @Test
    @DisplayName("Should retrieve a company by its exact name")
    void shouldGetCompanyByName_HappyPath() {
        // Arrange
        Company company = Company.builder()
                .name("DHL Express")
                .registrationNumber("BG112233445")
                .build();
        companyRepository.save(company);

        // Act
        Company foundCompany = companyRepository.getCompanyByName("DHL Express");

        // Assert
        assertThat(foundCompany).isNotNull();
        assertThat(foundCompany.getRegistrationNumber()).isEqualTo("BG112233445");
    }

    @Test
    @DisplayName("Should return null when company name does not exist")
    void shouldReturnNullWhenCompanyByNameNotFound_ErrorCase() {
        // Act
        Company foundCompany = companyRepository.getCompanyByName("Nonexistent Company");

        // Assert
        assertThat(foundCompany).isNull();
    }

    @Test
    @DisplayName("Should retrieve company by name case sensitively")
    void shouldGetCompanyByNameCaseSensitive_EdgeCase() {
        // Arrange
        Company company = Company.builder()
                .name("FedEx")
                .registrationNumber("BG998877665")
                .build();
        companyRepository.save(company);

        // Act & Assert
        Company lowercaseSearch = companyRepository.getCompanyByName("fedex");
        assertThat(lowercaseSearch).isNull();

        Company exactCaseSearch = companyRepository.getCompanyByName("FedEx");
        assertThat(exactCaseSearch).isNotNull();
    }

    @Test
    @DisplayName("Should throw DataIntegrityViolationException when saving company with duplicate registration number")
    void shouldThrowExceptionOnDuplicateRegistrationNumber() {
        // Arrange
        Company company1 = Company.builder()
                .name("Econt Express")
                .registrationNumber("DUPLICATE_REG_NUM")
                .build();
        companyRepository.saveAndFlush(company1);

        Company company2 = Company.builder()
                .name("Another Company")
                .registrationNumber("DUPLICATE_REG_NUM")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> companyRepository.saveAndFlush(company2))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("duplicate key value violates unique constraint");
    }

    @Test
    @DisplayName("Should throw DataIntegrityViolationException when saving company with null name")
    void shouldThrowExceptionOnNullName() {
        // Arrange
        Company company = Company.builder()
                .name(null)
                .registrationNumber("BG987654321")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> companyRepository.saveAndFlush(company))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("null value in column \"name\"");
    }
    
    @Test
    @DisplayName("Should throw DataIntegrityViolationException when saving company with name exceeding max length")
    void shouldThrowExceptionOnNameExceedingMaxLength() {
        // Arrange
        String longName = "A".repeat(Constants.Validation.MAX_NAME_LENGTH + 1);
        Company company = Company.builder()
                .name(longName)
                .registrationNumber("VALID_REG_NUM")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> companyRepository.saveAndFlush(company))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("value too long for type character varying");
    }
}