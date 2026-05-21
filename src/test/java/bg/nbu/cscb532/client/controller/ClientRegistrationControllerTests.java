package bg.nbu.cscb532.client.controller;

import bg.nbu.cscb532.client.dto.ClientQuickRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientUpdateDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Client Controller: Registration and Profile Tests")
public class ClientRegistrationControllerTests extends AbstractClientControllerTestBase {

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
    @DisplayName("GET /api/clients/me")
    class GetMyProfileTests {

        @Test
        @DisplayName("Happy Path: Client should successfully retrieve their own profile")
        void clientShouldRetrieveOwnProfileSuccessfully() throws Exception {
            UUID clientId = UUID.randomUUID();
            CustomUserDetails clientUser = createMockAuthUser(clientId, ApplicationRole.CLIENT);
            ClientViewDto clientDto = createValidViewDto(clientId);

            given(clientService.getClientById(clientId)).willReturn(clientDto);

            mockMvc.perform(get(BASE_URL + "/me").with(user(clientUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(clientId.toString()))
                    .andExpect(jsonPath("$.firstName").value("John"));

            verify(clientService).getClientById(clientId);
        }

        @Test
        @DisplayName("Error Case: Should return 404 Not Found if client profile does not exist")
        void shouldReturn404WhenClientNotFound() throws Exception {
            UUID clientId = UUID.randomUUID();
            CustomUserDetails clientUser = createMockAuthUser(clientId, ApplicationRole.CLIENT);

            given(clientService.getClientById(clientId)).willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

            mockMvc.perform(get(BASE_URL + "/me").with(user(clientUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()));

            verify(clientService).getClientById(clientId);
        }
    }

    @Nested
    @DisplayName("PUT /api/clients/me")
    class UpdateMyProfileTests {

        @Test
        @DisplayName("Happy Path: Client should successfully update their own profile")
        void clientShouldUpdateOwnProfileSuccessfully() throws Exception {
            UUID clientId = UUID.randomUUID();
            CustomUserDetails clientUser = createMockAuthUser(clientId, ApplicationRole.CLIENT);
            ClientUpdateDto dto = ClientUpdateDto.builder()
                    .firstName("Jane")
                    .lastName("Doe")
                    .phoneNumber("0888999888")
                    .build();
            ClientViewDto responseDto = new ClientViewDto(clientId, "newClient", "client@example.com", "Jane", "Doe", "0888999888", true, false);

            given(clientService.updateClientProfile(eq(clientId), any(ClientUpdateDto.class))).willReturn(responseDto);

            mockMvc.perform(put(BASE_URL + "/me")
                            .with(user(clientUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.phoneNumber").value("0888999888"));

            verify(clientService).updateClientProfile(eq(clientId), any(ClientUpdateDto.class));
        }

        @Test
        @DisplayName("Validation Error: Should return 400 when missing fields")
        void shouldReturn400WhenMissingFields() throws Exception {
            UUID clientId = UUID.randomUUID();
            CustomUserDetails clientUser = createMockAuthUser(clientId, ApplicationRole.CLIENT);
            // Missing phone number
            ClientUpdateDto dto = ClientUpdateDto.builder().firstName("Jane").lastName("Doe").build();

            mockMvc.perform(put(BASE_URL + "/me")
                            .with(user(clientUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.phoneNumber").exists());

            verifyNoInteractions(clientService);
        }

        @Test
        @DisplayName("Business Conflict: Should return 409 Conflict when phone number is duplicate")
        void shouldReturn409WhenPhoneIsDuplicate() throws Exception {
            UUID clientId = UUID.randomUUID();
            CustomUserDetails clientUser = createMockAuthUser(clientId, ApplicationRole.CLIENT);
            ClientUpdateDto dto = ClientUpdateDto.builder().firstName("Jane").lastName("Doe").phoneNumber("0888999888").build();

            given(clientService.updateClientProfile(eq(clientId), any(ClientUpdateDto.class)))
                    .willThrow(new BusinessException(ErrorCode.PHONE_DUPLICATE));

            mockMvc.perform(put(BASE_URL + "/me")
                            .with(user(clientUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.PHONE_DUPLICATE.getCode()));

            verify(clientService).updateClientProfile(eq(clientId), any(ClientUpdateDto.class));
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
                    .andExpect(jsonPath("$.firstName").value("John"));

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

        @ParameterizedTest
        @ValueSource(strings = {"invalid-email", "plainAddress", "@no-local-part.com"})
        @DisplayName("Validation Error: Should return 400 when optional email is malformed")
        void shouldReturn400_WhenOptionalEmailIsMalformed(String invalidEmail) throws Exception {
            // Arrange
            CustomUserDetails clerkUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            ClientQuickRegistrationDto invalidDto = ClientQuickRegistrationDto.builder()
                    .firstName("Guest")
                    .lastName("User")
                    .phoneNumber("0888123456")
                    .email(invalidEmail)
                    .build();

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/quick-register")
                            .with(user(clerkUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists());

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

        @Test
        @DisplayName("Business Conflict: Should return 409 Conflict when optional email is duplicate")
        void shouldReturn409_WhenEmailIsDuplicate() throws Exception {
            // Arrange
            CustomUserDetails clerkUser = createMockAuthUser(UUID.randomUUID(), ApplicationRole.CLERK);
            ClientQuickRegistrationDto requestDto = ClientQuickRegistrationDto.builder()
                    .firstName("Guest")
                    .lastName("User")
                    .phoneNumber("0888123456")
                    .email("existing@test.com")
                    .build();

            given(clientService.quickRegister(any(ClientQuickRegistrationDto.class)))
                    .willThrow(new BusinessException(ErrorCode.EMAIL_DUPLICATE));

            // Act and Assert
            mockMvc.perform(post(BASE_URL + "/quick-register")
                            .with(user(clerkUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.EMAIL_DUPLICATE.getCode()));

            verify(clientService).quickRegister(any(ClientQuickRegistrationDto.class));
        }
    }
}
