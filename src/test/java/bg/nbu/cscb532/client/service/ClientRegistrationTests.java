package bg.nbu.cscb532.client.service;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.client.dto.ClientQuickRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.TokenType;
import bg.nbu.cscb532.user.User;
import bg.nbu.cscb532.user.VerificationToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService: Registration Logic Tests")
public class ClientRegistrationTests extends AbstractClientUnitTestBase {

    @Nested
    @DisplayName("register(ClientRegistrationDto) Tests")
    class RegisterTests {

        @Test
        @DisplayName("Happy Path: Should successfully register a new client and hash password")
        void shouldRegisterSuccessfully() {

            // Arrange
            ClientRegistrationDto dto = createValidRegistrationDto();
            Client savedClient = createMockSavedClient();

            given(userRepository.findByUsername(dto.username())).willReturn(Optional.empty());
            given(userRepository.findByEmail(dto.email())).willReturn(Optional.empty());
            given(clientRepository.findByPhoneNumber(dto.phoneNumber())).willReturn(Optional.empty());
            given(passwordEncoder.encode(dto.password())).willReturn("hashedPassword123");
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);


            // Act
            ClientViewDto result = clientService.register(dto);

            // Assert: API Result
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(savedClient.getId());
            assertThat(result.username()).isEqualTo(dto.username());

            // Assert: Client State Verification
            verify(clientRepository).save(clientCaptor.capture());
            Client capturedClient = clientCaptor.getValue();
            assertThat(capturedClient.getPassword()).isEqualTo("hashedPassword123");
            assertThat(capturedClient.getApplicationRole()).isEqualTo(ApplicationRole.CLIENT);
            assertThat(capturedClient.isActive()).isTrue();
            assertThat(capturedClient.isEmailVerified()).isFalse();

            // Assert: Security Token Verification
            verify(verificationTokenRepository).save(tokenCaptor.capture());
            VerificationToken capturedToken = tokenCaptor.getValue();
            assertThat(capturedToken.getTokenType()).isEqualTo(TokenType.EMAIL_VERIFICATION);
            assertThat(capturedToken.getUser()).isEqualTo(savedClient);
            assertThat(capturedToken.getTokenHash()).hasSize(64); // SHA-256 Hex length
            assertThat(capturedToken.getExpiryDate()).isAfter(Instant.now());

            // Assert: Infrastructure Side Effects
            verify(emailService).sendVerificationEmail(eq(dto.email()), anyString());

            verify(userRepository).findByUsername(dto.username());
            verify(userRepository).findByEmail(dto.email());
        }

