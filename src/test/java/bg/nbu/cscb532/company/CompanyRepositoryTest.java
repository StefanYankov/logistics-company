package bg.nbu.cscb532.company;

import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.office.CityRepository;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.config.JpaConfig;
import bg.nbu.cscb532.shared.location.AddressDetails;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

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

    @Autowired
    private CityRepository cityRepository;

    private City testCity;

    @BeforeEach
    void setUp() {
        // Ensure a city exists to satisfy the foreign key constraint for AddressDetails
        testCity = cityRepository.saveAndFlush(new City("Test City", "1234"));
    }

    // --- DATA FACTORY ---

    private Company createUniqueCompanyEntity(String name) {
        AddressDetails address = AddressDetails.builder()
                .city(testCity)
                .street("123 Test Street")
                .build();

        return Company.builder()
                .name(name)
                .registrationNumber("REG-" + UUID.randomUUID().toString().substring(0, 8))
                .addressDetails(address)
                .build();
    }

    @Nested
    @DisplayName("Persistence & Auditing")
    class PersistenceTests {

        @Test
        @DisplayName("Should save valid company and populate auditing fields")
        void shouldSaveAndAuditCompany() {
            // Arrange
            Company company = createUniqueCompanyEntity("Speedy Logistics");

            // Act
            Company saved = companyRepository.saveAndFlush(company);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getAddressDetails().getStreet()).isEqualTo("123 Test Street");
        }
    }

    @Nested
    @DisplayName("Query Methods (findByName & findByRegistrationNumber)")
    class QueryMethodTests {

        @Test
        @DisplayName("Happy Path: Should retrieve company by unique name")
        void shouldFindByName() {
            // Arrange
            companyRepository.saveAndFlush(createUniqueCompanyEntity("DHL Express"));

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
            companyRepository.saveAndFlush(createUniqueCompanyEntity("FedEx"));

            // Act and Assert
            assertThat(companyRepository.findByName("fedex")).isEmpty();
            assertThat(companyRepository.findByName("FedEx")).isPresent();
        }

        @Test
        @DisplayName("Happy Path: Should retrieve company by registration number")
        void shouldFindByRegistrationNumber() {
            // Arrange
            Company savedCompany = companyRepository.saveAndFlush(createUniqueCompanyEntity("Econt"));
            String regNumber = savedCompany.getRegistrationNumber();

            // Act
            Optional<Company> found = companyRepository.findByRegistrationNumber(regNumber);

            // Assert
            assertThat(found).isPresent();
            assertThat(found.get().getRegistrationNumber()).isEqualTo(regNumber);
        }
    }

    @Nested
    @DisplayName("Data Integrity & Constraints")
    class ConstraintTests {

        @Test
        @DisplayName("Constraint: Should throw exception on duplicate registration number")
        void shouldThrowOnDuplicateReg() {
            // Arrange
            Company companyA = createUniqueCompanyEntity("Company A");
            String uniqueReg = companyA.getRegistrationNumber();
            companyRepository.saveAndFlush(companyA);
            
            Company duplicate = createUniqueCompanyEntity("Company B");
            duplicate.setRegistrationNumber(uniqueReg);

            // Act and Assert
            assertThatThrownBy(() -> companyRepository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }


        @Test
        @DisplayName("Constraint: Should throw exception when name is null")
        void shouldThrowOnNullName() {
            // Arrange
            Company invalid = createUniqueCompanyEntity(null);

            // Act and Assert
            assertThatThrownBy(() -> companyRepository.saveAndFlush(invalid))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Constraint: Should throw exception when name exceeds max length")
        void shouldThrowOnLongName() {
            // Arrange
            String longName = "A".repeat(Constants.Validation.MAX_NAME_LENGTH + 1);
            Company invalid = createUniqueCompanyEntity(longName);

            // Act and Assert
            assertThatThrownBy(() -> companyRepository.saveAndFlush(invalid))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
