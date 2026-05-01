package bg.nbu.cscb532.user;

import bg.nbu.cscb532.client.ClientService;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.user.dto.ForgotPasswordRequestDto;
import bg.nbu.cscb532.user.dto.LoginRequestDto;
import bg.nbu.cscb532.user.dto.ResetPasswordRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String BASE_URL = "/api/auth";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ClientService clientService;

    @Nested
    @DisplayName("POST /login")
    class LoginTests {

        @Test
        @DisplayName("Happy Path: Should return 200 OK with JWT when credentials are valid")
        void shouldReturn200_WhenCredentialsAreValid() throws Exception {

            // Arrange
            LoginRequestDto requestDto = new LoginRequestDto("testuser", "password");
            UserDetails userDetails = new User("testuser", "password", Collections.emptyList());
            String fakeToken = "fake.jwt.token";

            given(userDetailsService.loadUserByUsername("testuser")).willReturn(userDetails);
            given(jwtService.generateToken(userDetails)).willReturn(fakeToken);

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(fakeToken));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userDetailsService).loadUserByUsername("testuser");
            verify(jwtService).generateToken(userDetails);
        }

        @Test
        @DisplayName("Validation Error: Should return 400 Bad Request when username is blank")
        void shouldReturn400_WhenUsernameIsBlank() throws Exception {

            // Arrange
            LoginRequestDto requestDto = new LoginRequestDto(" ", "password");

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"));

            verifyNoInteractions(authenticationManager, userDetailsService, jwtService);
        }

        @Test
        @DisplayName("Authentication Failure: Should return 401 Unauthorized when credentials are bad")
        void shouldReturn401_WhenCredentialsAreBad() throws Exception {

            // Arrange
            LoginRequestDto requestDto = new LoginRequestDto("testuser", "wrong-password");
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willThrow(new BadCredentialsException("Invalid credentials"));

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid credentials"));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verifyNoInteractions(userDetailsService, jwtService);
        }
    }

    @Nested
    @DisplayName("POST /forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Happy Path: Should return 200 OK when processing a valid forgot password request")
        void shouldReturn200ForValidRequest() throws Exception {
            // Arrange
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("client@example.com");

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(clientService).requestPasswordReset(any(ForgotPasswordRequestDto.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "not-an-email"})
        @DisplayName("Validation Error: Should return 400 Bad Request for invalid email format")
        void shouldReturn400ForInvalidEmail(String invalidEmail) throws Exception {
            // Arrange
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto(invalidEmail);

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists());

            verifyNoInteractions(clientService);
        }
    }

    @Nested
    @DisplayName("POST /reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("Happy Path: Should return 200 OK when password reset is successful")
        void shouldReturn200ForValidReset() throws Exception {
            // Arrange
            ResetPasswordRequestDto request = new ResetPasswordRequestDto(UUID.randomUUID().toString(), "newStrongPassword!");

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(clientService).resetPassword(any(ResetPasswordRequestDto.class));
        }

        @Test
        @DisplayName("Validation Error: Should return 400 Bad Request for missing token")
        void shouldReturn400ForMissingToken() throws Exception {
            // Arrange
            ResetPasswordRequestDto request = new ResetPasswordRequestDto("", "newStrongPassword!");

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.token").exists());

            verifyNoInteractions(clientService);
        }

        @Test
        @DisplayName("Security Error: Should return 400 Bad Request (INVALID_TOKEN) if service rejects the token")
        void shouldReturn400IfTokenInvalid() throws Exception {
            // Arrange
            ResetPasswordRequestDto request = new ResetPasswordRequestDto("invalid-token", "newStrongPassword!");

            willThrow(new BusinessException(ErrorCode.INVALID_TOKEN))
                    .given(clientService).resetPassword(any(ResetPasswordRequestDto.class));

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_TOKEN.getCode()));
        }
    }
}