        @Test
        @DisplayName("Happy Path: Should merge with offline account when registering with existing phone")
        void shouldMergeWithOfflineAccountWhenRegisteringWithExistingPhone() {
            // Arrange
            ClientRegistrationDto onlineDto = createValidRegistrationDto();

            Client offlineGrandma = new Client();
            offlineGrandma.setId(UUID.randomUUID());
            offlineGrandma.setPhoneNumber(onlineDto.phoneNumber());
            offlineGrandma.setUsername("walkin_123");
            offlineGrandma.setEmail(null);
            offlineGrandma.setFirstName("Grandma");

            given(userRepository.findByUsername(onlineDto.username())).willReturn(Optional.empty());
            given(userRepository.findByEmail(onlineDto.email())).willReturn(Optional.empty());
            given(clientRepository.findByPhoneNumber(onlineDto.phoneNumber())).willReturn(Optional.of(offlineGrandma));
            given(passwordEncoder.encode(onlineDto.password())).willReturn("newHashedPassword");
            given(clientRepository.save(any(Client.class))).willReturn(offlineGrandma);

            // Act
            clientService.register(onlineDto);

            // Assert
            verify(clientRepository).save(clientCaptor.capture());
            Client mergedClient = clientCaptor.getValue();

            assertThat(mergedClient.getId()).isEqualTo(offlineGrandma.getId());
            assertThat(mergedClient.getUsername()).isEqualTo(onlineDto.username());
            assertThat(mergedClient.getEmail()).isEqualTo(onlineDto.email());
            assertThat(mergedClient.getPassword()).isEqualTo("newHashedPassword");
            assertThat(mergedClient.getFirstName()).isEqualTo(onlineDto.firstName());
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {

            // Arrange
            ClientRegistrationDto dto = createValidRegistrationDto();
            User existingUser = new Client();
            given(userRepository.findByUsername(dto.username())).willReturn(Optional.of(existingUser));

            // Act and Assert
            assertThatThrownBy(() -> clientService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USERNAME_DUPLICATE);

            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(clientRepository);
            verifyNoInteractions(verificationTokenRepository);
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when email already exists")
        void shouldThrowExceptionWhenEmailExists() {

            // Arrange
            ClientRegistrationDto dto = createValidRegistrationDto();
            given(userRepository.findByUsername(dto.username())).willReturn(Optional.empty());

            User existingUser = new Client();
            given(userRepository.findByEmail(dto.email())).willReturn(Optional.of(existingUser));

            // Act and Assert
            assertThatThrownBy(() -> clientService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATE);

            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(clientRepository);
            verifyNoInteractions(verificationTokenRepository);
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Edge Case: Should normalize inputs before checking duplicates and saving")
        void shouldNormalizeInputsBeforeProcessing() {

            // Arrange
            ClientRegistrationDto dirtyDto = ClientRegistrationDto.builder()
                    .username("  spacedUser  ")
                    .email("  UPPER@Example.com  ")
                    .password("rawPassword123")
                    .firstName(" John ")
                    .lastName(" Doe ")
                    .phoneNumber(" 0888123456 ")
                    .build();

            Client savedClient = new Client();
            savedClient.setId(UUID.randomUUID());
            savedClient.setEmail("upper@example.com");
            savedClient.setUsername("spacedUser");

            given(userRepository.findByUsername("spacedUser")).willReturn(Optional.empty());
            given(userRepository.findByEmail("upper@example.com")).willReturn(Optional.empty());
            given(clientRepository.findByPhoneNumber("0888123456")).willReturn(Optional.empty());
            given(passwordEncoder.encode(anyString())).willReturn("hash");
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);

            // Act
            clientService.register(dirtyDto);

            // Assert
            verify(userRepository).findByUsername("spacedUser");
            verify(userRepository).findByEmail("upper@example.com");
            verify(emailService).sendVerificationEmail(eq("upper@example.com"), anyString());

            verify(clientRepository).save(clientCaptor.capture());
            Client capturedClient = clientCaptor.getValue();
            assertThat(capturedClient.getUsername()).isEqualTo("spacedUser");
            assertThat(capturedClient.getEmail()).isEqualTo("upper@example.com");
            assertThat(capturedClient.getFirstName()).isEqualTo("John");
        }
    }

    @Nested
    @DisplayName("quickRegister(ClientQuickRegistrationDto) Tests")
    class QuickRegisterTests {

        @Test
        @DisplayName("Happy Path: Should successfully create client without email and auto-generate credentials")
        void shouldQuickRegisterSuccessfullyWithoutEmail() {
            // Arrange
            ClientQuickRegistrationDto dto = ClientQuickRegistrationDto.builder()
                    .firstName("Grandma")
                    .lastName("Smith")
                    .phoneNumber("0888999111")
                    .build();

            Client savedClient = new Client();
            savedClient.setId(UUID.randomUUID());
            savedClient.setPhoneNumber(dto.phoneNumber());

            given(clientRepository.findByPhoneNumber(dto.phoneNumber())).willReturn(Optional.empty());

            given(passwordEncoder.encode(anyString())).willReturn("hashed-auto-password");
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);

            // Act
            ClientViewDto result = clientService.quickRegister(dto);

            // Assert
            assertThat(result).isNotNull();

            verify(clientRepository).save(clientCaptor.capture());
            Client capturedClient = clientCaptor.getValue();

            assertThat(capturedClient.getFirstName()).isEqualTo("Grandma");
            assertThat(capturedClient.getPhoneNumber()).isEqualTo("0888999111");
            assertThat(capturedClient.getEmail()).isNull();
            assertThat(capturedClient.getUsername()).startsWith("walkin_");
            assertThat(capturedClient.getPassword()).isEqualTo("hashed-auto-password");
            assertThat(capturedClient.getApplicationRole()).isEqualTo(ApplicationRole.CLIENT);

            verifyNoInteractions(verificationTokenRepository, emailService);
        }

        @Test
        @DisplayName("Happy Path: Should create client and trigger password reset if email is provided")
        void shouldQuickRegisterAndTriggerResetWithEmail() {
            // Arrange
            ClientQuickRegistrationDto dto = ClientQuickRegistrationDto.builder()
                    .firstName("Busy")
                    .lastName("Mom")
                    .phoneNumber("0888555444")
                    .email("mom@example.com")
                    .build();

            Client savedClient = new Client();
            savedClient.setId(UUID.randomUUID());
            savedClient.setEmail(dto.email());
            savedClient.setApplicationRole(ApplicationRole.CLIENT);

            given(clientRepository.findByPhoneNumber(dto.phoneNumber())).willReturn(Optional.empty());

            given(userRepository.findByEmail(dto.email())).willReturn(Optional.empty());
            given(passwordEncoder.encode(anyString())).willReturn("hash");
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);

            reset(userRepository);
            when(userRepository.findByEmail(dto.email()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedClient));

            // Act
            clientService.quickRegister(dto);

            // Assert
            verify(clientRepository).save(clientCaptor.capture());
            assertThat(clientCaptor.getValue().getEmail()).isEqualTo("mom@example.com");

            verify(verificationTokenRepository).save(any(VerificationToken.class));
            verify(emailService).sendPasswordResetEmail(eq("mom@example.com"), anyString());
        }

        @Test
        @DisplayName("Error Case: Should throw PHONE_DUPLICATE if phone number exists")
        void shouldThrowIfPhoneDuplicate() {
            // Arrange
            ClientQuickRegistrationDto dto = ClientQuickRegistrationDto.builder()
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("0888111222")
                    .build();

            given(clientRepository.findByPhoneNumber(dto.phoneNumber())).willReturn(Optional.of(new Client()));

            // Act and Assert
            assertThatThrownBy(() -> clientService.quickRegister(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PHONE_DUPLICATE);

            verifyNoInteractions(passwordEncoder, userRepository);
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should throw EMAIL_DUPLICATE if provided email exists")
        void shouldThrowIfEmailDuplicate() {
            // Arrange
            ClientQuickRegistrationDto dto = ClientQuickRegistrationDto.builder()
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("0888111222")
                    .email("existing@test.com")
                    .build();

            given(clientRepository.findByPhoneNumber(dto.phoneNumber())).willReturn(Optional.empty());
            given(userRepository.findByEmail(dto.email())).willReturn(Optional.of(new Client()));

            // Act and Assert
            assertThatThrownBy(() -> clientService.quickRegister(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATE);

            verify(clientRepository, never()).save(any());
        }
    }
}
