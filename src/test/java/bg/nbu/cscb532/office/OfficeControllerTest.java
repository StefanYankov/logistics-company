package bg.nbu.cscb532.office;

import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import bg.nbu.cscb532.office.dto.OfficeDto;
import bg.nbu.cscb532.office.dto.OfficeViewDto;
import bg.nbu.cscb532.office.dto.OperatingHourDto;
import bg.nbu.cscb532.office.dto.OperatingHourViewDto;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvcTest for the OfficeController.
 */
@WebMvcTest(controllers = {OfficeController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class OfficeControllerTest {

    private static final String BASE_URL = "/api/offices";
    private static final Long VALID_OFFICE_ID = 1L;
    private static final Long VALID_COMPANY_ID = 10L;
    private static final Long VALID_CITY_ID = 20L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OfficeService officeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // --- TEST DATA FACTORY METHODS ---

    private AddressDetailsDto createValidAddressDto() {
        return new AddressDetailsDto(VALID_CITY_ID, "Vitosha Blvd", "Mladost", "10", "A", "5", "12", 42.69, 23.32);
    }

    private OperatingHourDto createValidOperatingHourDto() {
        return new OperatingHourDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
    }

    private OfficeDto createValidOfficeDto() {
        return new OfficeDto(
                VALID_COMPANY_ID,
                createValidAddressDto(),
                Set.of(createValidOperatingHourDto())
        );
    }

    private OperatingHourViewDto createValidOperatingHourViewDto() {
        return new OperatingHourViewDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
    }

    private OfficeViewDto createValidOfficeViewDto() {
        return new OfficeViewDto(
                VALID_OFFICE_ID,
                VALID_COMPANY_ID,
                "Sofia",
                "1000",
                "Vitosha Blvd, Mladost, bl. 10, ent. A, fl. 5, ap. 12, Sofia 1000",
                Set.of(createValidOperatingHourViewDto())
        );
    }

    private EmployeeViewDto createValidEmployeeViewDto() {
        return EmployeeViewDto.builder()
                .id(UUID.randomUUID())
                .username("janedoe")
                .email("jane@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .employeeNumber("EMP-123")
                .hireDate(LocalDate.now())
                .salary(BigDecimal.valueOf(2500))
                .applicationRole(ApplicationRole.CLERK)
                .isActive(true)
                .officeId(VALID_OFFICE_ID)
                .build();
    }

    @Nested
    @DisplayName("POST /api/offices")
    class CreateOfficeTests {

        @Test
        @DisplayName("Should successfully create an office and return 201 Created with AssertJ deserialization")
        void shouldCreateOfficeWhenValid() throws Exception {

            // Arrange
            OfficeDto requestDto = createValidOfficeDto();
            OfficeViewDto expectedResponse = createValidOfficeViewDto();

            given(officeService.create(any(OfficeDto.class))).willReturn(expectedResponse);

            // Act
            String jsonResponse = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Location", "http://localhost/api/offices/1"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Assert
            OfficeViewDto actualResponse = objectMapper.readValue(jsonResponse, OfficeViewDto.class);
            assertThat(actualResponse)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);

            verify(officeService).create(any(OfficeDto.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when top-level validation fails (missing companyId)")
        void shouldReturn400WhenTopLevelValidationFails() throws Exception {

            // Arrange
            OfficeDto invalidDto = new OfficeDto(null, createValidAddressDto(), Set.of());

            // Act & Assert
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:logistics:validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors.companyId").exists());

            verifyNoInteractions(officeService);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when nested Address validation fails (e.g., missing street)")
        void shouldReturn400WhenNestedAddressValidationFails() throws Exception {

            // Arrange
            AddressDetailsDto invalidAddress = new AddressDetailsDto(VALID_CITY_ID, "   ", null, null, null, null, null, null, null);
            OfficeDto invalidDto = new OfficeDto(VALID_COMPANY_ID, invalidAddress, Set.of());

            // Act & Assert
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors['address.street']").exists());

            verifyNoInteractions(officeService);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when nested Operating Hour validation fails (e.g., missing dayOfWeek)")
        void shouldReturn400WhenNestedOperatingHourValidationFails() throws Exception {

            // Arrange
            OperatingHourDto invalidHour = new OperatingHourDto(null, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
            OfficeDto invalidDto = new OfficeDto(VALID_COMPANY_ID, createValidAddressDto(), Set.of(invalidHour));

            // Act & Assert
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").exists());

            verifyNoInteractions(officeService);
        }

        @Test
        @DisplayName("Should return 404 Not Found when BusinessException is thrown (e.g. Company not found)")
        void shouldReturn404WhenCompanyNotFound() throws Exception {

            // Arrange
            OfficeDto requestDto = createValidOfficeDto();

            given(officeService.create(any(OfficeDto.class)))
                    .willThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.COMPANY_NOT_FOUND.getDefaultMessage()));
        }

        @Test
        @DisplayName("Should return 404 Not Found when BusinessException is thrown (e.g. City not found)")
        void shouldReturn404WhenCityNotFound() throws Exception {

            // Arrange
            OfficeDto requestDto = createValidOfficeDto();

            given(officeService.create(any(OfficeDto.class)))
                    .willThrow(new BusinessException(ErrorCode.CITY_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.CITY_NOT_FOUND.getDefaultMessage()));
        }
    }

    @Nested
    @DisplayName("PUT /api/offices/{id}")
    class UpdateOfficeTests {

        @Test
        @DisplayName("Should return 200 OK and updated office details")
        void shouldUpdateSuccessfully() throws Exception {

            // Arrange
            OfficeDto requestDto = createValidOfficeDto();
            OfficeViewDto expectedResponse = createValidOfficeViewDto();

            given(officeService.update(eq(VALID_OFFICE_ID), any(OfficeDto.class))).willReturn(expectedResponse);

            // Act
            String jsonResponse = mockMvc.perform(put(BASE_URL + "/{id}", VALID_OFFICE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Assert
            OfficeViewDto actualResponse = objectMapper.readValue(jsonResponse, OfficeViewDto.class);
            assertThat(actualResponse)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);

            verify(officeService).update(eq(VALID_OFFICE_ID), any(OfficeDto.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when update payload violates validation rules")
        void shouldReturn400WhenUpdateFailsDueToValidation() throws Exception {

            // Arrange
            OfficeDto invalidDto = new OfficeDto(null, createValidAddressDto(), Set.of());

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/{id}", VALID_OFFICE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").exists());

            verifyNoInteractions(officeService);
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating a non-existent office")
        void shouldReturn404WhenOfficeNotFound() throws Exception {

            // Arrange
            Long nonExistentId = 999L;
            OfficeDto requestDto = createValidOfficeDto();

            given(officeService.update(eq(nonExistentId), any(OfficeDto.class)))
                    .willThrow(new BusinessException(ErrorCode.OFFICE_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(put(BASE_URL + "/{id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.OFFICE_NOT_FOUND.getDefaultMessage()));

            verify(officeService).update(eq(nonExistentId), any(OfficeDto.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/offices/{id}")
    class DeleteOfficeTests {

        @Test
        @DisplayName("Should return 204 No Content when deletion is successful")
        void shouldDeleteSuccessfully() throws Exception {

            // Arrange & Assert
            mockMvc.perform(delete(BASE_URL + "/{id}", VALID_OFFICE_ID))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(officeService).delete(VALID_OFFICE_ID);
        }

        @Test
        @DisplayName("Should return 404 Not Found when deleting a non-existent office")
        void shouldReturn404WhenOfficeNotFound() throws Exception {

            // Arrange
            Long nonExistentId = 999L;
            doThrow(new BusinessException(ErrorCode.OFFICE_NOT_FOUND))
                    .when(officeService).delete(nonExistentId);

            // Act & Assert
            mockMvc.perform(delete(BASE_URL + "/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.OFFICE_NOT_FOUND.getDefaultMessage()));

            verify(officeService).delete(nonExistentId);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ID is not a valid number")
        void shouldReturn400WhenIdIsInvalidType() throws Exception {

            // Arrange
            String invalidId = "invalid-string-id";

            // Act & Assert
            mockMvc.perform(delete(BASE_URL + "/{id}", invalidId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(officeService);
        }
    }

    @Nested
    @DisplayName("GET /api/offices/{id}")
    class GetOfficeByIdTests {

        @Test
        @DisplayName("Should return 200 OK and complex Office details")
        void shouldReturn200WhenOfficeFound() throws Exception {

            // Arrange
            OfficeViewDto expectedResponse = createValidOfficeViewDto();
            given(officeService.getById(VALID_OFFICE_ID)).willReturn(expectedResponse);

            // Act
            String jsonResponse = mockMvc.perform(get(BASE_URL + "/{id}", VALID_OFFICE_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Assert
            OfficeViewDto actualResponse = objectMapper.readValue(jsonResponse, OfficeViewDto.class);
            assertThat(actualResponse)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);

            verify(officeService).getById(VALID_OFFICE_ID);
        }

        @Test
        @DisplayName("Should return 404 Not Found when office ID does not exist")
        void shouldReturn404WhenOfficeNotFound() throws Exception {

            // Arrange
            Long nonExistentId = 999L;
            given(officeService.getById(nonExistentId))
                    .willThrow(new BusinessException(ErrorCode.OFFICE_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/{id}", nonExistentId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.OFFICE_NOT_FOUND.getDefaultMessage()));

            verify(officeService).getById(nonExistentId);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ID is not a valid number")
        void shouldReturn400WhenIdIsInvalidType() throws Exception {

            // Arrange
            String invalidId = "invalid-string-id";

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/{id}", invalidId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(officeService);
        }
    }

    @Nested
    @DisplayName("GET /api/offices")
    class GetAllOfficesTests {

        @Test
        @DisplayName("Should return 200 OK and default paginated list of offices")
        void shouldReturn200WithDefaultPagination() throws Exception {

            // Arrange
            OfficeViewDto office = createValidOfficeViewDto();
            PageImpl<OfficeViewDto> pagedResponse = new PageImpl<>(List.of(office));

            given(officeService.getAll(any(PageRequest.class))).willReturn(pagedResponse);

            // Act & Assert
            mockMvc.perform(get(BASE_URL)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(VALID_OFFICE_ID));

            verify(officeService).getAll(any(PageRequest.class));
        }

        @Test
        @DisplayName("Should return 200 OK and correctly map explicit pagination parameters")
        void shouldReturn200WithExplicitPagination() throws Exception {

            // Arrange
            OfficeViewDto office = createValidOfficeViewDto();
            PageImpl<OfficeViewDto> pagedResponse = new PageImpl<>(List.of(office));

            given(officeService.getAll(any(PageRequest.class))).willReturn(pagedResponse);

            // Act & Assert
            mockMvc.perform(get(BASE_URL)
                            .param("page", "2")
                            .param("size", "5")
                            .param("sort", "id,desc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(officeService).getAll(any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("GET /api/offices/city/{cityId}")
    class GetOfficesByCityIdTests {

        @Test
        @DisplayName("Should return 200 OK and list of offices")
        void shouldReturn200WhenCityIdProvided() throws Exception {

            // Arrange
            OfficeViewDto expectedOffice = createValidOfficeViewDto();
            List<OfficeViewDto> expectedResponse = List.of(expectedOffice);
            
            given(officeService.getOfficesByCityId(VALID_CITY_ID)).willReturn(expectedResponse);

            // Act
            String jsonResponse = mockMvc.perform(get(BASE_URL + "/city/{cityId}", VALID_CITY_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Assert
            List<OfficeViewDto> actualResponse = objectMapper.readValue(jsonResponse, new TypeReference<>() {});
            assertThat(actualResponse)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);

            verify(officeService).getOfficesByCityId(VALID_CITY_ID);
        }

        @Test
        @DisplayName("Should return 200 OK and empty array when city has no offices")
        void shouldReturn200AndEmptyArrayWhenNoOfficesInCity() throws Exception {

            // Arrange
            Long cityWithNoOffices = 999L;
            given(officeService.getOfficesByCityId(cityWithNoOffices)).willReturn(List.of());

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/city/{cityId}", cityWithNoOffices)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(0));

            verify(officeService).getOfficesByCityId(cityWithNoOffices);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when city ID is invalid type")
        void shouldReturn400WhenCityIdIsInvalid() throws Exception {

            // Arrange
            String invalidCityId = "abc";

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/city/{cityId}", invalidCityId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(officeService);
        }
    }

    @Nested
    @DisplayName("GET /api/offices/nearest")
    class GetNearestOfficesTests {

        @Test
        @DisplayName("Should return 200 OK and list of nearest offices")
        void shouldReturn200WhenCoordinatesAreValid() throws Exception {

            // Arrange
            double lat = 42.69;
            double lon = 23.32;
            double radius = 10.0;
            OfficeViewDto expectedOffice = createValidOfficeViewDto();
            List<OfficeViewDto> expectedResponse = List.of(expectedOffice);

            given(officeService.getNearestOffices(lat, lon, radius)).willReturn(expectedResponse);

            // Act
            String jsonResponse = mockMvc.perform(get(BASE_URL + "/nearest")
                            .param("lat", String.valueOf(lat))
                            .param("lon", String.valueOf(lon))
                            .param("radiusKm", String.valueOf(radius))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Assert
            List<OfficeViewDto> actualResponse = objectMapper.readValue(jsonResponse, new TypeReference<>() {});
            assertThat(actualResponse)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);

            verify(officeService).getNearestOffices(lat, lon, radius);
        }

        @Test
        @DisplayName("Should return 200 OK and empty array when no offices are within radius")
        void shouldReturn200AndEmptyArrayWhenNoOfficesInRadius() throws Exception {

            // Arrange
            given(officeService.getNearestOffices(42.69, 23.32, 1.0)).willReturn(List.of());

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/nearest")
                            .param("lat", "42.69")
                            .param("lon", "23.32")
                            .param("radiusKm", "1.0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(0));
            
            verify(officeService).getNearestOffices(42.69, 23.32, 1.0);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when lat parameter is missing")
        void shouldReturn400WhenMissingParameters() throws Exception {

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/nearest")
                            .param("lon", "23.32")
                            .param("radiusKm", "10.0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(officeService);
        }
        
        @Test
        @DisplayName("Should return 400 Bad Request when lat parameter is an invalid type")
        void shouldReturn400WhenCoordinateTypeIsInvalid() throws Exception {

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/nearest")
                            .param("lat", "abc")
                            .param("lon", "23.32")
                            .param("radiusKm", "10.0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(officeService);
        }
        
        @Test
        @DisplayName("Should return 400 Bad Request when coordinates violate domain logic")
        void shouldReturn400WhenCoordinatesAreMathematicallyInvalid() throws Exception {

            // Arrange
            double invalidLat = 100.0;
            
            given(officeService.getNearestOffices(invalidLat, 23.32, 10.0))
                    .willThrow(new BusinessException(ErrorCode.VALIDATION_FAILED));

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/nearest")
                            .param("lat", String.valueOf(invalidLat))
                            .param("lon", "23.32")
                            .param("radiusKm", "10.0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(ErrorCode.VALIDATION_FAILED.getDefaultMessage()));

            verify(officeService).getNearestOffices(invalidLat, 23.32, 10.0);
        }
    }

    @Nested
    @DisplayName("GET /api/offices/{id}/clerks")
    class GetClerksForOfficeTests {

        @Test
        @DisplayName("Happy Path: Should return 200 OK with paginated list of clerks")
        void shouldReturn200WithPaginatedClerks() throws Exception {
            // Arrange
            EmployeeViewDto employee = createValidEmployeeViewDto();
            Page<EmployeeViewDto> pagedResponse = new PageImpl<>(List.of(employee), PageRequest.of(0, 10), 1);

            given(officeService.getClerksByOfficeId(eq(VALID_OFFICE_ID), any(Pageable.class)))
                    .willReturn(pagedResponse);

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/{id}/clerks", VALID_OFFICE_ID)
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(employee.id().toString()))
                    .andExpect(jsonPath("$.content[0].applicationRole").value("CLERK"));

            verify(officeService).getClerksByOfficeId(eq(VALID_OFFICE_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("Error Case: Should return 404 Not Found when office does not exist")
        void shouldReturn404WhenOfficeNotFound() throws Exception {
            // Arrange
            Long invalidId = 999L;
            given(officeService.getClerksByOfficeId(eq(invalidId), any(Pageable.class)))
                    .willThrow(new BusinessException(ErrorCode.OFFICE_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/{id}/clerks", invalidId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.OFFICE_NOT_FOUND.getCode()));

            verify(officeService).getClerksByOfficeId(eq(invalidId), any(Pageable.class));
        }
    }
}
