package bg.nbu.cscb532.employee;

import bg.nbu.cscb532.company.Company;
import bg.nbu.cscb532.company.CompanyRepository;
import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.office.CityRepository;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.office.OfficeRepository;
import bg.nbu.cscb532.shared.config.JpaConfig;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.user.ApplicationRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
public class OfficeClerkRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private OfficeClerkRepository officeClerkRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CityRepository cityRepository;

    // --- TEST DATA FACTORY ---
    private Company createAndSaveCompany() {
        Company company = new Company();
        company.setName("Test Company");
        company.setRegistrationNumber("TEST-REG-123");
        return companyRepository.saveAndFlush(company);
    }

    private City createAndSaveCity(String name, String postcode) {
        City city = new City();
        city.setName(name);
        city.setPostcode(postcode);
        return cityRepository.saveAndFlush(city);
    }

    private Office createAndSaveOffice(Company company, City city) {
        Office office = new Office();
        office.setCompany(company);

        AddressDetails addressDetails = new AddressDetails();
        addressDetails.setCity(city);
        addressDetails.setStreet("Test Street 1");
        office.setAddressDetails(addressDetails);

        return officeRepository.saveAndFlush(office);
    }

    private void createAndSaveOfficeClerk(String username, String email, String employeeNumber, Office office) {
        OfficeClerk clerk = new OfficeClerk();
        clerk.setUsername(username);
        clerk.setEmail(email);
        clerk.setPassword("hashed-password");
        clerk.setFirstName("Test");
        clerk.setLastName("Clerk");
        clerk.setApplicationRole(ApplicationRole.CLERK);
        clerk.setEmployeeNumber(employeeNumber);
        clerk.setHireDate(LocalDate.now());
        clerk.setSalary(BigDecimal.valueOf(2500.00));
        clerk.setOffice(office);

        officeClerkRepository.saveAndFlush(clerk);
    }

    @Nested
    @DisplayName("findOfficeClerksByOfficeId(Long, Pageable) Tests")
    class FindByOfficeIdTests {

        @Test
        @DisplayName("Happy Path: Should return paginated list of clerks for a specific office")
        void shouldReturnClerksForOffice() {

            // Arrange
            Company company = createAndSaveCompany();
            City city = createAndSaveCity("Sofia", "1000");
            Office office1 = createAndSaveOffice(company, city);
            Office office2 = createAndSaveOffice(company, city);

            createAndSaveOfficeClerk("ClerkA", "a@test.com", "ID-A", office1);
            createAndSaveOfficeClerk("ClerkB", "b@test.com", "ID-B", office1);
            createAndSaveOfficeClerk("ClerkC", "c@test.com", "ID-C", office1);
            createAndSaveOfficeClerk("ClerkD", "d@test.com", "ID-D", office2);

            // Act
            Page<OfficeClerk> resultPage = officeClerkRepository.findOfficeClerksByOfficeId(office1.getId(), PageRequest.of(0, 2));

            // Assert
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(3);
            assertThat(resultPage.getTotalPages()).isEqualTo(2);
            assertThat(resultPage.getContent()).hasSize(2);
            assertThat(resultPage.getContent().get(0).getOffice().getId()).isEqualTo(office1.getId());
            assertThat(resultPage.getContent().get(1).getOffice().getId()).isEqualTo(office1.getId());
        }

        @Test
        @DisplayName("Edge Case: Should return empty page when office has no clerks")
        void shouldReturnEmptyPageForEmptyOffice() {

            // Act
            Page<OfficeClerk> resultPage = officeClerkRepository.findOfficeClerksByOfficeId(999L, PageRequest.of(0, 10));

            // Assert
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.isEmpty()).isTrue();
            assertThat(resultPage.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Isolation Case: Should not return clerks belonging to other offices")
        void shouldNotReturnClerksFromOtherOffices() {

            // Arrange
            Company company = createAndSaveCompany();
            City city = createAndSaveCity("Varna", "9000");

            Office officeA = createAndSaveOffice(company, city);
            Office officeB = createAndSaveOffice(company, city);

            createAndSaveOfficeClerk("officeA_clerk", "a@test.com", "A001", officeA);
            createAndSaveOfficeClerk("officeB_clerk", "b@test.com", "B001", officeB);

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<OfficeClerk> result = officeClerkRepository.findOfficeClerksByOfficeId(officeA.getId(), pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getUsername()).isEqualTo("officeA_clerk");
            assertThat(result.getContent())
                    .extracting(OfficeClerk::getUsername)
                    .doesNotContain("officeB_clerk");
        }
    }

}
