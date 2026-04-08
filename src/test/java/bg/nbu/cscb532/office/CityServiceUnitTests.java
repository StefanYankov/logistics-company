package bg.nbu.cscb532.office;

import bg.nbu.cscb532.office.dto.CityDto;
import bg.nbu.cscb532.office.dto.CityViewDto;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("City Service Unit Tests")
public class CityServiceUnitTests {

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityServiceImpl cityService;


    private CityDto createValidCityDto() {
        return CityDto.builder()
                .name("Troyan")
                .postcode("5600")
                .build();
    }

    private City createValidCityEntity(Long id) {
        City city = City.builder()
                .name("Troyan")
                .postcode("5600")
                .build();
        city.setId(id);
        return city;
    }


    @Nested
    @DisplayName("City Create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create a city when given a valid DTO")
        void shouldCreateSuccessfully_HappyPath(){
            // Arrange
            var createDto = createValidCityDto();
            var savedEntity = createValidCityEntity(1L);

            given(cityRepository.findByNameAndPostcode("Troyan", "5600")).willReturn(Optional.empty());
            given(cityRepository.save(any(City.class))).willReturn(savedEntity);

            // Act
            var result = cityService.create(createDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Troyan");
            assertThat(result.postcode()).isEqualTo("5600");

            verify(cityRepository).findByNameAndPostcode("Troyan", "5600");
            verify(cityRepository).save(any(City.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when creating a city with an exact existing name and postcode")
        void shouldThrowDuplicateException_ErrorCase(){
            // Arrange
            var createDto = createValidCityDto();

            // Simulate the database already having this exact combination
            given(cityRepository.findByNameAndPostcode(anyString(), anyString()))
                    .willReturn(Optional.of(createValidCityEntity(1L)));

            // Act & Assert
            assertThatThrownBy(() -> cityService.create(createDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.CITY_DUPLICATE.getDefaultMessage())
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CITY_DUPLICATE);

            verify(cityRepository).findByNameAndPostcode("Troyan", "5600");
            verifyNoMoreInteractions(cityRepository); 
        }

        @Test
        @DisplayName("Should throw NullPointerException when create DTO is null (Defense in Depth)")
        void shouldThrowNpeWhenCreateDtoIsNull_ErrorCase(){
            // Act & Assert
            assertThatThrownBy(() -> cityService.create(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(Constants.DeveloperErrors.DTO_NULL);

            verifyNoInteractions(cityRepository);
        }
    }

    @Nested
    @DisplayName("City Update Tests")
    class UpdateTests{

        @Test
        @DisplayName("Should update city name when given valid ID and DTO")
        void shouldUpdateCompanyWhenIdAndDtoAreValid_HappyPath() {

            // Arrange
            Long targetId = 1L;
            var updateDto = createValidCityDto();
            var existingEntity = createValidCityEntity(targetId);

            given(cityRepository.findById(targetId)).willReturn(Optional.of(existingEntity));
            given(cityRepository.save(any(City.class))).willReturn(existingEntity);

            // Act
            var result = cityService.update(targetId, updateDto);

            // Assert
            assertThat(result.name()).isEqualTo(updateDto.name());
            assertThat(result.postcode()).isEqualTo(updateDto.postcode());

            verify(cityRepository).findById(targetId);
            verify(cityRepository).save(existingEntity);

        }

        @Test
        @DisplayName("Should throw BusinessException when updating a non-existent city ID")
        void shouldThrowExceptionWhenUpdatingNonExistentId_ErrorCase() {

            // Arrange
            Long invalidId = 999L;
            var updateDto = createValidCityDto();

            given(cityRepository.findById(invalidId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cityService.update(invalidId, updateDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.CITY_NOT_FOUND.getDefaultMessage());

            verify(cityRepository).findById(invalidId);
            verifyNoMoreInteractions(cityRepository);
        }

        @Test
        @DisplayName("Should throw NullPointerException when update ID is null (Defense in Depth)")
        void shouldThrowNpeWhenUpdateIdIsNull_ErrorCase() {

            // Arrange
            var validDto = createValidCityDto();

            // Act & Assert
            assertThatThrownBy(() -> cityService.update(null, validDto))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(Constants.DeveloperErrors.ENTITY_ID_NULL);

            verifyNoInteractions(cityRepository);
        }

        @Test
        @DisplayName("Should throw NullPointerException when update DTO is null (Defense in Depth)")
        void shouldThrowNpeWhenUpdateDtoIsNull_ErrorCase() {
            // Act & Assert
            assertThatThrownBy(() -> cityService.update(1L, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(Constants.DeveloperErrors.DTO_NULL);

            verifyNoInteractions(cityRepository);
        }
    }

    @Nested
    @DisplayName("City Get By ID Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should retrieve city when ID exists")
        void shouldGetCityWhenIdExists_HappyPath() {
            // Arrange
            Long targetId = 1L;
            var existingEntity = createValidCityEntity(targetId);

            given(cityRepository.findById(targetId)).willReturn(Optional.of(existingEntity));

            // Act
            var result = cityService.getById(targetId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(targetId);
            assertThat(result.name()).isEqualTo("Troyan");

            verify(cityRepository).findById(targetId);
        }

        @Test
        @DisplayName("Should throw BusinessException when ID does not exist")
        void shouldThrowExceptionWhenIdDoesNotExist_ErrorCase() {
            // Arrange
            Long targetId = 999L;

            given(cityRepository.findById(targetId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cityService.getById(targetId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.CITY_NOT_FOUND.getDefaultMessage());

            verify(cityRepository).findById(targetId);
        }

        @Test
        @DisplayName("Should throw NullPointerException when ID is null")
        void shouldThrowNpeWhenIdIsNull_ErrorCase() {
            // Act & Assert
            assertThatThrownBy(() -> cityService.getById(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(Constants.DeveloperErrors.ENTITY_ID_NULL);

            verifyNoInteractions(cityRepository);
        }
    }

    @Nested
    @DisplayName("City Get By Name Tests")
    class GetByNameTests {

        @Test
        @DisplayName("Should return a list of cities when name matches exactly")
        void shouldReturnListWhenNameMatches_HappyPath() {
            // Arrange
            String targetName = "Troyan";
            var entity1 = createValidCityEntity(1L);
            var entity2 = createValidCityEntity(2L);
            entity2.setPostcode("6491");

            given(cityRepository.findAllByName(targetName)).willReturn(List.of(entity1, entity2));

            // Act
            var results = cityService.getByName(targetName);

            // Assert
            assertThat(results).isNotNull();
            assertThat(results).hasSize(2);
            assertThat(results).extracting(CityViewDto::id).containsExactlyInAnyOrder(1L, 2L);

            verify(cityRepository).findAllByName(targetName);
        }

        @Test
        @DisplayName("Should throw BusinessException when no cities match the name")
        void shouldThrowExceptionWhenNoCitiesMatch_ErrorCase() {
            // Arrange
            String targetName = "Atlantis";
            given(cityRepository.findAllByName(targetName)).willReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> cityService.getByName(targetName))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage());

            verify(cityRepository).findAllByName(targetName);
        }

        @Test
        @DisplayName("Should throw NullPointerException when name is null")
        void shouldThrowNpeWhenNameIsNull_ErrorCase() {
            // Act & Assert
            assertThatThrownBy(() -> cityService.getByName(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(Constants.DeveloperErrors.NAME_NULL);

            verifyNoInteractions(cityRepository);
        }
    }

    @Nested
    @DisplayName("City Get All Tests")
    class GetAllTests {

        @Test
        @DisplayName("Should return a paginated list of cities")
        void shouldReturnPaginatedList_HappyPath() {
            // Arrange
            var pageRequest = PageRequest.of(0, 10);
            var entity = createValidCityEntity(1L);
            var pagedResponse = new PageImpl<>(List.of(entity));

            given(cityRepository.findAll(pageRequest)).willReturn(pagedResponse);

            // Act
            var result = cityService.getAll(pageRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().name()).isEqualTo("Troyan");

            verify(cityRepository).findAll(pageRequest);
        }

        @Test
        @DisplayName("Should throw NullPointerException when Pageable is null")
        void shouldThrowNpeWhenPageableIsNull_ErrorCase() {
            // Act & Assert
            assertThatThrownBy(() -> cityService.getAll(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(Constants.DeveloperErrors.PAGEABLE_NULL);

            verifyNoInteractions(cityRepository);
        }
    }

    @Nested
    @DisplayName("City Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete a city when ID exists")
        void shouldDeleteSuccessfully_HappyPath() {
            // Arrange
            Long targetId = 1L;
            var existingEntity = createValidCityEntity(targetId);

            given(cityRepository.findById(targetId)).willReturn(Optional.of(existingEntity));

            // Act
            cityService.delete(targetId);

            // Assert
            verify(cityRepository).findById(targetId);
            verify(cityRepository).delete(existingEntity);
        }

        @Test
        @DisplayName("Should throw BusinessException when deleting a non-existent city")
        void shouldThrowExceptionWhenDeletingNonExistentId_ErrorCase() {
            // Arrange
            Long invalidId = 999L;

            given(cityRepository.findById(invalidId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cityService.delete(invalidId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.CITY_NOT_FOUND.getDefaultMessage());

            verify(cityRepository).findById(invalidId);
            verifyNoMoreInteractions(cityRepository);
        }

        @Test
        @DisplayName("Should throw NullPointerException when delete ID is null")
        void shouldThrowNpeWhenDeleteIdIsNull_ErrorCase() {
            // Act & Assert
            assertThatThrownBy(() -> cityService.delete(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(Constants.DeveloperErrors.ENTITY_ID_NULL);

            verifyNoInteractions(cityRepository);
        }
    }
}