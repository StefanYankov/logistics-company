package bg.nbu.cscb532.office;

import bg.nbu.cscb532.office.dto.CityDto;
import bg.nbu.cscb532.office.dto.CityViewDto;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.user.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvcTest for the CityController.
 */
@WebMvcTest(controllers = {CityController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
public class CityControllerTest {

    private static final Long VALID_CITY_ID = 1L;
    private static final String VALID_CITY_NAME = "Troyan";
    private static final String VALID_POSTCODE = "5600";
    private static final String INVALID_POSTCODE = "123";
    private static final String BLANK_NAME = "   ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CityService cityService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // --- TEST DATA FACTORY METHODS ---

    private CityDto createValidCityDto() {
        return CityDto.builder()
                .name(VALID_CITY_NAME)
                .postcode(VALID_POSTCODE)
                .build();
    }

    private CityViewDto createValidCityViewDto() {
        return new CityViewDto(
                VALID_CITY_ID,
                VALID_CITY_NAME,
                VALID_POSTCODE
        );
    }

    @Nested
    @DisplayName("POST /api/cities")
    class CreateCityTests {

        @Test
        @DisplayName("Should successfully create a city and return 201 Created")
        void shouldCreateCityWhenValid() throws Exception {

            // Arrange
            CityDto requestDto = createValidCityDto();
            CityViewDto responseDto = createValidCityViewDto();

            given(cityService.create(any(CityDto.class)))
                    .willReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post("/api/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/cities/1"))
                    .andExpect(jsonPath("$.id").value(VALID_CITY_ID))
                    .andExpect(jsonPath("$.name").value(VALID_CITY_NAME))
                    .andExpect(jsonPath("$.postcode").value(VALID_POSTCODE));

            verify(cityService).create(any(CityDto.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request with RFC 9457 ProblemDetail when validation fails")
        void shouldReturn400WhenValidationFails() throws Exception {

            // Arrange
            CityDto invalidDto = CityDto.builder()
                    .name(BLANK_NAME)
                    .postcode(INVALID_POSTCODE)
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:logistics:validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors.name").exists());
        }

        @Test
        @DisplayName("Should return 409 Conflict when business rule violates unique registration")
        void shouldReturn409WhenBusinessRuleFails() throws Exception {

            // Arrange
            CityDto requestDto = CityDto.builder()
                    .name("Lovech")
                    .postcode(VALID_POSTCODE)
                    .build();

            given(cityService.create(any(CityDto.class)))
                    .willThrow(new BusinessException(ErrorCode.CITY_DUPLICATE));

            // Act & Assert

            mockMvc.perform(post("/api/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:logistics:business-error"))
                    .andExpect(jsonPath("$.errorCode").value("E2001"));
        }
    }

    @Nested
    @DisplayName("PUT /api/cities/{id}")
    class UpdateCityTests {

        @Test
        @DisplayName("Should return 200 OK when update is successful")
        void shouldUpdateSuccessfully() throws Exception {

            // Arrange
            CityDto requestDto = createValidCityDto();
            CityViewDto responseDto = createValidCityViewDto();

            given(cityService.update(eq(VALID_CITY_ID), any(CityDto.class)))
                    .willReturn(responseDto);

            // Act & Assert
            mockMvc.perform(put("/api/cities/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(VALID_CITY_NAME));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when update payload violates validation rules")
        void shouldReturn400WhenUpdateFailsDueToValidation() throws Exception {

            // Arrange
            CityDto invalidDto = CityDto.builder()
                    .name(BLANK_NAME)
                    .postcode(INVALID_POSTCODE)
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/cities/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").exists());

            verifyNoInteractions(cityService);
        }

        @Test
        @DisplayName("Should return 404 Not found when updating a non-existent city")
        void shouldReturn404WhenCityNotFound() throws Exception {

            // Arrange
            Long nonExistentId = 999L;
            CityDto requestDto = createValidCityDto();

            given(cityService.update(eq(nonExistentId), any(CityDto.class)))
                    .willThrow(new BusinessException(ErrorCode.CITY_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(put("/api/cities/" + nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.CITY_NOT_FOUND.getDefaultMessage()));
            verify(cityService).update(eq(nonExistentId), any(CityDto.class));
        }

        @Test
        @DisplayName("Should return 409 Conflict when update results in a duplicate postcode")
        void shouldReturn409WhenDuplicatePostcode() throws Exception {

            // Arrange
            CityDto requestDto = createValidCityDto();

            given(cityService.update(eq(VALID_CITY_ID), any(CityDto.class)))
                    .willThrow(new BusinessException(ErrorCode.CITY_DUPLICATE));

            // Act & Assert
            mockMvc.perform(put("/api/cities/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.CITY_DUPLICATE.getDefaultMessage()));

            verify(cityService).update(eq(VALID_CITY_ID), any(CityDto.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/cities/{id}")
    class DeleteCityTests {

        @Test
        @DisplayName("Should return 204 No content when deletion is successful")
        void shouldDeleteSuccessfully() throws Exception {

            // Arrange & Assert
            mockMvc.perform(delete("/api/cities/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(cityService).delete(1L);
        }

        @Test
        @DisplayName("Should return 404 Not found when deleting a non-existent city")
        void shouldReturn404WhenCityNotFound() throws Exception {

            // Arrange
            Long nonExistentId = 999L;

            doThrow(new BusinessException(ErrorCode.CITY_NOT_FOUND))
                    .when(cityService).delete(nonExistentId);

            // Act & Assert
            mockMvc.perform(delete("/api/cities/" + nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.CITY_NOT_FOUND.getDefaultMessage()));
            verify(cityService).delete(nonExistentId);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ID is not a valid number")
        void shouldReturn400WhenIdIsInvalidType() throws Exception {

            // Arrange

            String invalidId = "an-invalid-string-id";

            // Act & Assert
            mockMvc.perform(delete("/api/cities/" + invalidId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(cityService);
        }
    }

    @Nested
    @DisplayName("GET /api/cities/{id}")
    class GetCityByIdTests {

        @Test
        @DisplayName("Should return 200 OK when city exists")
        void shouldReturn200WhenExists() throws Exception {

            // Arrange
            CityViewDto responseDto = createValidCityViewDto();

            given(cityService.getById(VALID_CITY_ID))
                    .willReturn(responseDto);

            // Act & Assert
            mockMvc.perform(get("/api/cities/" + VALID_CITY_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(VALID_CITY_ID))
                    .andExpect(jsonPath("$.name").value(VALID_CITY_NAME))
                    .andExpect(jsonPath("$.postcode").value(VALID_POSTCODE));

            verify(cityService).getById(VALID_CITY_ID);
        }

        @Test
        @DisplayName("Should return 404 Not Found when city ID does not exist")
        void shouldReturn404WhenCityNotFound() throws Exception {

            // Arrange
            Long nonExistentId = 999L;

            given(cityService.getById(nonExistentId))
                    .willThrow(new BusinessException(ErrorCode.CITY_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get("/api/cities/" + nonExistentId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.CITY_NOT_FOUND.getDefaultMessage()));

            verify(cityService).getById(nonExistentId);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ID is not a valid number")
        void shouldReturn400WhenIdIsInvalidType() throws Exception {

            // Arrange
            String invalidId = "invalid-string-id";

            // Act & Assert
            mockMvc.perform(get("/api/cities/" + invalidId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(cityService);
        }

    }

    @Nested
    @DisplayName("GET /api/cities/search")
    class SearchCitiesByNameTests {

        @Test
        @DisplayName("Should return 200 OK and list of cities when name matches exactly")
        void shouldReturn200WhenNameMatches() throws Exception {
            // Arrange
            CityViewDto city1 = createValidCityViewDto();
            CityViewDto city2 = new CityViewDto(2L, VALID_CITY_NAME, "6491");

            given(cityService.getByName(VALID_CITY_NAME)).willReturn(List.of(city1, city2));

            // Act & Assert
            mockMvc.perform(get("/api/cities/" + "search")
                            .param("name", VALID_CITY_NAME)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(VALID_CITY_ID))
                    .andExpect(jsonPath("$[1].postcode").value("6491"));

            verify(cityService).getByName(VALID_CITY_NAME);
        }

        @Test
        @DisplayName("Should return 404 Not Found when no cities match the name")
        void shouldReturn404WhenNoMatchesFound() throws Exception {
            // Arrange
            String missingName = "Atlantis";
            given(cityService.getByName(missingName))
                    .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get("/api/cities" + "/search")
                            .param("name", missingName)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage()));

            verify(cityService).getByName(missingName);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when name parameter is missing")
        void shouldReturn400WhenMissingParameter() throws Exception {
            // Act & Assert
            // Omitting the .param("name", ...) entirely
            mockMvc.perform(get("/api/cities" + "/search")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            // Prove the framework rejected the request before hitting our code
            verifyNoInteractions(cityService);
        }
    }

    @Nested
    @DisplayName("GET /api/cities")
    class GetAllCitiesTests {

        @Test
        @DisplayName("Should return 200 OK and default paginated list of cities")
        void shouldReturn200WithDefaultPagination() throws Exception {

            // Arrange
            CityViewDto city = createValidCityViewDto();
            PageImpl<CityViewDto> pagedResponse = new PageImpl<>(List.of(city));

            given(cityService.getAll(any())).willReturn(pagedResponse);

            // Act & Assert
            mockMvc.perform(get("/api/cities")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value(VALID_CITY_NAME))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(cityService).getAll(any());
        }

        @Test
        @DisplayName("Should return 200 OK and correctly map explicit pagination parameters")
        void shouldReturn200WithExplicitPagination() throws Exception {

            // Arrange
            CityViewDto city = createValidCityViewDto();
            PageImpl<CityViewDto> pagedResponse = new PageImpl<>(List.of(city));

            given(cityService.getAll(any())).willReturn(pagedResponse);

            // Act & Assert
            mockMvc.perform(get("/api/cities")
                            .param("page", "2")
                            .param("size", "5")
                            .param("sort", "name,desc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(cityService).getAll(any());
        }
    }
}