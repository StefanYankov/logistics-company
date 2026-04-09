package bg.nbu.cscb532.office;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("deprecation")
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // can work without this as we have @ServiceConnection, but good to have so we do not default to H2 and flyway migration fail
class CityRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private CityRepository cityRepository;

    @Nested
    @DisplayName("findByPostcode(String) Tests")
    class FindByPostcodeTests {

        @Test
        @DisplayName("Happy Path: Should find city when exact postcode match exists")
        void shouldFindCityWhenExactMatchExists() {

            // Arrange:
            var city = City.builder()
                    .name("Troyan")
                    .postcode("5600")
                    .build();
            cityRepository.save(city);

            // Act:
            Optional<City> result = cityRepository.findByPostcode("5600");

            // Assert:
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Troyan");
            assertThat(result.get().getPostcode()).isEqualTo("5600");
        }

        @Test
        @DisplayName("Error Case: Should return empty when postcode does not exist")
        void shouldReturnEmptyWhenPostcodeNotFound() {

            // Arrange:
            var city = City.builder()
                    .name("Troyan")
                    .postcode("5600")
                    .build();
            cityRepository.save(city);

            // Act:
            Optional<City> result = cityRepository.findByPostcode("6491");

            // Assert:
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByName(String) Tests")
    class FindAllByNameTests {

        @Test
        @DisplayName("Happy Path: Should return all cities with the exact same name")
        void shouldReturnMultipleCitiesWhenSameNameExists() {

            // Arrange: The "Two Troyans" scenario
            City city1 = City.builder()
                    .name("Troyan")
                    .postcode("5600") // Lovech province
                    .build();
            City city2 = City.builder()
                    .name("Troyan")
                    .postcode("6491") // Haskovo province
                    .build();
                    
            cityRepository.saveAll(List.of(city1, city2));

            // Act:
            var results = cityRepository.findAllByName("Troyan");

            // Assert:
            assertThat(results).hasSize(2);
            assertThat(results).extracting(City::getPostcode)
                    .containsExactlyInAnyOrder("5600", "6491");
        }

        @Test
        @DisplayName("Error Case: Should return empty list when name does not exist")
        void shouldReturnEmptyListWhenNameNotFound() {

            // Act:
            var results = cityRepository.findAllByName("Atlantis");

            // Assert:
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Database Constraints (Schema Validation)")
    class DatabaseConstraintTests {

        @Test
        @DisplayName("Happy Path: Should allow saving two cities with the same name if postcodes differ")
        void shouldAllowSameNameDifferentPostcode() {

            // Arrange:
            City city1 = City.builder().name("Troyan").postcode("5600").build();
            City city2 = City.builder().name("Troyan").postcode("6491").build();

            // Act & Assert:
            cityRepository.saveAndFlush(city1);
            cityRepository.saveAndFlush(city2); 
            
            assertThat(cityRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("Error Case: Should throw Exception when inserting duplicate postcode even if name differs")
        void shouldThrowExceptionOnDuplicatePostcode() {

            // Arrange
            City city1 = City.builder().name("Lovech").postcode("5600").build();
            cityRepository.saveAndFlush(city1);

            City city2 = City.builder().name("Troyan").postcode("5600").build();

            // Act & Assert:
            // Proves that uk_city_postcode is actively enforced by PostgreSQL, 
            // blocking two different cities from claiming the same postcode!
            assertThatThrownBy(() -> cityRepository.saveAndFlush(city2))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("duplicate key value violates unique constraint");
        }
        
        @Test
        @DisplayName("Error Case: Should throw Exception when postcode exceeds max length")
        void shouldThrowExceptionWhenPostcodeTooLong() {

            // Arrange:
            String invalidPostcode = "1".repeat(Constants.Validation.MAX_POSTAL_CODE_LENGTH + 1);
            City city = City.builder().name("Sofia").postcode(invalidPostcode).build();

            // Act & Assert:
            assertThatThrownBy(() -> cityRepository.saveAndFlush(city))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("value too long for type character varying");
        }
    }
}