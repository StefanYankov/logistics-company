package bg.nbu.cscb532.employee;

import bg.nbu.cscb532.employee.dto.AdminPasswordResetDto;
import bg.nbu.cscb532.employee.dto.EmployeeCreationDto;
import bg.nbu.cscb532.employee.dto.EmployeeUpdateDto;
import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import bg.nbu.cscb532.shared.config.SecurityConfig;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.JwtService;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EmployeeController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {

    private static final String BASE_URL = "/api/employees";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // --- TEST DATA FACTORY ---
    private EmployeeCreationDto.EmployeeCreationDtoBuilder createValidRegistrationDtoBuilder() {
        return EmployeeCreationDto.builder()
                .username("NewEmployee")
                .email("emp@example.com")
                .password("rawPassword123")
                .firstName("John")
                .lastName("Doe")
                .employeeNumber("EMP-123")
                .hireDate(LocalDate.now())
                .salary(BigDecimal.valueOf(2000.00))
                .applicationRole(ApplicationRole.COURIER);
    }

    private EmployeeViewDto createValidViewDto(UUID id) {
        return EmployeeViewDto.builder()
                .id(id)
                .username("NewEmployee")
                .email("emp@example.com")
                .firstName("John")
                .lastName("Doe")
                .employeeNumber("EMP-123")
                .hireDate(LocalDate.now())
                .salary(BigDecimal.valueOf(2000.00))
                .applicationRole(ApplicationRole.COURIER)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("POST /api/employees")
    @WithMockUser(roles = "ADMIN")
    class CreateEmployeeTests {

        @Test
        @DisplayName("Happy Path: Should return 201 Created when creation is successful")
        void shouldReturn201_WhenCreationIsSuccessful() throws Exception {
            // Arrange
            EmployeeCreationDto requestDto = createValidRegistrationDtoBuilder().build();
            UUID newEmpId = UUID.randomUUID();
            EmployeeViewDto responseDto = createValidViewDto(newEmpId);

            given(employeeService.create(any(EmployeeCreationDto.class))).willReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/employees/" + newEmpId))
                    .andExpect(jsonPath("$.id").value(newEmpId.toString()))
                    .andExpect(jsonPath("$.applicationRole").value("COURIER"));

            verify(employeeService).create(any(EmployeeCreationDto.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Validation Error: Should return 400 when username is invalid")
        void shouldReturn400_WhenUsernameIsInvalid(String invalidUsername) throws Exception {
            // Arrange
            EmployeeCreationDto invalidDto = createValidRegistrationDtoBuilder().username(invalidUsername).build();

            // Act & Assert
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.username").exists());

            verifyNoInteractions(employeeService);
        }

        @Test
        @DisplayName("Business Conflict: Should return 409 Conflict when employee number is duplicate")
        void shouldReturn409_WhenEmployeeNumberIsDuplicate() throws Exception {
            // Arrange
            EmployeeCreationDto requestDto = createValidRegistrationDtoBuilder().build();

            given(employeeService.create(any(EmployeeCreationDto.class)))
                    .willThrow(new BusinessException(ErrorCode.EMPLOYEE_NUMBER_DUPLICATE));

            // Act & Assert
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.EMPLOYEE_NUMBER_DUPLICATE.getCode()));
        }
    }

    @Nested
    @DisplayName("GET /api/employees/{id}")
    @WithMockUser(roles = "ADMIN")
    class GetByIdTests {

        @Test
        @DisplayName("Happy Path: Should return 200 OK when employee is found")
        void shouldReturn200_WhenFound() throws Exception {
            UUID empId = UUID.randomUUID();
            EmployeeViewDto responseDto = createValidViewDto(empId);

            given(employeeService.getById(empId)).willReturn(responseDto);

            mockMvc.perform(get(BASE_URL + "/" + empId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(empId.toString()));
        }

        @Test
        @DisplayName("Error Case: Should return 404 Not Found when employee does not exist")
        void shouldReturn404_WhenNotFound() throws Exception {
            UUID empId = UUID.randomUUID();
            given(employeeService.getById(empId)).willThrow(new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));

            mockMvc.perform(get(BASE_URL + "/" + empId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.EMPLOYEE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("GET /api/employees")
    @WithMockUser(roles = "ADMIN")
    class GetAllTests {

        @Test
        @DisplayName("Happy Path: Should return 200 OK with paginated list")
        void shouldReturn200_WithPagination() throws Exception {
            UUID empId = UUID.randomUUID();
            EmployeeViewDto responseDto = createValidViewDto(empId);
            Page<EmployeeViewDto> page = new PageImpl<>(List.of(responseDto), PageRequest.of(0, 10), 1);

            given(employeeService.getAll(any())).willReturn(page);

            mockMvc.perform(get(BASE_URL)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(empId.toString()));
        }
    }

    @Nested
    @DisplayName("PUT /api/employees/{id}")
    @WithMockUser(roles = "ADMIN")
    class UpdateBasicInfoTests {

        @Test
        @DisplayName("Happy Path: Should return 200 OK on successful update")
        void shouldReturn200_OnSuccessfulUpdate() throws Exception {
            UUID empId = UUID.randomUUID();
            EmployeeUpdateDto updateDto = EmployeeUpdateDto.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane@example.com")
                    .salary(BigDecimal.valueOf(3000))
                    .build();
            
            EmployeeViewDto responseDto = createValidViewDto(empId);

            given(employeeService.updateBasicInfo(eq(empId), any(EmployeeUpdateDto.class))).willReturn(responseDto);

            mockMvc.perform(put(BASE_URL + "/" + empId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk());
                    
            verify(employeeService).updateBasicInfo(eq(empId), any(EmployeeUpdateDto.class));
        }

        @Test
        @DisplayName("Validation Error: Should return 400 when salary is negative")
        void shouldReturn400_WhenSalaryIsNegative() throws Exception {
            UUID empId = UUID.randomUUID();
            EmployeeUpdateDto updateDto = EmployeeUpdateDto.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane@example.com")
                    .salary(BigDecimal.valueOf(-100))
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + empId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.salary").exists());
                    
            verifyNoInteractions(employeeService);
        }

        @Test
        @DisplayName("Error Case: Should return 404 Not Found when employee does not exist")
        void shouldReturn404_WhenNotFound() throws Exception {
            UUID empId = UUID.randomUUID();
            EmployeeUpdateDto updateDto = EmployeeUpdateDto.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane@example.com")
                    .salary(BigDecimal.valueOf(3000))
                    .build();

            given(employeeService.updateBasicInfo(eq(empId), any(EmployeeUpdateDto.class)))
                    .willThrow(new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));

            mockMvc.perform(put(BASE_URL + "/" + empId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.EMPLOYEE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("DELETE /api/employees/{id}")
    @WithMockUser(roles = "ADMIN")
    class DeactivateTests {

        @Test
        @DisplayName("Happy Path: Should return 204 No Content on successful deactivation")
        void shouldReturn204_OnSuccessfulDeactivation() throws Exception {
            UUID empId = UUID.randomUUID();

            mockMvc.perform(delete(BASE_URL + "/" + empId))
                    .andExpect(status().isNoContent());

            verify(employeeService).deactivate(empId);
        }

        @Test
        @DisplayName("Error Case: Should return 404 Not Found when employee does not exist")
        void shouldReturn404_WhenNotFound() throws Exception {
            UUID empId = UUID.randomUUID();

            willThrow(new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND)).given(employeeService).deactivate(empId);

            mockMvc.perform(delete(BASE_URL + "/" + empId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.EMPLOYEE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("POST /api/employees/{id}/reset-password")
    @WithMockUser(roles = "ADMIN")
    class ForcePasswordResetTests {

        @Test
        @DisplayName("Happy Path: Should return 204 No Content on successful reset")
        void shouldReturn204_OnSuccessfulReset() throws Exception {
            UUID empId = UUID.randomUUID();
            AdminPasswordResetDto resetDto = new AdminPasswordResetDto("newSecurePass123!");

            mockMvc.perform(post(BASE_URL + "/" + empId + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetDto)))
                    .andExpect(status().isNoContent());

            verify(employeeService).forcePasswordReset(eq(empId), any(AdminPasswordResetDto.class));
        }

        @Test
        @DisplayName("Validation Error: Should return 400 when new password is too short")
        void shouldReturn400_WhenPasswordTooShort() throws Exception {
            UUID empId = UUID.randomUUID();
            AdminPasswordResetDto resetDto = new AdminPasswordResetDto("short");

            mockMvc.perform(post(BASE_URL + "/" + empId + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.newPassword").exists());
                    
            verifyNoInteractions(employeeService);
        }
    }
}
