package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.user.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ClientController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
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
    private ClientRegistrationDto createValidRegistrationDto() {
        return new ClientRegistrationDto(
                "newclient",
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
                "newclient",
                "client@example.com",
                "John",
                "Doe",
                "0888123456"
        );
    }

    @Nested
    @DisplayName("POST /api/clients/register")
    class RegisterClientTests {

        @Test
        @DisplayName("Happy Path: Should return 201 Created when registration is successful")
        void shouldReturn201_WhenRegistrationIsSuccessful() throws Exception {
            // Arrange
            ClientRegistrationDto requestDto = createValidRegistrationDto();
            UUID newClientId = UUID.randomUUID();
            ClientViewDto responseDto = createValidViewDto(newClientId);

            given(clientService.register(any(ClientRegistrationDto.class))).willReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/clients/" + newClientId))
                    .andExpect(jsonPath("$.id").value(newClientId.toString()))
                    .andExpect(jsonPath("$.username").value("newclient"));

            verify(clientService).register(any(ClientRegistrationDto.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Validation Error: Should return 400 when username is invalid")
        void shouldReturn400_WhenUsernameIsInvalid(String invalidUsername) throws Exception {
            // Arrange
            ClientRegistrationDto invalidDto = new ClientRegistrationDto(invalidUsername, "client@example.com", "password123", "John", "Doe", "123456");

            // Act & Assert
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
            ClientRegistrationDto invalidDto = new ClientRegistrationDto("newclient", "client@example.com", invalidPassword, "John", "Doe", "123456");

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.password").exists());

            verifyNoInteractions(clientService);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "plainaddress", "@no-local-part.com"})
        @DisplayName("Validation Error: Should return 400 when email is invalid")
        void shouldReturn400_WhenEmailIsInvalid(String invalidEmail) throws Exception {
            // Arrange
            ClientRegistrationDto invalidDto = new ClientRegistrationDto("newclient", invalidEmail, "password123", "John", "Doe", "123456");

            // Act & Assert
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

            // Act & Assert
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

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.EMAIL_DUPLICATE.getCode()));

            verify(clientService).register(any(ClientRegistrationDto.class));
        }
    }
}
