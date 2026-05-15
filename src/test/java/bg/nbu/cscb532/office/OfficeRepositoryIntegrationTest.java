package bg.nbu.cscb532.office;

import bg.nbu.cscb532.company.Company;
import bg.nbu.cscb532.company.CompanyRepository;
import bg.nbu.cscb532.shared.config.JpaConfig;
import bg.nbu.cscb532.shared.location.AddressDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OfficeRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CompanyRepository companyRepository;

    // --- TEST DATA FACTORY ---
    
    private Company savedCompany;
    private City savedSofia;
    private City savedPlovdiv;

    @BeforeEach
    void setUp() {
        // Persisting the parent entities first to satisfy the FK constraints
        savedCompany = companyRepository.findAll().stream().findFirst().orElseGet(() -> companyRepository.saveAndFlush(Company.builder()
                .name("Speedy Logistics")
                .registrationNumber("BG123")
                .build()));

        savedSofia = cityRepository.saveAndFlush(City.builder()
                .name("Sofia")
                .postcode(UUID.randomUUID().toString().substring(0, 5))
                .build());

        savedPlovdiv = cityRepository.saveAndFlush(City.builder()
                .name("Plovdiv")
                .postcode(UUID.randomUUID().toString().substring(0, 5))
                .build());
    }

    private Office createOfficeEntity(City city, String street, Double lat, Double lon) {
        return Office.builder()
                .company(savedCompany)
                .addressDetails(AddressDetails.builder()
                        .city(city)
                        .street(street)
                        .latitude(lat)
                        .longitude(lon)
                        .build())
                .build();
    }


    @Nested
    @DisplayName("findAllByAddressDetailsCityId(Long) Tests")
    class FindAllByCityIdTests {

        @Test
        @DisplayName("Should find all offices located in a specific city")
        void shouldFindOfficesByCityId() {
            // Arrange
            Office sofiaOffice1 = createOfficeEntity(savedSofia, "Vitosha Blvd", 42.69, 23.32);
            Office sofiaOffice2 = createOfficeEntity(savedSofia, "Tsarigradsko", 42.66, 23.38);
            Office plovdivOffice = createOfficeEntity(savedPlovdiv, "Glavnata", 42.14, 24.74);

            officeRepository.saveAllAndFlush(List.of(sofiaOffice1, sofiaOffice2, plovdivOffice));

            // Act
            List<Office> sofiaResults = officeRepository.findAllByAddressDetailsCityId(savedSofia.getId());
            List<Office> plovdivResults = officeRepository.findAllByAddressDetailsCityId(savedPlovdiv.getId());

            // Assert
            assertThat(sofiaResults).hasSize(2);
            assertThat(sofiaResults).extracting(o -> o.getAddressDetails().getStreet())
                    .containsExactlyInAnyOrder("Vitosha Blvd", "Tsarigradsko");

            assertThat(plovdivResults).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty list when city has no offices")
        void shouldReturnEmptyListWhenCityHasNoOffices() {
            // Act
            List<Office> results = officeRepository.findAllByAddressDetailsCityId(999L);

            // Assert
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("findNearestOfficesWithinRadius(double, double, double) Tests")
    class GeospatialQueryTests {

        @Test
        @DisplayName("Should return offices within the specified radius, ordered by distance")
        void shouldFindNearestOfficesWithinRadius() {
            // Arrange
            // Target Location: Sofia Center (approx 42.697, 23.321)
            double userLat = 42.697;
            double userLon = 23.321;
            
            // Clean up existing seeded offices for pure isolation
            officeRepository.deleteAllInBatch();

            Office closestOffice = createOfficeEntity(savedSofia, "NDK", 42.695, 23.319);
            Office furtherOffice = createOfficeEntity(savedSofia, "Mladost", 42.650, 23.380);
            Office outOfBoundsOffice = createOfficeEntity(savedPlovdiv, "Plovdiv Center", 42.140, 24.740);
            
            Office missingCoordsOffice = createOfficeEntity(savedSofia, "Unknown Location", null, null);

            officeRepository.saveAllAndFlush(List.of(furtherOffice, outOfBoundsOffice, missingCoordsOffice, closestOffice));

            // Act
            List<Office> results = officeRepository.findNearestOfficesWithinRadius(userLat, userLon, 10.0);

            // Assert
            assertThat(results).hasSize(2);
            
            assertThat(results.get(0).getAddressDetails().getStreet()).isEqualTo("NDK");
            assertThat(results.get(1).getAddressDetails().getStreet()).isEqualTo("Mladost");
        }

        @Test
        @DisplayName("Should return empty list when no offices are within radius")
        void shouldReturnEmptyWhenNoneInRadius() {
            // Arrange
            officeRepository.deleteAllInBatch();
            Office plovdivOffice = createOfficeEntity(savedPlovdiv, "Plovdiv Center", 42.140, 24.740); 
            officeRepository.saveAndFlush(plovdivOffice);

            // Act
            List<Office> results = officeRepository.findNearestOfficesWithinRadius(42.697, 23.321, 5.0);

            // Assert
            assertThat(results).isEmpty();
        }
    }
}