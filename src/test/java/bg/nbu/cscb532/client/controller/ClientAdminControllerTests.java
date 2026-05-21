package bg.nbu.cscb532.client.controller;

import bg.nbu.cscb532.client.dto.ClientViewDto;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Client Controller: Admin Management Tests")
public class ClientAdminControllerTests extends AbstractClientControllerTestBase {

    private ClientViewDto createValidViewDto(UUID id) {
        return new ClientViewDto(
                id,
                "newClient",
                "client@example.com",
                "John",
                "Doe",
                "0888123456",
                true,
                false
        );
    }

    @Nested
    @DisplayName("Authorization Constraints")
    class AuthorizationTests {

        @Test
        @DisplayName("Security: Should return 403 Forbidden when Non-Admin attempts to GET clients")
        void shouldReturn403WhenNonAdminAttemptsToGetClients() throws Exception {
            CustomUserDetails clerkUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);

            mockMvc.perform(get(BASE_URL)
                            .with(user(clerkUser)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(clientService);
        }

        @Test
        @DisplayName("Security: Should return 403 Forbidden when Client attempts to search clients")
        void shouldReturn403WhenClientAttemptsToSearchClients() throws Exception {
            CustomUserDetails clientUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("term", "Doe")
                            .with(user(clientUser)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(clientService);
        }
    }

    @Nested
    @DisplayName("GET /api/clients")
    class GetAllClientsTests {

        @Test
        @DisplayName("Happy Path: Admin should successfully retrieve paginated list of clients")
        void adminShouldRetrieveClientsSuccessfully() throws Exception {
            // Arrange
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            ClientViewDto clientDto = createValidViewDto(UUID.randomUUID());
            Page<ClientViewDto> pagedResponse = new PageImpl<>(List.of(clientDto), PageRequest.of(0, 10), 1);

            given(clientService.getAllClients(any(Pageable.class))).willReturn(pagedResponse);

            // Act and Assert
            mockMvc.perform(get(BASE_URL)
                            .with(user(adminUser))
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].username").value("newClient"));

            verify(clientService).getAllClients(any(Pageable.class));
        }

        @Test
        @DisplayName("Edge Case: Should return empty page when no clients exist")
        void shouldReturnEmptyPageWhenNoClients() throws Exception {
            // Arrange
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);
            Page<ClientViewDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            given(clientService.getAllClients(any(Pageable.class))).willReturn(emptyPage);

            // Act and Assert
            mockMvc.perform(get(BASE_URL)
                            .with(user(adminUser))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isEmpty());

            verify(clientService).getAllClients(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/clients/search")
    class SearchClientsTests {

        @Test
        @DisplayName("Happy Path: Staff should successfully search clients")
        void staffShouldSearchClientsSuccessfully() throws Exception {
            // Arrange
            CustomUserDetails clerkUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            ClientViewDto clientDto = createValidViewDto(UUID.randomUUID());
            Page<ClientViewDto> pagedResponse = new PageImpl<>(List.of(clientDto), PageRequest.of(0, 10), 1);
            String searchTerm = "John";

            given(clientService.searchClients(eq(searchTerm), any(Pageable.class))).willReturn(pagedResponse);

            // Act and Assert
            mockMvc.perform(get(BASE_URL + "/search")
                            .with(user(clerkUser))
                            .param("term", searchTerm)
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].firstName").value("John"));

            verify(clientService).searchClients(eq(searchTerm), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/clients/{id}")
    class DeactivateEndpointTests {

        @Test
        @DisplayName("Happy Path: Admin should deactivate client and return 204 No Content")
        void adminShouldDeactivateClient() throws Exception {
            UUID clientId = UUID.randomUUID();
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);

            mockMvc.perform(delete(BASE_URL + "/{id}", clientId)
                            .with(user(adminUser)))
                    .andExpect(status().isNoContent());

            verify(clientService).deactivate(clientId);
        }

        @Test
        @DisplayName("Security: Non-admin should be forbidden from deactivating client")
        void nonAdminShouldBeForbidden() throws Exception {
            UUID clientId = UUID.randomUUID();
            CustomUserDetails clientUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(delete(BASE_URL + "/{id}", clientId)
                            .with(user(clientUser)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(clientService);
        }
    }

    @Nested
    @DisplayName("PATCH /api/clients/{id}/activate")
    class ActivateEndpointTests {

        @Test
        @DisplayName("Happy Path: Admin should activate client and return 204 No Content")
        void adminShouldActivateClient() throws Exception {
            UUID clientId = UUID.randomUUID();
            CustomUserDetails adminUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.ADMIN);

            mockMvc.perform(patch(BASE_URL + "/{id}/activate", clientId)
                            .with(user(adminUser)))
                    .andExpect(status().isNoContent());

            verify(clientService).activate(clientId);
        }

        @Test
        @DisplayName("Security: Non-admin should be forbidden from activating client")
        void nonAdminShouldBeForbidden() throws Exception {
            UUID clientId = UUID.randomUUID();
            CustomUserDetails clientUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);

            mockMvc.perform(patch(BASE_URL + "/{id}/activate", clientId)
                            .with(user(clientUser)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(clientService);
        }
    }
}
