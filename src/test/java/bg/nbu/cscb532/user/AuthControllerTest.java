package bg.nbu.cscb532.user;

import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.user.dto.LoginRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

            // Act & Assert
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

            // Act & Assert
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

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid credentials"));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verifyNoInteractions(userDetailsService, jwtService);
        }
    }
}
