package bg.nbu.cscb532.employee.controller;

import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Employee Controller: Admin Management Tests")
public class EmployeeAdminControllerTests extends AbstractEmployeeControllerTestBase {

    @Nested
    @DisplayName("GET /api/employees")
    class GetAllEmployeesTests {

        @Test
        @DisplayName("Happy Path: Admin should successfully retrieve paginated list of employees")
        void adminShouldRetrieveEmployeesSuccessfully() throws Exception {
            // Arrange
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            EmployeeViewDto employeeDto = EmployeeViewDto.builder().id(UUID.randomUUID()).username("test").build();
            Page<EmployeeViewDto> pagedResponse = new PageImpl<>(List.of(employeeDto), PageRequest.of(0, 10), 1);

            given(employeeService.getAll(any(Pageable.class))).willReturn(pagedResponse);

            // Act and Assert
            mockMvc.perform(get(BASE_URL)
                            .with(user(adminUser))
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].username").value("test"));

            verify(employeeService).getAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Security: Non-admin should be forbidden from getting all employees")
        void nonAdminShouldBeForbidden() throws Exception {
            CustomUserDetails clientUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(get(BASE_URL)
                            .with(user(clientUser)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(employeeService);
        }
    }

    @Nested
    @DisplayName("DELETE /api/employees/{id}")
    class DeactivateEndpointTests {

        @Test
        @DisplayName("Happy Path: Admin should deactivate employee and return 204 No Content")
        void adminShouldDeactivateEmployee() throws Exception {
            UUID employeeId = UUID.randomUUID();
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);

            mockMvc.perform(delete(BASE_URL + "/{id}", employeeId)
                            .with(user(adminUser)))
                    .andExpect(status().isNoContent());

            verify(employeeService).deactivate(employeeId);
        }

        @Test
        @DisplayName("Security: Non-admin should be forbidden from deactivating employee")
        void nonAdminShouldBeForbidden() throws Exception {
            UUID employeeId = UUID.randomUUID();
            CustomUserDetails clientUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(delete(BASE_URL + "/{id}", employeeId)
                            .with(user(clientUser)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(employeeService);
        }
    }

    @Nested
    @DisplayName("PATCH /api/employees/{id}/activate")
    class ActivateEndpointTests {

        @Test
        @DisplayName("Happy Path: Admin should activate employee and return 204 No Content")
        void adminShouldActivateEmployee() throws Exception {
            UUID employeeId = UUID.randomUUID();
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);

            mockMvc.perform(patch(BASE_URL + "/{id}/activate", employeeId)
                            .with(user(adminUser)))
                    .andExpect(status().isNoContent());

            verify(employeeService).activate(employeeId);
        }

        @Test
        @DisplayName("Security: Non-admin should be forbidden from activating employee")
        void nonAdminShouldBeForbidden() throws Exception {
            UUID employeeId = UUID.randomUUID();
            CustomUserDetails clientUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(patch(BASE_URL + "/{id}/activate", employeeId)
                            .with(user(clientUser)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(employeeService);
        }
    }
}
