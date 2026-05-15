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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("deprecation")
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CityRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private CityRepository cityRepository;

    private City createUniqueCity(String name) {
        return City.builder()
                .name(name)
                .postcode(UUID.randomUUID().toString().substring(0, 5))
                .build();
    }

    @Nested
    @DisplayName("findByPostcode(String) Tests")
    class FindByPostcodeTests {

        @Test
        @DisplayName("Happy Path: Should find city when exact postcode match exists")
        void shouldFindCityWhenExactMatchExists() {

            // Arrange:
            var city = createUniqueCity("Troyan");
            String savedPostcode = city.getPostcode();
            cityRepository.save(city);

            // Act:
            Optional<City> result = cityRepository.findByPostcode(savedPostcode);

            // Assert:
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Troyan");
            assertThat(result.get().getPostcode()).isEqualTo(savedPostcode);
        }

        @Test
        @DisplayName("Error Case: Should return empty when postcode does not exist")
        void shouldReturnEmptyWhenPostcodeNotFound() {

            // Arrange
            City city = createUniqueCity("Troyan");
            cityRepository.save(city);

            // Act
            Optional<City> result = cityRepository.findByPostcode("99999");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByName(String) Tests")
    class FindAllByNameTests {

        @Test
        @DisplayName("Happy Path: Should return all cities with the exact same name")
        void shouldReturnMultipleCitiesWhenSameNameExists() {

            // Arrange
            City city1 = createUniqueCity("Troyan");
            City city2 = createUniqueCity("Troyan");
                    
            cityRepository.saveAll(List.of(city1, city2));

            // Act
            var results = cityRepository.findAllByName("Troyan");

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(City::getPostcode)
                    .containsExactlyInAnyOrder(city1.getPostcode(), city2.getPostcode());
        }

        @Test
        @DisplayName("Error Case: Should return empty list when name does not exist")
        void shouldReturnEmptyListWhenNameNotFound() {

            // Act
            var results = cityRepository.findAllByName("Atlantis");

            // Assert
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Database Constraints (Schema Validation)")
    class DatabaseConstraintTests {

        @Test
        @DisplayName("Happy Path: Should allow saving two cities with the same name if postcodes differ")
        void shouldAllowSameNameDifferentPostcode() {

            // Arrange
            City city1 = createUniqueCity("Troyan");
            City city2 = createUniqueCity("Troyan");

            // Act and Assert
            cityRepository.saveAndFlush(city1);
            cityRepository.saveAndFlush(city2); 
            
            assertThat(cityRepository.count()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Error Case: Should throw Exception when inserting duplicate postcode even if name differs")
        void shouldThrowExceptionOnDuplicatePostcode() {

            // Arrange
            String duplicatePostcode = "99999";
            City city1 = City.builder().name("Lovech").postcode(duplicatePostcode).build();
            cityRepository.saveAndFlush(city1);

            City city2 = City.builder().name("Troyan").postcode(duplicatePostcode).build();

            // Act and Assert
            assertThatThrownBy(() -> cityRepository.saveAndFlush(city2))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("duplicate key value violates unique constraint");
        }
        
        @Test
        @DisplayName("Error Case: Should throw Exception when postcode exceeds max length")
        void shouldThrowExceptionWhenPostcodeTooLong() {

            // Arrange
            String invalidPostcode = "1".repeat(Constants.Validation.MAX_POSTAL_CODE_LENGTH + 1);
            City city = City.builder().name("Sofia").postcode(invalidPostcode).build();

            // Act and Assert
            assertThatThrownBy(() -> cityRepository.saveAndFlush(city))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("value too long for type character varying");
        }
    }
}