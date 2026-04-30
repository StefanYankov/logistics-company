package bg.nbu.cscb532.company;

import bg.nbu.cscb532.company.dto.CompanyDto;
import bg.nbu.cscb532.company.dto.CompanyViewDto;
import bg.nbu.cscb532.shared.config.SecurityConfig;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.user.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvcTest for the CompanyController.
 */
@WebMvcTest(controllers = {CompanyController.class, GlobalExceptionHandler.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("POST /api/companies")
    class CreateCompanyTests {

        @Test
        @DisplayName("Should return 201 Created and Location header when DTO is valid")
        @WithMockUser(roles = "ADMIN")
        void shouldCreateCompanyWhenValid() throws Exception {
            // Arrange
            CompanyDto requestDto = CompanyDto.builder()
                    .name("Speedy Logistics")
                    .registrationNumber("BG12345")
                    .build();

            CompanyViewDto responseDto = new CompanyViewDto(1L, "Speedy Logistics", "BG12345");

            given(companyService.create(any(CompanyDto.class))).willReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post("/api/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/companies/1"))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Speedy Logistics"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request with RFC 9457 ProblemDetail when validation fails")
        void shouldReturn400WhenValidationFails() throws Exception {
            // Arrange
            CompanyDto invalidDto = CompanyDto.builder()
                    .name("")
                    .registrationNumber("BG12345")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:logistics:validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors.name").exists());
        }

        @Test
        @DisplayName("Should return 409 Conflict when business rule violates unique registration")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn409WhenBusinessRuleFails() throws Exception {
            // Arrange
            CompanyDto requestDto = CompanyDto.builder()
                    .name("Speedy")
                    .registrationNumber("DUPLICATE")
                    .build();

            given(companyService.create(any(CompanyDto.class)))
                    .willThrow(new BusinessException(ErrorCode.COMPANY_REGISTRATION_DUPLICATE));

            // Act & Assert
            mockMvc.perform(post("/api/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:logistics:business-error"))
                    .andExpect(jsonPath("$.errorCode").value("E1002"));
        }
    }

    @Nested
    @DisplayName("GET /api/companies/{id}")
    class GetCompanyByIdTests {

        @Test
        @DisplayName("Should return 200 OK when company exists")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200WhenExists() throws Exception {
            CompanyViewDto responseDto = new CompanyViewDto(1L, "Speedy Logistics", "BG12345");

            given(companyService.getById(1L)).willReturn(responseDto);

            mockMvc.perform(get("/api/companies/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Speedy Logistics"));
        }

        @Test
        @DisplayName("Should return 404 Not Found with ProblemDetail when company does not exist")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenNotFound() throws Exception {
            given(companyService.getById(99L))
                    .willThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

            mockMvc.perform(get("/api/companies/99")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("E1001"));
        }
    }

    @Nested
    @DisplayName("GET /api/companies")
    class GetAllCompaniesTests {

        @Test
        @DisplayName("Should return 200 OK and resolve Pageable automatically")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200AndMapPageable() throws Exception {
            // Arrange
            CompanyViewDto company = new CompanyViewDto(1L, "Speedy", "BG123");
            Page<CompanyViewDto> page = new PageImpl<>(List.of(company), PageRequest.of(0, 10), 1);

            // We use 'any' because Spring constructs the Pageable dynamically from query params
            given(companyService.getAll(any())).willReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/companies")
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Speedy"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/companies/{id}")
    class UpdateCompanyTests {

        @Test
        @DisplayName("Should return 200 OK when update is successful")
        @WithMockUser(roles = "ADMIN")
        void shouldUpdateSuccessfully() throws Exception {
            CompanyDto requestDto = CompanyDto.builder()
                    .name("New Name")
                    .registrationNumber("BG123")
                    .build();

            CompanyViewDto responseDto = new CompanyViewDto(1L, "New Name", "BG123");

            given(companyService.update(eq(1L), any(CompanyDto.class))).willReturn(responseDto);

            mockMvc.perform(put("/api/companies/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Name"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/companies/{id}")
    class DeleteCompanyTests {

        @Test
        @DisplayName("Should return 204 No Content when deletion is successful")
        @WithMockUser(roles = "ADMIN")
        void shouldDeleteSuccessfully() throws Exception {
            mockMvc.perform(delete("/api/companies/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string("")); // Proves the body is empty

            verify(companyService).delete(1L);
        }
    }

    @Nested
    @DisplayName("Security & Role Authorization")
    class SecurityTests {

        @Test
        @DisplayName("RBAC: Should return 403 Forbidden when user is not an ADMIN")
        @WithMockUser(roles = "CLERK")
        void shouldReturn403ForNonAdmin() throws Exception {
            mockMvc.perform(get("/api/companies")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }
}