package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientQuickRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.config.SecurityConfig;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import bg.nbu.cscb532.user.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ClientController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class ClientControllerTest {

    private static final String BASE_URL = "/api/clients";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // --- TEST DATA FACTORY ---
    
    private CustomUserDetails createMockAuthUser(UUID id, ApplicationRole role) {
        return new CustomUserDetails(
                id,
                "testUser",
                "password",
                role,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
    
    private ClientRegistrationDto createValidRegistrationDto() {
        return new ClientRegistrationDto(
                "newClient",
                "client@example.com",
                "rawPassword123",
                "John",
                "Doe",
                "0888123456"
        );
    }

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
        
        @Test
        @DisplayName("Security: Should return 403 Forbidden when Client attempts to use quick register")
        void shouldReturn403WhenClientAttemptsToQuickRegister() throws Exception {
            CustomUserDetails clientUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLIENT);
            ClientQuickRegistrationDto dto = ClientQuickRegistrationDto.builder()
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("0888123456")
                    .build();

            mockMvc.perform(post(BASE_URL + "/quick-register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
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
    @DisplayName("POST /api/clients/register")
    class RegisterClientTests {

        @Test
        @DisplayName("Happy Path: Should return 201 Created when registration is successful")
        void shouldReturn201_WhenRegistrationIsSuccessful() throws Exception {
            // Arrange
            ClientRegistrationDto requestDto = new ClientRegistrationDto(
                    "newClient", "client@example.com", "rawPassword123", "John", "Doe", "0888123456"
            );
            UUID newClientId = UUID.randomUUID();
            ClientViewDto responseDto = createValidViewDto(newClientId);

            given(clientService.register(any(ClientRegistrationDto.class))).willReturn(responseDto);

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/clients/" + newClientId))
                    .andExpect(jsonPath("$.id").value(newClientId.toString()))
                    .andExpect(jsonPath("$.username").value("newClient"))
                    .andExpect(jsonPath("$.isEmailVerified").value(false))
                    .andExpect(jsonPath("$.isActive").value(true));

            verify(clientService).register(any(ClientRegistrationDto.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Validation Error: Should return 400 when username is invalid")
        void shouldReturn400_WhenUsernameIsInvalid(String invalidUsername) throws Exception {
            // Arrange
            ClientRegistrationDto invalidDto = new ClientRegistrationDto(invalidUsername, "client@example.com", "password123", "John", "Doe", "123456");

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.username").exists());

            verifyNoInteractions(clientService);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "short"})
        @DisplayName("Validation Error: Should return 400 when password is invalid")
        void shouldReturn400_WhenPasswordIsInvalid(String invalidPassword) throws Exception {
            // Arrange
            ClientRegistrationDto invalidDto = new ClientRegistrationDto("newClient", "client@example.com", invalidPassword, "John", "Doe", "123456");

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.password").exists());

            verifyNoInteractions(clientService);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "plainAddress", "@no-local-part.com"})
        @DisplayName("Validation Error: Should return 400 when email is invalid")
        void shouldReturn400_WhenEmailIsInvalid(String invalidEmail) throws Exception {
            // Arrange
            ClientRegistrationDto invalidDto = new ClientRegistrationDto("newClient", invalidEmail, "password123", "John", "Doe", "123456");

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists());

            verifyNoInteractions(clientService);
        }

        @Test
        @DisplayName("Business Conflict: Should return 409 Conflict when username is duplicate")
        void shouldReturn409_WhenUsernameIsDuplicate() throws Exception {
            // Arrange
            ClientRegistrationDto requestDto = createValidRegistrationDto();

            given(clientService.register(any(ClientRegistrationDto.class)))
                    .willThrow(new BusinessException(ErrorCode.USERNAME_DUPLICATE));

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.USERNAME_DUPLICATE.getCode()));

            verify(clientService).register(any(ClientRegistrationDto.class));
        }
        
        @Test
        @DisplayName("Business Conflict: Should return 409 Conflict when email is duplicate")
        void shouldReturn409_WhenEmailIsDuplicate() throws Exception {
            // Arrange
            ClientRegistrationDto requestDto = createValidRegistrationDto();

            given(clientService.register(any(ClientRegistrationDto.class)))
                    .willThrow(new BusinessException(ErrorCode.EMAIL_DUPLICATE));

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.EMAIL_DUPLICATE.getCode()));

            verify(clientService).register(any(ClientRegistrationDto.class));
        }
    }
    
    @Nested
    @DisplayName("POST /api/clients/quick-register")
    class QuickRegisterClientTests {

        @Test
        @DisplayName("Happy Path: Should return 201 Created when quick registration is successful")
        void shouldReturn201_WhenQuickRegistrationIsSuccessful() throws Exception {
            // Arrange
            CustomUserDetails clerkUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            ClientQuickRegistrationDto requestDto = ClientQuickRegistrationDto.builder()
                    .firstName("Guest")
                    .lastName("User")
                    .phoneNumber("0888123456")
                    .build();
                    
            UUID newClientId = UUID.randomUUID();
            ClientViewDto responseDto = createValidViewDto(newClientId);

            given(clientService.quickRegister(any(ClientQuickRegistrationDto.class))).willReturn(responseDto);

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/quick-register")
                            .with(user(clerkUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/clients/" + newClientId))
                    .andExpect(jsonPath("$.id").value(newClientId.toString()))
                    .andExpect(jsonPath("$.firstName").value("John")); // Value from createValidViewDto mock

            verify(clientService).quickRegister(any(ClientQuickRegistrationDto.class));
        }

        @Test
        @DisplayName("Validation Error: Should return 400 when missing required fields")
        void shouldReturn400_WhenMissingFields() throws Exception {
            // Arrange
            CustomUserDetails clerkUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            ClientQuickRegistrationDto invalidDto = ClientQuickRegistrationDto.builder()
                    .firstName("Guest")
                    .lastName("User")
                    .build();

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/quick-register")
                            .with(user(clerkUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.phoneNumber").exists());

            verifyNoInteractions(clientService);
        }

        @Test
        @DisplayName("Business Conflict: Should return 409 Conflict when phone number is duplicate")
        void shouldReturn409_WhenPhoneIsDuplicate() throws Exception {
            // Arrange
            CustomUserDetails clerkUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            ClientQuickRegistrationDto requestDto = ClientQuickRegistrationDto.builder()
                    .firstName("Guest")
                    .lastName("User")
                    .phoneNumber("0888123456")
                    .build();

            given(clientService.quickRegister(any(ClientQuickRegistrationDto.class)))
                    .willThrow(new BusinessException(ErrorCode.PHONE_DUPLICATE));

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/quick-register")
                            .with(user(clerkUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.PHONE_DUPLICATE.getCode()));

            verify(clientService).quickRegister(any(ClientQuickRegistrationDto.class));
        }
    }
}